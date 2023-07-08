package javking.util.Spotify.exec;

import com.google.common.collect.Lists;
import javking.util.BulkOperationService;
import javking.util.Spotify.SpotifyService;
import javking.util.Spotify.SpotifyTrack;
import javking.util.Spotify.SpotifyTrackKind;
import javking.util.function.CheckedFunction;
import org.apache.commons.lang3.tuple.Pair;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SpotifyTrackBulkLoadingService extends BulkOperationService<SpotifyTrackBulkLoadingService.SpotifyItem, SpotifyTrack> {
    public SpotifyTrackBulkLoadingService(SpotifyApi spotifyApi) {
        this(spotifyApi, false);
    }

    public SpotifyTrackBulkLoadingService(SpotifyApi spotifyApi, boolean acceptNullValues) {
        super(50, new CheckedFunction<>() {

            // time of first request, spotify credentials expire after and hour so if the task takes too long the credentials have to be refreshed
            private final LocalDateTime conceptionTime = LocalDateTime.now();
            private final SpotifyService spotifyService = new SpotifyService(spotifyApi);
            private LocalDateTime timeToRefreshCredentials = conceptionTime.plusMinutes(50);

            @Override
            public List<Pair<SpotifyItem, SpotifyTrack>> doApply(List<SpotifyItem> ids) throws Exception {
                LocalDateTime now = LocalDateTime.now();
                if (now.compareTo(timeToRefreshCredentials) > 0) {
                    ClientCredentials credentials = spotifyApi.clientCredentials().build().execute();
                    spotifyApi.setAccessToken(credentials.getAccessToken());
                    timeToRefreshCredentials = now.plusMinutes(50);
                }

                Map<SpotifyTrackKind, List<SpotifyItem>> kindIdMap = ids.stream().collect(Collectors.groupingBy(SpotifyItem::getKind));
                List<Pair<SpotifyItem, SpotifyTrack>> keyValuePairs = Lists.newArrayList();

                for (SpotifyTrackKind spotifyTrackKind : kindIdMap.keySet()) {
                    List<SpotifyItem> spotifyItems = kindIdMap.get(spotifyTrackKind);
                    String[] currentKindIds = spotifyItems.stream().map(SpotifyItem::getId).toArray(String[]::new);

                    List<SpotifyTrack> spotifyTracks = spotifyTrackKind.loadSeveralItems(spotifyService, currentKindIds);

                    if (spotifyTracks.size() != spotifyItems.size()) {
                        throw new IllegalStateException("Number of resulting spotify tracks does not match provided items.");
                    }

                    for (int i = 0; i < spotifyTracks.size(); i++) {
                        SpotifyTrack spotifyTrack = spotifyTracks.get(i);

                        if (acceptNullValues || spotifyTrack != null) {
                            SpotifyItem sourceItem = spotifyItems.get(i);
                            keyValuePairs.add(Pair.of(sourceItem, spotifyTrack));
                        }
                    }
                }

                return keyValuePairs;
            }
        });
    }

    public static SpotifyItem createItem(String id, SpotifyTrackKind kind) {
        return new SpotifyItem(id, kind);
    }

    @Override
    public void add(SpotifyItem key, Consumer<SpotifyTrack> action) {
        if (key != null && key.id != null) {
            super.add(key, action);
        }
    }

    public static class SpotifyItem {

        private final String id;
        private final SpotifyTrackKind kind;

        public SpotifyItem(String id, SpotifyTrackKind kind) {
            this.id = id;
            this.kind = kind;
        }

        public String getId() {
            return id;
        }

        public SpotifyTrackKind getKind() {
            return kind;
        }

        @Override
        public int hashCode() {
            return id.hashCode() + kind.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SpotifyItem) {
                SpotifyItem other = (SpotifyItem) obj;
                return Objects.equals(id, other.id) && Objects.equals(kind, other.kind);
            }
            return false;
        }
    }
}
