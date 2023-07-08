package javking.rest.payload.data;

import javking.rest.controllers.models.ProxyGuild;

import java.util.List;

public class MemberGuild {
    private final List<ProxyGuild> mutualList, userGuildList;

    public MemberGuild(List<ProxyGuild> mutualList, List<ProxyGuild> userGuildList) {
        this.mutualList = mutualList;
        this.userGuildList = userGuildList;
    }

    public List<ProxyGuild> getMutualList() {
        return mutualList;
    }

    public List<ProxyGuild> getUserGuildList() {
        return userGuildList;
    }
}