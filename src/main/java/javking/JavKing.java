package javking;

import javking.audio.AudioManager;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.commands.manager.CommandManager;
import javking.database.MongoManager;
import javking.database.MongoService;
import javking.discord.GuildManager;
import javking.discord.MessageService;
import javking.util.Spotify.SpotifyComponent;
import javking.util.Spotify.login.LoginManager;
import javking.util.YouTube.YouTubeAudioSourceManager;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;

public class JavKing {

    public static final Logger LOGGER = LoggerFactory.getLogger(JavKing.class);

    private static JavKing instance;

    private final AudioManager audioManager;
    private final CommandExecutionQueueManager executionQueueManager;
    private final CommandManager commandManager;
    private final GuildManager guildManager;
    private final MongoManager mongoManager;
//    listeners for discord events
    private final ListenerAdapter[] registeredListeners;
    private final LoginManager loginManager;
    private final MessageService messageService;
    private final ShardManager shardManager;
    private final SpotifyApi.Builder spotifyApiBuilder;
    private final SpotifyComponent spotifyComponent;
    private final YouTubeAudioSourceManager youTubeAudioSourceManager;

    public JavKing(AudioManager audioManager,
                   CommandExecutionQueueManager executionQueueManager,
                   CommandManager commandManager,
                   GuildManager guildManager,
                   LoginManager loginManager,
                   MongoManager mongoManager,
                   MessageService messageService,
                   ShardManager shardManager,
                   SpotifyApi.Builder spotifyApiBuilder,
                   SpotifyComponent spotifyComponent,
                   YouTubeAudioSourceManager youTubeAudioSourceManager,
                   ListenerAdapter... listenerAdapters
    ) {
        this.audioManager = audioManager;
        this.executionQueueManager = executionQueueManager;
        this.commandManager = commandManager;
        this.guildManager = guildManager;
        this.loginManager = loginManager;
        this.mongoManager = mongoManager;
        this.messageService = messageService;
        this.shardManager = shardManager;
        this.spotifyApiBuilder = spotifyApiBuilder;
        this.spotifyComponent = spotifyComponent;
        this.youTubeAudioSourceManager = youTubeAudioSourceManager;
        this.registeredListeners = listenerAdapters;

        instance = this;
    }

    public static JavKing get() {
        if (instance == null) throw new IllegalStateException("JavKing not set up");
        return instance;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public CommandExecutionQueueManager getExecutionQueueManager() {
        return executionQueueManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public MongoManager getMongoManager() {
        return mongoManager;
    }

    public MongoService getMongoService() {
        return getMongoManager().getMongoService();
    }

    public ListenerAdapter[] getRegisteredListeners() {
        return registeredListeners;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public SpotifyApi.Builder getSpotifyApiBuilder() {
        return spotifyApiBuilder;
    }

    public SpotifyComponent getSpotifyComponent() {
        return spotifyComponent;
    }

    public YouTubeAudioSourceManager getYouTubeAudioSourceManager() {
        return youTubeAudioSourceManager;
    }

    public void registerListeners() {
        ShardManager shardManager = this.getShardManager();
        ListenerAdapter[] registerListeners = this.getRegisteredListeners();
        shardManager.addEventListener((Object[]) registerListeners);
        shardManager.setStatus(OnlineStatus.DO_NOT_DISTURB);
        LOGGER.info("Registered listeners");
    }
}
