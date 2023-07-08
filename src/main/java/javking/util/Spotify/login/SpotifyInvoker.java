package javking.util.Spotify.login;

import com.neovisionaries.i18n.CountryCode;
import org.jetbrains.annotations.NotNull;
import se.michaelthelin.spotify.SpotifyApi;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

public class SpotifyInvoker {

    private final SpotifyApi spotifyApi;
    @Nullable
    private final Login login;
    @Nullable
    private final CountryCode market;

    public SpotifyInvoker(SpotifyApi spotifyApi) {
        this(spotifyApi, null);
    }

    public SpotifyInvoker(SpotifyApi spotifyApi, @Nullable Login login) {
        this(spotifyApi, login, null);
    }

    public SpotifyInvoker(SpotifyApi spotifyApi, @Nullable Login login, @Nullable CountryCode market) {
        this.spotifyApi = spotifyApi;
        this.login = login;
        this.market = market;
    }


    public static SpotifyInvoker create(SpotifyApi spotifyApi) {
        return new SpotifyInvoker(spotifyApi);
    }

    public static SpotifyInvoker create(SpotifyApi spotifyApi, String marketCountryCode) {
        return new SpotifyInvoker(spotifyApi, null, CountryCode.valueOf(marketCountryCode));
    }

    public static SpotifyInvoker create(SpotifyApi spotifyApi, Login login) {
        return new SpotifyInvoker(spotifyApi, login);
    }

    public static SpotifyInvoker create(SpotifyApi spotifyApi, Login login, String marketCountryCode) {
        return new SpotifyInvoker(spotifyApi, login, CountryCode.valueOf(marketCountryCode));
    }

    public <E> E invoke(Callable<E> task) throws Exception {
        if (login != null) {
            task = new SpotifyUserAuthorization(login, spotifyApi).wrap(task);
        } else {
            task = new SpotifyAuthorization(spotifyApi).wrap(task);
        }

        if (market != null) {
            return invoke(new SpotifyMarket(market), task);
        }
        return invoke(null, task);
    }

    private <T> T invoke(@Nullable SpotifyMarket spotifyMarket, @NotNull Callable<T> task) throws Exception {
        return task.call();
    }
}
