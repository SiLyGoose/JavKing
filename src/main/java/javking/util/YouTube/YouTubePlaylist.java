package javking.util.YouTube;

import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;

import java.util.List;
import java.util.stream.Collectors;

import static javking.util.YouTube.YouTubeConstants.*;

public class YouTubePlaylist {
    private final String title, id, url, thumbnail;
    private final List<HollowYouTubeVideo> videoList;

    public YouTubePlaylist(String title, String id, List<HollowYouTubeVideo> videoList) throws UnavailableResourceException {
        this.title = title;
        this.id = id;
        this.url = String.format("%s%s", PLAYLIST_URL_PREFIX, id);
        this.videoList = videoList;
        thumbnail = THUMBNAIL_BASE_PREFIX + videoList.get(0).getId() + THUMBNAIL_HQDEFAULT_SUFFIX;
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

    public List<HollowYouTubeVideo> getVideos() {
        return videoList;
    }

    public void cancelLoading() {
        videoList.stream().filter(HollowYouTubeVideo::isHollow).forEach(HollowYouTubeVideo::cancel);
    }
}
