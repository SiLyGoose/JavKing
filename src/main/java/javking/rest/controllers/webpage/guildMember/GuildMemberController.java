package javking.rest.controllers.webpage.guildMember;

import javking.rest.controllers.GuildMemberManager;
import javking.rest.payload.data.GuildMember;
import javking.rest.payload.data.MemberData;
import javking.rest.payload.data.MemberGuild;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class GuildMemberController {
    @PostMapping("/api/add-guild-member/")
    public ResponseEntity<GuildMember> addGuildMember(@RequestBody GuildMember guildMember) {
        GuildMemberManager.setGuildMember(guildMember.getId(), guildMember);
        return ResponseEntity.ok(guildMember);
    }

    @DeleteMapping("/api/remove-guild-member/{id}")
    public ResponseEntity<String> removeGuildMember(@PathVariable("id") String userId) {
        return ResponseEntity.ok(guildMemberData(GuildMemberManager.removeGuildMember(userId), false).toString());
    }

    @GetMapping("/api/guild-member/{id}")
    public ResponseEntity<String> getGuildMember(@PathVariable("id") String userId) {
        if (validUserId(userId)) return ResponseEntity.notFound().build();

        GuildMember member = GuildMemberManager.getGuildMember(userId);

        return ResponseEntity.ok(guildMemberData(member, true).toString());
    }

    @GetMapping("/api/guild-member-data/{id}")
    public ResponseEntity<String> getGuildMemberData(@PathVariable("id") String userId) {
        if (validUserId(userId)) return ResponseEntity.notFound().build();

        GuildMember member = GuildMemberManager.getGuildMember(userId);

        return ResponseEntity.ok(guildMemberData(member, false).toString());
    }

    private boolean validUserId(String userId) {
        return userId == null || !GuildMemberManager.hasGuildMember(userId);
    }

    private JSONObject guildMemberData(GuildMember member, boolean simplified) {
        JSONObject data = new JSONObject();
        data.put("id", member.getId());

        JSONObject userData = new JSONObject();
        MemberData memberData = member.getData();
        userData.put("username", memberData.getName());
        userData.put("discriminator", memberData.getDiscriminator());
        userData.put("avatar", memberData.getAvatar());

        if (!simplified) {
            JSONObject guildData = new JSONObject();
            MemberGuild memberGuild = memberData.getGuild();
            guildData.put("mutualList", memberGuild.getMutualList());
            guildData.put("userGuildList", memberGuild.getUserGuildList());

            userData.put("guild", guildData);
        }

        data.put("d", userData);

        return data;
    }
}
