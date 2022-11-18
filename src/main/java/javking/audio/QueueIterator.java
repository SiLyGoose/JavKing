package javking.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import javking.JavKing;
import javking.discord.MessageService;
import javking.exceptions.UnavailableResourceException;
import javking.exceptions.handlers.LoggingExceptionHandler;
import javking.models.music.Playable;
import javking.templates.Templates;
import javking.util.YouTube.HollowYouTubeVideo;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
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

    private static final ThreadPoolExecutor AUDIO_EVENT_POOL = new ThreadPoolExecutor(3, 50, 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    private transient static final Map<String, ScheduledFuture<?>> scheduleMap = new ConcurrentHashMap<>();

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
        ScheduledFuture<?> scheduledFuture = scheduleMap.get(audioPlayback.getGuild().getId());
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason reason) {
        if (reason.mayStartNext) {
            if (reason == AudioTrackEndReason.LOAD_FAILED) {
                iterateQueue(audioPlayback, queue, true);
            } else {
                iterateQueue(audioPlayback, queue);
            }
        }
    }

    AudioItem loadByIdentifier(String identifier) {
        return audioTrackLoader.loadByIdentifier(identifier);
    }

    void playNext() {
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
//                dk diff between getId and getPlaybackUrl
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
                audioPlayback.getAudioPlayer().playTrack(audioTrack);
                if (audioPlayback.isAnnounceSongs()) {
                    try {
                        audioManager.getYouTubeService().announceSong((HollowYouTubeVideo) track, audioPlayback, queue, messageService);
                    } catch (UnavailableResourceException ignored) {

                    }
                }
                currentlyPlaying = track;
            }
        } else iterateQueue(audioPlayback, queue);

//         always false?
//        if (track == null) audioPlayback.leaveChannel();
    }

    private void iterateQueue(AudioPlayback playback, AudioQueue queue) {
        iterateQueue(playback, queue, false);
    }

    private void iterateQueue(AudioPlayback playback, AudioQueue queue, boolean ignoreRepeat) {
        if (!queue.isRepeatOne() || ignoreRepeat) {
            if (queue.hasNext()) {
                queue.iterate();
                playNext();
            } else {
                queue.forceClear();
//                start timer for 5 min and leave channel if no songs played in the meantime
                ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
                scheduler.setRemoveOnCancelPolicy(true);

                scheduleMap.put(playback.getGuild().getId(), scheduler.schedule(playback::leaveChannel, 5, TimeUnit.MINUTES));
            }
        } else {
            playNext();
        }
    }
}
