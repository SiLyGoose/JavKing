package javking.exceptions;

public class VoiceChannelException extends RuntimeException {
//    check if user is in voice channel
//    could also be thrown if unable to join voice channel
    public VoiceChannelException(String message) {
        super(message);
    }
}
