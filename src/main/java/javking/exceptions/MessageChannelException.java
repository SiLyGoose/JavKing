package javking.exceptions;

public class MessageChannelException extends RuntimeException {
//    checks if bot is able to link to text channel
    public MessageChannelException(String message) {
        super(message);
    }
}
