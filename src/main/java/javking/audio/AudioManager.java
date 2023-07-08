package javking.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import javking.JavKing;
import javking.audio.exec.TrackLoadingExecutor;
import javking.audio.handlers.AudioPlayerSendHandler;
import javking.discord.GuildManager;
import javking.exceptions.MessageChannelException;
import javking.exceptions.VoiceChannelException;
import javking.exceptions.handlers.LoggingExceptionHandler;
import javking.models.command.CommandContext;
import javking.templates.Templates;
import javking.util.Spotify.SpotifyService;
import javking.util.YouTube.YouTubeService;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioManager implements Serializable {
    private static final long serialVersionUID = 10L;

    private final GuildManager guildManager;
    private final AudioTrackLoader audioTrackLoader;

    private ConnectionStatus connectionStatus;

    private transient final YouTubeService youTubeService;
    private transient final ExecutorService executorService;
    private transient final AudioPlayerManager playerManager;
    private transient final Logger logger;

    public AudioManager(YouTubeService youTubeService, GuildManager guildManager) {
        playerManager = new DefaultAudioPlayerManager();
        this.youTubeService = youTubeService;
        this.guildManager = guildManager;
        this.logger = LoggerFactory.getLogger(getClass());

        executorService = Executors.newFixedThreadPool(5, r -> {
            Thread thread = new Thread(r);
            thread.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            return thread;
        });

        audioTrackLoader = new AudioTrackLoader(playerManager);
        AudioSourceManagers.registerRemoteSources(playerManager);
        guildManager.setAudioManager(this);
        YoutubeAudioSourceManager youtubeAudioSourceManager = playerManager.source(YoutubeAudioSourceManager.class);
//        100 videos per page. 100 * 50 = 5000 videos limit
//        reduced to 1000 video limit
        youtubeAudioSourceManager.setPlaylistPageCount(10);
    }

    /**
     *
     * @return false if user in different voice channel or not prepared
     */
    public boolean preparedConnection(CommandContext context, Guild guild) {
        AudioPlayback playback = getPlaybackForGuild(guild);
        net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();

        try {
            return playback.getVoiceChannel().equals(context.getVoiceChannel()) || audioManager.getSendingHandler() != null;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void initializeConnection(Guild guild, MessageChannel messageChannel, VoiceChannel voiceChannel) {
        initializeConnection(getPlaybackForGuild(guild), guild, messageChannel, voiceChannel);
    }

    public void initializeConnection(AudioPlayback audioPlayback, Guild guild, MessageChannel messageChannel, VoiceChannel voiceChannel) {
        net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();

        audioPlayback.setMessageChannel(messageChannel);
        audioPlayback.setVoiceChannel(voiceChannel);

        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayback.getAudioPlayer()));
        try {
            audioManager.openAudioConnection(voiceChannel);
        } catch (InsufficientPermissionException e) {
            setConnectionStatus(ConnectionStatus.NOT_CONNECTED);
            throw new VoiceChannelException("I do not have permission to join this voice channel");
        }

        setConnectionStatus(ConnectionStatus.CONNECTED);
        JavKing.get().getMessageService().sendBold(Templates.command.check_mark.formatFull(String.format("Connected to `%s` and bound to `%s`", voiceChannel.getName(), messageChannel.getName())), messageChannel);
    }

    public void checkConnection(Guild guild) {
        checkConnection(getPlaybackForGuild(guild));
    }

    public void checkConnection(AudioPlayback playback) {
        if (playback.getChannel() == null)
            throw new MessageChannelException("Unable to set up text channel to message.");
        if (playback.getVoiceChannel() == null)
            throw new VoiceChannelException("Unable to set up voice channel to join.");

        assert connectionStatus == ConnectionStatus.CONNECTED;
    }

    private void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void startPlaying(Guild guild, boolean resumePaused) {
        AudioPlayback playback = getPlaybackForGuild(guild);

        QueueIterator iterator = new QueueIterator(playback, this);
        playback.setCurrentQueueIterator(iterator);

        if (playback.isPaused() || resumePaused) playback.resume();
        else iterator.playNext();
    }

    public YouTubeService getYouTubeService() {
        return youTubeService;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public PlayableFactory createPlayableFactory(SpotifyService spotifyService, TrackLoadingExecutor trackLoadingExecutor) {
        return new PlayableFactory(audioTrackLoader, spotifyService, trackLoadingExecutor, youTubeService);
    }

    public AudioPlayback getPlaybackForGuild(Guild guild) {
        return guildManager.getContextForGuild(guild).getAudioPlayback();
    }

    public AudioQueue getQueue(Guild guild) {
        return getPlaybackForGuild(guild).getAudioQueue();
    }
}
