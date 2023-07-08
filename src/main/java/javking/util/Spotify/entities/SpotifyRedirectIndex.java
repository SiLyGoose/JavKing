package javking.util.Spotify.entities;

public class SpotifyRedirectIndex {

    private String spotifyId;
    private String youTubeId;

//    private SpotifyItemKind spotifyItemKind;

    public SpotifyRedirectIndex() {
    }

    public SpotifyRedirectIndex(String spotifyId, String youTubeId) {
        this.spotifyId = spotifyId;
        this.youTubeId = youTubeId;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getYouTubeId() {
        return youTubeId;
    }

    public void setYouTubeId(String youTubeId) {
        this.youTubeId = youTubeId;
    }

//    public SpotifyItemKind getSpotifyItemKind() {
//        return spotifyItemKind;
//    }
}
