package javking.concurrent;

import javking.exceptions.handlers.LoggingUncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class LoggingThreadFactory implements ThreadFactory {
    private final String poolName;
    private final AtomicLong threadNumber = new AtomicLong(1);

    public LoggingThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(poolName + "-thread-" + threadNumber.getAndIncrement());
        thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
        return thread;
    }
}