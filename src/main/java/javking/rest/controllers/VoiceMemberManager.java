package javking.rest.controllers;

import javking.rest.payload.data.GuildMember;
import javking.rest.payload.voice.VoiceMember;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;

@Component
public class VoiceMemberManager implements Serializable {
    private static final long serialVersionUID = 61L;

    private static final HashMap<String, VoiceMember> voiceMembers = new HashMap<>();

    public static boolean hasVoiceMember(String id) {
        return voiceMembers.containsKey(id);
    }

    public static VoiceMember getVoiceMember(String id) {
        return voiceMembers.get(id);
    }

    public static VoiceMember removeVoiceMember(String id) {
        return voiceMembers.remove(id);
    }

    public static VoiceMember setVoiceMember(String id, VoiceMember voiceMember) {
        voiceMembers.put(id, voiceMember);
        return getVoiceMember(id);
    }
}