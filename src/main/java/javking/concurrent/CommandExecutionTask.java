package javking.concurrent;

import javking.exceptions.handlers.CommandExceptionHandler;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;

public class CommandExecutionTask extends QueuedTask {
    private final AbstractCommand command;

    public CommandExecutionTask(AbstractCommand command, ThreadExecutionQueue queue, Runnable runnable) {
        super(queue, runnable);
        this.command = command;
    }

    @Override
    public void run() {
        command.setTask(this);
        super.run();
    }

    @Override
    protected boolean ignoreQueue() {
        return command.ignoreQueue();
    }

//    protected ExceptionHandlerExecutor
    @Override
    public CommandExceptionHandler setUncaughtExceptionHandler() {
        return new CommandExceptionHandler(command);
    }

    public AbstractCommand getCommand() {
        return command;
    }

    public CommandContext getCommandContext() {
        return command.getContext();
    }
}
