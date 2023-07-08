package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.discord.listeners.VoiceUpdateListener;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import static javking.util.function.populator.SocketDataPopulator.handleTrackMutatorEvent;

public class pause extends AbstractCommand {

    public pause() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "pauses the current song";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"stop"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        MessageChannel messageChannel = context.getChannel();

        AudioManager audioManager = JavKing.get().getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

        if (!audioPlayback.isPaused()) {
            getMessageService().sendBold(Templates.music.paused_queue.formatFull("Paused"), messageChannel);
            audioPlayback.pause();
        } else {
            getMessageService().sendBold(Templates.command.x_mark.formatFull("The player is already paused"), messageChannel);
        }

        handleTrackMutatorEvent("stationUpdate", context.getUserContext().getId(), audioPlayback);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
