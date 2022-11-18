package javking.util.Spotify.login;

import org.jetbrains.annotations.NotNull;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import java.util.concurrent.Callable;

public class SpotifyAuthorization {
    private final SpotifyApi spotifyApi;

    public SpotifyAuthorization(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public <E> @NotNull Callable<E> wrap(@NotNull Callable<E> callable) {
        return () -> {
            try {
                ClientCredentials credentials = spotifyApi.clientCredentials().build().execute();
                spotifyApi.setAccessToken(credentials.getAccessToken());

                return callable.call();
            } finally {
                spotifyApi.setAccessToken(null);
            }
        };
    }
}
