package javking.exceptions;

public class NoResultsFoundException extends UserException {
    public NoResultsFoundException() {
        super();
    }

    public NoResultsFoundException(String message) {
        super(message);
    }

    public NoResultsFoundException(Throwable cause) {
        super(cause);
    }

    public NoResultsFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
