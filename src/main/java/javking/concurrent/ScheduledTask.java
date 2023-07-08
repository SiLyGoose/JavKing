package javking.concurrent;

import javking.audio.AudioPlayback;
import javking.discord.listeners.VoiceUpdateListener;
import javking.rest.controllers.StationClient;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledTask {
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private boolean inProgress = false;

    public void startScheduledTask(AudioPlayback audioPlayback, StationClient stationClient) {
        setInProgress(true);
        executorService = Executors.newSingleThreadScheduledExecutor();

        scheduledFuture = executorService.scheduleAtFixedRate(sendTimeUpdate(audioPlayback, stationClient), 5, 10, TimeUnit.SECONDS);
    }

    public void stopScheduledTask() {
        setInProgress(false);
        if (scheduledFuture != null) scheduledFuture.cancel(false);

        if (executorService != null) executorService.shutdown();
    }

    public Runnable sendTimeUpdate(AudioPlayback audioPlayback, StationClient stationClient) {
        return () -> {
            JSONObject data = new JSONObject();
            data.put("positionMs", audioPlayback.getCurrentPositionMs());
            VoiceUpdateListener.sendEvent("timeUpdate", stationClient, data);
        };
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }
}
