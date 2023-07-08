package javking.rest.controllers.webpage.guildMember;

import com.google.api.client.util.Lists;
import javking.JavKing;
import javking.discord.GuildManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class GuildChannelsController {
    @GetMapping("/api/guild-channels-data/{id}")
    public ResponseEntity<List<String>> getGuildChannelsData(@PathVariable("id") String guildId) {
        ShardManager shardManager = JavKing.get().getShardManager();
        Guild guild = shardManager.getGuildById(guildId);

        if (guild == null) return ResponseEntity.badRequest().build();

        List<String> guildChannelsId = Lists.newArrayList();
        guild.getChannels().parallelStream().forEach(channel -> guildChannelsId.add(channel.getId()));

        return ResponseEntity.ok(guildChannelsId);
    }
}
