package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import javking.util.PropertiesLoadingService;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.SelfUser;

public class nowplaying extends AbstractCommand {

    public nowplaying() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "gets current playing song";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"np"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        MessageChannel messageChannel = context.getChannel();
        JavKing instance = JavKing.get();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        AudioQueue queue = audioManager.getQueue(guild);

        Playable song = queue.getCurrent();
        if (song == null) {
            getMessageService().sendBold(Templates.command.x_mark.formatFull("Nothing currently playing"), messageChannel);
        } else {
            StringBuilder builder = new StringBuilder();
            int count = 0;
            long intervals = song.durationMs() / 3000L;
            long currentTime = playback.getCurrentPositionMs();

            for (int i = 0; i <= 30; i++) {
                String trackLine = "▬";
                if (count < 1 && i * 100L * intervals >= currentTime) {
                    String button = "\uD83D\uDD18";
                    builder.append(button);
                    count++;
                } else builder.append(trackLine);
            }

            String description = String.format("[%s](%s)\n\n`%s`\n\n`%s / %s`\n\n`Requested By:` %s", song.getTitle(), song.getPlaybackUrl(),
                    builder, TimeConvertingService.millisecondsToHHMMSS(currentTime), TimeConvertingService.millisecondsToHHMMSS(song.durationMs()),
                    song.getRequester());

            SelfUser JavKing = context.getJda().getSelfUser();

            getMessageService().send(new EmbedTemplate().setAuthor("Now Playing ♪", PropertiesLoadingService.requireProperty("BOT_SITE"),
                            JavKing.getEffectiveAvatarUrl()).setThumbnail(song.getThumbnailUrl())
                    .setDescription(description).build(), messageChannel);
        }
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
