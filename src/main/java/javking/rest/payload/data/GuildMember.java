package javking.rest.payload.data;

import javking.rest.controllers.models.ProxyGuild;
import javking.rest.payload.voice.VoiceMember;

import java.io.Serializable;
import java.util.List;

public class GuildMember implements Serializable {
    private static final long serialVersionUID = 100L;

    private final String id;
    private final MemberData data;

    public GuildMember(String id, String name, String discriminator, String avatar, String accessToken, String refreshToken, String tokenType, String expiresIn, List<ProxyGuild> mutualList, List<ProxyGuild> userGuildList) {
        this.id = id;
        this.data = new MemberData(name, discriminator, avatar, accessToken, refreshToken, tokenType, expiresIn, mutualList, userGuildList);
    }

    public String getId() {
        return id;
    }

    public MemberData getData() {
        return data;
    }
}
