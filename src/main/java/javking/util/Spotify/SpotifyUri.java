package javking.util.Spotify;

import com.google.common.collect.Lists;
import javking.audio.PlayableFactory;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.CommandRuntimeException;
import javking.models.music.Playable;
import javking.util.Spotify.login.SpotifyInvoker;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.model_objects.specification.Episode;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;
import java.util.regex.Pattern;

public class SpotifyUri {
    private static final Pattern URI_REGEX = Pattern.compile("https://open\\.spotify\\.com/(track|album|playlist|episode|show)/([a-zA-Z0-9]+)(.*)");

    private final String id;
    private final Type type;

    private final SpotifyService spotifyService;

    public SpotifyUri(String uri) {
        this(uri, null);
    }

    public SpotifyUri(String uri, SpotifyService spotifyService) {
        if (Type.TRACK.getPattern().matcher(uri).matches()) {
            type = Type.TRACK;
        } else if (Type.ALBUM.getPattern().matcher(uri).matches()) {
            type = Type.ALBUM;
        } else if (Type.PLAYLIST.getPattern().matcher(uri).matches()) {
            type = Type.PLAYLIST;
        } else if (Type.EPISODE.getPattern().matcher(uri).matches()) {
            type = Type.EPISODE;
        } else {
            throw new CommandRuntimeException(new Throwable("Unsupported URI! Supported: spotify:track, spotify:album, spotify:playlist, spotify:episode, spotify:show"));
        }
        id = parseId(uri);
        this.spotifyService = spotifyService;
    }

    public static SpotifyUri parse(String uri) {
        return new SpotifyUri(uri);
    }

    public static boolean isSpotifyUri(String s) {
        return URI_REGEX.matcher(s).matches();
    }

    private String parseId(String uri) {
        String[] domain = uri.split("/");
        String[] subdirectory = domain[domain.length - 1].split("\\?");
        return subdirectory[0];
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public SpotifyService getSpotifyService() {
        return spotifyService;
    }

    public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                        SpotifyService spotifyService,
                                        boolean redirect,
                                        boolean mayInterrupt) throws Exception {
        return type.loadPlayables(playableFactory, spotifyService, this, redirect, mayInterrupt);
    }

   public enum Type {
        TRACK(Pattern.compile("https://open\\.spotify\\.com/track/([a-zA-Z0-9]+)(.*)")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                SpotifyInvoker invoker = SpotifyInvoker.create(spotifyService.getSpotifyApi());
                Track track;
                try {
                    track = invoker.invoke(() -> spotifyService.getTrack(uri.getId()));
                } catch (NotFoundException e) {
                    throw new CommandExecutionException("No results found for id " + uri.getId());
                }
                return List.of(playableFactory.createPlayable(redirect, track));
            }
        },
        ALBUM(Pattern.compile("https://open\\.spotify\\.com/album/([a-zA-Z0-9]+)(.*)")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                SpotifyInvoker invoker = SpotifyInvoker.create(spotifyService.getSpotifyApi());
                List<Track> tracks;
                try {
                    tracks = invoker.invoke(() -> spotifyService.getAlbumTracks(uri.getId()));
                } catch (NotFoundException e) {
                    throw new CommandExecutionException("No results found for id " + uri.getId());
                }
                return playableFactory.createPlayables(redirect, tracks);
            }
        },
        PLAYLIST(Pattern.compile("https://open\\.spotify\\.com/playlist/([a-zA-Z0-9]+)(.*)")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                SpotifyInvoker invoker = SpotifyInvoker.create(spotifyService.getSpotifyApi());
                List<SpotifyTrack> tracks;
                try {
                    tracks = invoker.invoke(() -> spotifyService.getPlaylistTracks(uri.getId()));
                } catch (NotFoundException e) {
                    throw new CommandExecutionException("No results found for id " + uri.getId());
                }
                return playableFactory.createPlayables(redirect, tracks);
            }
        },
        EPISODE(Pattern.compile("https://open\\.spotify\\.com/episode/([a-zA-Z0-9]+)(.*)")) {
            @Override
            public List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                SpotifyService spotifyService,
                                                SpotifyUri uri,
                                                boolean redirect,
                                                boolean mayInterrupt) throws Exception {
                SpotifyInvoker invoker = SpotifyInvoker.create(spotifyService.getSpotifyApi());
                Episode episode;
                try {
                    episode = invoker.invoke(() -> spotifyService.getEpisode(uri.getId()));
                } catch (NotFoundException e) {
                    throw new CommandExecutionException("No results found for id " + uri.getId());
                }
                return Lists.newArrayList(playableFactory.createPlayable(redirect, episode));
            }
        };

        private final Pattern pattern;

        Type(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public abstract List<Playable> loadPlayables(PlayableFactory playableFactory,
                                                     SpotifyService spotifyService,
                                                     SpotifyUri uri,
                                                     boolean redirect,
                                                     boolean mayInterrupt) throws Exception;

    }

}