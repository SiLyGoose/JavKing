package javking.discord.listeners;

import javking.rest.controllers.webpage.websocket.StationClient;
import javking.rest.controllers.VoiceMemberManager;
import javking.rest.controllers.webpage.websocket.StationClientManager;
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

import javax.annotation.Nonnull;

public class VoiceUpdateListener extends ListenerAdapter {
    public VoiceUpdateListener() {

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

        StationClient stationClient = StationClientManager.getStationClientByUser(entityUser.getId());
        if (stationClient == null) return;

        stationClient.sendEvent("voiceStatusUpdate", data);
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
