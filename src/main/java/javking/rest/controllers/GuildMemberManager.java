package javking.rest.controllers;

import java.util.HashMap;

public class GuildMemberManager {
    private static final HashMap<String, GuildMember> guildMembers = new HashMap<>();

    public static boolean hasGuildMember(String id) {
        return guildMembers.containsKey(id);
    }

    public static GuildMember getGuildMember(String id) {
        return guildMembers.get(id);
    }

    public static GuildMember setGuildMember(String id, GuildMember userContext) {
        guildMembers.put(id, userContext);
        return getGuildMember(id);
    }
}
