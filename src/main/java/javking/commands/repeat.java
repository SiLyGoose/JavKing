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

import static javking.util.function.populator.SocketDataPopulator.handleTrackMutatorEvent;

public class repeat extends AbstractCommand {

    public repeat() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "loops either current playing track or entire cumulative queue";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"loop"};
    }

    @Override
    public String[] getUsage() {
        return new String[]{"[all]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        JavKing instance = JavKing.get();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

        boolean isRepeatMode, userInput = getCommandBody().length > 0;
        String usageAssist = null;

        if (userInput && (getCommandBody()[0].equalsIgnoreCase("queue") || getCommandBody()[0].equalsIgnoreCase("all"))) {
            isRepeatMode = !audioPlayback.isRepeatAll();
            audioPlayback.setRepeatOne(false);
            audioPlayback.setRepeatAll(isRepeatMode);
        } else {
            isRepeatMode = !audioPlayback.isRepeatOne();
            audioPlayback.setRepeatOne(isRepeatMode);
            audioPlayback.setRepeatAll(false);

            if (userInput) {
                usageAssist = "Did you mean repeat `queue`?";
            }
        }

        String inRepeatModeMessage = (isRepeatMode ? "Enabled" : "Disabled") +
                (audioPlayback.isRepeatAll() ? " for queue" : "") + "!";

        getMessageService().sendBold(Templates.music.repeat_song.formatFull(inRepeatModeMessage), context.getChannel());

        if (usageAssist != null) {
            getMessageService().sendItalics(usageAssist, context.getChannel());
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
