package javking.models.guild.property.properties;

import javking.models.guild.IGuildPropertyType;
import javking.util.DiscordService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

public class SetDJProperty implements IGuildPropertyType {
    private final boolean allowNull;

    public SetDJProperty(boolean allowNull) {
        this.allowNull = allowNull;
    }

    @Override
    public String typeName() {
        return "set-dj";
    }

    @Override
    public boolean validate(Guild guild, String value) {
        if (allowNull && (value == null || value.isEmpty() || value.equalsIgnoreCase("false"))) {
            return true;
        }
        long id = DiscordService.mentionToId(value);

        if (DiscordService.isRoleMention(value)) {
            return guild.getRoleById(id) != null;
        }
        return DiscordService.findRole(guild, id) != null;
    }

    @Override
    public String fromInput(Guild guild, String value) {
        if (allowNull && (value == null || value.isEmpty() || value.equalsIgnoreCase("false"))) {
            return "";
        }
        long id = DiscordService.mentionToId(value);

        if (DiscordService.isRoleMention(value)) {
            Role role = guild.getRoleById(id);
            if (role != null) {
                return role.getId();
            }
        }
        Role role = DiscordService.findRole(guild, id);
        if (role != null) {
            return role.getId();
        }
        return "";
    }

    @Override
    public String toDisplay(Guild guild, String value) {
        if (value == null || value.isEmpty() || !value.matches("\\d{10,}")) {
            return null;
        }
        Role role = guild.getRoleById(value);
        if (role != null) {
            return role.getName();
        }
        return null;
    }
}
