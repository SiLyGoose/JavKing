package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.discord.listeners.VoiceUpdateListener;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import static javking.util.function.populator.SocketDataPopulator.handleQueueMutatorEvent;
import static javking.util.function.populator.SocketDataPopulator.handleTrackMutatorEvent;

public class leave extends AbstractCommand {
    private AudioPlayback playback;

    public leave() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "Disconnects the bot from voice channel";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"dismiss"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        AudioManager audioManager = JavKing.get().getAudioManager();

        playback = audioManager.getPlaybackForGuild(guild);
        if (playback.getVoiceChannel() == null) setFailed(true);

        playback.clear(true);
        playback.stop();

        handleQueueMutatorEvent("stationUpdate", context.getUserContext().getId(), null);
    }

    @Override
    public void onFailed() {
        getMessageService()
                .sendBold(Templates.command.x_mark.formatFull("I am currently not connected to your voice channel!"),
                        getContext().getChannel());
    }

    @Override
    public void onSuccess() {
        getMessageService()
                .sendBold(Templates.command.check_mark.formatFull("Successfully left voice channel!"),
                        getContext().getChannel());
    }
}
