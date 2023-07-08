package javking.rest.payload.data;

import javking.rest.controllers.models.ProxyGuild;

import java.util.List;

public class MemberData {
    private final String name, discriminator, avatar, accessToken, refreshToken, tokenType, expiresIn;
    private final MemberGuild guild;

    public MemberData(String name, String discriminator, String avatar, String accessToken, String refreshToken, String tokenType, String expiresIn, List<ProxyGuild> mutualList, List<ProxyGuild> userGuildList) {
        this.name = name;
        this.discriminator = discriminator;
        this.avatar = avatar;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.guild = new MemberGuild(mutualList, userGuildList);
    }

    public String getName() {
        return name;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public MemberGuild getGuild() {
        return guild;
    }
}