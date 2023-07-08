package javking.discord.listeners;

import com.google.common.base.Strings;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.commands.manager.CommandManager;
import javking.concurrent.ThreadExecutionQueue;
import javking.discord.GuildManager;
import javking.discord.MessageService;
import javking.exceptions.handlers.LoggingExceptionHandler;
import javking.models.command.CommandContext;
import javking.models.guild.GuildContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandListener extends ListenerAdapter {
    private final CommandExecutionQueueManager executionQueueManager;
    private final CommandManager commandManager;
    private final ExecutorService commandConceptionPool;
    private final GuildManager guildManager;
    private final MessageService messageService;
    private final SpotifyApi.Builder spotifyApiBuilder;

    public CommandListener(CommandExecutionQueueManager executionQueueManager,
                           CommandManager commandManager,
                           GuildManager guildManager,
                           MessageService messageService,
                           SpotifyApi.Builder spotifyApiBuilder) {
        this.executionQueueManager = executionQueueManager;
        this.commandManager = commandManager;
        this.commandConceptionPool = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            return thread;
        });
        this.guildManager = guildManager;
        this.messageService = messageService;
        this.spotifyApiBuilder = spotifyApiBuilder;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            commandConceptionPool.execute(() -> {
                Guild guild = event.getGuild();
                Message message = event.getMessage();
                String msg = message.getContentDisplay();
                GuildContext guildContext = guildManager.getContextForGuild(guild);
                String prefix = guildContext.getPrefix();

                if (!Strings.isNullOrEmpty(prefix) && msg.toLowerCase().startsWith(prefix.toLowerCase())) {
                    startCommandExecution(prefix, message, guild, guildContext, event);
                }
            });
        }
    }

    private void startCommandExecution(String prefix, Message message, Guild guild, GuildContext guildContext, MessageReceivedEvent event) {
        ThreadExecutionQueue queue = executionQueueManager.getForGuild(guild);
//        splits message into [command identifier, rest of user input]
        String[] input = message.getContentDisplay().substring(prefix.length()).trim().split("\\s+", 2);
        String[] commandBody = input.length == 1 ? new String[]{} : input[1].split("\\s+");
        CommandContext commandContext = new CommandContext(event, guildContext, spotifyApiBuilder, commandBody);

        commandManager.executeCommand(input[0].toLowerCase(), commandContext, queue);
    }
}
