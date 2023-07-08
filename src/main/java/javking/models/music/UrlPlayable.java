package javking.models.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.exceptions.UnavailableResourceException;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UrlPlayable implements Playable {
    private final String url;
    private final String display;
    private final long duration;
    @Nullable
    private AudioTrack audioTrack;

    public UrlPlayable(AudioTrack audioTrack) {
        url = audioTrack.getInfo().uri;
        display = audioTrack.getInfo().title;
        duration = audioTrack.getDuration();
        this.audioTrack = audioTrack;
    }

    public UrlPlayable(UrlTrack urlTrack) {
        url = urlTrack.getUrl();
        display = urlTrack.getTitle();
        duration = urlTrack.getDuration();
        audioTrack = null;
    }

    @Override
    public String getPlaybackUrl() {
        return url;
    }

    @Override
    public String getId() throws UnavailableResourceException {
        return getPlaybackUrl();
    }

    @Override
    public String getTitle() throws UnavailableResourceException {
        return display;
    }

    @Override
    public String getTitle(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getTitle();
    }

    @Override
    public String getThumbnailUrl() throws UnavailableResourceException {
        return null;
    }

    @Override
    public User getRequester() throws UnavailableResourceException {
        return null;
    }

    @Override
    public String getChannel() throws UnavailableResourceException {
        return null;
    }

    @Override
    public long getDurationMs() throws UnavailableResourceException {
        return duration;
    }

    @Override
    public long getDurationMs(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getDurationMs();
    }

    @Nullable
    @Override
    public String getAlbumCoverUrl() {
        return null;
    }

    @Override
    public Source getSource() {
        return null;
    }

    @Nullable
    @Override
    public AudioTrack getCached() {
        return audioTrack;
    }

    @Override
    public void setCached(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    @Override
    public JSONObject toJSONObject() throws UnavailableResourceException {
        JSONObject requesterData = new JSONObject();
        requesterData.put("avatar", getRequester().getAvatarUrl());
        requesterData.put("id", getRequester().getId());
        requesterData.put("username", getRequester().getName().concat("#").concat(getRequester().getDiscriminator()));

        JSONObject trackData = new JSONObject();
        trackData.put("requester", requesterData);
        trackData.put("durationMs", getDurationMs());
        trackData.put("id", getId());
        trackData.put("url", getPlaybackUrl());
        trackData.put("title", getTitle());
        trackData.put("thumbnail", getThumbnailUrl());

        return trackData;
    }
}
