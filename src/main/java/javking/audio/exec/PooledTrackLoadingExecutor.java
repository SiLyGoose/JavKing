package javking.audio.exec;

import javking.concurrent.QueuedTask;
import javking.concurrent.ThreadExecutionQueue;
import javking.exceptions.CommandRuntimeException;
import javking.models.guild.GuildContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PooledTrackLoadingExecutor implements TrackLoadingExecutor {
    static final ThreadPoolExecutor GLOBAL_POOL = new ThreadPoolExecutor(3, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactory() {
                private final AtomicLong threadId = new AtomicLong(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("pooled-track-loading-thread-" + threadId.getAndIncrement());
//                    thread.setUncaughtExceptionHandler(new TrackLoadingUncaughtExceptionHandler(LoggerFactory.getLogger(PooledTrackLoadingExecutor.class)));
                    return thread;
                }
            });

    private final ThreadExecutionQueue queue;
    private final GuildContext guildContext;

    public PooledTrackLoadingExecutor(String guildId, GuildContext guildContext) {
        this.queue = new ThreadExecutionQueue("pooled-track-loading-guild" + guildId, 3, GLOBAL_POOL);
        this.guildContext = guildContext;
    }

    @Override
    public void execute(Runnable trackLoadingRunnable) {
        QueuedTask thread = new QueuedTask(queue, () -> {
            try {
                trackLoadingRunnable.run();
            } catch (Exception e) {
                CommandRuntimeException.throwException(e);
            }
        });
        queue.add(thread);
    }

    public void abortAll() {
        queue.abortAll();
    }

    public boolean isIdle() {
        return queue.isIdle();
    }
}
