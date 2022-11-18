package javking.rest.controllers;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class RestService {
    private final RestTemplate restTemplate;

    public RestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String getPostPlainJSON() {
        String url = "https://jsonplaceholder.typicode.com/posts";
        return this.restTemplate.getForObject(url, String.class);
    }

    public void updatePost(GuildMember userContext) {
        String url = "http://javking-api.herokuapp.com/guild-member";

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `content-type` header
        headers.setContentType(MediaType.APPLICATION_JSON);
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // create a post object
        Map<String, String> map = new HashMap<>();
        map.put("id", userContext.getId());
        map.put("username", userContext.getName());
        map.put("avatar", userContext.getAvatar());
        map.put("access_token", userContext.getAccessToken());
        map.put("token_type", userContext.getTokenType());
        map.put("guild_list", userContext.getGuildIdList());
        System.out.println(map);

        // build the request
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<GuildMember> response = this.restTemplate.postForEntity(url, entity, GuildMember.class);

        System.out.println(response.getStatusCode());
        if (response.getStatusCode() == HttpStatus.CREATED) {
            System.out.println(response.getBody());
        } else {
            System.out.println("null");
        }
        System.out.println(entity);

        // send PUT request to update post with `id` 10
//        restTemplate.put(url, entity, GuildMember.class);
    }
}
