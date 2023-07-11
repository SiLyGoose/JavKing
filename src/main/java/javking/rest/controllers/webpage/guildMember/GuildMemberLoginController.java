package javking.rest.controllers.webpage.guildMember;

import com.google.api.client.util.Lists;
import jakarta.servlet.http.HttpServletResponse;
import javking.discord.GuildManager;
import javking.rest.controllers.GuildMemberManager;
import javking.rest.controllers.models.ProxyGuild;
import javking.rest.payload.data.GuildMember;
import javking.rest.payload.oauth.OAuthData;
import javking.util.PropertiesLoadingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class GuildMemberLoginController {
    @PostMapping("/login-callback")
    public ResponseEntity<?> getGuildMemberCode(@RequestParam("code") String code, @RequestParam("uuid") String uuid, HttpServletResponse response) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String requestBody = String.format("client_id=%s&client_secret=%s&grant_type=%s&code=%s&redirect_uri=%s",
                PropertiesLoadingService.requireProperty("CLIENT_ID"),
                PropertiesLoadingService.requireProperty("CLIENT_SECRET"),
                "authorization_code",
                code,
                PropertiesLoadingService.requireProperty("CLIENT_REDIRECT"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/oauth2/token"))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> oauthResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        assert oauthResponse.body() != null;
        JSONObject postObject = new JSONObject(oauthResponse.body());
        System.out.println(postObject);
        String tokenType = postObject.getString("token_type");
        String accessToken = postObject.getString("access_token");
        String refreshToken = postObject.getString("refresh_token");
        int expiresIn = postObject.getInt("expires_in");

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me"))
                .header("Accept", "application/json")
                .header("Authorization", String.format("%s %s", tokenType, accessToken)).build();

        oauthResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assert oauthResponse.body() != null;
        JSONObject object = new JSONObject(oauthResponse.body());

//        user data
        System.out.println(object);
        String id = object.getString("id");
        String avatar = object.getString("avatar");
        String username = object.getString("username");
        String discriminator = object.getString("discriminator");

//      ordered first 10 guilds bot is in
        List<String> userGuildIdList = Lists.newArrayList();
        int min = Math.min(10, GuildManager.getGuildIdList().size());
        Collections.sort(GuildManager.getGuildIdList());
        for (int i = 0; i < min; i++) {
            userGuildIdList.add(GuildManager.getGuildIdList().get(i));
        }

//        fetch user guilds
        URI uri = URI.create("https://discord.com/api/v10/users/@me/guilds");
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", tokenType + " " + accessToken);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

//        int responseCode = connection.getResponseCode();

//        read user guilds response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder res = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            res.append(line);
        }
        reader.close();

        JSONArray resData = new JSONArray(res.toString());

        List<ProxyGuild> userGuildList = Lists.newArrayList();

        resData.forEach(guild -> {
            JSONObject guildJSON = new JSONObject(guild.toString());
            boolean hasIcon = guildJSON.isNull("icon");
            String icon = !hasIcon ? guildJSON.getString("icon") : "https://cdn.discordapp.com/embed/avatars/0.png";

            userGuildList.add(new ProxyGuild(guildJSON.getString("id"), guildJSON.getString("name"),
                    icon));
        });

//        sort by increasing guildId for user
        userGuildList.sort(Comparator.comparingLong(ProxyGuild::getIdLong));

//        filter guilds that have bot
        List<ProxyGuild> mutualGuildList = userGuildList.parallelStream().filter(guild -> userGuildIdList.contains(guild.getId())).collect(Collectors.toList());
        userGuildList.removeAll(mutualGuildList);

        connection.disconnect();

        GuildMember member;
        if (!GuildMemberManager.hasGuildMember(uuid)) {
            member = new GuildMember(uuid, id, username, discriminator, avatar, accessToken, refreshToken, tokenType, String.valueOf(expiresIn), mutualGuildList, userGuildList);
            GuildMemberManager.setGuildMember(uuid, member);
        } else member = GuildMemberManager.getGuildMember(uuid);

//        checks if user still registered by bot but token has expired
        if (!member.getUuid().equals(UUID.fromString(uuid))) {
            member.setUuid(uuid);
        }

        return ResponseEntity.ok().build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleInternalException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }
}
