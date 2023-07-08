package javking.models.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.exceptions.UnavailableResourceException;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Playable {

    String UNAVAILABLE_STRING = "[UNAVAILABLE]";
    String LOADING_STRING = "Loading...";

    /**
     * @return the url of the music file to stream from
     * @throws UnavailableResourceException
     */
    String getPlaybackUrl() throws UnavailableResourceException;

    /**
     * @return unique id
     * @throws UnavailableResourceException
     */
    String getId() throws UnavailableResourceException;

    /**
     * @return title of track
     * @throws UnavailableResourceException
     */
    String getTitle() throws UnavailableResourceException;

    default String title() {
        try {
            return getTitle();
        } catch (UnavailableResourceException e) {
            return UNAVAILABLE_STRING;
        }
    }

    String getTitle(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException;

    default String title(long timeOut, TimeUnit unit) {
        try {
            return getTitle(timeOut, unit);
        } catch (UnavailableResourceException e) {
            return UNAVAILABLE_STRING;
        } catch (TimeoutException e) {
            return LOADING_STRING;
        }
    }

    /**
     * @return thumbnail of track
     * @throws UnavailableResourceException
     */
    String getThumbnailUrl() throws UnavailableResourceException;

    /**
     * @return discord user requester
     * @throws UnavailableResourceException
     */
    User getRequester() throws UnavailableResourceException;

    /**
     * @return YT: Channel name, Spotify: Artist
     * @throws UnavailableResourceException
     */
    String getChannel() throws UnavailableResourceException;

    /**
     * @return duration of track in milliseconds
     * @throws UnavailableResourceException
     */
    long getDurationMs() throws UnavailableResourceException;

    default long durationMs() {
        try {
            return getDurationMs();
        } catch (UnavailableResourceException e) {
            return 0;
        }
    }

    long getDurationMs(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException;

    default long durationMs(long timeOut, TimeUnit unit) {
        try {
            return getDurationMs(timeOut, unit);
        } catch (UnavailableResourceException | TimeoutException e) {
            return 0;
        }
    }

    @Nullable
    String getAlbumCoverUrl();

    Source getSource();

    @Nullable
    AudioTrack getCached();

    void setCached(AudioTrack audioTrack);

    default Playable fetch() throws UnavailableResourceException, IOException {
        return this;
    }

    JSONObject toJSONObject() throws UnavailableResourceException;

    enum Source {
        SPOTIFY("Spotify"),
        SOUNDCLOUD("SoundCloud"),
        YOUTUBE("YouTube");

        private final String name;

        Source(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
