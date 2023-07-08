package javking.util.Spotify;

import com.google.common.collect.Lists;
import com.neovisionaries.i18n.CountryCode;
import javking.JavKing;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.NoResultsFoundException;
import javking.exceptions.UnavailableResourceException;
import javking.models.guild.GuildContext;
import javking.models.music.Playable;
import javking.models.music.UrlPlayable;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubeService;
import net.dv8tion.jda.api.entities.User;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.AbstractDataPagingRequest;
import se.michaelthelin.spotify.requests.data.AbstractDataRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SpotifyService {
    private final SpotifyApi spotifyApi;

    public SpotifyService(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public void handlePlayables(User user, GuildContext guildContext, List<Playable> playables) throws UnavailableResourceException {
        if (playables.isEmpty()) {
            throw new NoResultsFoundException("Result is empty!");
        }

        guildContext.getPooledTrackLoadingExecutor().execute(() ->
                playables.parallelStream()
                        .map(playable -> ((HollowYouTubeVideo) playable).setRequester(user))
                        .forEachOrdered(guildContext.getAudioPlayback()::add));

        JavKing instance = JavKing.get();
        YouTubeService youTubeService = instance.getAudioManager().getYouTubeService();
        try {
            youTubeService.announceSong((HollowYouTubeVideo) playables.get(0), guildContext.getGuild(), instance);
        } catch (ClassCastException ignored) {
            youTubeService.announceSong((UrlPlayable) playables.get(0), guildContext.getGuild(), instance);
        }
    }

    public Track getTrack(String id) throws IOException, SpotifyWebApiException, ParseException {
        return spotifyApi.getTrack(id).market(getCurrentMarket()).build().execute();
    }

    /**
     * Wrapper around {@link SpotifyApi#getSeveralTracks(String...)} that uses the current market and performs several
     * requests if the length of the provided array exceeds 50 automatically.
     *
     * @param ids Spotify track ids to lookup
     */
    public List<Track> getSeveralTrack(String... ids) throws ParseException, SpotifyWebApiException, IOException {
        List<Track> tracks = Lists.newArrayList();
        List<List<String>> batches = Lists.partition(Arrays.asList(ids), 50);
        for (List<String> batch : batches) {
            Track[] result = spotifyApi.getSeveralTracks(batch.toArray(new String[0])).market(getCurrentMarket()).build().execute();
            tracks.addAll(Arrays.asList(result));
        }

        return tracks;
    }

    public Episode getEpisode(String id) throws IOException, SpotifyWebApiException, ParseException {
        return spotifyApi.getEpisode(id).market(getCurrentMarket()).build().execute();
    }

    /**
     * Wrapper around {@link SpotifyApi#getSeveralEpisodes(String...)} that uses the current market and performs several
     * requests if the length of the provided array exceeds 50 automatically.
     *
     * @param ids Spotify track ids to lookup
     */
    public List<Episode> getSeveralEpisodes(String... ids) throws ParseException, SpotifyWebApiException, IOException {
        List<Episode> tracks = Lists.newArrayList();
        List<List<String>> batches = Lists.partition(Arrays.asList(ids), 50);
        for (List<String> batch : batches) {
            Episode[] result = spotifyApi.getSeveralEpisodes(batch.toArray(new String[0])).market(getCurrentMarket()).build().execute();
            tracks.addAll(Arrays.asList(result));
        }

        return tracks;
    }

    public Playlist getPlaylist(String id) throws IOException, SpotifyWebApiException, ParseException {
        return spotifyApi.getPlaylist(id).market(getCurrentMarket()).build().execute();
    }

    public List<SpotifyTrack> getPlaylistTracks(PlaylistSimplified playlistSimplified) throws IOException, SpotifyWebApiException, ParseException {
        return getPlaylistTracks(playlistSimplified.getId());
    }

    public List<SpotifyTrack> getPlaylistTracks(String playlistId) throws IOException, SpotifyWebApiException, ParseException {
        return getItemsOf(() -> spotifyApi.getPlaylistsItems(playlistId), (results, batch) -> results.addAll(Arrays.stream(batch)
                .filter(Objects::nonNull)
                .map(PlaylistTrack::getTrack)
                .filter(Objects::nonNull)
                .map(SpotifyTrack::wrap)
                .collect(Collectors.toList())
        ));
//        List<SpotifyTrack> results = Lists.newArrayList();
//        int limit = 50;
//        int offset = 0;
//        String nextPage;
//        do {
//            Paging<PlaylistTrack> paging = spotifyApi.getPlaylistsItems(playlistId).offset(offset).limit(limit).build().execute();
//            PlaylistTrack[] items = paging.getItems();
//            results.addAll(Arrays.stream(items)
//                    .filter(Objects::nonNull)
//                    .map(PlaylistTrack::getTrack)
//                    .filter(Objects::nonNull)
//                    .map(SpotifyTrack::wrap)
//                    .collect(Collectors.toList()));
//            offset = offset + limit;
//            nextPage = paging.getNext();
//        } while (nextPage != null);
//        return results;
    }

    public List<Track> getAlbumTracks(AlbumSimplified albumSimplified) throws IOException, SpotifyWebApiException, ParseException {
        return getAlbumTracks(albumSimplified.getId());
    }

    public List<Track> getAlbumTracks(String albumId) throws IOException, SpotifyWebApiException, ParseException {
        ResultHandler<Track, TrackSimplified> resultHandler = getResultHandler(TrackSimplified::getId, ids -> spotifyApi.getSeveralTracks(ids).market(getCurrentMarket()));
        return getItemsOf(() -> spotifyApi.getAlbumsTracks(albumId).market(getCurrentMarket()), resultHandler);
    }

    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    private <T, E, R extends AbstractDataPagingRequest.Builder<E, ?>> List<T> getItemsOf(Supplier<R> requestSupplier, ResultHandler<T, E> resultHandler) throws IOException, ParseException, SpotifyWebApiException {
        List<T> results = Lists.newArrayList();
        int limit = 50;
        int offset = 0;
        String nextPage;
        do {
            Paging<E> paging = requestSupplier.get().offset(offset).limit(limit).build().execute();
            E[] items = paging.getItems();
            resultHandler.apply(results, items);
            offset = offset + limit;
            nextPage = paging.getNext();
        } while (nextPage != null);
        return results;
    }

    private <S, T extends IPlaylistItem, R extends AbstractDataRequest.Builder<T[], ?>> ResultHandler<T, S> getResultHandler(Function<S, String> idExtractor, Function<String[], R> requestProducer) {
        return (results, batch) -> {
            String[] ids = Arrays.stream(batch).filter(Objects::nonNull).map(idExtractor).toArray(String[]::new);
            T[] result = requestProducer.apply(ids).build().execute();
            results.addAll(Arrays.asList(result));
        };
    }

    private CountryCode getCurrentMarket() {
        return JavKing.get().getSpotifyComponent().getCurrentMarket();
    }

    @FunctionalInterface
    private interface ResultHandler<T, E> {

        void apply(List<T> results, E[] batch) throws IOException, SpotifyWebApiException, ParseException;

    }
}
