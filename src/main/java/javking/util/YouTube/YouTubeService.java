package javking.util.YouTube;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.common.base.Joiner;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.JavKing;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.audio.AudioTrackLoader;
import javking.concurrent.LoggingThreadFactory;
import javking.discord.MessageService;
import javking.exceptions.NoResultsFoundException;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.models.music.UrlPlayable;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import javking.util.PropertiesLoadingService;
import javking.util.Spotify.SpotifyTrack;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.ShowSimplified;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static javking.util.YouTube.YouTubeUri.*;

public class YouTubeService {
    private final YouTube youTube;
    private final String apiKey;

//    private final AtomicInteger currentQuota = new AtomicInteger(getPersistentQuota());
//    private final int quotaThreshold;

    private static final int REDIRECT_SEARCH_AMOUNT = 5;
    //    private static final int persistentQuota = 0;
    private static final ExecutorService UPDATE_QUOTA_SERVICE = Executors.newSingleThreadExecutor(new LoggingThreadFactory("update-youtube-quota-pool"));

    public YouTubeService(YouTube youTube, String apiKey) {
        this.youTube = youTube;
        this.apiKey = apiKey;
//        int youtubeApiDailyQuota = Integer.parseInt(PropertiesLoadingService.requireProperty("GOOGLE_API_DAILY_QUOTA"));
//        double factor = youtubeApiDailyQuota > 50000 ? .75 : .5;
//        quotaThreshold = (int) (youtubeApiDailyQuota * factor);
    }

    //    spotify doesn't allow direct full playback so temporary workaround with YT is used
    public void redirectSpotify(HollowYouTubeVideo youTubeVideo) throws IOException, UnavailableResourceException {
        SpotifyTrack spotifyTrack = youTubeVideo.getRedirectedSpotifyTrack();

        if (spotifyTrack == null) {
            throw new IllegalArgumentException(youTubeVideo + " is not a placeholder for redirected Spotify Track");
        }

        String artists = spotifyTrack.exhaustiveMatch(track -> {
                    StringBuilder builder = new StringBuilder();
                    ArtistSimplified[] trackArtists = track.getArtists();
                    for (int i = 0; i < trackArtists.length; i++) {
                        builder.append(trackArtists[i].getName());
                        if (i < trackArtists.length - 1) builder.append(", ");
                    }
                    return builder.toString();
                },
                episode -> {
                    ShowSimplified show = episode.getShow();
                    if (show != null) {
                        return show.getName();
                    }

                    return "";
                });

        String[] searchTerm = new String[]{spotifyTrack.getName(), " ", artists};

//        if (currentQuota.get() < quotaThreshold) {
//                UPDATE_QUOTA_SERVICE.execute(() -> {
//                    currentQuota.getAndIncrement();
//                    try {
//                        video.set((HollowYouTubeVideo) searchVideo(searchTerm, null));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                });
//        } else {

        try {
            youTubeVideo = (HollowYouTubeVideo) searchVideo(searchTerm, null);
        } catch (GoogleJsonResponseException ignored) {
            loadDefault(youTubeVideo, searchTerm, null, null);
        }
    }

//    =============== YouTubeVideoLoader ===============

    private YouTubeVideo searchVideo(String[] args, @Nullable User user) throws IOException {
        String input = Joiner.on(" ").join(args);
        if (isYouTubeVideo(input)) return searchVideo(getVideoId(input), user);

        List<SearchResult> results = youTube.search().list(Collections.singletonList("id")).setQ(input)
                .setMaxResults(1L).setType(Collections.singletonList("video")).setFields("items(id/videoId)")
                .setKey(apiKey).execute().getItems();

        if (!results.isEmpty()) {
            return searchVideo(results.get(0).getId().getVideoId(), user);
        }
        return null;
    }

    private YouTubeVideo searchVideo(String id, @Nullable User user) throws IOException {
        List<Video> list = youTube.videos().list(Collections.singletonList("snippet,id,contentDetails"))
                .setId(Collections.singletonList(id)).setKey(apiKey).setMaxResults(1L).execute().getItems();

        if (list.isEmpty()) throw new NoResultsFoundException(String.format("No results found for `%s`", id));
        return searchVideo(list.get(0), user);
    }

    private YouTubeVideo searchVideo(Video video, @Nullable User user) {
        JavKing instance = JavKing.get();
        HollowYouTubeVideo hollowYouTubeVideo = new HollowYouTubeVideo(instance.getAudioManager().getYouTubeService());
        VideoSnippet snippet = video.getSnippet();

        hollowYouTubeVideo.setId(video.getId(), true)
                .setThumbnail(snippet.getThumbnails().getHigh().getUrl())
                .setTitle(snippet.getTitle())
                .setDuration(Duration.parse(video.getContentDetails().getDuration()).getSeconds() * 1000L)
                .setChannel(snippet.getChannelTitle());

        if (user != null) {
            hollowYouTubeVideo.setRequester(user);
        }

        return hollowYouTubeVideo;
    }

