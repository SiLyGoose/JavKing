package javking.models.guild.property;

import javking.JavKing;
import javking.audio.AudioPlayback;
import javking.exceptions.DJNotSetException;
import javking.exceptions.UserException;
import javking.models.guild.GuildContext;
import javking.models.guild.property.properties.PrefixProperty;
import javking.util.DiscordService;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.entities.Guild;

import java.io.Serializable;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuildSpecification implements Serializable {
    private static final long serialVersionUID = 23L;

    private final AudioPlayback audioPlayback;

    private final Guild guild;

    private long djRole;
    private String prefix;

    public GuildSpecification(GuildContext guildContext) {
        this(guildContext.getGuild(), guildContext.getAudioPlayback());
    }

    public GuildSpecification(Guild guild, AudioPlayback audioPlayback) {
        this.guild = guild;
        this.audioPlayback = audioPlayback;

        djRole = 0L;
        prefix = PropertiesLoadingService.requireProperty("PREFIX");
    }

    public Guild getGuild() {
        return guild;
    }

    private boolean isAnnounceSongs() {
        return audioPlayback.isAnnounceSongs();
    }

    public void setAnnounceSongs(boolean announceSongs) {
        audioPlayback.setAnnounceSongs(announceSongs);
    }

    private int getVolume() {
        return audioPlayback.getVolume();
    }

    public void setVolume(int volume) {
        audioPlayback.setVolume(volume);
    }

    private boolean isDjOnly() {
        return audioPlayback.isDjOnly();
    }

    /**
     *
     * @param force forced to default settings on reset
     */
    public void setDjOnly(boolean djOnly, boolean force) {
        if (!force && djRole == 0L) throw new DJNotSetException("DJ role must be set first.");
        audioPlayback.setDjOnly(djOnly);
    }

    private long getDjRole() {
        return djRole;
    }

    public void setDjRole(long djRoleIdLong) {
        try {
            this.djRole = DiscordService.findRole(guild, djRoleIdLong).getIdLong();
        } catch (IndexOutOfBoundsException e) {
            this.djRole = 0L;
        }
    }

    public void setDjRole(String djRoleId) {
        this.djRole = DiscordService.mentionToId(djRoleId);
        if (this.djRole == 0L) {
            setDjRole(Long.parseLong(djRoleId));
        }
    }

    private String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setGuildProperty(String propertyName, String value) {
        GuildProperties property = getEnumProperty(propertyName);
        if (property.isValidValue(guild, value)) {
            try {
                switch (propertyName.toLowerCase()) {
                    case "announcesongs":
                        setAnnounceSongs(GuildContext.detectBoolean(value));
                        break;
                    case "djonly":
                        setDjOnly(GuildContext.detectBoolean(value), false);
                        break;
                    case "setdj":
                        setDjRole(value);
                        break;
                    case "prefix":
                        setPrefix(value);
                        break;
                    case "volume":
                        setVolume(Integer.parseInt(value));
                        break;
                }
            } catch (NullPointerException ignored) {
                throw new UserException("Invalid setting value given: " + value);
            }
        }
        JavKing.get().getMongoService().updateGuildSpecification(this);
    }

    public GuildProperties getEnumProperty(String propertyName) {
        return Stream.of(GuildProperties.values())
                .filter(property -> String.join("", property.toString().split("_")).equalsIgnoreCase(propertyName))
                .collect(Collectors.toList())
                .get(0);
    }

    public String getOrDefault(String propertyName) {
        String value = null;
        switch (propertyName.toLowerCase()) {
            case "announcesongs":
                value = unpackBoolean(isAnnounceSongs());
                break;
            case "djonly":
                value = unpackBoolean(isDjOnly());
                break;
            case "setdj":
                value = String.valueOf(getDjRole());
                break;
            case "prefix":
                value = getPrefix();
                break;
            case "volume":
                value = String.valueOf(getVolume());
                break;
        }

        if (value == null) value = getEnumProperty(propertyName).getDefaultValue();
        return value;
    }

    public void resetGuildProperties() {
        setAnnounceSongs(false);
        setDjRole(0L);
        setDjOnly(false, true);
        setPrefix(PrefixProperty.DEFAULT);
        setVolume(100);

        JavKing.get().getMongoService().updateGuildSpecification(this);
    }

    private String unpackBoolean(boolean value) {
        return value ? "on" : "off";
    }
}