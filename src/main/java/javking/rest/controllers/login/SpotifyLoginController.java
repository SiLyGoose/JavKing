package javking.rest.controllers.login;

import jakarta.servlet.http.HttpServletResponse;
import javking.JavKing;
import javking.util.PropertiesLoadingService;
import javking.util.Spotify.login.Login;
import javking.util.Spotify.login.LoginManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;

import java.io.IOException;
import java.util.Objects;

@Controller
public class SpotifyLoginController {
    @GetMapping(value = "spotify-login-callback")
    public String getSpotifyUserCode(@RequestParam("code") String code, @RequestParam("state") String userGuildId, HttpServletResponse response) throws IOException {
        JavKing instance = JavKing.get();
        SpotifyApi spotifyApi = instance.getSpotifyApiBuilder().build();
        AuthorizationCodeRequest codeRequest = spotifyApi.authorizationCode(code).build();

        try {
            final AuthorizationCodeCredentials codeCredentials = codeRequest.execute();

            String userId = userGuildId.substring(0, 18);
            String guildId = userGuildId.substring(18);
            Guild guild = instance.getShardManager().getGuildById(guildId);
            assert guild != null;
            User user = Objects.requireNonNull(guild.getMemberById(userId)).getUser();

            Login login = new Login(user, codeCredentials.getAccessToken(), codeCredentials.getRefreshToken(),
                    codeCredentials.getExpiresIn(), spotifyApi);
            LoginManager loginManager = instance.getLoginManager();
            loginManager.getPendingLogin(user).complete(login);
            loginManager.addLogin(login);

            spotifyApi.setAccessToken(codeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(codeCredentials.getRefreshToken());

            System.out.println("Login successful from user " + login.getUser().getAsTag() +
                    " | Expires in: " + codeCredentials.getExpiresIn());
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            e.printStackTrace();
        }
//        change redirect to successful login and return to discord
        response.sendRedirect(PropertiesLoadingService.requireProperty("BOT_SITE"));
        return null;
    }
}