    //    seems redundant, try to minimize?
    public YouTubeVideo resolveYouTubeVideo(String[] input, @Nullable User user, @Nullable Guild guild) throws IOException, UnavailableResourceException {
        JavKing instance = JavKing.get();
        HollowYouTubeVideo hollowYouTubeVideo = (HollowYouTubeVideo) searchVideo(input, user);

        if (guild != null) {
            announceAddedSong(hollowYouTubeVideo, guild, instance);
        }

        return hollowYouTubeVideo;
    }

    public YouTubeVideo resolveYouTubeVideo(String id) throws UnavailableResourceException, IOException {
        return resolveYouTubeVideo(YouTubeConstants.VIDEO_URL_PREFIX + id, null, null);
    }

    public YouTubeVideo resolveYouTubeVideo(String uri, @Nullable User user, @Nullable Guild guild) throws IOException, UnavailableResourceException {
        JavKing instance = JavKing.get();
        HollowYouTubeVideo hollowYouTubeVideo = (HollowYouTubeVideo) searchVideo(getVideoId(uri), user);

        if (guild != null) {
            announceAddedSong(hollowYouTubeVideo, guild, instance);
        }

        return hollowYouTubeVideo;
    }
//    =============== YouTubePlaylistLoader ===============

    public YouTubePlaylist resolveYouTubePlaylist(YouTubeUri uri, User user, Guild guild) throws UnavailableResourceException {
        JavKing instance = JavKing.get();
        AudioPlaylist audioPlaylist;

        String id = uri.getId();

        try {
            audioPlaylist = instance.getYouTubeAudioSourceManager().buildPlaylist(id, null);
        } catch (RuntimeException e) {
            audioPlaylist = (AudioPlaylist) new AudioTrackLoader(instance.getAudioManager().getPlayerManager()).loadByIdentifier(id);
        }

        ArrayList<HollowYouTubeVideo> videoList = new ArrayList<>();
        CompletableFuture<HollowYouTubeVideo> videoCompletableFuture = new CompletableFuture<>();

//        for (int i = 0; i < audioPlaylist.getTracks().size(); i++) {
//            HollowYouTubeVideo item = new HollowYouTubeVideo(this);
//            item.setRequester(user);
//        }

        assert audioPlaylist != null;
        for (AudioTrack track : audioPlaylist.getTracks()) {
            HollowYouTubeVideo item = new HollowYouTubeVideo(this);
            item.setRequester(user)
                    .setDuration(track.getDuration())
                    .setTitle(track.getInfo().title)
                    .setId(track.getIdentifier(), true)
                    .setChannel(track.getInfo().author);

            videoList.add(item);
            if (!videoCompletableFuture.isDone()) {
                announceAddedSong(videoCompletableFuture, videoList, guild, instance);
            }
        }


//        YouTubePlaylist youTubePlaylist = new YouTubePlaylist(audioPlaylist.getName(), id, videoList);
//
//        idk how threads work
//        trackLoadingExecutor.load(() -> {
//            populateList(audioPlaylist, youTubePlaylist);
//        }, true);
//
//        if (!videoCompletableFuture.isDone()) {
//            announceAddedSong(videoCompletableFuture, youTubePlaylist.getVideos(), guild, instance);
//        }
//
//        return youTubePlaylist;

        return new YouTubePlaylist(audioPlaylist.getName(), id, videoList);
    }

//    private void populateList(AudioPlaylist audioPlaylist, YouTubePlaylist youTubePlaylist) {
//        List<HollowYouTubeVideo> hollowVideos = youTubePlaylist.getVideos();
//        List<HollowYouTubeVideo> filledVideos = Lists.newArrayList();
//        int index = 0;
//
//        for (AudioTrack track : audioPlaylist.getTracks()) {
//            if (index < hollowVideos.size()) {
//                HollowYouTubeVideo item = hollowVideos.get(index);
//                item.setDuration(track.getDuration())
//                        .setTitle(track.getInfo().title)
//                        .setId(track.getIdentifier(), true)
//                        .setChannel(track.getInfo().author);
//
//                filledVideos.add(item);
//            }
//            ++index;
//        }
//    }

//    ========== END YouTubePlaylistLoader ===============

    public void announceSong(UrlPlayable playable, Guild guild, JavKing instance) throws UnavailableResourceException {
        HollowYouTubeVideo hollowYouTubeVideo = new HollowYouTubeVideo(this)
                .setTitle(playable.getTitle())
                .setChannel(playable.getChannel())
                .setDuration(playable.durationMs())
                .setThumbnail(playable.getThumbnailUrl())
                .setId(playable.getId(), false)
                .setUri(playable.getPlaybackUrl());

        announceSong(hollowYouTubeVideo, guild, instance);
    }

