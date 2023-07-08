package javking.database;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.connection.*;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.event.ConnectionPoolListener;
import javking.JavKing;
import javking.audio.AudioPlayback;
import javking.exceptions.UnavailableResourceException;
import javking.models.guild.GuildContext;
import javking.models.guild.property.GuildSpecification;
import javking.models.music.Playable;
import javking.util.PropertiesLoadingService;
import javking.util.Spotify.SpotifyPlaylist;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubePlaylist;
import javking.util.YouTube.YouTubeService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MongoService {
    private final Logger logger;
    private MongoClient mongoClient;

    public MongoService() {
        mongoClient = getMongoClient();
        logger = LoggerFactory.getLogger(getClass());
    }

    private MongoClient createConnection() {
        ConnectionString connectionString = new ConnectionString(PropertiesLoadingService.requireProperty("MONGO_URL"));

//        ClusterSettings clusterSettings = ClusterSettings.builder()
//                .applyConnectionString(connectionString)
//                .hosts(Collections.singletonList(new ServerAddress(connectionString.getHosts().get(0))))
//                .build();

        SocketSettings socketSettings = SocketSettings.builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();


        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                .maxSize(100)
                .maxWaitTime(500, TimeUnit.SECONDS)
                .build();

        SslSettings sslSettings = SslSettings.builder()
                .enabled(Boolean.TRUE.equals(connectionString.getSslEnabled()))
                .build();

        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
//                .applyToClusterSettings(builder -> builder.applySettings(clusterSettings))
                .applyToSocketSettings(builder -> builder.applySettings(socketSettings))
                .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                .applyToSslSettings(builder -> builder.applySettings(sslSettings))
                .serverApi(serverApi)
                .streamFactoryFactory(NettyStreamFactoryFactory.builder().build())
                .build();

        return MongoClients.create(settings);
    }

    public MongoClient getMongoClient() {
        if (mongoClient == null) {
            mongoClient = createConnection();
        }
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        return getMongoClient().getDatabase(JavKing.class.getSimpleName());
    }

    private MongoCollection<Document> getCollection(String collectionName) {
        return getDatabase().getCollection(collectionName);
    }

    private Document getCollection(String collectionName, Bson filter) {
        return getCollection(collectionName).find(filter).first();
    }

    public CompletableFuture<Document> retrieve(String collection, Guild guild) {
        return retrieve(collection, null, guild);
    }

    public CompletableFuture<Document> retrieve(String collection, @Nullable String spotifyId, @Nullable Guild guild) {
        Bson filter = decideFilter(spotifyId, guild);

        CompletableFuture<Document> document = new CompletableFuture<>();

        document.complete(getCollection(collection, filter));
        return document;
    }

    public GuildSpecification pullGuildSpecification(GuildContext guildContext) {
        return pullGuildSpecification(guildContext.getGuild(), guildContext.getAudioPlayback());
    }

    public GuildSpecification pullGuildSpecification(Guild guild, AudioPlayback audioPlayback) {
        GuildSpecification guildSpecification = new GuildSpecification(guild, audioPlayback);

        retrieve("guildSpecification", guild).whenComplete((document, throwable) -> {
            guildSpecification.setAnnounceSongs(document.getBoolean("announceSongs"));
            guildSpecification.setDjRole(document.getString("setDj"));
            guildSpecification.setDjOnly(document.getBoolean("djOnly"), false);
            guildSpecification.setPrefix(document.getString("prefix"));
            guildSpecification.setVolume(document.getInteger("volume"));
        });

        return guildSpecification;
    }

    public HollowYouTubeVideo pullLastPlayed(Guild guild) {
        JavKing instance = JavKing.get();
        YouTubeService youTubeService = instance.getAudioManager().getYouTubeService();
        HollowYouTubeVideo hollowYouTubeVideo = new HollowYouTubeVideo(youTubeService);

        retrieve("lastplayed", guild).whenComplete((document, throwable) -> {
            hollowYouTubeVideo.setThumbnail(document.getString("thumbnail"));
            hollowYouTubeVideo.setUri(document.getString("uri"));
            hollowYouTubeVideo.setTitle(document.getString("title"));
        });

        return hollowYouTubeVideo;
    }

    private Document fillGuildSpecifications(GuildSpecification guildSpecification) {
        Document document = new Document();
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        String[] properties = new String[]{"announceSongs", "djOnly", "setDj", "prefix"};
        for (String s : properties) {
            document.append(s, atomicInteger.getAndIncrement() < 2
                    ? detectBoolean(guildSpecification.getOrDefault(s))
                    : guildSpecification.getOrDefault(s));
        }
        return document.append("volume", Integer.parseInt(guildSpecification.getOrDefault("volume")));
    }

    private Document fillSpotifyRedirect(String spotifyId, String videoId) {
        return new Document("spotifyId", spotifyId)
                .append("videoId", videoId);
    }

    private Document fillLastPlayed(Playable playable, Guild guild) throws UnavailableResourceException {
        return fillLastPlayed(guild.getId(),
                playable.getPlaybackUrl(),
                playable.getThumbnailUrl(),
                playable.getTitle(),
                true);
    }

    private Document fillLastPlayed(YouTubePlaylist playlist, Guild guild) {
        return fillLastPlayed(guild.getId(),
                playlist.getUrl(),
                playlist.getThumbnail(),
                playlist.getTitle(),
                true);
    }

    private Document fillLastPlayed(SpotifyPlaylist playlist, Guild guild) {
        return fillLastPlayed(guild.getId(),
                playlist.getUrl(),
                playlist.getThumbnail(),
                playlist.getTitle(),
                playlist.isPublic());
    }

    private Document fillLastPlayed(Object... values) {
        assert values.length == 5;
        Iterator<Object> iterator = Arrays.stream(values).iterator();
        return new Document("guildId", iterator.next())
                .append("uri", String.valueOf(iterator.next()))
                .append("thumbnail", String.valueOf(iterator.next()))
                .append("title", String.valueOf(iterator.next()))
                .append("public", Boolean.valueOf(String.valueOf(iterator.next())));
    }


    private Document fillSetModifier(Document document) {
        return new Document("$set", document);
    }

    public void updateGuildSpecification(GuildContext guildContext) {
        updateGuildSpecification(guildContext.getGuildSpecification());
    }

    public void updateGuildSpecification(GuildSpecification guildSpecification) {
        update("guildSpecification", fillGuildSpecifications(guildSpecification), guildSpecification.getGuild());
    }

    public void updateSpotifyRedirect(String spotifyId, String videoId) {
        update("spotifyRedirect", fillSpotifyRedirect(spotifyId, videoId), null);
    }

    public void updateLastPlayed(Playable playable, MessageChannel channel) {
        updateLastPlayed(playable, ((TextChannel) channel).getGuild());
    }

    public void updateLastPlayed(Playable playable, Guild guild) {
        try {
            updateLastPlayed(fillLastPlayed(playable, guild), guild);
        } catch (UnavailableResourceException e) {
            System.out.printf("Failed to update lastplayed! %s%n", e.getMessage());
        }
    }

    public void updateLastPlayed(YouTubePlaylist playlist, Guild guild) {
        updateLastPlayed(fillLastPlayed(playlist, guild), guild);
    }

    public void updateLastPlayed(SpotifyPlaylist playlist, Guild guild) {
        updateLastPlayed(fillLastPlayed(playlist, guild), guild);
    }

    private void updateLastPlayed(Document document, Guild guild) {
        update("lastplayed", document, guild);
    }

//    private void update(String collectionName, Document document, String guildId) {
//            MongoCollection<Document> collection = getCollection(collectionName);
//            Bson filter = Filters.eq("guildId", guildId);
//
//            collection.updateOne(filter, fillSetModifier(document), new UpdateOptions().upsert(true));
//    }

    private void update(String collectionName, Document document, @Nullable Guild guild) {
        MongoCollection<Document> collection = getCollection(collectionName);
        Bson filter = decideFilter(document.getString("spotifyId"), guild);

        collection.updateOne(filter, fillSetModifier(document), new UpdateOptions().upsert(true));
    }

    private Bson decideFilter(@Nullable String spotifyId, @Nullable Guild guild) {
        if (spotifyId != null || guild == null) return Filters.eq("spotifyId", spotifyId);
        else return Filters.eq("guildId", guild.getId());
    }

    private String unpackBoolean(boolean value) {
        return value ? "on" : "off";
    }

    private boolean detectBoolean(String value) {
        return value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("true")
                || ((value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false"))
                ? false
                : null
        );
    }
}
