package javking.util.Spotify.login;

import com.neovisionaries.i18n.CountryCode;
import javking.util.Spotify.SpotifyContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public class SpotifyMarket {
    private final CountryCode market;

    public SpotifyMarket(CountryCode market) {
        this.market = market;
    }

    public <E> @NotNull Callable<E> wrap(@NotNull Callable<E> callable) {
        return () -> {
            SpotifyContext spotifyContext = new SpotifyContext();
            spotifyContext.setMarket(market);
            return callable.call();
        };
    }
}
