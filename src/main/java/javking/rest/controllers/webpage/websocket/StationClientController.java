package javking.rest.controllers.webpage.websocket;

import com.corundumstudio.socketio.SocketIOClient;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.UnavailableResourceException;
import javking.rest.controllers.GuildMemberManager;
import javking.rest.payload.data.GuildMember;
import javking.templates.Template;
import javking.templates.Templates;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static javking.util.function.populator.SocketDataPopulator.*;

@RestController
@RequestMapping("/v2.socket.io")
public class StationClientController {
    @PostMapping(value = "/add-client", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> addStationClient(@RequestBody StationSocket stationSocket) {
        StationClient stationClient = new StationClient(stationSocket);
        UUID uuid = stationClient.getUuid();

        if (StationClientManager.hasStationClient(uuid) && StationClientManager.getStationClient(uuid).equals(stationClient))
            return ResponseEntity.ok().build();

        GuildMember guildMember = GuildMemberManager.getGuildMember(uuid);

        stationClient.setUserId(guildMember.getId());
        StationClientManager.setStationClient(uuid, stationClient);

        System.out.println("Client connected: " + stationClient.getUuid() + " with Station ID: " + stationClient.getStationId() + " with User ID: " + guildMember.getId());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/station-client-data/{uuid}")
    public ResponseEntity<StationClient> getStationClientData(@PathVariable("uuid") String token) {
        if (validToken(token)) return ResponseEntity.notFound().build();

        StationClient stationClient = StationClientManager.getStationClient(token);

        return ResponseEntity.ok(stationClient);
    }

//    @DeleteMapping("/socket.io/remove-client")
//    public void removeStationClient(@R)

    @PostMapping("/stationAccessed/{token}")
    public void stationAccessed(@PathVariable("token") String token) throws UnavailableResourceException {
        StationData stationData = new StationData(token);

        assert stationData.guildMember != null;
        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), stationData.audioPlayback);
    }

    @PostMapping("/stationPrevious/{token}/{loadPrevious}")
    public void stationPrevious(@PathVariable("token") String token, @PathVariable("loadPrevious") boolean loadPrevious) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;
        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        assert stationData.guildMember != null;
        String userId = stationData.guildMember.getId();
        String message = stationData.guildMember.getName() + " has started playing the ";

//      true if previous track is requested. initial request sets current positionMs
//      to 0 and sequential requests, with refreshing timers, moves the current position
//      backwards to play "played" songs again
        if (loadPrevious && audioQueue.hasPrevious()) {
            audioQueue.setPosition(audioQueue.getPosition() - 1);
            message += "previous track!";
            handleQueueMutatorEvent("stationUpdate", userId, audioPlayback);
        } else {
            audioPlayback.setCurrentPositionMs(0);
            message += "track from the beginning!";
            handleTrackUpdateEvent("stationUpdate", userId, audioPlayback);
        }

        stationData.audioManager.startPlaying(stationData.stationClient.getGuild(), true);

        stationData.sendBold(Templates.command.blue_check_mark.formatFull(message));
    }

    @PostMapping("/stationPaused/{token}/{paused}")
    public void stationPaused(@PathVariable("token") String token, @PathVariable("paused") boolean paused) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName() + " " + (paused ? "resumed" : "paused") + " the player!";
        Template template = paused ? Templates.music.resumed_queue : Templates.music.paused_queue;

        audioPlayback.togglePaused(!paused);

        stationData.sendBold(template.formatFull(message));

        handleTrackMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
    }

    @PostMapping("/stationSkipped/{token}/{offset}")
    public void stationSkipped(@PathVariable("token") String token, @PathVariable("offset") int offset) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioManager audioManager = stationData.audioManager;
        AudioPlayback audioPlayback = stationData.audioPlayback;

        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        if (!audioQueue.hasNext()) {
            audioPlayback.clear(true);
            audioPlayback.stop();
        } else {
//          website version cannot skip more than one song per click
            audioQueue.setPosition(audioQueue.getPosition() + 1);
            audioManager.startPlaying(stationData.stationClient.getGuild(), false);
        }

        assert stationData.guildMember != null;
        stationData.sendBold(Templates.music.skipped_song.formatFull(stationData.guildMember.getName() + " skipped the current track!"));

        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
    }

    @PostMapping("/stationRepeat/{token}/{rO}/{rA}")
    public void stationRepeat(@PathVariable("token") String token, @PathVariable("rO") boolean rO, @PathVariable("rA") boolean rA) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName() + " set repeat ";

//      rO: repeatOne, rA: repeatAll
        if (rA) {
            audioPlayback.setRepeatAll(true);
            audioPlayback.setRepeatOne(false);
            message += "queue!";
            message = Templates.music.repeat_queue.formatFull(message);
        } else if (rO) {
            audioPlayback.setRepeatAll(false);
            audioPlayback.setRepeatOne(true);
            message += "track!";
            message = Templates.music.repeat_song.formatFull(message);
        } else {
            audioPlayback.setRepeatAll(false);
            audioPlayback.setRepeatOne(false);
            message += "off!";
            message = Templates.music.repeat_song.formatFull(message);
        }

        stationData.sendBold(message);

        handleTrackMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
    }

    @PostMapping("/stationShuffled/{token}")
    public void stationShuffled(@PathVariable("token") String token) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName();

//                return true if queue is shuffled
        if (audioPlayback.isShuffled()) {
            audioPlayback.setShuffled(false);
            message += " normalized the queue!";
        } else {
            audioPlayback.setShuffled(true);
            message += " shuffled the queue!";
        }

        stationData.sendBold(Templates.music.shuffle_queue.formatFull(message));
        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
    }

    @PostMapping("/stationSeek/{token}/{positionMs}")
    public void stationSeek(@PathVariable("token") String token, @PathVariable("positionMs") long positionMs) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        audioPlayback.setCurrentPositionMs(positionMs);

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName() + " has updated track to play from: " + TimeConvertingService.millisecondsToHHMMSS(positionMs);
        stationData.sendBold(Templates.command.blue_check_mark.formatFull(message));
    }

    @PostMapping("/stationTrackRemoved/{token}/{index}")
    public void stationTrackRemoved(@PathVariable("token") String token, @PathVariable("index") int index) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        assert stationData.guildMember != null;

        audioPlayback.remove(index);

        stationData.sendBold(Templates.command.blue_check_mark.formatFull(String.format("%s removed track at position `%s`", stationData.guildMember.getName(), index)));

        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
    }

    private boolean validToken(String token) {
        return StationClientManager.hasStationClient(token);
    }

    static class StationData {
        private final StationClient stationClient;
        private final AudioManager audioManager;
        private final AudioPlayback audioPlayback;
        private final GuildMember guildMember;

        StationData(String token) {
            UUID uuid = UUID.fromString(token);
            stationClient = StationClientManager.getStationClient(uuid);
            guildMember = GuildMemberManager.getGuildMember(uuid);
            audioManager = JavKing.get().getAudioManager();
            audioPlayback = audioManager.getPlaybackForGuild(stationClient.getGuild());
        }

        public void sendBold(String message) {
            MessageChannel channel = audioPlayback.getChannel();
            if (channel != null) JavKing.get().getMessageService().sendBold(message, channel);
        }
    }
}
