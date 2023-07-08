package javking.exceptions;

import javax.annotation.Nonnull;

public class CommandRuntimeException extends RuntimeException {
    public CommandRuntimeException(@Nonnull Throwable cause) {
        super(cause);
    }

    /**
     * Throws wrapped CommandRuntimeException
     *
     * @param e potentially wrapped exception to throw
     */
    public static void throwException(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else if (e instanceof Error) {
            throw (Error) e;
        } else {
            throw new CommandRuntimeException(e);
        }
    }
}
