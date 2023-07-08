package javking.rest.controllers.models;

public abstract class ProxyChannel {
    public abstract String getVoiceId();
    public abstract void setVoiceId(String voiceId);
    public abstract String getVoiceName();
    public abstract void setVoiceName(String voiceName);
    public abstract boolean isBotJoinable();
    public abstract void setBotJoinable(boolean botJoinable);
    public abstract String getBotVoiceId();
    public abstract void setBotVoiceId(String botVoiceId);
    public abstract String getBotVoiceName();
    public abstract void setBotVoiceName(String botVoiceName);
    public abstract boolean isBotSpeakable();
    public abstract void setBotSpeakable(boolean botSpeakable);
}
