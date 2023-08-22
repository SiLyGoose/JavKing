package javking.rest.payload.voice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

public class VoiceMember {
    private UserChannel userChannel;
    private BotChannel botChannel;

    public VoiceMember() {
        this.userChannel = new UserChannel();
        this.botChannel = new BotChannel();
    }

    public VoiceMember(UserChannel userChannel, BotChannel botChannel) {
        this.userChannel = userChannel;
        this.botChannel = botChannel;
    }

    public VoiceMember(String voiceId, String voiceName, String botVoiceId, String botVoiceName, boolean botJoinable, boolean botSpeakable) {
        this.userChannel = new UserChannel(voiceId, voiceName, botJoinable);
        this.botChannel = new BotChannel(botVoiceId, botVoiceName, botSpeakable);
    }

    public UserChannel getUserChannel() {
        if (userChannel == null) setUserChannel(new UserChannel());
        return userChannel;
    }

    public VoiceMember setUserChannel(UserChannel userChannel) {
        if (userChannel == null) {
            userChannel = new UserChannel();
            userChannel.setVoiceId(null);
            userChannel.setVoiceName(null);
            userChannel.setBotJoinable(false);
        }
        this.userChannel = userChannel;
        return this;
    }

    public BotChannel getBotChannel() {
        if (botChannel == null) setBotChannel(new BotChannel());
        return botChannel;
    }

    public VoiceMember setBotChannel(BotChannel botChannel) {
        if (botChannel == null) {
            botChannel = new BotChannel();
            botChannel.setBotVoiceId(null);
            botChannel.setBotVoiceName(null);
            botChannel.setBotSpeakable(false);
        }
        this.botChannel = botChannel;
        return this;
    }

    public JSONObject toJSONObject() {
        JSONObject data = new JSONObject();
//        user channel object
        JSONObject uChObj = new JSONObject();
//        bot user channel object
        JSONObject bUChObj = new JSONObject();

        if (userChannel != null) {
            uChObj.put("voiceId", userChannel.getVoiceId());
            uChObj.put("voiceName", userChannel.getVoiceName());
            uChObj.put("botJoinable", userChannel.isBotJoinable());
        }

        if (botChannel != null) {
            bUChObj.put("botVoiceId", botChannel.getBotVoiceId());
            bUChObj.put("botVoiceName", botChannel.getBotVoiceName());
            bUChObj.put("botSpeakable", botChannel.isBotSpeakable());
        }

        data.put("userChannel", userChannel == null ? JSONObject.NULL : uChObj);
        data.put("botChannel", botChannel == null ? JSONObject.NULL : bUChObj);

        return data;
    }
}
