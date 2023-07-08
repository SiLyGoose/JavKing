package javking.commands;

import javking.JavKing;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import javking.util.PropertiesLoadingService;
import javking.util.Spotify.SpotifyUri;
import javking.util.YouTube.YouTubeUri;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;

public class lastplayed extends AbstractCommand {

    @Override
    public String getDescription() {
        return "displays the most recently played song/playlist";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"lp"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        JavKing instance = JavKing.get();
        MessageChannel messageChannel = context.getChannel();
        Guild guild = context.getGuild();

        instance.getMongoService().retrieve(getClass().getSimpleName(), guild).whenComplete((document, throwable) -> {
            if (document.isEmpty()) {
                getMessageService().sendBold(Templates.command.o_mark.formatFull("No `lastplayed` track/playlist found on this server!"), messageChannel);
                return;
            }

            String thumbnail = document.getString("thumbnail");
            String title = document.getString("title");
            String url = document.getString("uri");

            boolean youtubePlaylist = YouTubeUri.isYouTubePlaylist(url) && YouTubeUri.parse(url).getType() == YouTubeUri.Type.PLAYLIST;
            boolean spotifyPlaylist = SpotifyUri.isSpotifyUri(url) && SpotifyUri.parse(url).getType() == SpotifyUri.Type.PLAYLIST;

            String playlistCheck = youtubePlaylist || spotifyPlaylist
                    ? "Yes"
                    : "No";

            Color color = Color.decode(spotifyPlaylist
                    ? PropertiesLoadingService.requireProperty("SPOTIFY_HEX")
                    : PropertiesLoadingService.requireProperty("YOUTUBE_HEX"));

            EmbedBuilder embed = new EmbedTemplate()
                    .clearEmbed()
                    .setColor(color)
                    .setAuthor("Last Played in Queue for " + guild.getName(), null, context.getUserContext().getEffectiveAvatarUrl())
                    .setDescription(String.format("[%s](%s)", title, url))
                    .setThumbnail(thumbnail)
                    .addField("**How to play:**", String.format("Type `%splay lastplayed` to play!", context.getGuildContext().getPrefix()), false)
                    .addField("**Playlist?**", playlistCheck, true);

            if (spotifyPlaylist) {
                embed.addField("**Requires login?**", document.getBoolean("public") ? "No" : "Yes", true);
            }

            getMessageService().send(embed, messageChannel);
        });
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
