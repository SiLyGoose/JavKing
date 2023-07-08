package javking.discord;

import com.google.common.collect.Lists;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MessageService {
    private final int limit;
    private final Logger logger;

    private static final EnumSet<Permission> ESSENTIAL_PERMISSIONS = EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY);

    public MessageService() {
        this(1000);
    }

    public MessageService(int limit) {
        this.limit = limit;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public CompletableFuture<Message> send(String message, MessageChannel channel) {
        if (message.length() < limit) {
            return sendInternal(channel, message);
        } else {
            List<String> outputParts = separateMessage(message);

            List<CompletableFuture<Message>> futureMessages = outputParts.stream()
                    .map(part -> sendInternal(channel, part))
                    .collect(Collectors.toList());

            return futureMessages.get(futureMessages.size() - 1);
        }
    }

    public CompletableFuture<Message> send(String message, User user) {
        if (message.length() < limit) {
            return executeMessageAction(user, messageChannel -> messageChannel.sendMessage(message));
        } else {
            List<String> outputParts = separateMessage(message);

            List<CompletableFuture<Message>> futureMessages = outputParts.stream()
                    .map(part -> executeMessageAction(user, messageChannel -> messageChannel.sendMessage(part)))
                    .collect(Collectors.toList());


            return futureMessages.get(futureMessages.size() - 1);
        }
    }

    public CompletableFuture<Message> send(String message, Guild guild) {
        return executeMessageAction(guild, messageChannel -> messageChannel.sendMessage(message));
    }

    public CompletableFuture<Message> send(MessageEmbed messageEmbed, MessageChannel messageChannel) {
        return sendInternal(messageChannel, messageEmbed);
    }

    public CompletableFuture<Message> send(MessageEmbed messageEmbed, User user) {
        return executeMessageAction(user, messageChannel -> messageChannel.sendMessageEmbeds(messageEmbed));
    }

    public CompletableFuture<Message> send(MessageEmbed messageEmbed, Guild guild) {
        return executeMessageAction(guild, channel -> channel.sendMessageEmbeds(messageEmbed), Permission.MESSAGE_EMBED_LINKS);
    }

    public CompletableFuture<Message> send(EmbedBuilder embedBuilder, MessageChannel channel) {
        return send(buildEmbed(embedBuilder, null), channel);
    }

    public CompletableFuture<Message> send(EmbedBuilder embedBuilder, Guild guild) {
        return send(buildEmbed(embedBuilder, null), guild);
    }

    public CompletableFuture<Message> sendEmbed(String title, String message, Color color, User user, boolean temporary) {
        return executeForUser(user, privateChannel -> sendEmbed(title, message, color, privateChannel, temporary));
    }

    public CompletableFuture<Message> sendEmbed(String title, String message, Color color, MessageChannel channel, boolean temporary) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(color);
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(message);
        if (temporary) {
            return sendTemporary(embedBuilder.build(), channel);
        } else {
            return send(embedBuilder.build(), channel);
        }
    }

    public CompletableFuture<Message> sendTemporary(MessageEmbed messageEmbed, MessageChannel messageChannel) {
        CompletableFuture<Message> futureMessage = send(messageEmbed, messageChannel);
        futureMessage.thenAccept(message -> new TempMessageDeletionTask(message).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendTemporary(EmbedBuilder embedBuilder, MessageChannel messageChannel) {
        CompletableFuture<Message> futureMessage = send(embedBuilder, messageChannel);
        futureMessage.thenAccept(message -> new TempMessageDeletionTask(message).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendTemporary(String message, MessageChannel messageChannel) {
        CompletableFuture<Message> futureMessage = send(message, messageChannel);
        futureMessage.thenAccept(msg -> new TempMessageDeletionTask(msg).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendTemporary(MessageEmbed messageEmbed, User user) {
        return executeForUser(user, privateChannel -> sendTemporary(messageEmbed, privateChannel));
    }

    public CompletableFuture<Message> sendTemporary(String message, User user) {
        return executeForUser(user, privateChannel -> sendTemporary(message, privateChannel));
    }

    public CompletableFuture<Message> sendTemporary(MessageEmbed messageEmbed, Guild guild) {
        CompletableFuture<Message> futureMessage = send(messageEmbed, guild);
        futureMessage.thenAccept(message -> new TempMessageDeletionTask(message).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendTemporary(EmbedBuilder embedBuilder, Guild guild) {
        CompletableFuture<Message> futureMessage = send(embedBuilder, guild);
        futureMessage.thenAccept(message -> new TempMessageDeletionTask(message).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendTemporary(String message, Guild guild) {
        CompletableFuture<Message> futureMessage = send(message, guild);
        futureMessage.thenAccept(msg -> new TempMessageDeletionTask(msg).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> sendException(String message, MessageChannel messageChannel) {
        return send("❌ ".concat(message), messageChannel);
    }

    public CompletableFuture<Message> sendItalics(String message, MessageChannel messageChannel) {
        return send("*" + message + "*", messageChannel);
    }

    public CompletableFuture<Message> sendItalics(String message, User user) {
        return executeForUser(user, privateChannel -> sendItalics(message, privateChannel));
    }

    public CompletableFuture<Message> sendBold(String message, MessageChannel messageChannel) {
        return send("**" + message + "**", messageChannel);
    }

    public CompletableFuture<Message> sendBold(String message, User user) {
        return executeForUser(user, privateChannel -> sendBold(message, privateChannel));
    }

    public CompletableFuture<Message> sendBoldItalics(String message, MessageChannel messageChannel) {
        return send("***" + message + "***", messageChannel);
    }

    public CompletableFuture<Message> sendBoldItalics(String message, User user) {
        return executeForUser(user, privateChannel -> sendBoldItalics(message, privateChannel));
    }

    public CompletableFuture<Message> sendBoldTemporary(String message, MessageChannel messageChannel) {
        CompletableFuture<Message> futureMessage = sendBold(message, messageChannel);
        futureMessage.thenAccept(msg -> new TempMessageDeletionTask(msg).schedule());
        return futureMessage;
    }

    public CompletableFuture<Message> executeMessageAction(MessageChannel channel, Function<MessageChannel, MessageCreateAction> function, Permission... additionalPermissions) {
        CompletableFuture<Message> futureMessage = new CompletableFuture<>();

        try {
            if (channel instanceof TextChannel) {
                TextChannel textChannel = (TextChannel) channel;
                Guild guild = textChannel.getGuild();
                Member self = guild.getSelfMember();
                if (!(self.hasAccess(textChannel) && textChannel.canTalk(self))) {
                    futureMessage.cancel(false);
                }

                for (Permission additionalPermission : additionalPermissions) {
                    if (!self.hasPermission(additionalPermission)) {
                        if (!ESSENTIAL_PERMISSIONS.contains(additionalPermission)) {
                            String message = String.format("%s is missing permission: ", self.getEffectiveName());
                            send(message, channel);
                        }
                        futureMessage.cancel(false);
                    }
                }
            }
            MessageCreateAction messageCreateAction = function.apply(channel);
            messageCreateAction.timeout(10, TimeUnit.SECONDS).queue(futureMessage::complete, e -> {
                handleError(e, channel);
                futureMessage.completeExceptionally(e);
            });
        } catch (InsufficientPermissionException e) {
            Permission permission = e.getPermission();
            if (permission == Permission.MESSAGE_SEND || permission == Permission.MESSAGE_HISTORY) {
                String message = String.format("Unable to send messages to channel `%s`", channel);
                if (channel instanceof TextChannel) {
                    message = String.format("%s on guild %s", message, ((TextChannel) channel).getGuild());
                }
                logger.warn(message);
                futureMessage.completeExceptionally(e);
            } else {
                futureMessage.completeExceptionally(e);
                if (permission != Permission.VIEW_CHANNEL) {
                    String message = String.format("Bot is missing permission: %s", permission.getName());
                    send(message, channel);
                }
            }
        }
        return futureMessage;
    }

    public CompletableFuture<Message> executeMessageAction(User user, Function<MessageChannel, MessageCreateAction> function) {
        return executeForUser(user, privateChannel -> executeMessageAction(privateChannel, function));
    }

    public CompletableFuture<Message> executeMessageAction(Guild guild, Function<MessageChannel, MessageCreateAction> function, Permission... additionalPermissions) {
        TextChannel textChannel = getTextChannelForGuild(guild);

        if (textChannel == null) {
            logger.warn("Unable to send any messages to guild " + guild.getName() + " (" + guild.getId() + ")");
            return CompletableFuture.failedFuture(new CancellationException());
        } else {
            return executeMessageAction(textChannel, function, additionalPermissions);
        }
    }

    public MessageEmbed buildEmbed(EmbedBuilder embedBuilder, @Nullable Color color) {
        if (color != null)embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    private CompletableFuture<Message> executeForUser(User user, Function<PrivateChannel, CompletableFuture<Message>> action) {
        CompletableFuture<Message> futureMessage = new CompletableFuture<>();
        user.openPrivateChannel().queue(channel -> {
            CompletableFuture<Message> future = action.apply(channel);
            future.whenComplete((msg, e) -> {
                if (e != null) {
                    futureMessage.completeExceptionally(e);
                } else {
                    futureMessage.complete(msg);
                }
            });
        }, futureMessage::completeExceptionally);

        return futureMessage;
    }

    private CompletableFuture<Message> sendInternal(MessageChannel channel, String text) {
        return executeMessageAction(channel, c -> c.sendMessage(text));
    }

    private CompletableFuture<Message> sendInternal(MessageChannel channel, MessageEmbed messageEmbed) {
        return executeMessageAction(channel, c -> c.sendMessageEmbeds(messageEmbed), Permission.MESSAGE_EMBED_LINKS);
    }

    private void handleError(Throwable e, MessageChannel channel) {
//        Guild guild;
//        if (channel instanceof TextChannel) {
//            guild = ((TextChannel) channel).getGuild();
//        } else {
//            guild = null;
//        }

        send(e.getMessage(), channel);
    }

    private List<String> separateMessage(String message) {
        List<String> outputParts = Lists.newArrayList();
        for (String line : message.split("\n")) {
            outputParts.addAll(Arrays.asList(line.split(" ")));
        }
        return outputParts;
    }

    private TextChannel getTextChannelForGuild(Guild guild) {
        TextChannel defaultChannel = (TextChannel) guild.getDefaultChannel();
        if (defaultChannel != null && defaultChannel.canTalk()) {
            return defaultChannel;
        } else {
            TextChannel systemChannel = guild.getSystemChannel();
            if (systemChannel != null && systemChannel.canTalk()) {
                return guild.getSystemChannel();
            }
        }
        List<TextChannel> availableChannels = guild.getTextChannels().stream().filter(TextChannel::canTalk).collect(Collectors.toList());
        if (availableChannels.isEmpty()) {
            return null;
        } else {
            return availableChannels.get(0);
        }
    }

    private class TempMessageDeletionTask {

        private final Message message;

        private TempMessageDeletionTask(Message message) {
            this.message = message;
        }

        private void schedule() {
            int timeoutSeconds;
            try {
                timeoutSeconds = getTimeout();
            } catch (Exception e) {
                logger.error("Exception loading tempMessageTimeout property", e);
                return;
            }

            if (timeoutSeconds > 0) {
                message.delete().queueAfter(timeoutSeconds, TimeUnit.SECONDS, v -> {
                });
            }
        }

        private int getTimeout() {
            return Integer.parseInt(PropertiesLoadingService.requireProperty("MESSAGE_TIMEOUT"));
        }
    }
}
