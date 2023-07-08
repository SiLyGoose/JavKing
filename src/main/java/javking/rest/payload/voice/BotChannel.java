package javking.rest.payload.voice;

public class BotChannel {
    private String botVoiceId, botVoiceName;
    private boolean botSpeakable;

    public BotChannel() {

    }

    public BotChannel(String botVoiceId, String botVoiceName, boolean botSpeakable) {
        this.botVoiceId = botVoiceId;
        this.botVoiceName = botVoiceName;
        this.botSpeakable = botSpeakable;
    }

    public String getBotVoiceId() {
        return botVoiceId;
    }

    public void setBotVoiceId(String botVoiceId) {
        this.botVoiceId = botVoiceId;
    }

    public String getBotVoiceName() {
        return botVoiceName;
    }

    public void setBotVoiceName(String botVoiceName) {
        this.botVoiceName = botVoiceName;
    }

    public boolean isBotSpeakable() {
        return botSpeakable;
    }

    public void setBotSpeakable(boolean botSpeakable) {
        this.botSpeakable = botSpeakable;
    }
}
