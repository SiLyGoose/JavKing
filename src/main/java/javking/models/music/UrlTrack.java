package javking.models.music;

import javking.exceptions.UnavailableResourceException;
import net.dv8tion.jda.api.entities.User;
import se.michaelthelin.spotify.model_objects.specification.Playlist;

public class UrlTrack {
    private String url;
    private String title;
    private final User user;
    long duration;

    public UrlTrack(UrlPlayable playable, User user, Playlist playlist) throws UnavailableResourceException {
        this.url = playable.getPlaybackUrl();
        this.title = playable.getTitle();
        this.duration = playable.getDurationMs();
        this.user = user;
    }

    public UrlPlayable asPlayable() {
        return new UrlPlayable(this);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }
}
