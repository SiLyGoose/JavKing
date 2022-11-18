package javking.commands;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class clear extends AbstractCommand {

    public clear() {
        super.setRequiresVoice(true);
        super.setRequiresVoiceChannel(true);
    }

    @Override
    public String getDescription() {
        return "clears queue";
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        Guild guild = context.getGuild();
        MessageChannel messageChannel = context.getChannel();
        AudioManager audioManager = JavKing.get().getAudioManager();

        AudioPlayback playback = audioManager.getPlaybackForGuild(guild);
        playback.clear(false);

        getMessageService().sendBold(Templates.command.boom.formatFull("Queue cleared!"), messageChannel);
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
