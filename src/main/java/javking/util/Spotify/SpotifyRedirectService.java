package javking.util.Spotify;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Strings;
import javking.JavKing;
import javking.concurrent.LoggingThreadFactory;
import javking.exceptions.UnavailableResourceException;
import javking.util.Spotify.entities.SpotifyRedirectIndex;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubeService;
import javking.util.YouTube.YouTubeVideo;
import org.bson.Document;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class SpotifyRedirectService {
    private final YouTubeService youTubeService;

    private static final ExecutorService SINGLE_THREAD_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(new LoggingThreadFactory("spotify-redirect-service-pool"));

    public SpotifyRedirectService(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    public void redirectTrack(HollowYouTubeVideo youTubeVideo) throws UnavailableResourceException, IOException {
        SpotifyTrack spotifyTrack = youTubeVideo.getRedirectedSpotifyTrack();

        if (spotifyTrack == null) {
            throw new IllegalArgumentException(youTubeVideo + " is not a placeholder for a redirected Spotify Track");
        }

        if (youTubeVideo.isDone()) {
            return;
        }

        youTubeVideo.markLoading();
        String spotifyId = spotifyTrack.getId();
        Optional<SpotifyRedirectIndex> persistedSpotifyRedirectIndex;

        if (!Strings.isNullOrEmpty(spotifyId)) {
            persistedSpotifyRedirectIndex = queryExistingIndex(spotifyId);
        } else {
            persistedSpotifyRedirectIndex = Optional.empty();
        }

//        if id found in db, search yt
//        otherwise, redirect
        if (persistedSpotifyRedirectIndex.isPresent()) {
            SpotifyRedirectIndex spotifyRedirectIndex = persistedSpotifyRedirectIndex.get();
            try {
                YouTubeVideo video = youTubeService.resolveYouTubeVideo(spotifyRedirectIndex.getYouTubeId());

                if (video != null) {
                    youTubeVideo.setId(video.getVideoId(), true);
                    youTubeVideo.setDuration(video.getDuration());
                    youTubeVideo.setChannel(video.getChannel());
//                    youTubeVideo.setTitle(spotifyTrack.getDisplay());
                    youTubeVideo.setTitle(video.getTitle());
                    return;
                }
            } catch (GoogleJsonResponseException ignored) {
            }
        }

        youTubeService.redirectSpotify(youTubeVideo);

//        add to db for easy access
        if (!youTubeVideo.isCancelled() && !Strings.isNullOrEmpty(spotifyTrack.getId())) {
            SINGLE_THREAD_EXECUTOR_SERVICE.execute(() -> {
                try {
                    String videoId = youTubeVideo.getVideoId();
                    JavKing instance = JavKing.get();
                    instance.getMongoService().updateSpotifyRedirect(spotifyTrack.getId(), videoId);
                } catch (UnavailableResourceException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public synchronized Optional<SpotifyRedirectIndex> queryExistingIndex(String spotifyId) {
        JavKing instance = JavKing.get();
        AtomicReference<Optional<SpotifyRedirectIndex>> persistedSpotifyRedirectIndex = new AtomicReference<>(Optional.empty());

        CompletableFuture<Document> document = instance.getMongoService()
                .retrieve("spotifyRedirect", spotifyId, null);

        document.whenComplete((doc, throwable) ->
                persistedSpotifyRedirectIndex.set(
                        Optional.of(
                                new SpotifyRedirectIndex(doc.getString("spotifyId"), doc.getString("videoId")))));

        return persistedSpotifyRedirectIndex.get();
    }
}
