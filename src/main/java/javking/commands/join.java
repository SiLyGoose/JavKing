package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import net.dv8tion.jda.api.entities.Guild;

public class join extends AbstractCommand {

    public join() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "Summons the bot to your voice channel";
    }

    @Override
    public String[] getAlias() {
        return new String[]{"summon"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();

        AudioManager audioManager = JavKing.get().getAudioManager();
        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);

        playback.joinChannel(audioManager, context);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
