package javking.models.music;

import javking.exceptions.UnavailableResourceException;
import javking.util.Spotify.SpotifyTrack;
import javking.util.Spotify.SpotifyTrackKind;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayableTrackWrapper extends AbstractSoftCachedPlayable implements Playable {
    private final SpotifyTrack trackWrapper;

    public PlayableTrackWrapper(SpotifyTrack track) {
        this.trackWrapper = track;
    }

    @Override
    public String getPlaybackUrl() {
        return trackWrapper.getPreviewUrl();
    }

    @Override
    public String getId() throws UnavailableResourceException {
        return trackWrapper.getId();
    }

    @Override
    public String getTitle() throws UnavailableResourceException {
        return trackWrapper.getName();
    }

    @Override
    public String getTitle(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getTitle();
    }

    @Override
    public String getThumbnailUrl() {
        return getAlbumCoverUrl();
    }

    @Override
    public User getRequester() {
        return null;
    }

    @Override
    public String getChannel() throws UnavailableResourceException {
        return trackWrapper.getDisplay();
    }

    @Override
    public long getDurationMs() {
        return trackWrapper.getDurationMs();
    }

    @Override
    public long getDurationMs(long timeOut, TimeUnit unit) {
        return getDurationMs();
    }

    @Nullable
    @Override
    public String getAlbumCoverUrl() {
        return trackWrapper.getAlbumCoverUrl();
    }

    @Override
    public Source getSource() {
        return Source.SPOTIFY;
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

    public SpotifyTrack getTrack() {
        return trackWrapper;
    }

    public SpotifyTrackKind getKind() {
        return trackWrapper.getKind();
    }
}
