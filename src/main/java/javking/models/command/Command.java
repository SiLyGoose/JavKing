package javking.models.command;

import javking.concurrent.CommandExecutionTask;
import net.dv8tion.jda.api.entities.MessageEmbed;

public abstract class Command {
    private CommandContext context;
    private boolean isFailed;

    public Command() {

    }

    public abstract CommandContext getContext();

    //    command name
    public abstract String getIdentifier();
    //    user input
    public abstract String[] getCommandBody();
    public abstract String getDescription();
    public abstract String[] getAlias();
    public abstract String[] getUsage();

    //    ignores thread queue and calls immediately
    public abstract boolean ignoreQueue();

//    whether command needs input
    public abstract void setRequiresInput(boolean requiresInput);

    public abstract void execute(CommandContext context) throws Exception;
    public abstract void onFailed();
    public abstract boolean isFailed();
    public abstract void onSuccess();

    //
    public abstract CommandExecutionTask getTask();
    public abstract void setTask(CommandExecutionTask task);
    public abstract Thread getThread();

    public abstract MessageEmbed buildHelpEmbed();
}
