package javking.discord.listeners;

import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOServer;
import javking.rest.controllers.StationClient;
import javking.rest.controllers.VoiceMemberManager;
import javking.rest.payload.voice.BotChannel;
import javking.rest.payload.voice.UserChannel;
import javking.rest.payload.voice.VoiceMember;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VoiceUpdateListener extends ListenerAdapter {
    private static SocketIOServer server;
    //    sessionId -> StationClient Map
    private static Map<String, StationClient> stationManager;
    private BufferedWriter writer;

    public VoiceUpdateListener(SocketIOServer server) {
        VoiceUpdateListener.server = server;
        VoiceUpdateListener.stationManager = new HashMap<>();

        server.addConnectListener(socketIOClient -> {
            String sessionId = socketIOClient.getSessionId().toString();
            HandshakeData handShakeData = socketIOClient.getHandshakeData();

            String stationId = handShakeData.getSingleUrlParam("s");
            String userId = handShakeData.getSingleUrlParam("u");

            stationManager.put(userId, new StationClient(stationId, socketIOClient));

            System.out.println("Client connected: " + sessionId + " with Station ID: " + stationId + " with User ID: " + userId);
        });

        server.addDisconnectListener(socketIOClient -> {
            stationManager.remove(socketIOClient.getHandshakeData().getSingleUrlParam("u"));

            System.out.println("Client disconnected: " + socketIOClient.getSessionId());
        });

        try {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StationClient getClientById(String userId) {
//        sessionId -> uid
//        for (String sessionId : stationManager.keySet()) {
//            if (guildId.equals(stationManager.get(sessionId).getStationId())) {
//                return getClient(sessionId);
//            }
//        }
        return stationManager.get(userId);
    }

    public static StationClient getClientByGuildId(String guildId) {
        for (StationClient stationClient : stationManager.values()) {
            if (stationClient.getGuild().getId().equals(guildId)) {
                return stationClient;
            }
        }
        return null;
    }

    public static void sendEvent(String op, StationClient stationClient, JSONObject data) {
//        JSONObject revisedData = new JSONObject();
//        revisedData.put("op", op);
//        revisedData.put("data", data);
        sendEvent(op, stationClient.getClient().getSessionId(), data.toString());
    }

    public static void sendEvent(String op, UUID uuid, Object... data) {
        System.out.println("UUID: " + uuid + " " + op + ": " + Arrays.toString(data));
        server.getClient(uuid).sendEvent(op, data);
    }

    @Override
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        GuildVoiceState botVoiceState = guild.getSelfMember().getVoiceState();

        Member entityMember = event.getEntity();
        User entityUser = entityMember.getUser();

        String id = entityMember.getId();

        VoiceMember voiceMember = VoiceMemberManager.getVoiceMember(id);
        if (voiceMember == null) voiceMember = new VoiceMember();

        UserChannel memberUserChannel = voiceMember.getUserChannel();
        BotChannel memberBotChannel = voiceMember.getBotChannel();

        JSONObject data = new JSONObject(),
                userChannelObject = new JSONObject(),
                botChannelObject = new JSONObject();

        if (event.getChannelJoined() != null) {
            VoiceChannel joinedChannel = event.getChannelJoined().asVoiceChannel();
            boolean botJoinable = guild.getSelfMember().hasPermission(joinedChannel, Permission.VOICE_CONNECT);

            updateUserChannel(memberUserChannel, data, userChannelObject, joinedChannel, botJoinable);

//            if bot joined voice channel
            if (botVoiceState != null && botVoiceState.inAudioChannel()) {
                VoiceChannel botVoiceChannel = botVoiceState.getChannel().asVoiceChannel();
                boolean botSpeakable = guild.getSelfMember().hasPermission(joinedChannel, Permission.VOICE_SPEAK);

                updateBotChannel(memberBotChannel, data, botChannelObject, botVoiceChannel, botSpeakable);
//                if user joined voice channel and bot not in voice channel
            } else {
                voiceMember.setBotChannel(null);
                data.put("botChannel", JSONObject.NULL);
            }
        }

        if (event.getChannelLeft() != null) {
            VoiceChannel leftChannel = event.getChannelLeft().asVoiceChannel();

            if (entityUser.isBot()) {
//                 if bot left and user NOT in voice channel
                if (leftChannel.getMembers().size() == 0) {
                    updateAllLeftChannel(voiceMember, data);

//                if bot left and user still in voice channel
                } else {
                    boolean botJoinable = guild.getSelfMember().hasPermission(leftChannel, Permission.VOICE_CONNECT);

                    updateUserChannel(memberUserChannel, data, userChannelObject, leftChannel, botJoinable);

                    voiceMember.setBotChannel(null);
                    data.put("botChannel", JSONObject.NULL);
                }
            } else {
//                if user left and bot in voice channel
                if (botVoiceState != null && botVoiceState.inAudioChannel()) {
                    boolean botSpeakable = guild.getSelfMember().hasPermission(leftChannel, Permission.VOICE_SPEAK);

                    updateBotChannel(memberBotChannel, data, botChannelObject, leftChannel, botSpeakable);

                    voiceMember.setUserChannel(null);
                    data.put("userChannel", JSONObject.NULL);
//                    if user left and bot NOT in voice channel
                } else {
                    updateAllLeftChannel(voiceMember, data);
                }
            }
        }

        VoiceMemberManager.setVoiceMember(entityUser.getId(), voiceMember);

        StationClient stationClient = getClientById(entityUser.getId());
        if (stationClient == null) return;

        sendEvent("voiceStatusUpdate", stationClient, data);
    }

    private void updateUserChannel(UserChannel memberUserChannel, JSONObject data, JSONObject userChannelObject, VoiceChannel voiceChannel, boolean botJoinable) {
        userChannelObject.put("voiceId", voiceChannel.getId());
        userChannelObject.put("voiceName", voiceChannel.getName());
        userChannelObject.put("botJoinable", botJoinable);

        memberUserChannel.setVoiceId(voiceChannel.getId());
        memberUserChannel.setVoiceName(voiceChannel.getName());
        memberUserChannel.setBotJoinable(botJoinable);

        data.put("userChannel", userChannelObject);
    }

    private void updateBotChannel(BotChannel memberBotChannel, JSONObject data, JSONObject botChannelObject, VoiceChannel voiceChannel, boolean botSpeakable) {
        botChannelObject.put("botVoiceId", voiceChannel.getId());
        botChannelObject.put("botVoiceName", voiceChannel.getName());
        botChannelObject.put("botSpeakable", botSpeakable);

        memberBotChannel.setBotVoiceId(voiceChannel.getId());
        memberBotChannel.setBotVoiceName(voiceChannel.getName());
        memberBotChannel.setBotSpeakable(botSpeakable);

        data.put("botChannel", botChannelObject);
    }

    private void updateAllLeftChannel(VoiceMember voiceMember, JSONObject data) {
        voiceMember.setUserChannel(null);
        data.put("userChannel", JSONObject.NULL);

        voiceMember.setBotChannel(null);
        data.put("botChannel", JSONObject.NULL);
    }
}
