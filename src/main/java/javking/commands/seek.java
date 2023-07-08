package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.UserException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.Templates;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import static javking.util.function.populator.SocketDataPopulator.handleTrackUpdateEvent;

public class seek extends AbstractCommand {

    public seek() {
        super.setRequiresInput(true);
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "seeks to specific time in track";
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[]{"<[hh:mm:]ss>"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        String[] args = getCommandBody();

        if (args.length == 0) throw new UserException("SEEK: no available time");

        String concat = String.join(" ", args);

        Guild guild = context.getGuild();
        JavKing instance = JavKing.get();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);
        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        Playable currentPlayable = audioQueue.getCurrent();
        long duration = currentPlayable.durationMs();
        long seekTo = concat.contains(":")
                ? TimeConvertingService.HHMMSStoMilliseconds(args[0])
                : Long.parseLong(args[0]) * 1000;

        String message;

        if (seekTo > duration) {
            message = Templates.command.x_mark.formatFull(String.format("**Seek to time must not exceed `%s` seconds!**", duration), context.getChannel());
        } else {
            message = Templates.command.blue_check_mark.formatFull(String.format("**Playing `%s` from `%s`** - Now!", currentPlayable.getTitle(), TimeConvertingService.millisecondsToHHMMSS(seekTo)));
            audioPlayback.setCurrentPositionMs(seekTo);
        }

        getMessageService().send(message, context.getChannel());

        handleTrackUpdateEvent("trackTimeUpdate", context.getUserContext().getId(), audioPlayback);
    }

    @Override
    public void onFailed() {
        MessageChannel channel = getContext().getChannel();
        getMessageService().send(buildHelpEmbed(), channel);
        getMessageService().send("**_Some examples_**:\n`01:30:00` for 1 hour and 30 minutes\n" +
                "`01:30` for 1 minute and 30 seconds\n`90` also for 1 minute and 30 seconds", channel);
    }

    @Override
    public void onSuccess() {

    }
}
