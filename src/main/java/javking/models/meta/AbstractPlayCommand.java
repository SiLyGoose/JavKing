package javking.models.meta;

import com.google.api.client.util.Joiner;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.PlayableFactory;
import javking.database.MongoManager;
import javking.database.MongoService;
import javking.exceptions.CommandRuntimeException;
import javking.exceptions.UnavailableResourceException;
import javking.models.guild.GuildContext;
import javking.models.guild.user.UserContext;
import javking.models.music.Playable;
import javking.templates.Template;
import javking.templates.Templates;
import javking.util.SoundCloud.SoundCloudUri;
import javking.util.Spotify.SpotifyPlaylist;
import javking.util.Spotify.SpotifyService;
import javking.util.Spotify.SpotifyUri;
import javking.util.Spotify.login.LoginManager;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubePlaylist;
import javking.util.YouTube.YouTubeService;
import javking.util.YouTube.YouTubeUri;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractPlayCommand extends AbstractCommand {
    public AbstractPlayCommand() {
        super.setRequiresInput(true);
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    private void handleResults(Object item) {
        Guild guild = getContext().getGuild();

        JavKing instance = JavKing.get();
        MongoManager manager = instance.getMongoManager();
        MongoService service = manager.getMongoService();

        manager.manageSession(instance, guild, () -> {
            if (item instanceof SpotifyPlaylist) {
                service.updateLastPlayed((SpotifyPlaylist) item, guild);
            } else if (item instanceof YouTubePlaylist) {
                service.updateLastPlayed((YouTubePlaylist) item, guild);
            } else if (item instanceof HollowYouTubeVideo) {
                service.updateLastPlayed((HollowYouTubeVideo) item, guild);
            } else if (item instanceof Playable) {
                service.updateLastPlayed((Playable) item, guild);
            }
        });
    }

    protected void loadSoundCloud(AudioManager audioManager) throws UnavailableResourceException {
        announceSearch("soundcloud");
        UserContext user = getContext().getUserContext();
        GuildContext guildContext = getContext().getGuildContext();

        Object soundcloudItem;

        SoundCloudUri soundCloudUri = SoundCloudUri.parse(getCommandBody()[0]);
        SpotifyService spotifyService = getSpotifyService();

        PlayableFactory playableFactory = audioManager.createPlayableFactory(spotifyService, guildContext.getPooledTrackLoadingExecutor());
        List<Playable> playables = soundCloudUri.loadPlayables(playableFactory, audioManager, true);

        spotifyService.handlePlayables(user, guildContext, playables);

        soundcloudItem = playables.get(0);
        handleResults(soundcloudItem);
    }

    protected void loadSpotify(AudioManager audioManager) throws Exception {
        announceSearch("spotify");
        UserContext user = getContext().getUserContext();
        LoginManager loginManager = JavKing.get().getLoginManager();

        Object spotifyItem = null;

        SpotifyUri spotifyUri = SpotifyUri.parse(getCommandBody()[0]);
        SpotifyService spotifyService = getSpotifyService();

        GuildContext guildContext = getContext().getGuildContext();

        PlayableFactory playableFactory = audioManager.createPlayableFactory(spotifyService, guildContext.getPooledTrackLoadingExecutor());
        List<Playable> playables = spotifyUri.loadPlayables(playableFactory, spotifyService, true, true);

        spotifyService.handlePlayables(user, guildContext, playables);

        try {
            if (spotifyUri.getType() == SpotifyUri.Type.PLAYLIST) {
                spotifyItem = new SpotifyPlaylist(spotifyUri.getId(), user);
            } else {
                spotifyItem = playables.get(0);
            }
        } catch (HttpResponseException hre) {
            hre.printStackTrace();
            if (hre.getStatusCode() == 404) {
                loginManager.requireLoginForUser(user);
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            CommandRuntimeException.throwException(e);
        }

        handleResults(spotifyItem);
    }

    protected void loadYouTube(AudioManager audioManager, JavKing instance) throws Exception {
        Guild guild = getContext().getGuild();
        String videoUrl = instance.getMongoService().pullLastPlayed(guild).getVideoUrl();
        setCommandBody(new String[]{videoUrl});

        loadYouTube(audioManager, true);
    }

    protected void loadYouTube(AudioManager audioManager, boolean found) throws IOException, UnavailableResourceException {
        announceSearch("youtube");
        User user = getContext().getUserContext();
        Guild guild = getContext().getGuild();

        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        YouTubeService youTubeService = JavKing.get().getAudioManager().getYouTubeService();

        Object youtubeItem;

        YouTubeUri youTubeUri = YouTubeUri.parse(found ? getCommandBody()[0] : String.join(" ", getCommandBody()));
        youtubeItem = youTubeUri.loadPlayables(youTubeService, user, guild);

        if (youTubeUri.getType() == YouTubeUri.Type.PLAYLIST) {
            playback.add((YouTubePlaylist) youtubeItem);
        } else {
            playback.add((HollowYouTubeVideo) youtubeItem);
        }

        handleResults(youtubeItem);
    }

    protected void announceSearch(String source) {
        Template template;

        switch (source.toLowerCase()) {
            case "youtube":
                template = Templates.music.youtube;
                break;
            case "spotify":
                template = Templates.music.spotify;
                break;
            case "soundcloud":
                template = Templates.music.soundcloud;
                break;
            default:
                template = Templates.music.playing_now;
        }

        getMessageService().sendBold(template.formatFull(String.format("Searching for \uD83D\uDD0D `%s`", String.join(" ", getCommandBody()))), getContext().getChannel());
    }
}
