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

    public void setUserChannel(UserChannel userChannel) {
        if (userChannel == null) {
            userChannel = new UserChannel();
            userChannel.setVoiceId(null);
            userChannel.setVoiceName(null);
            userChannel.setBotJoinable(false);
        }
        this.userChannel = userChannel;
    }

    public BotChannel getBotChannel() {
        if (botChannel == null) setBotChannel(new BotChannel());
        return botChannel;
    }

    public void setBotChannel(BotChannel botChannel) {
        if (botChannel == null) {
            botChannel = new BotChannel();
            botChannel.setBotVoiceId(null);
            botChannel.setBotVoiceName(null);
            botChannel.setBotSpeakable(false);
        }
        this.botChannel = botChannel;
    }
}
