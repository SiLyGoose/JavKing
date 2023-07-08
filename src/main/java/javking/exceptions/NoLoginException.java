package javking.exceptions;

import net.dv8tion.jda.api.entities.User;

public class NoLoginException extends UserException {

    /**
     * Thrown when Spotify login is needed but none found for specific user
     */
    public NoLoginException(User user) {
        super(String.format("User %s is not logged in. Continue by using the login command!", user.getAsMention()));
    }
}
