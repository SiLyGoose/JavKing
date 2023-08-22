package javking.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import javking.JavKing;
import javking.concurrent.ScheduledTask;
import javking.discord.MessageService;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.UnavailableResourceException;
import javking.exceptions.handlers.LoggingExceptionHandler;
import javking.models.music.Playable;
import javking.rest.controllers.webpage.websocket.StationClient;
import javking.rest.controllers.webpage.websocket.StationClientManager;
import javking.templates.Templates;
import javking.util.YouTube.HollowYouTubeVideo;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class QueueIterator extends AudioEventAdapter implements Serializable {
    private static final long serialVersionUID = 14L;

    private final AudioPlayback audioPlayback;
    private final AudioQueue queue;
    private final AudioManager audioManager;
    private final AudioTrackLoader audioTrackLoader;

    private transient final MessageService messageService;
    private transient Playable currentlyPlaying;

    private transient volatile boolean isReplaced;

    private static final ThreadPoolExecutor AUDIO_EVENT_POOL = new ThreadPoolExecutor(3, 50, 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    //    map of guilds and amt of time lingering in voice channel before leaving
    private transient static final Map<String, ScheduledFuture<?>> lingerDelayMap = new ConcurrentHashMap<>();

    private transient final AtomicInteger attemptCount = new AtomicInteger(0);

    QueueIterator(AudioPlayback audioPlayback, AudioManager audioManager) {
        AUDIO_EVENT_POOL.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("audio-event-pool-thread-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new LoggingExceptionHandler());
                return t;
            }
        });

        this.audioPlayback = audioPlayback;
        this.queue = audioPlayback.getAudioQueue();
        this.audioManager = audioManager;

        messageService = JavKing.get().getMessageService();
        audioTrackLoader = new AudioTrackLoader(audioManager.getPlayerManager());
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        handleAudioEvent(() -> {
            ScheduledFuture<?> scheduledFuture = lingerDelayMap.get(audioPlayback.getGuild().getId());
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }

            Playable current = track.getUserData(Playable.class);
            if (audioPlayback.isAnnounceSongs()) {
                try {
                    audioManager.getYouTubeService().announceSong((HollowYouTubeVideo) current, audioPlayback, queue, messageService);
                } catch (UnavailableResourceException ignored) {

                }
            }
            handleTrackEvent("trackStarted");
        });
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason reason) {
        if (reason.mayStartNext) {
            List<Member> members = audioPlayback.getVoiceChannel().getMembers();
            if (members.size() == 1 && members.contains(audioPlayback.getGuild().getSelfMember())) {
                audioPlayback.clear(true);
                return;
            }

            handleAudioEvent(() -> {
                if (reason == AudioTrackEndReason.LOAD_FAILED) {
                    iterateQueue(audioPlayback, queue, true);
                } else {
                    resetAttemptCount();
                    iterateQueue(audioPlayback, queue);
                }
                handleTrackEvent("trackEnded");
            });
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Throwable e = exception;
        while (e.getCause() != null) {
            e = e.getCause();
        }

        iterateQueue(audioPlayback, queue, true);
        if (attemptCount.get() == 10) {
            try {
                messageService.sendBold(Templates.command.boom.formatFull("Something went wrong during playback of `" + track.getInfo().title + "`"), audioPlayback.getChannel());
            } catch (NullPointerException ignored) {
            }
        }

        handleTrackEvent("trackException", e);
    }

    private void handleTrackEvent(String event) {
        handleTrackEvent(event, null);
    }

    private void handleTrackEvent(String event, @Nullable Throwable e) {
        StationClient stationClient = StationClientManager.getStationClientByGuild(audioPlayback.getGuild().getId());
        if (stationClient == null) return;

        if (e != null) {
            stationClient.sendEvent(event, new JSONObject().put("trackException", e.getMessage()));
        } else {
            JSONObject data = new JSONObject();
            data.put("id", audioPlayback.getGuild().getId());

//            JSONArray tracksData = new JSONArray();
//            AtomicInteger count = new AtomicInteger(0);
//            for (Playable playable : queue.getTracks()) {
//                try {
//                    tracksData.put(count.getAndIncrement(), playable.toJSONObject());
//                } catch (UnavailableResourceException ignored) {
//                    System.out.println("UNABLE TO CONVERT TO JSON OBJECT :: " + count.get());
//                }
//            }
//
//            data.put("q", tracksData);

//            JSONObject repeatData = new JSONObject();
//            repeatData.put("rO", audioPlayback.isRepeatOne());
//            repeatData.put("rA", audioPlayback.isRepeatAll());
//
//            data.put("r", repeatData);
//            data.put("shuffled", audioPlayback.isShuffled());
//            data.put("paused", audioPlayback.isPaused());
            int position = queue.getPosition();
            data.put("position", position);
            try {
                data.put("track", queue.getTrack(position).toJSONObject());
            } catch (UnavailableResourceException | CommandExecutionException ignored) {
            }

//            check if timeUpdate has begun. if not, start with 5s delay and 10s interval
            ScheduledTask task = stationClient.getScheduledTask();
            if (!task.isInProgress()) task.startScheduledTask(audioPlayback, stationClient);

            stationClient.sendEvent(event, data);
        }
    }

    AudioItem loadByIdentifier(String identifier) {
        return audioTrackLoader.loadByIdentifier(identifier);
    }

    void setReplaced() {
        isReplaced = true;
    }

    void playNext() {
        if (isReplaced) return;

        if (attemptCount.incrementAndGet() > 10) {
            messageService.sendBold(Templates.command.x_mark.formatFull("Too many unplayable tracks automatically skipped. You can continue skipping manually."), audioPlayback.getChannel());
            audioPlayback.stop();
            resetAttemptCount();
            return;
        }

        Playable track = queue.getCurrent();
        if (track == null) {
            audioManager.getPlaybackForGuild(audioPlayback.getGuild()).stop();
            return;
        }
        AudioItem result = null;
        AudioTrack cachedTracked = track.getCached();

        if (cachedTracked != null) result = cachedTracked.makeClone();
        if (result == null) {
            String playbackId;
            try {
//                replaced track.getId();
                playbackId = track.getPlaybackUrl();
            } catch (UnavailableResourceException e) {
                iterateQueue(audioPlayback, queue, true);
                return;
            }

            try {
                result = loadByIdentifier(playbackId);
            } catch (FriendlyException e) {
                iterateQueue(audioPlayback, queue, true);
                System.out.println("QueueIterator - handled");
                messageService.sendBold(Templates.command.x_mark.formatFull(e.getMessage() + " Skipping..."), audioPlayback.getChannel());
                return;
            }
        }

        if (result != null) {
            if (result instanceof AudioTrack) {
                AudioTrack audioTrack = (AudioTrack) result;
                track.setCached(audioTrack);
                audioTrack.setUserData(track);
                // Calling startTrack with the noInterrupt set to true will start the track only
                // if nothing is currently playing. If
                // something is playing, it returns false and does nothing. In that case the
                // player was already playing so this
                // track goes to the queue instead.
                try {
                    audioPlayback.getAudioPlayer().playTrack(audioTrack);
                    currentlyPlaying = track;
                } catch (FriendlyException e) {
                    messageService.sendBold(Templates.command.x_mark.formatFull(e.getMessage() + " Skipping..."), audioPlayback.getChannel());
                }
            }
        } else iterateQueue(audioPlayback, queue);

//         always false?
//        if (track == null) audioPlayback.leaveChannel();
    }

    private void iterateQueue(AudioPlayback playback, AudioQueue queue) {
        iterateQueue(playback, queue, false);
    }

    private void iterateQueue(AudioPlayback playback, AudioQueue queue, boolean ignoreRepeat) {
        if (isReplaced) return;

        if (!queue.isRepeatOne() || ignoreRepeat) {
            if (queue.hasNext()) {
                queue.iterate();
//                if (!queue.hasNext() && queue.isRepeatAll() && queue.isShuffle()) {
//                    queue.randomize();
//                }
                playNext();
            } else {
                queue.forceClear();
//                start timer for 5 min and leave channel if no songs played in the meantime
                ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
                scheduler.setRemoveOnCancelPolicy(true);

                lingerDelayMap.put(playback.getGuild().getId(), scheduler.schedule(() -> {
                    playback.leaveChannel();
//                        remove timeUpdate events since bot has left channel
                    StationClientManager.closeStationTasks(playback.getGuild().getId());
                }, 5, TimeUnit.MINUTES));
            }
        } else {
            playNext();
        }
    }

    private void resetAttemptCount() {
        attemptCount.set(0);
    }

    private void handleAudioEvent(Runnable runnable) {
        AUDIO_EVENT_POOL.execute(() -> {
            if (isReplaced) {
                return;
            }

            synchronized (this) {
                runnable.run();
            }
        });
    }
}
