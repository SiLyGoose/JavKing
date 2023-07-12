package javking.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.exceptions.UnavailableResourceException;
import javking.models.command.CommandContext;
import javking.models.music.Playable;
import javking.rest.controllers.webpage.websocket.StationClient;
import javking.rest.controllers.webpage.websocket.StationClientManager;
import javking.util.YouTube.YouTubePlaylist;
import javking.util.YouTube.YouTubeVideo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioPlayback implements Serializable {
    private static final long serialVersionUID = 11L;

    private final AudioQueue audioQueue;
    private String voiceId, channelId;
    private Message lastPlaybackNotification;
    private QueueIterator currentQueueIterator;
    private boolean announceSongs = false;
    private boolean isDjOnly = false;

    private transient VoiceChannel voiceChannel;
    private transient MessageChannel messageChannel;
    private transient final Guild guild;
    private transient final AudioPlayer audioPlayer;
    private transient final Logger logger;

    @Nullable
    private LocalDateTime aloneSince, lastPlayedSince;

    public AudioPlayback(AudioPlayer player, Guild guild) {
        this.guild = guild;
        audioPlayer = player;
        this.logger = LoggerFactory.getLogger(getClass());
        audioQueue = new AudioQueue();
    }

    public void add(YouTubeVideo video) {
        add(List.of(video));
    }

    public void add(YouTubePlaylist playlist) {
        add(new ArrayList<>(playlist.getVideos()));
    }

    public void add(Collection<Playable> playables) {
        AtomicInteger count = new AtomicInteger(0);
        JSONArray tracksData = new JSONArray();

        StationClient stationClient = StationClientManager.getStationClientByGuild(guild.getId());

        playables.parallelStream().forEachOrdered(playable -> {
            try {
                audioQueue.add(playable);

                tracksData.put(count.getAndIncrement(), playable.toJSONObject());
            } catch (UnavailableResourceException ignored) {
//                unable to retrieve playable values
                ignored.printStackTrace();
//                VoiceUpdateListener.sendEvent("activityLogUpdate", stationClient, playable);
            }
        });

        if (stationClient == null) return;

        JSONObject data = new JSONObject();
        data.put("q", tracksData);

        stationClient.sendEvent("tracksAdded", data);
    }

    public void remove() {
        remove(0);
    }

    public void remove(int index) {
        remove(List.of(index));
    }

    public void remove(List<Integer> indices) {
        AtomicInteger offset = new AtomicInteger(0);
        indices.forEach(index -> {
            AudioQueue audioQueue = getAudioQueue();
            Playable playable = audioQueue.getTrack(index - offset.getAndIncrement());
            int trackIndex = audioQueue.getTracks().indexOf(playable);

            audioQueue.remove(trackIndex);
        });
    }

    public boolean isDjOnly() {
        return isDjOnly;
    }

    public void setDjOnly(boolean isDjOnly) {
        this.isDjOnly = isDjOnly;
    }

    public boolean isPlaying() {
        return !isPaused() && audioPlayer.getPlayingTrack() != null;
    }

    public void pause() {
        audioPlayer.setPaused(true);
    }

    public void resume() {
        audioPlayer.setPaused(false);
    }

    public void togglePaused(boolean paused) {
        audioPlayer.setPaused(paused);
    }

    public boolean isPaused() {
        return audioPlayer.isPaused() && audioPlayer.getPlayingTrack() != null;
    }

    public void stop() {
        audioPlayer.stopTrack();
        setLastPlaybackNotification(null);
        leaveChannel();
    }

    public Guild getGuild() {
        return guild;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public AudioQueue getAudioQueue() {
        return audioQueue;
    }

    public VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceChannel(VoiceChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
        this.voiceId = voiceChannel.getId();
    }

    public MessageChannel getChannel() {
        return messageChannel;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setMessageChannel(MessageChannel messageChannel) {
        this.messageChannel = messageChannel;
        this.channelId = messageChannel.getId();
    }

//    public void shuffle() {
//        audioQueue.shuffle();
//    }

    public boolean isShuffled() {
        return audioQueue.isShuffled();
    }

    public void setShuffled(boolean shuffled) {
        audioQueue.setShuffled(shuffled);
    }

    public boolean isRepeatOne() {
        return audioQueue.isRepeatOne();
    }

    public void setRepeatOne(boolean repeatOne) {
        audioQueue.setRepeatOne(repeatOne);
    }

    public boolean isRepeatAll() {
        return audioQueue.isRepeatAll();
    }

    public void setRepeatAll(boolean repeatAll) {
        audioQueue.setRepeatAll(repeatAll);
    }

    public long getCurrentPositionMs() {
        AudioTrack playingTrack = audioPlayer.getPlayingTrack();
        return playingTrack != null ? playingTrack.getPosition() : 0;
    }

    public void setCurrentPositionMs(long ms) {
        audioPlayer.getPlayingTrack().setPosition(ms);
    }

    public int getVolume() {
        return audioPlayer.getVolume();
    }

    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    public boolean joinChannel(AudioManager audioManager, CommandContext context) {
        Guild guild = context.getGuild();
        MessageChannel messageChannel = context.getChannel();
        VoiceChannel voiceChannel = context.getVoiceChannel();

        if (!audioManager.preparedConnection(context, guild)) {
            audioManager.initializeConnection(guild, messageChannel, voiceChannel);
            audioManager.checkConnection(guild);
        }
        return true;
    }

    public boolean leaveChannel() {
        if (voiceChannel != null) {
            guild.getAudioManager().closeAudioConnection();
            voiceChannel = null;
        }

        if (messageChannel != null) messageChannel = null;

        return true;
    }

    public void clear(boolean force) {
        setVolume(100);
        setRepeatOne(false);
        setRepeatAll(false);
        setShuffled(false);

        if (!audioQueue.isEmpty() && !force) audioQueue.clear();
        else audioQueue.forceClear();
        if (!isPaused()) audioPlayer.stopTrack();
    }

    public void setLastPlaybackNotification(Message message) {
        if (lastPlaybackNotification != null) {
            try {
                lastPlaybackNotification.delete().queue();
            } catch (Throwable e) {
                OffsetDateTime timeCreated = lastPlaybackNotification.getTimeCreated();
                ZonedDateTime zonedDateTime = timeCreated.atZoneSameInstant(ZoneId.systemDefault());
                logger.warn(String.format("Cannot delete playback notification message from %s for channel %s on guild %s",
                        zonedDateTime, messageChannel, guild), e);
            }
        }
        this.lastPlaybackNotification = message;
    }

    public void setAnnounceSongs(boolean announceSongs) {
        this.announceSongs = announceSongs;
    }

    public boolean isAnnounceSongs() {
        return announceSongs;
    }

    public QueueIterator getCurrentQueueIterator() {
        return currentQueueIterator;
    }

    public void setCurrentQueueIterator(QueueIterator queueIterator) {
        if (currentQueueIterator != null) {
            audioPlayer.removeListener(currentQueueIterator);
            currentQueueIterator.setReplaced();
        }

        currentQueueIterator = queueIterator;
        audioPlayer.addListener(queueIterator);
    }

    @Nullable
    public LocalDateTime getAloneSince() {
        return aloneSince;
    }

    public void setAloneSince(@Nullable LocalDateTime aloneSince) {
        this.aloneSince = aloneSince;
    }

    @Nullable
    public LocalDateTime getLastPlayedSince() {
        return lastPlayedSince;
    }

    public void setLastPlayedSince(@Nullable LocalDateTime lastPlayedSince) {
        this.lastPlayedSince = lastPlayedSince;
    }
}
