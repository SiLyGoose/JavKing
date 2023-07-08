package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.discord.listeners.VoiceUpdateListener;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractPlayCommand;
import javking.models.music.Playable;
import javking.rest.controllers.StationClient;
import javking.templates.Templates;
import javking.util.Spotify.SpotifyUri;
import javking.util.YouTube.HollowYouTubeVideo;
import javking.util.YouTube.YouTubeUri;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import java.util.Arrays;

public class play extends AbstractPlayCommand {

    public play() {
        super();
    }

    @Override
    public String getDescription() {
        return "plays a YouTube or Spotify song";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"p"};
    }

    @Override
    public String[] getUsage() {
        return new String[]{"<url/keywords>"};
    }

    @Override
    public boolean ignoreQueue() {
        return true;
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        JavKing instance = JavKing.get();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

        // TODO: efficiently determine source and redirect
        String[] input = getCommandBody();

        if (input.length == 0) {
            getMessageService().send(buildHelpEmbed(), getContext().getChannel());
            return;
        }

        instance.getCommandManager().executeCommand("join", context, instance.getExecutionQueueManager().getForGuild(guild));

        if (YouTubeUri.isYouTubeUri(input[0])) {
            loadYouTube(audioManager, true);
        } else if (SpotifyUri.isSpotifyUri(input[0])) {
            loadSpotify(audioManager);
//            not sure how to use scsearch: without soundcloud api
//        } else if (SoundCloudUri.isSoundCloudUri(input[0])) {
//            loadSoundCloud(audioManager);
        } else if (input[0].equalsIgnoreCase("lastplayed")) {
            loadYouTube(audioManager, instance);
        } else {
            loadYouTube(audioManager, false);
        }

        audioManager.startPlaying(guild, audioPlayback.isPlaying());
    }

    @Override
    public void onFailed() {
        getMessageService().sendBold(Templates.command.boom.formatFull("Unable to load song. Skipping..."), getContext().getChannel());
    }

    @Override
    public void onSuccess() {

    }
}
