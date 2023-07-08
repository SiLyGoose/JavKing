package javking.rest.controllers;

import com.corundumstudio.socketio.SocketIOClient;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.concurrent.ScheduledTask;
import javking.models.command.CommandContext;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Duration;
import java.util.UUID;

public class StationClient {
    private final String stationId;
    private Guild guild;
    private SocketIOClient client;
    private CommandContext context;
    private CommandExecutionQueueManager executionQueueManager;
    private AudioManager audioManager;
    private final UUID uuid;

    private final RateLimiterConfig config;
    private final RateLimiter rateLimiter;

    private final ScheduledTask scheduledTask;

    public StationClient(String stationId, SocketIOClient client) {
        this(stationId, client.getSessionId());
        setClient(client);
    }

    public StationClient(String stationId, UUID uuid) {
        this.stationId = stationId;
        setGuild(JavKing.get().getShardManager().getGuildById(stationId));
        this.uuid = uuid;

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

    public SocketIOClient getClient() {
        return client;
    }

    public StationClient setClient(SocketIOClient client) {
        this.client = client;
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

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public ScheduledTask getScheduledTask() {
        return scheduledTask;
    }
}
