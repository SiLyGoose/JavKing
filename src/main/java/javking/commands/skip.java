package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.VoiceChannelException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;

public class skip extends AbstractCommand {
    private int songSkipped = 0;
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

        try {
//            queue.setPosition() then play from position instead
            for (int i = 0; i < Integer.parseInt(input[0]) - 1; i++) {
                if (audioQueue.size() < 1) break;
                audioQueue.iterate();
                songSkipped++;
            }
            skippedTo = audioQueue.getCurrent().getTitle();
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            audioQueue.iterate();
            songSkipped++;
        }

        audioManager.startPlaying(guild, false);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {
        String successMessage = songSkipped > 1 ? String.format("Skipped to `%s`", skippedTo) : "Skipped";
        getMessageService().sendBoldItalics(Templates.music.skipped_song.formatFull(successMessage), getContext().getChannel());
        songSkipped = 0;
    }
}
