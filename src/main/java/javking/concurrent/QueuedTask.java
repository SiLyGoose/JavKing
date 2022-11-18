package javking.concurrent;

import javking.exceptions.CommandRuntimeException;
import javking.exceptions.handlers.CommandExceptionHandler;

public class QueuedTask implements Runnable {
    private final Runnable task;
    private final ThreadExecutionQueue queue;

    private volatile boolean completed;
    private volatile boolean terminated;

    private String name;
    private Thread thread;

    public QueuedTask(ThreadExecutionQueue queue, Runnable task) {
        this.task = task;
        this.queue = queue;
    }

    @Override
    public void run() {
        if (terminated) {
            return;
        }

        thread = Thread.currentThread();
        String oldName = null;
        if (name != null) {
            oldName = thread.getName();
            thread.setName(name);
        }

        try {
            if (ignoreQueue()) {
                task.run();
            } else {
                runWithSlot();
            }
        } catch (Throwable e) {
            CommandRuntimeException.throwException(e);
        } finally {
            completed = true;

            queue.removeFromPool(this);

            if (oldName != null) {
                thread.setName(oldName);
            }
        }
    }

    public boolean isComplete() {
        return completed;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public boolean isDone() {
        return isComplete() || isTerminated();
    }

    public void terminate() {
        terminated = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Thread getThread() {
        return thread;
    }

    protected boolean ignoreQueue() {
        return false;
    }

    protected CommandExceptionHandler setUncaughtExceptionHandler() {
        return null;
    }

    private void runWithSlot() {
        Object slot;
        try {
            slot = queue.takeSlot();
        } catch (InterruptedException e) {
            return;
        }

        try {
            task.run();
        } finally {
            queue.freeSlot(slot);
        }
    }
}
