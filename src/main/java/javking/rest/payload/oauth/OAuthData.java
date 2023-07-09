package javking.rest.payload.oauth;

import java.util.UUID;

public class OAuthData {
    private String access_token;
    private String token_type;
    private int expires_in;
    private String refresh_token;
    private UUID uuid;

    public OAuthData(String access_token, String token_type, int expires_in, String refresh_token, String uuid) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.uuid = UUID.fromString(uuid);
    }

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public int getExpiresIn() {
        return expires_in;
    }

    public void setExpiresIn(int expires_in) {
        this.expires_in = expires_in;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        setUuid(UUID.fromString(uuid));
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
