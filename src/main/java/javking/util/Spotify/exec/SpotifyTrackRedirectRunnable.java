package javking.util.Spotify.exec;

import javking.exceptions.UnavailableResourceException;
import javking.util.Spotify.SpotifyRedirectService;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubeService;
import javking.util.function.ChainableRunnable;

import java.io.IOException;
import java.util.Collection;


public class SpotifyTrackRedirectRunnable extends ChainableRunnable {
    private final Collection<HollowYouTubeVideo> tracksToRedirect;
    private final YouTubeService youTubeService;

    public SpotifyTrackRedirectRunnable(Collection<HollowYouTubeVideo> tracksToRedirect, YouTubeService youTubeService) {
        this.tracksToRedirect = tracksToRedirect;
        this.youTubeService = youTubeService;
    }

    @Override
    public void doRun() {
        if (!tracksToRedirect.isEmpty()) {
            SpotifyRedirectService spotifyRedirectService = new SpotifyRedirectService(youTubeService);
            for (HollowYouTubeVideo youTubeVideo : tracksToRedirect) {
                try {
                    spotifyRedirectService.redirectTrack(youTubeVideo);
                } catch (IOException | UnavailableResourceException e) {
                    tracksToRedirect.stream().filter(HollowYouTubeVideo::isHollow).forEach(HollowYouTubeVideo::cancel);
                }
            }
        }
    }
}
