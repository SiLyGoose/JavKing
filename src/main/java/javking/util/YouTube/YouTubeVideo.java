package javking.util.YouTube;

import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.util.Spotify.SpotifyTrack;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface YouTubeVideo extends Playable {
    String getVideoId() throws UnavailableResourceException;

    @Override
    default String getId() throws UnavailableResourceException {
        return getRedirectedSpotifyTrack() != null ? getRedirectedSpotifyTrack().getId() : getVideoId();
    }

    @Override
    default String getTitle() throws UnavailableResourceException {
        return getRedirectedSpotifyTrack() != null ? getRedirectedSpotifyTrack().getName() : title();
    }

    @Override
    default String getTitle(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getRedirectedSpotifyTrack() != null ? getRedirectedSpotifyTrack().getName() : title(timeOut, unit);
    }

    @Override
    default String getPlaybackUrl() throws UnavailableResourceException {
        return YouTubeConstants.VIDEO_URL_PREFIX + getVideoId();
    }

    long getDuration() throws UnavailableResourceException;
    long getDuration(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException;

    @Override
    default long getDurationMs() throws UnavailableResourceException {
        return getDuration();
    }

    @Override
    default long getDurationMs(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getDuration(timeOut, unit);
    }

    @Override
    String getChannel() throws UnavailableResourceException;

    @Override
    User getRequester() throws UnavailableResourceException;

//    only for spotify redirect to youtube
    @Nullable
    @Override
    default String getAlbumCoverUrl() {
        if (getRedirectedSpotifyTrack() != null) {
            return getRedirectedSpotifyTrack().getAlbumCoverUrl();
        }

        try {
            return String.format("https://img.youtube.com/vi/%s/maxresdefault.jpg", getVideoId());
        } catch (UnavailableResourceException e) {
            return null;
        }
    }

    @Override
    String getThumbnailUrl() throws UnavailableResourceException;

    @Override
    default Source getSource() {
        return getRedirectedSpotifyTrack() != null ? Source.SPOTIFY : Source.YOUTUBE;
    }

    @Nullable
    SpotifyTrack getRedirectedSpotifyTrack();

    void setRedirectedSpotifyTrack(@Nullable SpotifyTrack track);

    @Override
    JSONObject toJSONObject() throws UnavailableResourceException;
}
