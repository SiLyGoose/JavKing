package javking.commands;

import javking.JavKing;
import javking.exceptions.UserException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import javking.util.Spotify.login.Login;
import javking.util.Spotify.login.LoginManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.util.concurrent.*;

public class login extends AbstractCommand {
    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        User user = context.getUserContext();
        AuthorizationCodeUriRequest uriRequest = context.getSpotifyApiBuilder().build().authorizationCodeUri()
                .show_dialog(true)
                .state(user.getId() + context.getGuild().getId())
                .scope("playlist-read-private,playlist-read-collaborative,user-library-read")
                .build();

        LoginManager loginManager = JavKing.get().getLoginManager();
        CompletableFuture<Login> pendingLogin = new CompletableFuture<>();
        loginManager.expectLogin(user, pendingLogin);

        String loginUri = uriRequest.execute().toString();
        EmbedBuilder loginLinkEmbed = new EmbedTemplate()
                .setTitle("Spotify Login")
                .setDescription(String.format("Click [here](%s) to be redirected to Spotify", loginUri))
                .setColor(0x1DB954);
        CompletableFuture<Message> futurePrivateMessage = getMessageService().send(loginLinkEmbed.build(), user);
        CompletableFuture<Message> futureNoticeMessage = new CompletableFuture<>();
        try {
            futurePrivateMessage.get();
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setDescription("I have sent you a login link");
            getMessageService().send(embedBuilder, context.getChannel()).thenAccept(futureNoticeMessage::complete);
        } catch (CancellationException | ExecutionException e) {
            loginManager.removePendingLogin(user);
            throw new UserException("I was unable to send you a message. Please adjust your privacy settings to allow direct messages from guild members.");
        } catch (InterruptedException ignored) {
        }

        CompletableFuture<Login> futureLogin = pendingLogin.orTimeout(10, TimeUnit.MINUTES);
        futureLogin.whenComplete((login, throwable) -> {
            futureNoticeMessage.thenAccept(message -> message.delete().queue());
            futurePrivateMessage.thenAccept(message -> message.delete().queue());
            if (login != null) {
                getMessageService().send(Templates.command.blue_check_mark.formatFull("You have successfully connected your Spotify account and may now search and play tracks from your library"), user);
                getMessageService().sendBold(Templates.command.blue_check_mark.formatFull("User " + getContext().getUserContext().getName() + " logged in to Spotify"), context.getChannel());
            }
            if (throwable != null) {
                loginManager.removePendingLogin(user);

                if (throwable instanceof TimeoutException) {
                    getMessageService().sendBold(Templates.command.x_mark.formatFull("Login attempt timed out"), user);
                } else {
                    getMessageService().sendException("There has been an unexpected error while completing your login, please try again.", getContext().getChannel());
                    LoggerFactory.getLogger(getClass()).error("unexpected exception while completing login", throwable);
                }
                setFailed(true);
            }
        });
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
