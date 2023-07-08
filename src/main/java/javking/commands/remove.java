package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.discord.listeners.VoiceUpdateListener;
import javking.exceptions.CommandExecutionException;
import javking.models.command.CommandContext;
import javking.models.guild.GuildContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import static javking.util.function.populator.SocketDataPopulator.handleQueueMutatorEvent;

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
        Guild guild = context.getGuild();
        GuildContext guildContext = context.getGuildContext();
        MessageChannel messageChannel = context.getChannel();

        AudioManager audioManager = JavKing.get().getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);
        AudioQueue audioQueue = audioManager.getQueue(guild);

        audioManager.checkConnection(guild);

        int size = audioQueue.size();
        if (size < 1) throw new CommandExecutionException("At least 1 song required in queue!");

        String[] args = context.getMessage().getContentDisplay().substring(guildContext.getPrefix().length()).trim().split("\\s+");

//      args = {identifier, position, [end position]};
        if (args.length > 1) {
            int currentPosition = audioQueue.getPosition();
            int startPosition;
            try {
                startPosition = Integer.parseInt(args[1]);
//                ensure user can't remove current track
                assert startPosition > 0;
                startPosition += currentPosition;
            } catch (NumberFormatException ignored) {
                setFailed(true);
                return;
            }

            int endPosition;
//            Playable nextPlayable;
            assert startPosition < size && startPosition > 0;
            try {
                endPosition = Integer.parseInt(args[2]) + currentPosition;
                assert endPosition < size && startPosition < endPosition;
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {
//                nextPlayable = queue.getFirst();
                Playable removedPlayable = audioQueue.getTrack(startPosition);
                audioPlayback.remove(startPosition);
                getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Successfully removed `%s`", removedPlayable.getTitle())), messageChannel);
                return;
            }

            try {
                audioQueue.getTracks().subList(startPosition, endPosition + 1).clear();
            } catch (IndexOutOfBoundsException ignored) {
                throw new CommandExecutionException(String.format("Value(s) provided must be within `%s` and `%s`", 1, size));
            }

            getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Successfully removed `%d` songs starting from position `%d`", endPosition - startPosition - 1, startPosition - currentPosition)), messageChannel);

        } else getMessageService().send(buildHelpEmbed(), context.getChannel());

        handleQueueMutatorEvent("stationUpdate", context.getUserContext().getId(), audioPlayback);
    }

    @Override
    public void onFailed() {
        getMessageService().send(super.buildHelpEmbed(), getContext().getChannel());
    }

    @Override
    public void onSuccess() {

    }
}
