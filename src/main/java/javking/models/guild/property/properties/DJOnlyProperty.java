package javking.models.guild.property.properties;

import javking.models.guild.IGuildPropertyType;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Arrays;

public class DJOnlyProperty implements IGuildPropertyType {
    private final String[] accepted;

    public DJOnlyProperty() {
        accepted = new String[]{"on", "off", "true", "false"};
    }
    @Override
    public String typeName() {
        return "dj-only";
    }

    @Override
    public boolean validate(Guild guild, String value) {
        return value != null && Arrays.asList(accepted).contains(value);
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
