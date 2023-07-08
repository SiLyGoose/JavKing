package javking.util.Spotify;

import com.google.gson.Gson;
import com.neovisionaries.i18n.CountryCode;
import okhttp3.*;
import org.json.JSONObject;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;

import static javking.util.Spotify.SpotifyConstants.*;

public class SpotifyComponent {
    private final String spotifyClientId = CLIENT_ID,
            spotifyClientSecret = CLIENT_SECRET,
            redirectUri = CLIENT_REDIRECT;
    private final CountryCode defaultMarket = CountryCode.US;
    private String spotifyClientCredential = null;
    private final LocalDateTime conceptionTime = LocalDateTime.now();
    private LocalDateTime timeToRefreshCredentials = conceptionTime.plusMinutes(50);

    public String clientCredentialFlow() {
        LocalDateTime now = LocalDateTime.now();
        if (spotifyClientCredential != null && now.compareTo(timeToRefreshCredentials) < 0) return spotifyClientCredential;

        String base64EncodedAuthorization = new String(Base64.getUrlEncoder().encode(String.format("%s:%s", spotifyClientId, spotifyClientSecret).getBytes()));
        URL accessTokenUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("accounts.spotify.com")
                .addPathSegment("api")
                .addPathSegment("token")
                .build()
                .url();

        FormBody requestBody = new FormBody.Builder().add("grant_type", "client_credentials").build();

        Headers headers = new Headers.Builder()
                .add("content-type", "application/x-www-form-urlencoded")
                .add("Authorization", String.format("Basic %s", base64EncodedAuthorization))
                .build();

        Request request = new Request.Builder()
                .url(accessTokenUrl)
                .headers(headers)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            assert response != null && response.body() != null;
            JSONObject accessTokenJson = new Gson().fromJson(response.body().string(), JSONObject.class);

            System.out.println(accessTokenJson.toString());
            spotifyClientCredential = (String) accessTokenJson.get("access_token");
        } catch (IOException e) {
            e.printStackTrace();
        }
        timeToRefreshCredentials = now.plusMinutes(50);
        return spotifyClientCredential;
    }

    public SpotifyApi.Builder spotifyApiBuilder() {
        return new SpotifyApi.Builder()
                .setClientId(spotifyClientId)
                .setClientSecret(spotifyClientSecret)
                .setRedirectUri(SpotifyHttpManager.makeUri(redirectUri));
    }

    public SpotifyApi spotifyApi(SpotifyApi.Builder builder) {
        return builder.build();
    }

    public CountryCode getCurrentMarket() {
        SpotifyContext context = null;
        if (context != null) {
            CountryCode market = context.getMarket();
            if (market != null) {
                return market;
            }
        }

        return getDefaultMarket();
    }

    private CountryCode getDefaultMarket() {
        return defaultMarket;
    }
}
