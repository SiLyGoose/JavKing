package javking.exceptions;

import javking.models.command.Command;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class UserException extends RuntimeException {
    public UserException() {
        super();
    }

    public UserException(String message) {
        super(checkSize(message));
    }

    public UserException(String message, Throwable cause) {
        super(checkSize(message), cause);
    }

    public UserException(Throwable cause) {
        super(cause);
    }

    private static String checkSize(String message) {
        if (message.length() > 1000) return message.substring(0, 1000) + "...";
        return message;
    }

    public MessageEmbed buildHelpEmbed(Command command) {
        return command.buildHelpEmbed();
    }
}
