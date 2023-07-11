package javking.rest.controllers.webpage.websocket;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

@Component
public class StationClientManager {
    private static final long serialVersionUID = 62L;

    private static final HashMap<UUID, StationClient> stationClients = new HashMap<>();
    private static final HashMap<String, UUID> stationIdentifiers = new HashMap<>();

    public static boolean hasStationClient(String stationId) {
        return hasStationClient(getIdentifier(stationId));
    }

    public static boolean hasStationClient(UUID uuid) {
        return stationClients.containsKey(uuid) || stationIdentifiers.containsValue(uuid);
    }

    public static StationClient getStationClientByUser(String userId) {
        Optional<UUID> optionalUuid = getUserIdentifier(userId);
        return optionalUuid.map(StationClientManager::getStationClient).orElse(null);
    }

    public static StationClient getStationClient(String stationId) {
        return getStationClient(getIdentifier(stationId));
    }

    public static StationClient getStationClient(UUID uuid) {
        return stationClients.get(uuid);
    }

    public static StationClient removeStationClient(String stationId) {
        return removeStationClient(getIdentifier(stationId));
    }

    public static StationClient removeStationClient(UUID uuid) {
        stationIdentifiers.entrySet().parallelStream()
                .filter(entry -> entry.getValue().equals(uuid))
                .findFirst()
                .ifPresent(entry -> stationIdentifiers.remove(entry.getKey()));
        return stationClients.remove(uuid);
    }

    public static StationClient setStationClient(String stationId, StationClient stationClient) {
        return setStationClient(getIdentifier(stationId), stationClient);
    }

    public static StationClient setStationClient(UUID uuid, StationClient stationClient) {
        stationIdentifiers.put(stationClient.getStationId(), uuid);
        stationClients.put(uuid, stationClient);
        return getStationClient(uuid);
    }

    public static UUID getIdentifier(String stationId) {
        return stationIdentifiers.get(stationId);
    }

    public static Optional<UUID> getUserIdentifier(String userId) {
        return stationIdentifiers.entrySet().parallelStream()
                .filter(entry -> entry.getKey().equals(userId))
                .findFirst()
                .map(Map.Entry::getValue);
    }
}
