package javking.models.guild;

import javking.JavKing;
import javking.audio.AudioPlayback;
import javking.audio.exec.PooledTrackLoadingExecutor;
import javking.models.guild.property.GuildSpecification;
import javking.util.DiscordService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.io.Serializable;

public class GuildContext implements Serializable {
    private static final long serialVersionUID = 21L;

    private final Guild guild;
    private final GuildSpecification guildSpecification;
    private final AudioPlayback audioPlayback;

    private transient final PooledTrackLoadingExecutor pooledTrackLoadingExecutor;

    public GuildContext(Guild guild, AudioPlayback audioPlayback) {
        this.guild = guild;
        this.audioPlayback = audioPlayback;
        guildSpecification = JavKing.get().getMongoService().pullGuildSpecification(guild, audioPlayback);
        pooledTrackLoadingExecutor = new PooledTrackLoadingExecutor(guild.getId(), this);
    }

    public Guild getGuild() {
        return guild;
    }

    public GuildSpecification getGuildSpecification() {
        return guildSpecification;
    }

    public AudioPlayback getAudioPlayback() {
        return audioPlayback;
    }

    public PooledTrackLoadingExecutor getPooledTrackLoadingExecutor() {
        return pooledTrackLoadingExecutor;
    }

    public boolean isAnnounceSongs() {
        return audioPlayback.isAnnounceSongs();
    }

    public String getPrefix() {
        return guildSpecification.getOrDefault("prefix");
    }

    public boolean isDjOnly() {
        return audioPlayback.isDjOnly();
    }

    public Role getDjRole() {
        return DiscordService.findRole(guild, Long.parseLong(guildSpecification.getOrDefault("djRole")));
    }

    public int getVolume() {
        return audioPlayback.getVolume();
    }

    public static boolean detectBoolean(String value) {
        return value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("true")
                || ((value.equalsIgnoreCase("false") || value.equalsIgnoreCase("off"))
                ? false
                : null
        );
    }
}
