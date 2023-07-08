package javking.models.command;

import javking.models.guild.GuildContext;
import javking.models.guild.user.UserContext;
import javking.util.Spotify.SpotifyService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import se.michaelthelin.spotify.SpotifyApi;

import javax.annotation.Nullable;
import java.util.UUID;

public class CommandContext {
    protected final Guild guild;
    protected final GuildContext guildContext;
    protected final JDA jda;
    protected final Member member;
    protected final Message message;
    protected final SpotifyApi.Builder spotifyApiBuilder;
    protected final String[] commandBody;
    protected final String id;
    protected final UserContext userContext;

    protected SpotifyService spotifyService;

    public CommandContext(MessageReceivedEvent event, GuildContext guildContext, SpotifyApi.Builder spotifyApiBuilder, String[] commandBody) {
        this(event.getGuild(), guildContext, event.getJDA(), event.getMember(), event.getMessage(), spotifyApiBuilder, commandBody, new UserContext(event.getAuthor()));
    }

    public CommandContext(MessageReactionAddEvent event, GuildContext guildContext, Message message, SpotifyApi.Builder spotifyApiBuilder, String[] commandBody) {
        this(event.getGuild(), guildContext, event.getJDA(), event.getMember(), message, spotifyApiBuilder, commandBody, new UserContext(event.getUser()));
    }

    public CommandContext(Guild guild, GuildContext guildContext, JDA jda, Member member, Message message, SpotifyApi.Builder spotifyApiBuilder, String[] commandBody, UserContext userContext) {
        this(guild, guildContext, jda, member, message, spotifyApiBuilder, UUID.randomUUID().toString(), commandBody, userContext);
    }

    public CommandContext(Guild guild, GuildContext guildContext, JDA jda, Member member, Message message, SpotifyApi.Builder spotifyApiBuilder, String id, String[] commandBody, UserContext userContext) {
        this.guild = guild;
        this.guildContext = guildContext;
        this.jda = jda;
        this.member = member;
        this.message = message;
        this.spotifyApiBuilder = spotifyApiBuilder;
        this.id = id;
        this.commandBody = commandBody;
        this.userContext = userContext;
    }

    public Message getMessage() {
        return message;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public Member getMember() {
        return member;
    }

    @Nullable
    public VoiceChannel getVoiceChannel() {
        GuildVoiceState voiceState = getMember().getVoiceState();

        if (voiceState != null) {
            return (VoiceChannel) voiceState.getChannel();
        }

        return null;
    }

    public Guild getGuild() {
        return guild;
    }

    public MessageChannel getChannel() {
        return message.getChannel();
    }

    public JDA getJda() {
        return jda;
    }

    public String[] getCommandBody() {
        return commandBody;
    }

    public SpotifyApi.Builder getSpotifyApiBuilder() {
        return spotifyApiBuilder;
    }

    public GuildContext getGuildContext() {
        return guildContext;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return "CommandContext@" + id;
    }

    public SpotifyService getSpotifyService() {
        if (spotifyService == null) {
            spotifyService = new SpotifyService(spotifyApiBuilder.build());
        }

        return spotifyService;
    }
}
