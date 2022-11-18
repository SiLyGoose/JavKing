package javking.rest.controllers;

import java.io.Serializable;

public class GuildMember implements Serializable {
    private static final long serialVersionUID = 100L;

    private final String id;
    private final String name;
    private final String avatar;
    private final String accessToken;
    private final String tokenType;
    private final String guildIdList;
    public GuildMember(String id, String name, String avatar, String accessToken, String tokenType, String guildIdList) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.guildIdList = guildIdList;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getGuildIdList() {
        return guildIdList;
    }
}
