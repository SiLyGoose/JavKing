package javking.models.guild.property;

import javking.models.guild.IGuildPropertyType;
import javking.models.guild.property.properties.*;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.entities.Guild;

public enum GuildProperties {
    ANNOUNCE_SONGS("🔔", "off", new AnnounceSongsProperty(), "Announces title of each song upon playing", "on/off"),
    DJ_ONLY("🚷", "off", new DJOnlyProperty(), "DJs only mode", "on/off"),
    SET_DJ("\uD83D\uDCC3", "0", new SetDJProperty(true), "Changes DJ role", "role @mention"),
    PREFIX("❗", PrefixProperty.DEFAULT, new PrefixProperty(1, 4), "Changes prefix for JavKing", "Any text, max of 4 characters"),
    VOLUME("\uD83D\uDD0A", "100", new VolumeProperty(), "Sets the volume for audio playback",
            String.format("range: [%s, %s]", PropertiesLoadingService.requireProperty("MIN_VOLUME"), PropertiesLoadingService.requireProperty("MAX_VOLUME"))),
    RESET("♻️", null, null, null, null);

    private final String icon;
    private final String defaultValue;
    private final IGuildPropertyType propertyType;
    private final String description;
    private final String validProperty;

    GuildProperties(String icon, String defaultValue, IGuildPropertyType propertyType, String description, String validProperty) {
        this.icon = icon;
        this.defaultValue = defaultValue;
        this.propertyType = propertyType;
        this.description = description;
        this.validProperty = validProperty;
    }

    public String getIcon() {
        return icon;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public IGuildPropertyType getPropertyType() {
        return propertyType;
    }

    public String getDescription() {
        return description;
    }

    public String getValidProperty() {
        return validProperty;
    }

    public boolean isValidValue(Guild guild, String input) {
        return propertyType.validate(guild, input);
    }

    public String getValue(Guild guild, String input) {
        return propertyType.fromInput(guild, input);
    }

    public String toDisplay(Guild guild, String value) {
        return propertyType.toDisplay(guild, value);
    }
}
