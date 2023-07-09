package javking.rest.controllers;

import javking.rest.payload.voice.VoiceMember;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import static javking.rest.controllers.GuildMemberManager.getIdentifier;

@Component
public class VoiceMemberManager implements Serializable {
    private static final long serialVersionUID = 61L;

    private static final HashMap<UUID, VoiceMember> voiceMembers = new HashMap<>();

    public static boolean hasVoiceMember(String userId) {
        return hasVoiceMember(getIdentifier(userId));
    }

    public static boolean hasVoiceMember(UUID uuid) {
        return voiceMembers.containsKey(uuid);
    }

    public static VoiceMember getVoiceMember(String userId) {
        return getVoiceMember(getIdentifier(userId));
    }

    public static VoiceMember getVoiceMember(UUID uuid) {
        return voiceMembers.get(uuid);
    }

    public static VoiceMember removeVoiceMember(String userId) {
        return removeVoiceMember(getIdentifier(userId));
    }

    public static VoiceMember removeVoiceMember(UUID uuid) {
        return voiceMembers.remove(uuid);
    }

    public static VoiceMember setVoiceMember(String userId, VoiceMember voiceMember) {
        return setVoiceMember(getIdentifier(userId), voiceMember);
    }

    public static VoiceMember setVoiceMember(UUID uuid, VoiceMember voiceMember) {
        voiceMembers.put(uuid, voiceMember);
        return getVoiceMember(uuid);
    }
}