package javking.exceptions;

public class VoiceStateException extends RuntimeException {
//    check if user is deafened of any sort
//    could also be thrown if user voice state is not recognized
    public VoiceStateException(String message) {
        super(message);
    }
}
