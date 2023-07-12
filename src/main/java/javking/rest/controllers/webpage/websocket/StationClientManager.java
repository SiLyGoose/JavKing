package javking.rest.controllers.webpage.websocket;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class StationClientManager {
    private static final long serialVersionUID = 62L;

    //  token, StationClient
    private static final HashMap<UUID, StationClient> stationClients = new HashMap<>();
    //  { stationId, userId }, token
    private static final HashMap<StationIdentifier, UUID> stationIdentifiers = new HashMap<>();

    public static boolean hasStationClient(String stationId) {
        return hasStationClient(getByGuildIdentifier(stationId));
    }

    public static boolean hasStationClient(UUID uuid) {
        return stationClients.containsKey(uuid) || stationIdentifiers.containsValue(uuid);
    }

    public static StationClient getStationClientByUser(String userId) {
        return getByUserIdentifier(userId);
    }

    public static StationClient getStationClientByGuild(String stationId) {
        return getStationClient(getByGuildIdentifier(stationId));
    }

    public static StationClient getStationClient(UUID uuid) {
        return stationClients.get(uuid);
    }

    public static StationClient removeStationClient(String stationId, String socketId) {
        return removeStationClient(getByGuildIdentifier(stationId), socketId);
    }

    public static StationClient removeStationClient(UUID uuid, String socketId) {
        List<Map.Entry<StationIdentifier, UUID>> removalList = stationIdentifiers.entrySet()
                .stream()
                .filter(entry -> {
                    StationIdentifier key = entry.getKey();
                    UUID value = entry.getValue();
                    return value.equals(uuid) && (key.getSocketId() == null || key.getSocketId().equals(socketId));
                })
                .collect(Collectors.toList());

        removalList.forEach(entry -> stationIdentifiers.remove(entry.getKey()));
        return stationClients.remove(uuid);
    }

    public static StationClient setStationClient(String stationId, StationClient stationClient) {
        return setStationClient(getByGuildIdentifier(stationId), stationClient);
    }

    public static StationClient setStationClient(UUID uuid, StationClient stationClient) {
        StationIdentifier stationIdentifier = new StationIdentifier(stationClient.getStationId(), stationClient.getUserId(), stationClient.getSocketId());

        stationIdentifiers.put(stationIdentifier, uuid);
        stationClients.put(uuid, stationClient);

        System.out.println(stationClients.size());

        return getStationClient(uuid);
    }

    public static UUID getByGuildIdentifier(String stationId) {
        return stationIdentifiers.entrySet()
                .parallelStream()
                .filter(entry -> entry.getKey().getStationId().equals(stationId))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static StationClient getByUserIdentifier(String userId) {
        return stationClients.entrySet().parallelStream()
                .filter(entry -> entry.getValue().getUserId().equals(userId))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static void closeStationTasks(String stationId) {
        stationClients.values().parallelStream()
                .filter(client -> client.getStationId().equals(stationId))
                .forEach(client -> client.getScheduledTask().stopScheduledTask());
    }

    static class StationIdentifier {
        private final String userId, stationId, socketId;

        public StationIdentifier(String stationId, String userId, String socketId) {
            this.userId = userId;
            this.stationId = stationId;
            this.socketId = socketId;
        }

        public String getStationId() {
            return stationId;
        }

        public String getUserId() {
            return userId;
        }

        public String getSocketId() {
            return socketId;
        }
    }
}
