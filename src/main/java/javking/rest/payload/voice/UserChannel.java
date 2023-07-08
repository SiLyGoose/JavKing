package javking.rest.payload.voice;

public class UserChannel {

    private String voiceId, voiceName;
    private boolean botJoinable;

    public UserChannel() {

    }

    public UserChannel(String voiceId, String voiceName, boolean botJoinable) {
        this.voiceId = voiceId;
        this.voiceName = voiceName;
        this.botJoinable = botJoinable;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public boolean isBotJoinable() {
        return botJoinable;
    }

    public void setBotJoinable(boolean botJoinable) {
        this.botJoinable = botJoinable;
    }
}
