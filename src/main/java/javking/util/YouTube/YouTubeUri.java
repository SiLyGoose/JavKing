package javking.util.YouTube;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import javking.exceptions.UnavailableResourceException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeUri {
    private static final Pattern URI_REGEX = Pattern.compile("(?:https?://)?(?:www\\.)?youtu\\.?be(?:\\.com)?/?.*(?:watch|embed|playlist)?(?:.*v=|v/|.*list=|list/|/)([\\w\\-_]+)");

    private String id;
    private final Type type;

    private final YouTubeService youTubeService;

    public YouTubeUri(String uri) {
        this(uri, null);
    }

    public YouTubeUri(String uri, YouTubeService youTubeService) {
        if (Type.TRACK.getPattern().matcher(uri).find()) {
            type = Type.TRACK;
        } else if (Type.PLAYLIST.getPattern().matcher(uri).find()) {
            type = Type.PLAYLIST;
        } else {
            type = Type.DEFAULT;
        }

        if (type == Type.DEFAULT) this.id = uri;
        else this.id = parseId(uri);

        this.youTubeService = youTubeService;
    }

    public static YouTubeUri parse(String uri) {
        return new YouTubeUri(uri);
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public YouTubeService getYouTubeService() {
        return youTubeService;
    }

    public static boolean isYouTubeUri(String s) {
        return URI_REGEX.matcher(s).find();
    }

    public static boolean isYouTubeVideo(String input) {
        return Type.TRACK.getPattern().matcher(input).find();
    }

    public static String getVideoId(String input) {
        Matcher matcher = Type.TRACK.getPattern().matcher(input);
        if (!matcher.find()) return null;
        return matcher.group(1);
    }

    public static boolean isYouTubePlaylist(String input) {
        return Type.PLAYLIST.getPattern().matcher(input).find();
    }

    public static String getPlaylistId(String input) {
        Matcher matcher = Type.PLAYLIST.getPattern().matcher(input);
        if (!matcher.find()) return null;
        return matcher.group(1);
    }

    private String parseId(String s) {
        if (!isYouTubeUri(s)) return null;

        if (isYouTubeVideo(s)) return getVideoId(s);
        else if (isYouTubePlaylist(s)) return getPlaylistId(s);

        return null;
    }

    public Object loadPlayables(YouTubeService youTubeService, User user, Guild guild) throws UnavailableResourceException, IOException {
        try {
            return type.loadPlayables(youTubeService, this, user, guild);
        } catch (GoogleJsonResponseException e) {
            HollowYouTubeVideo hollowVideo = new HollowYouTubeVideo(youTubeService);

            if (type != Type.PLAYLIST && type != Type.TRACK) {
                id = "ytsearch:" + id;
            }

            youTubeService.loadDefault(hollowVideo, id, user, guild);
            return hollowVideo;
        }
    }

    public enum Type {
        TRACK(Pattern.compile("v=([^#&\\n\\r]+)")) {
            @Override
            public HollowYouTubeVideo loadPlayables(YouTubeService youTubeService, YouTubeUri youTubeUri, User user, Guild guild) throws UnavailableResourceException, IOException {
                return (HollowYouTubeVideo) youTubeService.resolveYouTubeVideo(youTubeUri.id, user, guild);
            }
        },
        PLAYLIST(Pattern.compile("list=(.*?)(?:&|$)")) {
            @Override
            public YouTubePlaylist loadPlayables(YouTubeService youTubeService, YouTubeUri youTubeUri, User user, Guild guild) throws UnavailableResourceException {
                return youTubeService.resolveYouTubePlaylist(youTubeUri, user, guild);
            }
        },
        DEFAULT(Pattern.compile("")) {
            @Override
            public HollowYouTubeVideo loadPlayables(YouTubeService youTubeService, YouTubeUri youTubeUri, User user, Guild guild) throws UnavailableResourceException, IOException {
                return (HollowYouTubeVideo) youTubeService.resolveYouTubeVideo(new String[]{youTubeUri.id}, user, guild);
            }
        };

        private final Pattern pattern;

        Type(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public abstract Object loadPlayables(YouTubeService youTubeService,
                                             YouTubeUri youTubeUri,
                                             User user,
                                             Guild guild) throws UnavailableResourceException, IOException;
    }
}
