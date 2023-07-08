package javking.commands.interceptors;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Strings;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import javking.discord.MessageService;
import javking.exceptions.*;
import javking.models.command.CommandContext;
import javking.models.guild.GuildContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;

public class CommandExecutionInterceptor implements CommandInterceptor {
    private final MessageService messageService;
    private final Logger logger;

    public CommandExecutionInterceptor(MessageService messageService) {
        this.messageService = messageService;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void intercept(AbstractCommand command) {

        boolean completedSuccessfully = false;
        String errorMessage = null;
        boolean errorMessageSent = false;
        boolean unexpectedException = false;
        CommandContext context = command.getContext();

        try {
            try {
//                channel permission check
                TextChannel channel = (TextChannel) context.getChannel();
                Member member = context.getGuild().getSelfMember();
                if (!member.hasPermission(channel, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)) {
                    throw new CommandExecutionException(String.format("Ensure %s has permission to `MANAGE MESSAGES`, `READ MESSAGE HISTORY`, and `VIEW CHANNEL`!", context.getJda().getSelfUser().getName()));
                }

                if (command.requiresExecAdmin() && !context.getUserContext().getId().equals("257214680823627777"))
                    return;

//                voiceState and voiceChannel checks (includes perms)
                if (command.requiresVoiceChannel() || command.requiresVoice()) {
                    GuildContext guildContext;
                    if ((guildContext = context.getGuildContext()).isDjOnly()) {
                        Role role;
                        try {
                            role = guildContext.getDjRole();
                        } catch (IndexOutOfBoundsException ignored) {
                            throw new DJNotSetException("DJ Role is not set!");
                        }
                        if (!member.getRoles().contains(role)) {
                            throw new DJOnlyException(String.format("DJ Role: `%s` required to use command!", role.getName()));
                        }
                    }
                    if (context.getVoiceChannel() == null && command.requiresVoiceChannel()) {
                        throw new VoiceChannelException("Unable to join voice channel!");
                    } else {
                        GuildVoiceState voiceState = context.getMember().getVoiceState();
                        if (command.requiresVoice()) {
                            if (voiceState == null)
                                throw new VoiceStateException("You must be in a voice channel first!");

                            if (voiceState.isSelfDeafened() || voiceState.isDeafened() || voiceState.isGuildDeafened()) {
                                throw new VoiceStateException("Unable to execute command while deafened!");
                            }
                        }

//                        voice perms
                        VoiceChannel voiceChannel = context.getVoiceChannel();
                        if (!context.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.VOICE_SPEAK, Permission.VOICE_CONNECT)) {
                            throw new CommandExecutionException(String.format("Ensure %s has permission to `CONNECT` and `SPEAK`!", context.getJda().getSelfUser().getName()));
                        }
                    }
                }

//                command input checks
                if (command.requiresInput() && context.getCommandBody() == null) {
                    throw new UserException("Expected user input however received none.");
                }

                command.setMessageService(messageService);
                command.execute(context);

            } catch (Throwable e) {
                if (e instanceof NoLoginException ||
                        e instanceof MessageChannelException ||
                        e instanceof VoiceChannelException ||
                        e instanceof DJNotSetException) throw e;

                try {
                    e.printStackTrace();
                    command.onFailed();
                } catch (Throwable failed) {
                    logger.error("Exception thrown in onFailed of command, logged error and throwing initial exception.", failed);
                    throw e;
                }
            }

            if (!command.isFailed()) {
                command.onSuccess();
                completedSuccessfully = true;
            } else {
                command.onFailed();
            }

        } catch (DJOnlyException e) {
            errorMessage = e.getMessage();
            messageService.sendBold(Templates.command.no_entry.formatFull(errorMessage), context.getChannel());
            errorMessageSent = true;

        } catch (DJNotSetException e) {
            errorMessage = e.getMessage();
            messageService.sendBold(Templates.command.warning.formatFull(errorMessage), context.getChannel());
            errorMessageSent = true;

        } catch (MessageChannelException | VoiceChannelException | VoiceStateException | CommandExecutionException e) {
            errorMessage = e.getMessage();
            messageService.sendBold(Templates.command.x_mark.formatFull(errorMessage), context.getChannel());
            errorMessageSent = true;

        } catch (UserException e) {
            errorMessage = e.getMessage();
            if (e instanceof NoLoginException || e instanceof NoResultsFoundException) {
                messageService.send(errorMessage, context.getChannel());
            } else messageService.send(e.buildHelpEmbed(command), context.getChannel());

            errorMessageSent = true;

        } catch (RateLimitException e) {
            errorMessage = e.getMessage();
            messageService.sendBold(Templates.command.hourglass.formatFull(errorMessage), context.getChannel());
            errorMessageSent = true;
        } catch (FriendlyException e) {
            StringBuilder builder = new StringBuilder("Could not load track. ");
            if (e.getMessage() != null) {
                builder.append(e.getMessage());
            }
            messageService.sendBold(Templates.command.x_mark.formatFull(builder.toString()), context.getChannel());
            errorMessageSent = true;

        } catch (TooManyRequestsException e) {
            String message = "Executing too many Spotify requests at the moment, please try again later.";
            messageService.sendException(message, command.getContext().getChannel());
            logger.warn("Executing too many Spotify requests", e);
            errorMessage = message;
            unexpectedException = true;

        } catch (GoogleJsonResponseException e) {
            String message = e.getDetails().getMessage();
            StringBuilder builder = new StringBuilder("Error occurred when requesting data from YouTube.");
            if (!Strings.isNullOrEmpty(message)) {
                builder.append(" Error response: ").append(message);
            }
            errorMessage = message;
            messageService.sendException(builder.toString(), context.getChannel());
            errorMessageSent = true;
            logger.error("Exception during YouTube request", e);
            unexpectedException = true;

        } catch (CommandRuntimeException e) {
            if (e.getCause() != null) {
                errorMessage = e.getCause().getClass().getSimpleName() + ": " + e.getMessage();
            } else {
                errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            unexpectedException = true;
            throw e;

        } catch (Throwable e) {
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            unexpectedException = true;
            throw new CommandRuntimeException(e);

        } finally {
            if (!completedSuccessfully && !errorMessageSent) {
                if (unexpectedException) {
                    messageService.sendBold(Templates.command.x_mark.formatFull("Something unexpected happened, please try again."), context.getChannel());
                    if (errorMessage != null) logger.error("Unexpected exception: " + errorMessage);
                }
            }
        }
    }
}
