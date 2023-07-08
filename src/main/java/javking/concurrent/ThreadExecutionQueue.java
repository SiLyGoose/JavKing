package javking.concurrent;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import javking.exceptions.RateLimitException;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadExecutionQueue {
    private final AtomicInteger threadNumber;
    private final BlockingQueue<QueuedTask> queue;
    private final BlockingQueue<Object> slotStack;
    private final Vector<QueuedTask> currentPool;
    private final ThreadPoolExecutor threadPool;

    private final String name;
    private final int queueSize;

    @Nullable
    private final RateLimiterConfig rateLimiterConfig;
    @Nullable
    private final RateLimiter rateLimiter;
    //    timeout duration when rate limit is hit
    @Nullable
    private final Duration violationTimeout;

//    useful for taking bot down for maintenance
//    secludes bot usage
    private volatile boolean closed;
    private volatile long timeoutNanosTimeStamp;

    public ThreadExecutionQueue(String name, int size, ThreadPoolExecutor threadPool) {
        this(name, size, 0, threadPool, null, 0, null, null);
    }

    public ThreadExecutionQueue(
            String name,
            int concurrentSize,
            int queueSize,
            ThreadPoolExecutor threadPool,
            @Nullable String rateLimiterIdentifier,
            int limitForPeriod,
            @Nullable Duration period,
            @Nullable Duration violationTimeout
    ) {
        threadNumber = new AtomicInteger(1);
        slotStack = new LinkedBlockingQueue<>(concurrentSize);
        this.name = name;
        this.threadPool = threadPool;
        currentPool = new Vector<>(concurrentSize);

        for (int i = 0; i < concurrentSize; i++) {
            slotStack.add(new Object());
        }

        this.queueSize = queueSize;
        if (queueSize == 0) {
            queue = new LinkedBlockingQueue<>();
        } else {
            queue = new LinkedBlockingQueue<>(queueSize);
        }

        if (rateLimiterIdentifier != null && limitForPeriod > 0 && period != null && violationTimeout != null) {
            this.rateLimiterConfig = RateLimiterConfig
                    .custom()
                    .limitForPeriod(limitForPeriod)
                    .limitRefreshPeriod(period)
                    .timeoutDuration(Duration.ofSeconds(1))
                    .build();

            this.rateLimiter = RateLimiterRegistry.of(rateLimiterConfig).rateLimiter(rateLimiterIdentifier, this.rateLimiterConfig);
            this.violationTimeout = violationTimeout;
        } else if (rateLimiterIdentifier != null || limitForPeriod > 0 || period != null || violationTimeout != null) {
            throw new IllegalArgumentException("Incomplete RateLimiter configuration");
        } else {
            this.rateLimiterConfig = null;
            this.rateLimiter = null;
            this.violationTimeout = null;
        }
    }

    /**
     * @param task {@link QueuedTask} to queue
     * @return true if currentPool has space, false if queued
     * @throws RateLimitException if rate limit is exceeded
     */
    public synchronized boolean add(QueuedTask task) throws RateLimitException {
        if (!closed) {
            if (timeoutNanosTimeStamp > 0) {
                long currentNanoTime = System.nanoTime();
                if (currentNanoTime < timeoutNanosTimeStamp) {
                    timeoutNanosTimeStamp = System.nanoTime() + violationTimeout.toNanos();
                    throw new RateLimitException(true);
                } else timeoutNanosTimeStamp = 0;
            }

            if (rateLimiter != null && !rateLimiter.acquirePermission()) {
                // violationTimeout is not null if RateLimiter is not null
                //noinspection ConstantConditions
                timeoutNanosTimeStamp = System.nanoTime() + violationTimeout.toNanos();
                // config is not null if RateLimiter is not null
                //noinspection ConstantConditions
                throw new RateLimitException(
                        false,
                        String.format(
                                "Hit rate limit for submitting tasks of %d per %d seconds. You may not enter any commands for %d seconds. " +
                                        "For each attempt to submit additional tasks during the timeout, the timeout is reset.",
                                rateLimiterConfig.getLimitForPeriod(),
                                rateLimiterConfig.getLimitRefreshPeriod().getSeconds(),
                                violationTimeout.toSeconds()
                        )
                );
            }

            task.setName(name + "-thread-" + threadNumber.getAndIncrement());

            if (task.ignoreQueue()) {
                currentPool.add(task);
                threadPool.execute(task);
                return true;
            } else {
                if (!queue.offer(task)) {
                    if (violationTimeout != null) {
                        timeoutNanosTimeStamp = System.nanoTime() + violationTimeout.toNanos();
                    }
                    throw new RateLimitException(
                            false,
                            String.format(
                                    "Execution queue of size %d is full. You can not submit commands until one is done.%s",
                                    queueSize,
                                    violationTimeout != null
                                            ? String.format(" Additionally, a %d second timeout has been raised. Task submissions during the timeout reset the timeout.", violationTimeout.toSeconds())
                                            : ""
                            )
                    );
                }

                if (!slotStack.isEmpty()) {
                    runNext();
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalStateException("This " + getClass().getSimpleName() + " has been closed");
        }
    }

    public void close() {
        closed = true;
    }

    //    clears current threads
    public synchronized void abortAll() {
        queue.clear();
        currentPool.forEach(QueuedTask::terminate);
        currentPool.clear();
    }

    /**
     * @return true if queue has no running or queued threads
     */
    public boolean isIdle() {
        return currentPool.isEmpty() && queue.isEmpty();
    }

    Object takeSlot() throws InterruptedException {
        return slotStack.take();
    }

    void removeFromPool(QueuedTask queuedTask) {
        currentPool.remove(queuedTask);
    }

    synchronized void freeSlot(Object slot) {
        slotStack.add(slot);
        if (!closed) {
            runNext();
        }
    }

    private void runNext() {
        QueuedTask poll = queue.poll();
        if (poll != null) {
            currentPool.add(poll);
            threadPool.execute(poll);
        }
    }
}
