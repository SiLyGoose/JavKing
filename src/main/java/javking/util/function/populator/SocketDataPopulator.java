package javking.util.function.populator;

import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.rest.controllers.webpage.websocket.StationClient;
import javking.rest.controllers.webpage.websocket.StationClientManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketDataPopulator {
    private static JSONObject handleMutatorData(@Nullable AudioPlayback audioPlayback) {
        return handleMutatorData(audioPlayback, new JSONObject());
    }

    private static JSONObject handleMutatorData(@Nullable AudioPlayback audioPlayback, JSONObject data) {
        JSONObject repeatData = new JSONObject();
        boolean defaultData = audioPlayback != null;
//        defaultData checks if audioPlayback is null
//        second check to ensure data is sent correctly
//        if is null, 0 && 0/1 => 0
//        if not null, 1 && 0/1 => 0/1
        repeatData.put("rO", defaultData && audioPlayback.isRepeatOne());
        repeatData.put("rA", defaultData && audioPlayback.isRepeatAll());

        data.put("r", repeatData);
        data.put("shuffled", defaultData && audioPlayback.isShuffled());
        data.put("paused", defaultData && audioPlayback.isPaused());

        return data;
    }

    public static void handleTrackMutatorEvent(String event, String userId, @Nullable AudioPlayback audioPlayback) {
        handleTrackMutatorEvent(event, userId, audioPlayback, new JSONObject());
    }

    public static void handleTrackMutatorEvent(String event, String userId, @Nullable AudioPlayback audioPlayback, JSONObject data) {
        StationClient stationClient = StationClientManager.getStationClientByUser(userId);
        if (stationClient == null) return;

        handleMutatorData(audioPlayback, data);
        stationClient.sendEvent(event, data);
    }

    public static void handleQueueMutatorEvent(String event, String userId, @Nullable AudioPlayback audioPlayback) throws UnavailableResourceException {
        handleQueueMutatorEvent(event, userId, audioPlayback, new JSONObject());
    }

    public static void handleQueueMutatorEvent(String event, String userId, @Nullable AudioPlayback audioPlayback, JSONObject data) throws UnavailableResourceException {
        StationClient stationClient = StationClientManager.getStationClientByUser(userId);
        if (stationClient == null) return;

        handleMutatorData(audioPlayback, data);

        if (audioPlayback == null) data.put("q", new JSONArray());
        else {
            AudioQueue queue = audioPlayback.getAudioQueue();

            JSONArray tracksData = new JSONArray();
            AtomicInteger count = new AtomicInteger(0);

            for (int i = 0; i < queue.size(); i++) {
                Playable playable = queue.getTrack(i);
                tracksData.put(count.getAndIncrement(), playable.toJSONObject());
            }

            data.put("q", tracksData);
        }

        stationClient.sendEvent(event, data);
    }

    public static void handleTrackUpdateEvent(String event, String userId, AudioPlayback audioPlayback) {
        StationClient stationClient = StationClientManager.getStationClientByUser(userId);
        if (stationClient == null) return;

        JSONObject data = handleMutatorData(audioPlayback);

        data.put("positionMs", audioPlayback.getCurrentPositionMs());
        data.put("position", audioPlayback.getAudioQueue().getPosition());

        stationClient.sendEvent(event, data);
    }
}
