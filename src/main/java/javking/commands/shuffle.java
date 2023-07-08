package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.discord.listeners.VoiceUpdateListener;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.VoiceChannelException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static javking.util.function.populator.SocketDataPopulator.handleQueueMutatorEvent;

public class shuffle extends AbstractCommand {

    public shuffle() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "shuffles the queue";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"mix"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        AudioManager audioManager = JavKing.get().getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

        if (!audioManager.preparedConnection(context, guild)) {
            throw new VoiceChannelException("User must be in the same channel as bot!");
        }

        AudioQueue queue = audioManager.getQueue(guild);
        if (queue.size() < 1) {
            throw new CommandExecutionException("Insufficient songs in queue to shuffle!");
        }

        audioPlayback.setShuffled(!audioPlayback.isShuffled());

//      onSuccess
        getMessageService().sendBold(Templates.music.shuffle_queue.formatFull(audioPlayback.isShuffled() ? "Shuffled queue!" : "Normalized queue!"), context.getChannel());

        handleQueueMutatorEvent("stationUpdate", context.getUserContext().getId(), audioPlayback);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
