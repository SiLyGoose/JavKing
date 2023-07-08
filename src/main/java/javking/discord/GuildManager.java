package javking.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.models.guild.GuildContext;
import javking.util.ISnowflakeMap;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GuildManager implements Serializable {
    private static final long serialVersionUID = 20L;

    private transient static final List<String> guildIdList = new ArrayList<>();

    private transient final ISnowflakeMap<GuildContext> guildContexts = new ISnowflakeMap<>();
    private transient final Logger logger;

    private AudioManager audioManager;

    public GuildManager() {
        logger = LoggerFactory.getLogger(getClass());
    }

    public void init(List<Guild> guildList) {
        guildList.parallelStream().forEach(this::initializeGuild);
    }

    public static List<String> getGuildIdList() {
        return guildIdList;
    }

    public GuildContext getContextForGuild(Guild guild) {
        GuildContext guildContext = guildContexts.get(guild);

        if (guildContext == null) return initializeGuild(guild);
        return guildContext;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    private GuildContext initializeGuild(Guild guild) {
        guildIdList.add(guild.getId());

        AudioPlayer player = audioManager.getPlayerManager().createPlayer();
        GuildContext guildContext = new GuildContext(guild, new AudioPlayback(player, guild));
        guildContexts.put(guild, guildContext);
        return guildContext;
    }
}
