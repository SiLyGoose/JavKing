package javking.util.Spotify.login;

import javking.concurrent.LoggingThreadFactory;
import net.dv8tion.jda.api.entities.User;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Login {
    private static final ScheduledExecutorService REFRESH_SERVICE = Executors.newScheduledThreadPool(3, new LoggingThreadFactory("login-refresh-pool"));

    private final User user;
    private ScheduledFuture<?> nextRefresh;
    private boolean expired = false;
    private String accessToken;
    private String refreshToken;

    public Login(User user, String accessToken, String refreshToken, int expiresIn, SpotifyApi spotifyApi) {
        this.user = user;
        nextRefresh = REFRESH_SERVICE.schedule(new AutoRefreshTask(spotifyApi), expiresIn, TimeUnit.SECONDS);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public User getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isExpired() {
        return expired;
    }

    public void expire() {
        expired = true;
    }

    public void cancel() {
        nextRefresh.cancel(false);
        expire();
    }

    private class AutoRefreshTask implements Runnable {

        private final SpotifyApi spotifyApi;

        private AutoRefreshTask(SpotifyApi spotifyApi) {
            this.spotifyApi = spotifyApi;
        }

        @Override
        public void run() {
            try {
                spotifyApi.setRefreshToken(getRefreshToken());
                AuthorizationCodeCredentials refreshCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
                setAccessToken(refreshCredentials.getAccessToken());

                nextRefresh = REFRESH_SERVICE.schedule(new AutoRefreshTask(spotifyApi), refreshCredentials.getExpiresIn(), TimeUnit.SECONDS);
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                Logger logger = LoggerFactory.getLogger(getClass());
                logger.warn("Failed to refresh login for user " + user.getName(), e);
                expire();
            }
        }
    }
}
