package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.discord.listeners.VoiceUpdateListener;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.rest.controllers.StationClient;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import static javking.util.function.populator.SocketDataPopulator.handleTrackMutatorEvent;

public class resume extends AbstractCommand {

    public resume() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "resumes the queue if paused";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"continue", "go"};
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

        if (!audioPlayback.isPlaying()) {
            getMessageService().sendBold(Templates.music.resumed_queue.formatFull("Resuming"), messageChannel);
            audioPlayback.resume();
        } else {
            getMessageService().sendBold(Templates.command.x_mark.formatFull("The player is not paused"), messageChannel);
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
