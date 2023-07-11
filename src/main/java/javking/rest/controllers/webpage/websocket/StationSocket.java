package javking.rest.controllers.webpage.websocket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class StationSocket {
    private final UUID token;
    private final String stationId, socketId;

    public StationSocket(String token, String stationId) {
        this(token, stationId, null);
    }

    public StationSocket(String token, String stationId, String socketId) {
        this.token = UUID.fromString(token);
        this.stationId = stationId;
        this.socketId = socketId;
    }

    public UUID getToken() {
        return token;
    }

    public String getStationId() {
        return stationId;
    }

    public String getSocketId() {
        return socketId;
    }

    @JsonCreator
    public static StationSocket createStationSocket(@JsonProperty("token") String token,
                                                    @JsonProperty("stationId") String stationId,
                                                    @JsonProperty("socketId") String socketId) {
        return new StationSocket(token, stationId, socketId);
    }
}
