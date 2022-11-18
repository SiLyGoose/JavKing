package javking.models.guild.property.properties;

import javking.models.guild.IGuildPropertyType;
import net.dv8tion.jda.api.entities.Guild;

public class PrefixProperty implements IGuildPropertyType {
    public static final String DEFAULT = "~";

    private final int min, max;

    public PrefixProperty(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String typeName() {
        return "prefix";
    }

    @Override
    public boolean validate(Guild guild, String value) {
        return value != null && value.length() >= min && value.length() <= max;
    }

    @Override
    public String fromInput(Guild guild, String value) {
        return value;
    }

    @Override
    public String toDisplay(Guild guild, String value) {
        return value;
    }
}
