package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.models.music.Playable;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.ArrayList;
import java.util.List;

public class leavecleanup extends AbstractCommand {

    public leavecleanup() {
        super.requiresVoice();
        super.requiresVoiceChannel();
    }

    @Override
    public String getDescription() {
        return "removes songs requested by absent users from queue";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"leavec", "lc"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        JavKing instance = JavKing.get();
        VoiceChannel voiceChannel = context.getVoiceChannel();
        MessageChannel messageChannel = context.getChannel();

        AudioManager audioManager = instance.getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        List<Playable> queue = playback.getAudioQueue().getTracks();
        List<Integer> replacement = new ArrayList<>();

        assert voiceChannel != null;
        List<String> memberListTag = new ArrayList<>();
        voiceChannel.getMembers().forEach(member -> memberListTag.add(member.getUser().getAsTag()));

        int counter = 0;

        for (int i = 1; i < queue.size(); i++) {
            Playable track = queue.get(i);
            if (memberListTag.contains(track.getRequester().getAsTag())) {
                replacement.add(i);
            } else counter++;
        }

        playback.remove(replacement);
        getMessageService().sendBold(Templates.command.blue_check_mark.formatFull(String.format("Removed `%d` songs"), counter), messageChannel);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
