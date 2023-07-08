package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.CommandExecutionException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class move extends AbstractCommand {

    public move() {
        super.setRequiresInput(true);
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "moves song to desired position";
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[]{"<initial position> <end position>"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        MessageChannel messageChannel = context.getChannel();
        JavKing instance = JavKing.get();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        AudioQueue queue = audioManager.getQueue(guild);

        audioManager.checkConnection(guild);

        if (queue.size() < 3) throw new CommandExecutionException("At least 2 songs required in queue!");

        String[] args = context.getMessage().getContentDisplay().substring(context.getGuildContext().getPrefix().length()).trim().split("\\s+");

//      args = {identifier, initialPosition, endPosition};
        if (args.length > 2) {
            int initialPosition = Integer.parseInt(args[1]);
            int endPosition = Integer.parseInt(args[2]);

            assert initialPosition < queue.size() && endPosition < queue.size();
            if (initialPosition > 1 && endPosition > 0) {
                Playable song = queue.getTrack(initialPosition);
                playback.remove(initialPosition);
                queue.add(endPosition, song);

//                  onSuccess
                getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Moved `%s` to position `%d`!",
                        song.getTitle(), endPosition)), messageChannel);

            } else throw new CommandExecutionException("SKIP: Parameters not within bounds");
        } else throw new CommandExecutionException("SKIP: Not enough parameters given");
    }

    @Override
    public void onFailed() {
        getMessageService().send(buildHelpEmbed(), getContext().getChannel());
    }

    @Override
    public void onSuccess() {

    }
}
