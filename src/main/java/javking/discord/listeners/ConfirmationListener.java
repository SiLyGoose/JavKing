package javking.discord.listeners;

import javking.JavKing;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.discord.GuildManager;
import javking.discord.MessageService;
import javking.exceptions.handlers.LoggingExceptionHandler;
import javking.models.guild.user.UserContext;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmationListener extends ListenerAdapter {
    private final CommandExecutionQueueManager executionQueueManager;
    private final ExecutorService commandConceptionPool;
    private final GuildManager guildManager;
    private final MessageService messageService;
    private final UserContext userContext;
    private final Message message;

    public ConfirmationListener(CommandExecutionQueueManager executionQueueManager,
                                GuildManager guildManager,
                                MessageService messageService,
                                UserContext userContext,
                                Message message) {
        this.executionQueueManager = executionQueueManager;
        this.guildManager = guildManager;
        this.messageService = messageService;
        this.userContext = userContext;
        this.message = message;

        commandConceptionPool = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            return thread;
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot() && event.getAuthor().getId().equals(userContext.getId())) {
            commandConceptionPool.execute(() -> {
                Guild guild = event.getGuild();

                String resultMessage;
                String content = event.getMessage().getContentRaw().toLowerCase();

                if (content.equals("yes") || content.equals("y")) {
                    JavKing.get().getGuildManager().getContextForGuild(guild).getGuildSpecification().resetGuildProperties();
                    resultMessage = Templates.command.blue_check_mark.formatFull(String.format("%s settings have been restored to default.", event.getJDA().getSelfUser().getName()));
                } else {
                    resultMessage = Templates.command.x_mark.formatFull("Reset aborted");
                }

                messageService.sendBold(resultMessage, event.getChannel());

                event.getJDA().removeEventListener(this);
                commandConceptionPool.shutdown();
            });
        }
    }

}
