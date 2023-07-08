package javking.rest.controllers;

import javking.rest.payload.data.GuildMember;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;

@Component
public class GuildMemberManager implements Serializable {
    private static final long serialVersionUID = 60L;

    private static final HashMap<String, GuildMember> guildMembers = new HashMap<>();

    public static boolean hasGuildMember(String id) {
        return guildMembers.containsKey(id);
    }

    public static GuildMember getGuildMember(String id) {
        return guildMembers.get(id);
    }

    public static GuildMember removeGuildMember(String id) {
        return guildMembers.remove(id);
    }

    public static GuildMember setGuildMember(String id, GuildMember guildMember) {
        guildMembers.put(id, guildMember);
        return getGuildMember(id);
    }
}
