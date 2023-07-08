package javking.rest.controllers.webpage.guildMember;

import com.google.common.collect.Lists;
import javking.JavKing;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.models.music.Playable;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Controller
public class GuildQueueController {
    @GetMapping("/api/queue-data/{id}")
    public ResponseEntity<List<Playable>> getQueueData(@PathVariable("id") String stationId) {
        Guild guild = JavKing.get().getShardManager().getGuildById(stationId);
        AudioPlayback audioPlayback = JavKing.get().getAudioManager().getPlaybackForGuild(guild);
        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        ConcurrentLinkedQueue<Playable> concurrentQueueData = new ConcurrentLinkedQueue<>(audioQueue.getTracks());
        List<Playable> queueData = new ArrayList<>(concurrentQueueData);

        return ResponseEntity.ok(queueData);
    }
}
