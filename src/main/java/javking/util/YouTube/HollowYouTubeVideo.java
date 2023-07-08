package javking.util.YouTube;

import javking.exceptions.UnavailableResourceException;
import javking.models.guild.user.UserContext;
import javking.models.music.AbstractSoftCachedPlayable;
import javking.models.music.Playable;
import javking.util.Spotify.SpotifyRedirectService;
import javking.util.Spotify.SpotifyTrack;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.*;

import static javking.util.YouTube.YouTubeConstants.THUMBNAIL_BASE_PREFIX;
import static javking.util.YouTube.YouTubeConstants.THUMBNAIL_HQDEFAULT_SUFFIX;

public class HollowYouTubeVideo extends AbstractSoftCachedPlayable implements YouTubeVideo, Serializable {
    private static final long serialVersionUID = 15L;

    private String playbackUri;
    private UserContext userContext;

    private transient boolean cancelled = false;
    private transient volatile boolean isHollow = true;
    private transient final CompletableFuture<String> title;
    private transient final CompletableFuture<String> id;
    private transient final CompletableFuture<String> uri;
    private transient final CompletableFuture<String> thumbnail;
    private transient final CompletableFuture<String> channel;
    private transient final CompletableFuture<Long> duration;
    private transient final CompletableFuture<UserContext> requester;

    private transient final YouTubeService youTubeService;
    @Nullable
    private SpotifyTrack redirectedSpotifyTrack;

    public HollowYouTubeVideo(YouTubeService youTubeService) {
        this(youTubeService, null);
    }

    public HollowYouTubeVideo(YouTubeService youTubeService, @Nullable SpotifyTrack redirectedSpotifyTrack) {
        this.youTubeService = youTubeService;
        this.title = new CompletableFuture<>();
        this.id = new CompletableFuture<>();
        this.uri = new CompletableFuture<>();
        this.thumbnail = new CompletableFuture<>();
        this.channel = new CompletableFuture<>();
        this.duration = new CompletableFuture<>();
        this.requester = new CompletableFuture<>();
        this.redirectedSpotifyTrack = redirectedSpotifyTrack;
    }

    @Override
    public String getTitle() throws UnavailableResourceException {
        return getCompleted(title);
    }

    public HollowYouTubeVideo setTitle(String title) {
        isHollow = false;
        this.title.complete(title);
        return this;
    }

    @Override
    public String getTitle(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getWithTimeout(title, timeOut, unit);
    }

    @Override
    public String getVideoId() throws UnavailableResourceException {
        return getCompleted(id);
    }

    public String getVideoUrl() throws UnavailableResourceException {
        return getCompleted(uri);
    }

    @Override
    public String getPlaybackUrl() {
        return playbackUri;
    }

    public HollowYouTubeVideo setId(String id) {
        return setId(id, false);
    }

    public HollowYouTubeVideo setId(String id, boolean thumbnail) {
        isHollow = false;
        this.id.complete(id);
        setUri(YouTubeConstants.VIDEO_URL_PREFIX + id);

        if (thumbnail) this.thumbnail.complete(THUMBNAIL_BASE_PREFIX + id + THUMBNAIL_HQDEFAULT_SUFFIX);

        return this;
    }

    public HollowYouTubeVideo setUri(String uri) {
        this.uri.complete(uri);
        playbackUri = uri;
        return this;
    }

    public HollowYouTubeVideo setChannel(String channel) {
        this.channel.complete(channel);
        return this;
    }

    @Override
    public String getChannel() throws UnavailableResourceException {
        return getCompleted(channel);
    }

    @Override
    public String getThumbnailUrl() throws UnavailableResourceException {
        return getCompleted(thumbnail);
    }

    public HollowYouTubeVideo setThumbnail(String thumbnail) {
        this.thumbnail.complete(thumbnail);
        return this;
    }

    @Override
    public long getDuration() throws UnavailableResourceException {
        return getCompleted(duration);
    }

    public HollowYouTubeVideo setDuration(long duration) {
        isHollow = false;
        this.duration.complete(duration);
        return this;
    }

    @Override
    public long getDuration(long timeOut, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        return getWithTimeout(duration, timeOut, unit);
    }

    @Override
    public User getRequester() throws UnavailableResourceException {
        return getCompleted(requester);
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public HollowYouTubeVideo setRequester(User user) {
        UserContext userContext = new UserContext(user);
        this.requester.complete(userContext);
        this.userContext = userContext;
        return this;
    }

    @Nullable
    @Override
    public SpotifyTrack getRedirectedSpotifyTrack() {
        return redirectedSpotifyTrack;
    }

    @Override
    public void setRedirectedSpotifyTrack(@Nullable SpotifyTrack track) {
        redirectedSpotifyTrack = track;
    }

    public void cancel() {
        cancelled = true;
        isHollow = true;
        title.cancel(false);
        id.cancel(false);
        duration.cancel(false);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isHollow() {
        return isHollow;
    }

    public boolean isDone() {
        return title.isDone() && id.isDone() && duration.isDone();
    }

    public void markLoading() {
        isHollow = false;
    }

    @Override
    public Playable fetch() throws UnavailableResourceException, IOException {
        if (isHollow() && redirectedSpotifyTrack != null) {
            markLoading();
            SpotifyRedirectService spotifyRedirectService = new SpotifyRedirectService(youTubeService);
            spotifyRedirectService.redirectTrack(this);
        }
        return this;
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

    public void awaitCompletion() {
        try {
            title.get(1, TimeUnit.MINUTES);
            id.get(1, TimeUnit.MINUTES);
            duration.get(1, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            throw new RuntimeException("Waiting for video timed out", e);
        } catch (InterruptedException | ExecutionException | CancellationException ignored) {
        }
    }

    private <E> E getCompleted(CompletableFuture<E> future) throws UnavailableResourceException {
        try {
            try {
                return future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException ignored) {
                fetch();

                return future.get(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Video loading timed out", e);
        } catch (CancellationException e) {
            throw new UnavailableResourceException();
        }
    }

    private <E> E getWithTimeout(CompletableFuture<E> future, long time, TimeUnit unit) throws UnavailableResourceException, TimeoutException {
        try {
            return future.get(time, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (CancellationException e) {
            throw new UnavailableResourceException();
        }
    }
}
