package javking.exceptions;

public class RateLimitException extends RuntimeException {
    private final boolean isTimeout;
    private final long timeoutNanos;

    public RateLimitException(boolean isTimeout) {
        super();
        this.isTimeout = isTimeout;
        this.timeoutNanos = 0L;
    }

    public RateLimitException(boolean isTimeout, long timeoutNanos) {
        super();
        this.isTimeout = isTimeout;
        this.timeoutNanos = timeoutNanos;
    }

    public RateLimitException(boolean isTimeout, String message) {
        super(message);
        this.isTimeout = isTimeout;
        this.timeoutNanos = 0L;
    }

    public RateLimitException(boolean isTimeout, String message, Throwable cause) {
        super(message, cause);
        this.isTimeout = isTimeout;
        this.timeoutNanos = 0L;
    }

    public RateLimitException(boolean isTimeout, Throwable cause) {
        super(cause);
        this.isTimeout = isTimeout;
        this.timeoutNanos = 0L;
    }

    /**
     * @return true if the exception was caused by the client having previously hit the rate limit and trying to submit
     * a task again while the time out is still active
     */
    public boolean isTimeout() {
        return isTimeout;
    }

    public long getTimeoutNanos() {
        return timeoutNanos;
    }
}
