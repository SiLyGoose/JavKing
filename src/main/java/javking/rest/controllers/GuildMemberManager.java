package javking.rest.controllers;

import javking.rest.payload.data.GuildMember;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

@Component
public class GuildMemberManager implements Serializable {
    private static final long serialVersionUID = 60L;

    private static final HashMap<UUID, GuildMember> guildMembers = new HashMap<>();
//    Map userID to Next app UUID to access guildMembers even if cookie expired
    private static final HashMap<String, UUID> memberIdentifiers = new HashMap<>();

    public static boolean hasGuildMember(String uuid) {
        return hasGuildMember(UUID.fromString(uuid));
    }

    public static boolean hasGuildMember(UUID uuid) {
        return guildMembers.containsKey(uuid) || memberIdentifiers.containsValue(uuid);
    }

    public static GuildMember getGuildMember(String uuid) {
        return getGuildMember(UUID.fromString(uuid));
    }

    public static GuildMember getGuildMember(UUID uuid) {
        return guildMembers.get(uuid);
    }

    public static GuildMember removeGuildMember(String uuid) {
        return removeGuildMember(UUID.fromString(uuid));
    }

    public static GuildMember removeGuildMember(UUID uuid) {
        memberIdentifiers.entrySet().parallelStream()
                .filter(entry -> entry.getValue().equals(uuid))
                .findFirst()
                .ifPresent(entry -> memberIdentifiers.remove(entry.getKey()));
        return guildMembers.remove(uuid);
    }

    public static GuildMember setGuildMember(String uuid, GuildMember guildMember) {
        return setGuildMember(UUID.fromString(uuid), guildMember);
    }

    public static GuildMember setGuildMember(UUID uuid, GuildMember guildMember) {
        memberIdentifiers.put(guildMember.getId(), uuid);
        guildMembers.put(uuid, guildMember);
        return getGuildMember(uuid);
    }

    public static UUID getIdentifier(String userId) {
        return memberIdentifiers.get(userId);
    }
}
