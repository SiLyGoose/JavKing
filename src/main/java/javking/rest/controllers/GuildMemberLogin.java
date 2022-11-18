package javking.rest.controllers;

import javking.discord.GuildManager;
import javking.rest.request.RequestContext;
import javking.rest.request.RequestHandlerInterceptor;
import javking.util.PropertiesLoadingService;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

@Controller
public class GuildMemberLogin {
    private static final HashMap<String, Cookie> guildMemberList = new HashMap<>();

    @GetMapping(value = "guild-member-login-callback")
    public void getGuildMemberCode(@RequestParam("code") String code, @RequestParam("state") String postRedirectPath, HttpServletResponse response) throws Exception {
//        clean up requesthandlerinterceptor
//        see below
        RequestHandlerInterceptor requestHandlerInterceptor = new RequestHandlerInterceptor(response);
        RequestContext context = requestHandlerInterceptor.getRequestContext();

        context.setUrl("https://discord.com/api/v10/oauth2/token")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .concatRequestBody("client_id", PropertiesLoadingService.requireProperty("CLIENT_ID"))
                .concatRequestBody("client_secret", PropertiesLoadingService.requireProperty("CLIENT_SECRET"))
                .concatRequestBody("grant_type", "authorization_code")
                .concatRequestBody("code", code)
                .concatRequestBody("redirect_uri", PropertiesLoadingService.requireProperty("CLIENT_REDIRECT"))
                .post();

        Response oauthResponse = requestHandlerInterceptor.executeRequest(null).getResponse();

        assert oauthResponse.body() != null;
        JSONObject postObject = new JSONObject(oauthResponse.body().string());
        String tokenType = postObject.getString("token_type");
        String accessToken = postObject.getString("access_token");
//        String refreshToken = postObject.getString("refresh_token");
        int expiresIn = postObject.getInt("expires_in");

        requestHandlerInterceptor.newRequestContext()
                .setUrl("https://discord.com/api/v10/users/@me")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", String.format("%s %s", tokenType, accessToken))
                .get();

        oauthResponse = requestHandlerInterceptor.executeRequest(null).getResponse();
        assert oauthResponse.body() != null;
        String body = oauthResponse.body().string();
        JSONObject object = new JSONObject(body);

        String id = object.getString("id");
        String avatar = object.getString("avatar");
        String username = object.getString("username");

        StringBuilder sb = new StringBuilder();
        int min = Math.min(10, GuildManager.getGuildIdList().size());
        Collections.sort(GuildManager.getGuildIdList());
        System.out.println(GuildManager.getGuildIdList());
        for (int i = 0; i < min; i++) {
            sb.append(GuildManager.getGuildIdList().get(i));
            if (i != min - 1) {
                sb.append(",");
            }
        }

//        UserContext user;
        GuildMember member;
        if (!GuildMemberManager.hasGuildMember(id)) {
//            user = new UserContext(accessToken, tokenType, /*refreshToken,*/ expiresIn);
//            user.extractJSONObject(object);
            member = new GuildMember(id, username, avatar, accessToken, tokenType, sb.toString());
            GuildMemberManager.setGuildMember(id, member);
        } else member = GuildMemberManager.getGuildMember(id);

        System.out.println(member);
        new RestService(new RestTemplateBuilder()).updatePost(member);

        response.sendRedirect("http://zenpai.herokuapp.com/projects/JavKing" + postRedirectPath + "?id=" + id);
    }

    @ExceptionHandler(Exception.class)
    public void getGuildMemberCodeError(Exception e, HttpServletResponse response) throws IOException {
        e.printStackTrace();
//        response.sendRedirect("http://zenpai.herokuapp.com/projects/JavKing/home.html");
    }
}
