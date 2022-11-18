package javking.commands.admin;

import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.exceptions.CommandRuntimeException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import net.dv8tion.jda.api.entities.Guild;

import java.io.FileOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

public class savequeue extends AbstractCommand {
    public savequeue() {
        super.setRequiresExecAdmin(true);
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
        JavKing instance = JavKing.get();
        List<Guild> guildList = instance.getShardManager().getGuilds();

//        JSONObject parentObject = new JSONObject();
        String filePath = "./queue.save";

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

        try {

            HashMap<String, HashMap<AudioManager, AudioPlayback>> objectStream = new HashMap<>();

            for (Guild guild : guildList) {
                AudioManager audioManager = instance.getAudioManager();
                AudioQueue audioQueue = audioManager.getQueue(guild);
                if (audioQueue.isEmpty()) continue;

                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                HashMap<AudioManager, AudioPlayback> audioStream = new HashMap<>();
                audioStream.put(audioManager, audioPlayback);
                objectStream.put(guild.getId(), audioStream);

//          playable queue in json form
//            List<JSONObject> nChildObjects = new ArrayList<>();

//            for (Playable playable : playables) {
//                JSONObject nChildObject = new JSONObject();
//                try {
//                    nChildObject.put("uri", playable.getPlaybackUrl());
//                    nChildObject.put("requester", playable.getRequester().getId());
//                } catch (UnavailableResourceException e) {
//                    e.printStackTrace();
//                }
////              individual songs added in json form
//                nChildObjects.add(nChildObject);
//            }
//
//            JSONObject childObject = new JSONObject();
//            childObject.put("channelId", audioPlayback.getChannel().getId());
//            childObject.put("voiceId", audioPlayback.getVoiceChannel().getId());
//            childObject.put("queue", nChildObjects);
//
//            parentObject.put(guild.getId(), childObject);
            }

            objectOutputStream.writeObject(objectStream);

        } catch (NotSerializableException e) {
//            clears the file
//            smth about guildContexts being kept alive??
            new FileOutputStream(filePath).close();
            CommandRuntimeException.throwException(e);
        } finally {
            objectOutputStream.flush();
            objectOutputStream.close();
        }

        getMessageService().sendBold("Successfully saved queues!", context.getChannel());
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
