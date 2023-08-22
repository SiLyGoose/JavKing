package javking.rest.controllers.webpage.websocket;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.rest.controllers.GuildMemberManager;
import javking.rest.controllers.VoiceMemberManager;
import javking.rest.payload.data.GuildMember;
import javking.rest.payload.voice.BotChannel;
import javking.rest.payload.voice.UserChannel;
import javking.rest.payload.voice.VoiceMember;
import javking.templates.Template;
import javking.templates.Templates;
import javking.util.TimeConvertingService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
    @PostMapping(value = "/add-client/", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> addStationClient(@RequestBody StationSocket stationSocket) {
        StationClient stationClient = new StationClient(stationSocket);
        UUID uuid = stationClient.getUuid();

        GuildMember guildMember = GuildMemberManager.getGuildMember(uuid);
        if (guildMember == null) return ResponseEntity.notFound().build();

        stationClient.setUserId(guildMember.getId());
        StationClientManager.setStationClient(uuid, stationClient);

        System.out.println("Client connected: " + stationClient.getUuid() + " with Station ID: " + stationClient.getStationId() + " with User ID: " + guildMember.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/remove-client", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> removeStationClient(@RequestBody StationSocket stationSocket) {
        UUID token = stationSocket.getToken();
        if (!StationClientManager.hasStationClient(token)) return ResponseEntity.ok().build();

        StationClient stationClient = StationClientManager.getStationClient(token);
        if (stationClient != null && stationClient.getScheduledTask().isInProgress())
            stationClient.getScheduledTask().stopScheduledTask();

        StationClientManager.removeStationClient(token, stationSocket.getSocketId());
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/station-client-data/{uuid}")
//    public ResponseEntity<StationClient> getStationClientData(@PathVariable("uuid") String token) {
//        if (!validToken(token)) return ResponseEntity.notFound().build();
//
//        StationClient stationClient = StationClientManager.getStationClientByGuild(token);
//
//        return ResponseEntity.ok(stationClient);
//    }

//    @DeleteMapping("/socket.io/remove-client")
//    public void removeStationClient(@R)

    @PostMapping("/voiceAccessed/{token}/{stationId}")
    public ResponseEntity<?> stationVoiceAccessed(@PathVariable("token") String token, @PathVariable("stationId") String stationId) {
        if (!validToken(token)) return ResponseEntity.notFound().build();
        StationData stationData = new StationData(token);
        VoiceMember voiceMember = VoiceMemberManager.getVoiceMember(UUID.fromString(token));

        Guild guild = stationData.stationClient.getGuild();
        if (!guild.getId().equals(stationId)) return ResponseEntity.notFound().build();

//        effectively final variable must be used for lambda expressions
        final VoiceMember tVoiceMember = voiceMember;
        if (voiceMember != null && guild.getVoiceChannels().parallelStream().anyMatch(vc -> vc.getId().equals(tVoiceMember.getUserChannel().getVoiceId())))
            return ResponseEntity.ok(voiceMember);

        String userId = stationData.guildMember.getId();
        VoiceChannel voiceChannel = guild.getVoiceChannels()
                .parallelStream()
                .filter(ch -> ch.getMembers().parallelStream().anyMatch(member -> member.getId().equals(userId)))
                .findFirst()
                .orElse(null);

        if (voiceChannel == null) return ResponseEntity.notFound().build();

        VoiceChannel botVoiceChannel;
        BotChannel botChannel = null;
        GuildVoiceState botVoiceState = guild.getSelfMember().getVoiceState();
        if (botVoiceState != null && botVoiceState.inAudioChannel()) {
            assert botVoiceState.getChannel() != null;
            try {
                botVoiceChannel = botVoiceState.getChannel().asVoiceChannel();
                boolean botSpeakable = guild.getSelfMember().hasPermission(botVoiceChannel, Permission.VOICE_SPEAK);
                botChannel = new BotChannel(botVoiceChannel.getId(), botVoiceChannel.getName(), botSpeakable);
            } catch (NullPointerException ignored) {
            }
        }

        boolean botJoinable = guild.getSelfMember().hasPermission(voiceChannel, Permission.VOICE_CONNECT);
        UserChannel userChannel = new UserChannel(voiceChannel.getId(), voiceChannel.getName(), botJoinable);

        voiceMember = new VoiceMember().setUserChannel(userChannel).setBotChannel(botChannel);
        return ResponseEntity.ok(voiceMember.toJSONObject().toString());
    }

    @PostMapping("/stationAccessed/{token}")
    public ResponseEntity<?> stationAccessed(@PathVariable("token") String token) throws UnavailableResourceException {
        if (!validToken(token)) return ResponseEntity.notFound().build();
        StationData stationData = new StationData(token);

        JSONObject data = new JSONObject();
        data.put("positionMs", stationData.audioPlayback.getCurrentPositionMs());
        data.put("position", stationData.audioPlayback.getAudioQueue().getPosition());

        assert stationData.guildMember != null;
        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), stationData.audioPlayback, data);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationPrevious/{token}/{loadPrevious}")
    public ResponseEntity<?> stationPrevious(@PathVariable("token") String token, @PathVariable("loadPrevious") boolean loadPrevious) throws UnavailableResourceException {
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationPaused/{token}/{paused}")
    public ResponseEntity<?> stationPaused(@PathVariable("token") String token, @PathVariable("paused") boolean paused) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName() + " " + (paused ? "resumed" : "paused") + " the player!";
        Template template = paused ? Templates.music.resumed_queue : Templates.music.paused_queue;

        audioPlayback.togglePaused(!paused);

        stationData.sendBold(template.formatFull(message));

        handleTrackMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationSkipped/{token}/{offset}")
    public ResponseEntity<?> stationSkipped(@PathVariable("token") String token, @PathVariable("offset") int offset) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioManager audioManager = stationData.audioManager;
        AudioPlayback audioPlayback = stationData.audioPlayback;

        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        if (!audioQueue.hasNext()) {
            audioPlayback.clear(true);
            audioPlayback.stop();
        } else {
//          website version cannot skip more than one song per click
            int currentPosition = audioQueue.getPosition();
            audioQueue.setPosition(currentPosition + offset);
            audioManager.startPlaying(stationData.stationClient.getGuild(), false);
        }

        assert stationData.guildMember != null;
        stationData.sendBold(Templates.music.skipped_song.formatFull(stationData.guildMember.getName() + " skipped " + (offset > 1 ? String.format("to `%s`!", audioQueue.getCurrent().getTitle()) : "the current track!")));

        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationRepeat/{token}/{rO}/{rA}")
    public ResponseEntity<?> stationRepeat(@PathVariable("token") String token, @PathVariable("rO") boolean rO, @PathVariable("rA") boolean rA) throws UnavailableResourceException {
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationShuffled/{token}")
    public ResponseEntity<?> stationShuffled(@PathVariable("token") String token) throws UnavailableResourceException {
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationSeek/{token}/{positionMs}")
    public ResponseEntity<?> stationSeek(@PathVariable("token") String token, @PathVariable("positionMs") long positionMs) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;

        audioPlayback.setCurrentPositionMs(positionMs);

        assert stationData.guildMember != null;
        String message = stationData.guildMember.getName() + " has updated track to play from: " + TimeConvertingService.millisecondsToHHMMSS(positionMs);
        stationData.sendBold(Templates.command.blue_check_mark.formatFull(message));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/stationTrackRemoved/{token}/{index}")
    public ResponseEntity<?> stationTrackRemoved(@PathVariable("token") String token, @PathVariable("index") int index) throws UnavailableResourceException {
        StationData stationData = new StationData(token);
        AudioPlayback audioPlayback = stationData.audioPlayback;
        AudioQueue audioQueue = audioPlayback.getAudioQueue();

        Playable playable = audioQueue.getTrack(index);
        audioPlayback.remove(index);

        JSONObject data = new JSONObject();
        data.put("position", audioQueue.getPosition());

        assert stationData.guildMember != null;
        stationData.sendBold(Templates.command.blue_check_mark.formatFull(String.format("%s removed track `%s` at position `%s`", stationData.guildMember.getName(), playable.getTitle(), index + 1)));
        handleQueueMutatorEvent("stationUpdate", stationData.guildMember.getId(), audioPlayback, data);

        return ResponseEntity.ok().build();
    }

    private boolean validToken(String token) {
        return StationClientManager.hasStationClient(UUID.fromString(token));
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
