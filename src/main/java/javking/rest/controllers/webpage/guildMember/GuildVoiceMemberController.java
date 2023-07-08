package javking.rest.controllers.webpage.guildMember;

import javking.JavKing;
import javking.rest.controllers.GuildMemberManager;
import javking.rest.controllers.VoiceMemberManager;
import javking.rest.controllers.models.ProxyGuild;
import javking.rest.payload.data.GuildMember;
import javking.rest.payload.voice.BotChannel;
import javking.rest.payload.voice.UserChannel;
import javking.rest.payload.voice.VoiceMember;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class GuildVoiceMemberController {
    @GetMapping("/api/voice-member-data/u={u}&stationId={stationId}")
    public ResponseEntity<String> getVoiceMemberData(@PathVariable("u") String userId, @PathVariable("stationId") String stationId) {
        VoiceMember voiceMember = VoiceMemberManager.getVoiceMember(userId);
        Guild guild = JavKing.get().getShardManager().getGuildById(stationId);
        assert guild != null;
        List<GuildChannel> guildChannelList = guild.getChannels();

        JSONObject data = new JSONObject();

        if (voiceMember == null) {
            data.put("userChannel", JSONObject.NULL);
            data.put("botChannel", JSONObject.NULL);
            return ResponseEntity.ok(data.toString());
        }

        UserChannel userChannel = voiceMember.getUserChannel();
        boolean sameStation = guildChannelList.parallelStream().anyMatch(c -> c.getId().equals(userChannel.getVoiceId()));
        if (userChannel.getVoiceId() != null && sameStation) {
            JSONObject userChannelObject = new JSONObject();
            userChannelObject.put("voiceId", userChannel.getVoiceId());
            userChannelObject.put("voiceName", userChannel.getVoiceName());
            userChannelObject.put("botJoinable", userChannel.isBotJoinable());

            data.put("userChannel", userChannelObject);
        } else data.put("userChannel", JSONObject.NULL);

        BotChannel botChannel = voiceMember.getBotChannel();
        boolean botSameStation = guildChannelList.parallelStream().anyMatch(c -> c.getId().equals(botChannel.getBotVoiceId()));
        if (botChannel.getBotVoiceId() != null && botSameStation) {
            JSONObject botChannelObject = new JSONObject();
            botChannelObject.put("botVoiceId", botChannel.getBotVoiceId());
            botChannelObject.put("botVoiceName", botChannel.getBotVoiceName());
            botChannelObject.put("botSpeakable", botChannel.isBotSpeakable());

            data.put("botChannel", botChannelObject);
        } else data.put("botChannel", JSONObject.NULL);

        return ResponseEntity.ok(data.toString());
    }
}
