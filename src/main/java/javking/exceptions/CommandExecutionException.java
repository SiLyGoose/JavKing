package javking.exceptions;

public class CommandExecutionException extends RuntimeException {
//    thrown whenever command fails from behind the scenes
//    example: queue size is 0 and user attempts to pull up queue
    public CommandExecutionException(String message) {
        super(message);
    }
}
