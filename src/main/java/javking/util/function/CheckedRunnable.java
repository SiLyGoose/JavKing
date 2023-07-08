package javking.util.function;

import javking.exceptions.CommandRuntimeException;

public interface CheckedRunnable extends Runnable {

    @Override
    default void run() {
        try {
            doRun();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            CommandRuntimeException.throwException(e);
        }
    }

    void doRun() throws Exception;
}