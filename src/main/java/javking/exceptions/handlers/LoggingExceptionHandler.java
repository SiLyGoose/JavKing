package javking.exceptions.handlers;

import javking.JavKing;

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        JavKing.LOGGER.error("Uncaught exception in thread " + t, e);
    }
}
