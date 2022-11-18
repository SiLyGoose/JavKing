package javking.audio;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.audio.exec.TrackLoadingExecutor;
import javking.models.music.Playable;
import javking.models.music.PlayableTrackWrapper;
import javking.models.music.UrlPlayable;
import javking.models.music.UrlTrack;
import javking.util.Spotify.SpotifyService;
import javking.util.Spotify.SpotifyTrack;
import javking.util.Spotify.exec.SpotifyTrackRedirectRunnable;
import javking.util.Spotify.login.SpotifyInvoker;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubePlaylist;
import javking.util.YouTube.YouTubeService;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayableFactory {
    private final AudioTrackLoader audioTrackLoader;
    private final SpotifyService spotifyService;
    private final SpotifyInvoker invoker;
    private final TrackLoadingExecutor trackLoadingExecutor;
    private final YouTubeService youTubeService;

    public PlayableFactory(AudioTrackLoader audioTrackLoader, SpotifyService spotifyService, TrackLoadingExecutor trackLoadingExecutor, YouTubeService youTubeService) {
        this.audioTrackLoader = audioTrackLoader;
        this.spotifyService = spotifyService;
        invoker = SpotifyInvoker.create(spotifyService.getSpotifyApi());
        this.trackLoadingExecutor = trackLoadingExecutor;
        this.youTubeService = youTubeService;
    }

    public Playable createPlayable(boolean redirectSpotify, Object track) {
        List<Playable> playables = createPlayables(redirectSpotify, Collections.singleton(track));

        if (playables.isEmpty()) {
            return null;
        } else if (playables.size() == 1) {
            return playables.get(0);
        } else {
            throw new IllegalStateException(String.format("Expected 1 but found %s playables", playables.size()));
        }
    }

    public List<Playable> createPlayables(boolean redirectSpotify, Object item) {
        if (item instanceof Collection) {
            return createPlayables(redirectSpotify, (Collection) item);
        } else {
            return createPlayables(redirectSpotify, Lists.newArrayList(item));
        }
    }

    public List<Playable> createPlayables(boolean redirectSpotify, Collection<?> items) {
        List<Playable> playables = Lists.newArrayList();
        List<HollowYouTubeVideo> tracksToRedirect = Lists.newArrayList();
        List<YouTubePlaylist> youTubePlaylistsToLoad = Lists.newArrayList();

        try {
            for (Object item : items) {
                if (item instanceof Playable) {
                    playables.add((Playable) item);
                } else if (item instanceof Track) {
                    handleTrack(SpotifyTrack.wrap((Track) item), redirectSpotify, tracksToRedirect, playables);
                } else if (item instanceof Episode) {
                    handleTrack(SpotifyTrack.wrap((Episode) item), redirectSpotify, tracksToRedirect, playables);
                } else if (item instanceof SpotifyTrack) {
                    handleTrack((SpotifyTrack) item, redirectSpotify, tracksToRedirect, playables);
                } else if (item instanceof UrlTrack) {
                    playables.add(((UrlTrack) item).asPlayable());
                } else if (item instanceof YouTubePlaylist) {
                    YouTubePlaylist youTubePlaylist = ((YouTubePlaylist) item);
                    playables.addAll(youTubePlaylist.getVideos());
                    youTubePlaylistsToLoad.add(youTubePlaylist);
                } else if (item instanceof PlaylistSimplified) {
                    List<SpotifyTrack> t = SpotifyInvoker.create(spotifyService.getSpotifyApi()).invoke(() -> spotifyService.getPlaylistTracks((PlaylistSimplified) item));
                    for (SpotifyTrack track : t) {
                        handleTrack(track, redirectSpotify, tracksToRedirect, playables);
                    }
                } else if (item instanceof AlbumSimplified) {
                    List<Track> t = invoker.invoke(() -> spotifyService.getAlbumTracks((AlbumSimplified) item));
                    for (Track track : t) {
                        handleTrack(SpotifyTrack.wrapIfNotNull(track), redirectSpotify, tracksToRedirect, playables);
                    }
                } else if (item instanceof AudioTrack) {
                    playables.add(new UrlPlayable((AudioTrack) item));
                } else if (item instanceof AudioPlaylist) {
                    List<Playable> convertedPlayables = ((AudioPlaylist) item).getTracks().stream().map(UrlPlayable::new).collect(Collectors.toList());
                    playables.addAll(convertedPlayables);
                } else if (item != null) {
                    throw new UnsupportedOperationException("Unsupported playable " + item.getClass());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while creating Playables", e);
        }

        if (!tracksToRedirect.isEmpty() || !youTubePlaylistsToLoad.isEmpty()) {
            trackLoadingExecutor.execute(new SpotifyTrackRedirectRunnable(tracksToRedirect, youTubeService));
        }

        return playables;
    }

    private void handleTrack(SpotifyTrack track, boolean redirectSpotify, List<HollowYouTubeVideo> tracksToRedirect, List<Playable> playables) {
        if (track == null) {
            return;
        }

        if (redirectSpotify) {
            HollowYouTubeVideo youTubeVideo = new HollowYouTubeVideo(youTubeService, track);
            tracksToRedirect.add(youTubeVideo);
            playables.add(youTubeVideo);
        } else {
            playables.add(new PlayableTrackWrapper(track));
        }
    }
}
