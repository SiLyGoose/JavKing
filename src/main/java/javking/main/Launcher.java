package javking.main;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.commands.manager.CommandExecutionQueueManager;
import javking.commands.manager.CommandManager;
import javking.database.MongoManager;
import javking.discord.GuildManager;
import javking.discord.MessageService;
import javking.discord.listeners.CommandListener;
import javking.discord.listeners.VoiceUpdateListener;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.util.PropertiesLoadingService;
import javking.util.Spotify.SpotifyComponent;
import javking.util.Spotify.login.LoginManager;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubeAudioSourceManager;
import javking.util.YouTube.YouTubeService;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;

import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.*;

public class Launcher {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Launcher.class);

        try {
            String prefix = PropertiesLoadingService.loadProperty("PREFIX");
            String discordToken = PropertiesLoadingService.loadProperty("TOKEN");
            String youTubeCredentials = PropertiesLoadingService.loadProperty("V3_API_KEY");

            String email = PropertiesLoadingService.loadProperty("G-EMAIL");
            String password = PropertiesLoadingService.loadProperty("G-PASSWORD");

            EnumSet<GatewayIntent> gatewayIntents = EnumSet
                    .of(GUILD_MESSAGES, GUILD_VOICE_STATES, MESSAGE_CONTENT, GUILD_EMOJIS_AND_STICKERS, DIRECT_MESSAGES);
            DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder
                    .create(discordToken, gatewayIntents)
                    .disableCache(EnumSet.of(
                            ACTIVITY,
                            CLIENT_STATUS,
                            ONLINE_STATUS,
                            MEMBER_OVERRIDES,
                            FORUM_TAGS,
                            ROLE_TAGS,
                            CacheFlag.SCHEDULED_EVENTS
                    ))
                    .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                    .setStatus(OnlineStatus.ONLINE)
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .setEventPassthrough(true)
                    .setAutoReconnect(true);

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory gsonFactory = new GsonFactory();
            YouTube youTube = new YouTube.Builder(httpTransport, gsonFactory, httpRequest -> {
            }).setApplicationName("javking-youtube-search").build();
            YouTubeService youTubeService = new YouTubeService(youTube, youTubeCredentials);
            YouTubeAudioSourceManager youTubeAudioSourceManager = new YouTubeAudioSourceManager(email, password);

            SpotifyComponent spotifyComponent = new SpotifyComponent();
//            System.out.println(spotifyComponent.clientCredentialFlow());
            SpotifyApi.Builder spotifyApiBuilder = spotifyComponent.spotifyApiBuilder();

            MessageService messageService = new MessageService();

            CommandManager commandManager = new CommandManager();
            GuildManager guildManager = new GuildManager();
            LoginManager loginManager = new LoginManager();

            AudioManager audioManager = new AudioManager(youTubeService, guildManager);
            CommandExecutionQueueManager executionQueueManager = new CommandExecutionQueueManager();

            MongoManager mongoManager = new MongoManager();

            ShardManager shardManager = shardManagerBuilder.build();
            CommandListener commandListener = new CommandListener(executionQueueManager, commandManager,
                    guildManager, messageService, spotifyApiBuilder);

            VoiceUpdateListener voiceUpdateListener = new VoiceUpdateListener();

            JavKing javking = new JavKing(audioManager, executionQueueManager, commandManager, guildManager,
                    loginManager, mongoManager, messageService, shardManager, spotifyApiBuilder, spotifyComponent,
                    youTubeAudioSourceManager, commandListener, voiceUpdateListener);

            javking.registerListeners();

            shardManager.setStatus(OnlineStatus.ONLINE);
            assert prefix != null;
            shardManager.setActivity(Activity.listening(prefix + "help"));
//            await all shards' ready status before continuing
            shardManager.getShards().parallelStream().forEach(jda -> {
                try {
                    jda.awaitReady();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                guildManager.init(jda.getGuilds());
            });

            commandManager.init(messageService);

            logger.info("Startup completed");

//            JSONParser jsonParser = new JSONParser();
//            try (FileReader reader = new FileReader("./queue.save")) {
//                Object baseObject = jsonParser.parse(reader);
//
//                JSONObject parentObject = (JSONObject) baseObject;
//                parentObject.forEach((guildId, childObject) -> {
//                    Guild guild = shardManager.getGuildById((String) guildId);
//                    MessageChannel messageChannel = guild.getTextChannelById((String) ((JSONObject) childObject).get("channelId"));
//                    VoiceChannel voiceChannel = guild.getVoiceChannelById((String) ((JSONObject) childObject).get("voiceId"));
//
//                    AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);
//                    audioPlayback.add
//                });
//            }

            String filePath = PropertiesLoadingService.requireProperty("QUEUE_SAVE_PATH");
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(filePath);
            } catch (FileNotFoundException ignored) {
                if (new File(filePath).createNewFile()) {
                    logger.info("Created queue.save file!");
                } else logger.warn("File " + filePath + " already exists!");

                fileInputStream = new FileInputStream(filePath);
            }

            try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                @SuppressWarnings("unchecked")
                HashMap<String, HashMap<AudioManager, AudioPlayback>> objectStream = (HashMap<String,
                        HashMap<AudioManager, AudioPlayback>>) objectInputStream.readObject();

                objectStream.forEach((guildId, audioStream) -> {
                    Guild guild = shardManager.getGuildById(guildId);
                    assert guild != null;

                    AudioManager audioManagerObject = (AudioManager) audioStream.keySet().toArray()[0];

                    AudioPlayback audioPlaybackObject = audioStream.get(audioManagerObject);
                    AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                    MessageChannel messageChannel = guild.getTextChannelById(audioPlaybackObject.getChannelId());
                    VoiceChannel voiceChannel = guild.getVoiceChannelById(audioPlaybackObject.getVoiceId());

                    audioManager.initializeConnection(audioPlayback, guild, messageChannel, voiceChannel);
                    audioManager.checkConnection(audioPlayback);

                    AudioQueue audioQueueObject = audioPlaybackObject.getAudioQueue();

                    List<Playable> playables = audioQueueObject.getTracks();
                    for (int i = 0; i < playables.size(); i++) {
                        HollowYouTubeVideo video = (HollowYouTubeVideo) playables.get(i);
                        try {
                            playables.set(i, youTubeService.resolveYouTubeVideo(video.getPlaybackUrl(),
                                    video.getUserContext(), guild));
                        } catch (IOException | UnavailableResourceException e) {
                            e.printStackTrace();
                        }
                    }

                    audioPlayback.add(playables);

                    audioManager.startPlaying(guild, false);
                });

//                  clears the file
//                  removed to keep GuildContext variables alive
                new FileOutputStream(filePath).close();
            } catch (StreamCorruptedException e) {
                logger.error("Exception in byte to object stream. Terminated.", e);
                System.exit(1);
            } catch (EOFException | ClassNotFoundException ignored) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ErrorResponseException e) {
//            handle somehow
        } catch (Throwable e) {
            logger.error("Exception in launcher. Terminated.", e);
            System.exit(1);
        }
    }
}
