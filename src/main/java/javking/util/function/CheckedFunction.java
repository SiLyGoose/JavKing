package javking.util.function;

import javking.exceptions.CommandRuntimeException;

import java.util.function.Function;

@FunctionalInterface
public interface CheckedFunction<P, R> extends Function<P, R> {

    @Override
    default R apply(P p) {
        try {
            return doApply(p);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandRuntimeException(e);
        }
    }

    R doApply(P p) throws Exception;

}