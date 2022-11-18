package javking.util.SoundCloud;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.audio.AudioManager;
import javking.audio.AudioTrackLoader;
import javking.audio.PlayableFactory;
import javking.exceptions.NoResultsFoundException;
import javking.models.music.Playable;

import java.util.List;
import java.util.regex.Pattern;

public class SoundCloudUri {
    private static final Pattern URI_REGEX = Pattern.compile("^(?:(https?):\\/\\/)?(?:(?:www|m)\\.)?(soundcloud\\.com|snd\\.sc)\\/(.*)$");
//    https?://(?:w\.|www\.|)(?:soundcloud\.com/)(?:(?:player/\?url=https%3A//api.soundcloud.com/tracks/)|)(((\w|-)[^A-z]{7})|([A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)*(?!/sets(?:/|$))(?:/[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)*){1,2}))

    private final String uri;

    public SoundCloudUri(String uri) {
        this.uri = parseId(uri);
    }

    public static boolean isSoundCloudUri(String s) {
        return URI_REGEX.matcher(s).matches();
    }

    public static SoundCloudUri parse(String s) {
        return new SoundCloudUri(s);
    }

    private String parseId(String uri) {
        String[] domain = uri.split("/");
        String[] subdirectory = domain[domain.length - 1].split("\\?");
//        https://soundcloud.com/fryzysound/emm-dirty?si=7cc49613135242e1bc922f51c0a0e247&utm_source=clipboard&utm_medium=text&utm_campaign=social_sharing
//        fryzysound emm-dirty
//        return String.format("%s %s", domain[3], subdirectory[0]);
        return uri;
    }

    public List<Playable> loadPlayables(PlayableFactory playableFactory, AudioManager audioManager, boolean redirect) {
        AudioTrackLoader trackLoader = new AudioTrackLoader(audioManager.getPlayerManager());
        AudioItem audioItem = trackLoader.loadByIdentifier("scsearch:" + uri);

        if (audioItem instanceof AudioTrack) {
            return List.of(playableFactory.createPlayable(redirect, audioItem));
        } else if (audioItem instanceof AudioPlaylist) {
            List<AudioTrack> tracks = ((AudioPlaylist) audioItem).getTracks();
            return playableFactory.createPlayables(redirect, tracks);
        }

        throw new NoResultsFoundException("No results found for: " + uri);
    }
}