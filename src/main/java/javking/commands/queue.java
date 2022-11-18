package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.UnavailableResourceException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import javking.util.PropertiesLoadingService;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class queue extends AbstractCommand {

    @Override
    public String getDescription() {
        return "returns the server's queue";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"q"};
    }

    @Override
    public String[] getUsage() {
        return new String[]{"[page/int]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        JavKing instance = JavKing.get();
        AudioManager audioManager = instance.getAudioManager();
        MessageChannel messageChannel = context.getChannel();

        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

        String[] commandBody = getCommandBody();

        int index = commandBody.length == 0 ? 1 : Integer.max(1, Integer.parseInt(commandBody[0]));
        int items = 10;
        int startIndex = (index - 1) * items + 1;
        int endIndex = startIndex + items;

        AudioQueue queue = audioPlayback.getAudioQueue();
        if (queue.isEmpty()) {
            getMessageService().sendBold(Templates.command.x_mark.formatFull("No songs in current queue"), messageChannel);
            return;
        }

        Playable firstPlayable = queue.getPlaying();

        String isRepeat = audioPlayback.isRepeatOne()
                ? " | **REPEAT ENABLED \uD83D\uDD02**"
                : audioPlayback.isRepeatAll()
                ? " | **REPEAT ALL ENABLED \uD83D\uDD01**"
                : null;

        String nowPlaying = buildPlayableString(firstPlayable);
        String title = new StringBuilder("[Queue for ")
                .append(guild.getName())
                .append("](")
                .append(PropertiesLoadingService.requireProperty("BOT_SITE"))
                .append(")")
                .append(isRepeat == null ? "" : isRepeat)
                .append("\n\n__Now Playing__:\n")
                .append(nowPlaying).toString();

        StringBuilder descriptionBuilder = new StringBuilder();
        EmbedBuilder embedBuilder = new EmbedTemplate().clearEmbed();

        if (queue.size() > 1) {
            for (int i = startIndex; i < endIndex; i++) {
                try {
                    Playable playable = queue.getTrack(i);
                    descriptionBuilder.append(buildPlayableString(playable, false, i));
                } catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
            embedBuilder.setDescription(title + "\n__Queue__:\n" + descriptionBuilder + "\n**" +
                    (queue.size() - 1) + " songs. Total length: " +
                    TimeConvertingService.millisecondsToHHMMSS(queue.getTotalDuration()) + "**");
        } else {
            embedBuilder.setDescription(title);
        }

        int tabs = (int) Math.ceil((double) queue.size() / items);
        embedBuilder.setFooter("Page " + index + " of " + tabs, context.getUserContext().getEffectiveAvatarUrl());

        getMessageService().send(embedBuilder, messageChannel);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }

    private String buildPlayableString(Playable playable) throws UnavailableResourceException {
        return buildPlayableString(playable, true, 0);
    }

    private String buildPlayableString(Playable playable, boolean nowPlaying, int startIndex) throws UnavailableResourceException {
        StringBuilder playableString = new StringBuilder();

        if (!nowPlaying) {
            playableString.append("`")
                    .append(startIndex)
                    .append(".` ");
        }

        return playableString.append("[")
                .append(playable.getTitle())
                .append("](")
                .append(playable.getPlaybackUrl())
                .append(") | `")
                .append(TimeConvertingService.millisecondsToHHMMSS(playable.getDurationMs()))
                .append(" Requested By: ")
                .append(playable.getRequester() == null ? "[UNKNOWN]" : playable.getRequester().getName())
                .append("`\n\n").toString();
    }
}
