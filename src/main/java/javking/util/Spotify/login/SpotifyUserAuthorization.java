package javking.util.Spotify.login;

import org.jetbrains.annotations.NotNull;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.concurrent.Callable;

public class SpotifyUserAuthorization {
    private final Login login;
    private final SpotifyApi spotifyApi;

    public SpotifyUserAuthorization(Login login, SpotifyApi spotifyApi) {
        this.login = login;
        this.spotifyApi = spotifyApi;
    }

    public <E> @NotNull Callable<E> wrap(@NotNull Callable<E> callable) {
        return () -> {
            try {
                spotifyApi.setAccessToken(login.getAccessToken());
                return callable.call();
            } finally {
                spotifyApi.setAccessToken(null);
            }
        };
    }
}
