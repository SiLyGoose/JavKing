package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioQueue;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.VoiceChannelException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;

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

        if (!audioManager.preparedConnection(context, guild)) {
            throw new VoiceChannelException("User must be in the same channel as bot!");
        }

        AudioQueue queue = audioManager.getQueue(guild);
        if (queue.size() < 1) {
            throw new CommandExecutionException("Insufficient songs in queue to shuffle!");
        }

        queue.shuffle();

//      onSuccess
        getMessageService().sendBold(Templates.music.shuffle_queue.formatFull("Shuffled queue!"), context.getChannel());
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
