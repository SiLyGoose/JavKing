package javking.models.meta;

import javking.concurrent.CommandExecutionTask;
import javking.discord.MessageService;
import javking.models.command.Command;
import javking.models.command.CommandContext;
import javking.templates.EmbedTemplate;
import javking.util.Spotify.SpotifyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
//import javking.util.Spotify.SpotifyService;

public abstract class AbstractCommand extends Command {
    private CommandContext commandContext;
    //    private CommandManager commandManager;

    private String[] commandBody;

    private String identifier;
    private String description;

    //    contributing admins
    private boolean requiresExecAdmin = false;
    //    guild admins
    private boolean requiresAdmin = false;

    private boolean requiresInput = false;
    private boolean requiresVoice = false;
    private boolean requiresVoiceChannel = false;
    private boolean isFailed = false;

    private CommandExecutionTask task;

    private MessageService messageService;

    public AbstractCommand(/*CommandContext commandContext, CommandManager commandManager, String commandBody, boolean requiresInput, String identifier, String description*/) {
//        this.commandContext = commandContext;
//        this.commandManager = commandManager;
//        this.commandBody = commandBody;
//        this.identifier = identifier;
//        this.description = description;
//        this.requiresInput = requiresInput;
//        this.messageService = JavKing.get().getMessageService();
    }

    public void setContext(CommandContext context) {
        this.commandContext = context;
    }

    @Override
    public CommandContext getContext() {
        return commandContext;
    }

    @Override
    public boolean ignoreQueue() {
        return false;
    }

    @Override
    public void setTask(CommandExecutionTask task) {
        this.task = task;
    }

    @Override
    public CommandExecutionTask getTask() {
        return task;
    }

    @Override
    public Thread getThread() {
        CommandExecutionTask task = getTask();
        if (task != null) {
            return task.getThread();
        }

        return null;
    }

    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public void setCommandBody(String[] commandBody) {
        this.commandBody = commandBody;
    }

    @Override
    public String[] getCommandBody() {
        return commandBody;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setRequiresExecAdmin(boolean requiresExecAdmin) {
        this.requiresExecAdmin = requiresExecAdmin;
    }

    public boolean requiresExecAdmin() {
        return requiresExecAdmin;
    }

    public void setRequiresAdmin(boolean requiresAdmin) {
        this.requiresAdmin = requiresAdmin;
    }

    public boolean requiresAdmin() {
        return requiresAdmin;
    }

    @Override
    public void setRequiresInput(boolean requiresInput) {
        this.requiresInput = requiresInput;
    }

    public boolean requiresInput() {
        return requiresInput;
    }

    public void setRequiresVoice(boolean requiresVoice) {
        this.requiresVoice = requiresVoice;
    }

    //    requires user to be undeafened, but not unmuted
    public boolean requiresVoice() {
        return requiresVoice;
    }

    public void setRequiresVoiceChannel(boolean requiresVoiceChannel) {
        this.requiresVoiceChannel = requiresVoiceChannel;
    }

    //    requires user to be in voice channel
    public boolean requiresVoiceChannel() {
        return requiresVoiceChannel;
    }

    //    public SpotifyService getSpotifyService() {
//        return commandContext.getSpotifyService();
//    }

    public void setFailed(boolean isFailed) {
        this.isFailed = isFailed;
    }

    @Override
    public boolean isFailed() {
        return isFailed;
    }

    public SpotifyService getSpotifyService() {
        return commandContext.getSpotifyService();
    }

    @Override
    public MessageEmbed buildHelpEmbed() {
        EmbedBuilder embedBuilder = new EmbedTemplate().clearEmbed();
        String simpleIdentifier = getClass().getSimpleName();
        StringBuilder stringBuilder = new StringBuilder("```ini\n[Command] ").append(simpleIdentifier).append("\n");

        if (getAlias().length > 0) {
            stringBuilder.append("[Aliases] ");
            for (String alias : getAlias()) {
                stringBuilder.append(alias);
                if (getAlias().length > 1) stringBuilder.append(", ");
            }
            stringBuilder.append("\n");
        }

        if (getUsage().length > 0) {
            stringBuilder.append("[Usage] ")
                    .append(getContext().getGuildContext().getPrefix())
                    .append(simpleIdentifier)
                    .append(" ")
                    .append(getUsage()[0])
                    .append("\n");
        }

        if (getDescription() != null) {
            stringBuilder.append("[Description] ").append(getDescription()).append("\n");
        }

        SelfUser selfMember = getContext().getJda().getSelfUser();

        embedBuilder.setAuthor(String.format("%s Help", selfMember.getName()), null,
                        selfMember.getEffectiveAvatarUrl())
                .setDescription(stringBuilder.append("```").toString());

        return embedBuilder.build();
    }
}
