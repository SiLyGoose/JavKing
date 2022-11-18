package javking.util.Spotify;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import javking.JavKing;
import javking.models.guild.user.UserContext;
import javking.util.Spotify.login.Login;
import okhttp3.*;
import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static javking.util.Spotify.SpotifyConstants.PLAYLIST_URL_PREFIX;

public class SpotifyPlaylist {
    private final String title, id, url, thumbnail;
    private boolean isPublic;
    private final List<String> trackList;
    private final Login login;

    public SpotifyPlaylist(String id, UserContext userContext) throws IOException {
        this(id, JavKing.get().getLoginManager().getLoginForUser(userContext));
    }

    public SpotifyPlaylist(String id, Login login) throws IOException {
        this.id = id;
        this.url = PLAYLIST_URL_PREFIX + id;
        this.login = login;

        title = scrapeTitle();
        thumbnail = scrapeThumbnail();
        trackList = scrapeTracks();
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public List<String> getTracks() {
        return trackList;
    }

    private List<String> scrapeTracks() throws IOException {
        List<String> searchTerms = Lists.newArrayList();
        String JSONResponse = buildRequest("fields", "items(track(name,artists))", "tracks");

        JSONArray items = (new JSONObject(JSONResponse)).getJSONArray("items");
        Iterator<Object> iterator = items.iterator();

        do {
            JSONObject track = ((JSONObject) iterator.next()).getJSONObject("track");
            JSONArray artists = track.getJSONArray("artists");


            String name = track.getString("name");
            final StringBuilder artist = new StringBuilder();
            artists.forEach(artistItem -> {
                JSONObject artistName = ((JSONObject) artistItem);
                artist.append(artistName.getString("name"));
            });

            searchTerms.add(name + " " + artist);
        } while (iterator.hasNext());

        return searchTerms;
    }

    private String scrapeTitle() throws IOException {
        String JSONResponse = buildRequest("fields", "name,public");

        JSONObject playlist = new JSONObject(JSONResponse);

        setPublic(playlist.getBoolean("public"));
        return playlist.getString("name");

//"https://api.spotify.com/v1/playlists/3xvXRLIlXm1mjHd64BsZtz?fields=name%2Cpublic"
//"https://api.spotify.com/v1/playlists/3cEYpjA9oz9GiPac4AsH4n/images"
    }

    private String scrapeThumbnail() throws IOException {
        String JSONResponse = buildRequest(null, null, "images");

        JSONArray thumbnailJSONArray = new JSONArray(JSONResponse);

        return ((JSONObject) thumbnailJSONArray.get(0)).getString("url");
    }

    private String buildRequest(@Nullable String queryName, @Nullable String queryValue, String... subdirectoryArray) throws IOException {
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("https")
                .host("api.spotify.com")
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id);

        for (String s : subdirectoryArray) {
            urlBuilder.addPathSegment(s);
        }

        if (!(Strings.isNullOrEmpty(queryName) || Strings.isNullOrEmpty(queryValue))) {
            urlBuilder.addQueryParameter(queryName, queryValue);
        }

        String token = JavKing.get().getSpotifyComponent().clientCredentialFlow();
        if (login != null && login.getAccessToken() != null) token = login.getAccessToken();

        Headers headers = new Headers.Builder()
                .add("Accept", "application/json")
                .add("Content-Type", "application/json")
                .add("Authorization", String.format("Bearer %s", token))
                .build();

        Request request = new Request.Builder()
                .url(String.valueOf(urlBuilder))
                .headers(headers)
                .build(); // GET default

        OkHttpClient client = new OkHttpClient();

        Response response = client.newCall(request).execute();

        assert response.body() != null;
        switch(response.code()) {
            case 400:
                throw new HttpResponseException(response.code(), "Only valid bearer authentication supported");
            case 401:
                throw new HttpResponseException(response.code(), "Bad token");
            case 403:
                throw new HttpResponseException(response.code(), "Bad OAuth request");
            case 404:
                throw new HttpResponseException(response.code(), "Service not found");
            case 429:
                throw new HttpResponseException(response.code(), "Too many requests");
        }

        return response.body().string();
    }
}
