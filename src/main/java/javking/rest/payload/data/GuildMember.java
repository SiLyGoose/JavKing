package javking.rest.payload.data;

import javking.rest.controllers.models.ProxyGuild;
import javking.rest.payload.voice.VoiceMember;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class GuildMember implements Serializable {
    private static final long serialVersionUID = 100L;

    private final String id;
    private UUID uuid;
    private final MemberData data;

    public GuildMember(String uuid, String id, String name, String discriminator, String avatar, String accessToken, String refreshToken, String tokenType, String expiresIn, List<ProxyGuild> mutualList, List<ProxyGuild> userGuildList) {
        this.uuid = UUID.fromString(uuid);
        this.id = id;
        this.data = new MemberData(name, discriminator, avatar, accessToken, refreshToken, tokenType, expiresIn, mutualList, userGuildList);
    }

    public String getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        setUuid(UUID.fromString(uuid));
    }

    public void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

    public MemberData getData() {
        return data;
    }
}
