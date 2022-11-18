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
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class remove extends AbstractCommand {

    public remove() {
        super.setRequiresInput(true);
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[]{"<position> [end position]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild  = context.getGuild();
        MessageChannel messageChannel = context.getChannel();

        AudioManager audioManager = JavKing.get().getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        AudioQueue queue = audioManager.getQueue(guild);

        audioManager.checkConnection(guild);

        if (queue.size() < 1) throw new CommandExecutionException("At least 1 song required in queue!");

        String[] args = context.getMessage().getContentDisplay().substring(context.getGuildContext().getPrefix().length()).trim().split("\\s+");

//      args = {identifier, position, [end position]};
        if (args.length > 1) {
            int initialPosition = Integer.parseInt(args[1]), endPosition, count = 0;
//            Playable nextPlayable;
            assert initialPosition < queue.size() && initialPosition > 0;
            try {
                endPosition = Integer.parseInt(args[2]);
                assert endPosition < queue.size() && initialPosition < endPosition;
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {
//                nextPlayable = queue.getFirst();
                Playable removedPlayable = queue.getTrack(initialPosition);
                playback.remove(initialPosition);
                getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Successfully removed `%s`", removedPlayable.getTitle())), messageChannel);
                return;
            }

            try {
                for (Iterator<Playable> iterator = queue.getTracks().listIterator(); iterator.hasNext(); ) {
                    iterator.next();
                    if (count >= initialPosition && count <= endPosition) {
                        iterator.remove();
                    }
                    count++;
                }
            } catch (ConcurrentModificationException | IllegalStateException e) {
                throw new CommandExecutionException("Error removing songs from queue");
            }
            getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Successfully removed `%d` songs starting from position `%d`", count, initialPosition)), messageChannel);
        } else getMessageService().send(buildHelpEmbed(), context.getChannel());
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