    public void announceSong(HollowYouTubeVideo video, AudioPlayback audioPlayback, AudioQueue queue, MessageService messageService) throws UnavailableResourceException {
        if (queue.size() - queue.getPosition() > 0)
            messageService.send(announceEmbedAddedSong(video, queue), audioPlayback.getChannel());
        else messageService.send(announceStringAddedSong(video), audioPlayback.getChannel());
    }

    public void announceSong(HollowYouTubeVideo video, Guild guild, JavKing instance) throws UnavailableResourceException {
        MessageService messageService = instance.getMessageService();
        AudioPlayback audioPlayback = instance.getAudioManager().getPlaybackForGuild(guild);
        AudioQueue queue = instance.getAudioManager().getQueue(guild);

        announceSong(video, audioPlayback, queue, messageService);
    }

    private void announceAddedSong(CompletableFuture<HollowYouTubeVideo> videoCompletableFuture, List<HollowYouTubeVideo> videoList, Guild guild, JavKing instance) throws UnavailableResourceException {
        HollowYouTubeVideo video = videoList.get(0);
        videoCompletableFuture.complete(video);

        announceAddedSong(video, guild, instance);
    }

    private void announceAddedSong(HollowYouTubeVideo video, Guild guild, JavKing instance) throws UnavailableResourceException {
//        instance.getMongoService().updateLastPlayed(video, guild);
        announceSong(video, guild, instance);
    }

    private String announceStringAddedSong(HollowYouTubeVideo video) throws UnavailableResourceException {
        return Templates.music.playing_now.formatFull("**Playing** :notes: `" + video.getTitle() + "` - Now!");
    }

    //    called before song is added to queue so implemented without decrement of queue.size()
    private EmbedBuilder announceEmbedAddedSong(HollowYouTubeVideo video, AudioQueue queue) throws UnavailableResourceException {
        return new EmbedTemplate()
                .clearEmbed()
                .setFooter(null)
                .setColor(Color.decode(PropertiesLoadingService.requireProperty("YOUTUBE_HEX")))
                .setDescription(String.format("[%s](%s)", video.getTitle(), video.getPlaybackUrl()))
                .setThumbnail(video.getThumbnailUrl())
                .addField("Channel", video.getChannel(), true)
                .addField("Song Duration", TimeConvertingService.millisecondsToHHMMSS(video.getDurationMs()), true)
                .addField("ET Until Playing",
                        TimeConvertingService.millisecondsToHHMMSS(queue.getTotalDuration()), true)
                .addField("Position In Queue", String.valueOf(queue.size() - queue.getPosition()), true)
                .setAuthor("Added to Queue!", PropertiesLoadingService.requireProperty("BOT_SITE"), video.getRequester().getEffectiveAvatarUrl());
    }

    public void loadDefault(HollowYouTubeVideo youTubeVideo, String[] searchTerm, @Nullable User user, @Nullable Guild guild) throws UnavailableResourceException {
        loadDefault(youTubeVideo, "ytsearch:" + Arrays.toString(searchTerm), user, guild);
    }

    public void loadDefault(HollowYouTubeVideo youTubeVideo, String id, @Nullable User user, @Nullable Guild guild) throws UnavailableResourceException {
        AudioTrackLoader audioTrackLoader = new AudioTrackLoader(JavKing.get().getAudioManager().getPlayerManager());
        AudioItem audioItem;

        try {
            audioItem = audioTrackLoader.loadByIdentifier(id);
        } catch (FriendlyException e) {
            e.printStackTrace();
            youTubeVideo.cancel();
            return;
        }

        List<AudioTrack> tracks;

        try {
            AudioPlaylist resultList = (AudioPlaylist) audioItem;
            assert resultList != null;
            tracks = resultList.getTracks();
        } catch (ClassCastException ignored) {
            AudioTrack audioTrack = (AudioTrack) audioItem;
            tracks = List.of(audioTrack);
        }

        if (tracks.isEmpty()) {
            youTubeVideo.cancel();
            return;
        }

        List<AudioTrack> audioTracks = tracks.subList(0, Math.min(tracks.size(), REDIRECT_SEARCH_AMOUNT));
        AudioTrack track = audioTracks.get(0);

        if (user != null) {
            youTubeVideo.setRequester(user);
        }

        youTubeVideo.setTitle(track.getInfo().title)
                .setDuration(track.getDuration())
                .setThumbnail(YouTubeConstants.THUMBNAIL_BASE_PREFIX + track.getIdentifier() + YouTubeConstants.THUMBNAIL_HQDEFAULT_SUFFIX)
                .setChannel(track.getInfo().author)
                .setId(track.getIdentifier());

        if (guild != null) {
            announceSong(youTubeVideo, guild, JavKing.get());
        }
    }
}
