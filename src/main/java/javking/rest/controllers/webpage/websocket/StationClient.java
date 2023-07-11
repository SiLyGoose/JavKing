package javking.rest.controllers.webpage.websocket;

import com.corundumstudio.socketio.SocketIOClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.concurrent.ScheduledTask;
import javking.models.command.CommandContext;
import javking.rest.payload.uuid.UUIDDeserializer;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class StationClient {
    private String userId;
    private final String stationId;
    private Guild guild;
    private CommandContext context;
    private CommandExecutionQueueManager executionQueueManager;
    private AudioManager audioManager;

    private final UUID uuid;
    private final String socketId;
    private final RateLimiterConfig config;
    private final RateLimiter rateLimiter;

    private final ScheduledTask scheduledTask;

    public StationClient(StationSocket stationSocket) {
        this(null, stationSocket.getToken(), stationSocket.getStationId(), stationSocket.getSocketId());
    }

    public StationClient(String userId, UUID uuid, String stationId, String socketId) {
        this.userId = userId;
        this.stationId = stationId;
        setGuild(JavKing.get().getShardManager().getGuildById(stationId));
        this.uuid = uuid;

        this.socketId = socketId;

        config = RateLimiterConfig.custom()
                .limitForPeriod(1) // max number of calls allowed in time period
                .limitRefreshPeriod(Duration.ofSeconds(2)) // time period for rate limit
                .build();

        rateLimiter = RateLimiter.of("stationRateLimiter", config);

        scheduledTask = new ScheduledTask();
    }

    public String getStationId() {
        return stationId;
    }

    public Guild getGuild() {
        return guild;
    }

    public StationClient setGuild(Guild guild) {
        this.guild = guild;
        return this;
    }

    public CommandContext getContext() {
        return context;
    }

    public StationClient setContext(CommandContext context) {
        this.context = context;
        return this;
    }

    public CommandExecutionQueueManager getExecutionQueueManager() {
        return executionQueueManager;
    }

    public StationClient setExecutionQueueManager(CommandExecutionQueueManager executionQueueManager) {
        this.executionQueueManager = executionQueueManager;
        return this;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public StationClient setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getSocketId() {
        return socketId;
    }

    public StationClient setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public ScheduledTask getScheduledTask() {
        return scheduledTask;
    }

    public void sendEvent(String op, JSONObject data) {
        try {
            URL url = URI.create(PropertiesLoadingService.requireProperty("CLIENT_SOCKET")).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);
            connection.setDoInput(true);

            JSONObject auth = new JSONObject();
            auth.put("token", uuid.toString());
            auth.put("stationId", stationId);
            auth.put("socketId", socketId);

            JSONObject POSTdata = new JSONObject();
            POSTdata.put("op", op);
            POSTdata.put("jAuth", auth);
            POSTdata.put("data", data);

            String requestBody = POSTdata.toString();
            System.out.println(requestBody);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            System.out.println(responseCode);
            connection.disconnect();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }
}
