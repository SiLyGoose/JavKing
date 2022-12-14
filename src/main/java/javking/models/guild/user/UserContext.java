package javking.models.guild.user;

import javking.JavKing;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;

public class UserContext implements User, Serializable {
    private static final long serialVersionUID = 40L;

    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private int expiresIn;

    private String name;
    private String discriminator;
    private String asMention;

    private String id;
    private String avatarId;

    private boolean isBot;
    private boolean isSystem;

    private int flagsRaw;

    private transient JDA jda;
    private transient User user;

    public UserContext(String accessToken, String tokenType, /*String refreshToken,*/ int expiresIn) {
//        setId(id);
//        setName(username);
//        setAvatarId(avatarId);
        setAccessToken(accessToken);
        setTokenType(tokenType);
//        setRefreshToken(refreshToken);
        setExpiresIn(expiresIn);
    }

    public UserContext(User user) {
        extractUser(user);
    }

    public void extractJSONObject(JSONObject jsonObject) {
        setAvatarId(jsonObject.getString("avatar"));
        setId(jsonObject.getString("id"));

        setName(jsonObject.getString("username"));
        setDiscriminator(jsonObject.getString("discriminator"));
        setAsMention("<!@" + getId() + ">");

        setIsBot(jsonObject.optBoolean("bot", false));
        setIsSystem(jsonObject.optBoolean("system", false));

        setFlagsRaw(jsonObject.getInt("flags"));
    }

    private void extractUser(User user) {
        setAvatarId(user.getAvatarId());
        setId(user.getId());

        setName(user.getName());
        setDiscriminator(user.getDiscriminator());
        setAsMention(user.getAsMention());

        setIsBot(user.isBot());
        setIsSystem(user.isSystem());

        setFlagsRaw(user.getFlagsRaw());

        setJDA(user.getJDA());
        setUser(user);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    @NotNull
    @Override
    public String getAsTag() {
        return name + "#" + discriminator;
    }

    @NotNull
    @Override
    public String getAsMention() {
        return asMention;
    }

    public void setAsMention(String asMention) {
        this.asMention = asMention;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getIdLong() {
        return Long.parseLong(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    @Override
    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    @NotNull
    @Override
    public String getDefaultAvatarId() {
        return "https://cdn.discordapp.com/embed/avatars/" + getDefaultAvatar() + ".png";
    }

    private int getDefaultAvatar() {
        return Integer.parseInt(getDiscriminator()) % 5;
    }

    @NotNull
    @Override
    public RestAction<Profile> retrieveProfile() {
        return null;
    }

    @Override
    public boolean hasPrivateChannel() {
        return false;
    }

    @NotNull
    @Override
    public RestAction<PrivateChannel> openPrivateChannel() {
        User user = JavKing.get().getShardManager().getUserById(id);
        assert user != null;
        return user.openPrivateChannel();
    }

    @NotNull
    @Override
    public List<Guild> getMutualGuilds() {
        return JavKing.get().getShardManager().getMutualGuilds(this);
    }

    @Override
    public boolean isBot() {
        return isBot;
    }

    public void setIsBot(boolean isBot) {
        this.isBot = isBot;
    }

    @Override
    public boolean isSystem() {
        return isSystem;
    }

    public void setIsSystem(boolean isSystem) {
        this.isSystem = isSystem;
    }

    @Override
    public JDA getJDA() {
        return jda;
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @NotNull
    @Override
    public EnumSet<UserFlag> getFlags() {
        return UserFlag.getFlags(flagsRaw);
    }

    @Override
    public int getFlagsRaw() {
        return flagsRaw;
    }

    public void setFlagsRaw(int flagsRaw) {
        this.flagsRaw = flagsRaw;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
}
