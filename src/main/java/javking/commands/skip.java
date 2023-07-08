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
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import static javking.util.function.populator.SocketDataPopulator.handleQueueMutatorEvent;
import static javking.util.function.populator.SocketDataPopulator.handleTrackMutatorEvent;

public class skip extends AbstractCommand {
    private int offset = 0;
    private String skippedTo = null;

    public skip() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "skips the current/multiple track(s)";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"next", "sk"};
    }

    @Override
    public String[] getUsage() {
        return new String[]{"[int]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        AudioManager audioManager = JavKing.get().getAudioManager();

        String[] input = getCommandBody();

        if (!audioManager.preparedConnection(context, guild)) {
            throw new VoiceChannelException("User must be in the same channel as bot!");
        }

        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);
        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        if (audioQueue.isEmpty()) {
            throw new CommandExecutionException("No songs to skip!");
        }

        offset = 1;

        int currentTrack = audioQueue.getPosition();
        try {
            offset = Integer.parseInt(input[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }

        int futureTrack = 0;
        int queueSize = audioQueue.size();

        boolean overflow = currentTrack + offset >= queueSize;
        if (!audioPlayback.isRepeatAll() && overflow) {
            audioPlayback.clear(true);
            audioPlayback.stop();

//            getMessageService().sendBoldItalics(Templates.music.skipped_song.formatFull("Skipped to end of queue!"), context.getChannel());
        } else if (overflow && audioPlayback.isRepeatAll()) {
//                in case some people LIKE POTATO OR ALVIN want to skip
//                100 songs with only 5 in the queue
            if (offset > queueSize)
                getMessageService().sendBold(Templates.command.warning.formatFull("Unable to skip further than one queue length!"), context.getChannel());

            offset = Math.min(queueSize, offset);

            int provisional = currentTrack + offset;
            int page = provisional / queueSize;
            futureTrack = provisional - (page * queueSize);
        } else {
            futureTrack = currentTrack + offset;
        }

        JSONObject data = new JSONObject();
        data.put("songSkipped", offset);

        if (audioPlayback.isRepeatAll() || !overflow) {
            audioQueue.setPosition(futureTrack);
            skippedTo = audioQueue.getTrack(futureTrack).getTitle();
            data.put("position", futureTrack);
        }

        audioManager.startPlaying(guild, false);

        handleQueueMutatorEvent("stationUpdate", context.getUserContext().getId(), audioPlayback, data);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {
        String successMessage = offset > 1 ? String.format("Skipped to `%s`", skippedTo) : "Skipped";
        getMessageService().sendBoldItalics(Templates.music.skipped_song.formatFull(successMessage), getContext().getChannel());
    }
}
