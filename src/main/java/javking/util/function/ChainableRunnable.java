package javking.util.function;

public abstract class ChainableRunnable implements CheckedRunnable {
    public Runnable then(Runnable next) {
        return () -> {
            run();
            next.run();
        };
    }
}
