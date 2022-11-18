package javking.util.Spotify;

import javking.util.PropertiesLoadingService;

public class SpotifyConstants {
    static final String SPOTIFY_ORIGIN = "https://open.spotify.com";
    static final String BASE_URL = "https://api.spotify.com/v1";
    static final String BASE_OATH_TOKEN = PropertiesLoadingService.requireProperty("BASE_OATH_TOKEN");

    static final String CLIENT_ID = PropertiesLoadingService.requireProperty("ID_KEY");
    static final String CLIENT_SECRET = PropertiesLoadingService.requireProperty("SECRET_KEY");
    static final String CLIENT_REDIRECT= PropertiesLoadingService.requireProperty("REDIRECT_KEY");

    static final String TRACKS_SUFFIX = "/tracks";
    static final String IMAGES_SUFFIX = "/images";

    static final String TRACK_URL_PREFIX = SPOTIFY_ORIGIN + "/track/";
    static final String PLAYLIST_PREFIX = "/playlist/";
    static final String PLAYLIST_API_PREFIX = "/playlists/";
    static final String PLAYLIST_URL_PREFIX = SPOTIFY_ORIGIN + PLAYLIST_PREFIX;

}
