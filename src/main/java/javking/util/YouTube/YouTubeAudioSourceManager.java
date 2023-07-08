package javking.util.YouTube;

import com.sedmelluq.discord.lavaplayer.source.youtube.DefaultYoutubePlaylistLoader;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class YouTubeAudioSourceManager extends YoutubeAudioSourceManager {
    private final HttpInterfaceManager httpInterfaceManager;
    private final DefaultYoutubePlaylistLoader playlistLoader;

    public YouTubeAudioSourceManager(String email, String password) {
        super(true, email, password);
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
//        YoutubeHttpContextFilter.setPAPISID(PropertiesLoadingService.requireProperty("_3PAPISID"));
//        YoutubeHttpContextFilter.setPSID(PropertiesLoadingService.requireProperty("_3PSID"));
        YoutubeHttpContextFilter youtubeHttpContextFilter = new YoutubeHttpContextFilter();
        youtubeHttpContextFilter.setTokenTracker(super.getAccessTokenTracker());
        httpInterfaceManager.setHttpContextFilter(youtubeHttpContextFilter);

        this.playlistLoader = new DefaultYoutubePlaylistLoader();
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }

//    public AudioItem playlist()
    public AudioPlaylist buildPlaylist(String playlistId, String selectedVideoId) {
//        System.out.println("Starting to load playlist with ID " + playlistId);
        try (HttpInterface httpInterface = getHttpInterface()) {
            return playlistLoader.load(httpInterface, playlistId, selectedVideoId,
                    YouTubeAudioSourceManager.this::buildTrackFromInfo);
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e);
        }
    }

    public YoutubeAudioTrack buildTrackFromInfo(AudioTrackInfo info) {
        return new YoutubeAudioTrack(info, this);
    }
}
