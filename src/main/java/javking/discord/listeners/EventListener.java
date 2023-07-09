package javking.discord.listeners;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import javking.JavKing;
import javking.audio.AudioManager;
import javking.audio.AudioPlayback;
import javking.audio.AudioQueue;
import javking.audio.exec.BlockingTrackLoadingExecutor;
import javking.concurrent.ScheduledTask;
import javking.discord.MessageService;
import javking.exceptions.UnavailableResourceException;
import javking.rest.controllers.GuildMemberManager;
import javking.rest.controllers.StationClient;
import javking.rest.payload.data.GuildMember;
import javking.templates.Templates;
import javking.util.TimeConvertingService;
import javking.util.function.ChainableRunnable;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import java.util.UUID;

import static javking.util.function.populator.SocketDataPopulator.*;

public class EventListener implements DataListener<String> {
    private final MessageService messageService;
    private final String event;
    private final EventKind eventKind;

    //    event listener for client side server
    public EventListener(String event, MessageService messageService) {
        this.messageService = messageService;
        this.event = event;
        if (event.equalsIgnoreCase("stationSkipped")) {
            this.eventKind = EventKind.SKIP;
        } else if (event.equalsIgnoreCase("stationRepeat")) {
            this.eventKind = EventKind.REPEAT;
        } else if (event.equalsIgnoreCase("stationPrevious")) {
            this.eventKind = EventKind.PREVIOUS;
        } else if (event.equalsIgnoreCase("stationShuffled")) {
            this.eventKind = EventKind.SHUFFLE;
        } else if (event.equalsIgnoreCase("stationAccessed")) {
            this.eventKind = EventKind.ACCESS;
        } else if (event.equalsIgnoreCase("stationSeek")) {
            this.eventKind = EventKind.SEEK;
        } else if (event.equalsIgnoreCase("stationTrackRemoved")) {
            this.eventKind = EventKind.REMOVED;
        } else {
            this.eventKind = EventKind.PAUSE;
        }
    }

    @Override
    public void onData(final SocketIOClient socketIOClient, String data, AckRequest ackRequest) throws Exception {
        JSONObject d = new JSONObject(data);
        GuildMember guildMember = GuildMemberManager.getGuildMember(d.getString("t"));
        String userId = guildMember.getId();

        StationClient client = VoiceUpdateListener.getClientById(userId);
        User user = JavKing.get().getShardManager().getUserById(userId);

        if (user == null || client == null) return;

//        execute call only if not rate limited (1 calls per 2 seconds)
        if (client.getRateLimiter().acquirePermission()) {
            new BlockingTrackLoadingExecutor().execute(eventKind.onData(socketIOClient, d, client, user, ackRequest, messageService));
        }
    }

    enum EventKind {
        REMOVED {
            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest ackRequest, MessageService messageService) throws Exception {
                return () -> {
                    Guild guild = client.getGuild();
                    AudioManager audioManager = JavKing.get().getAudioManager();
                    AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                    int index = d.getInt("index");

                    audioPlayback.remove(index);

                    messageService.sendBold(Templates.command.blue_check_mark.formatFull(
                            String.format("%s removed track at position `%s`", user.getName(), index)), audioPlayback.getChannel());

                    try {
                        handleQueueMutatorEvent("stationUpdate", user.getId(), audioPlayback);
                    } catch (UnavailableResourceException e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        },
        SEEK {
            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest ackRequest, MessageService messageService) throws Exception {
                return () -> {
                    Guild guild = client.getGuild();
                    AudioManager audioManager = JavKing.get().getAudioManager();
                    AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                    long positionMs = d.getLong("positionMs");

                    audioPlayback.setCurrentPositionMs(positionMs);

                    messageService.sendBold(Templates.command.blue_check_mark.formatFull(
                            String.format("%s updated track to play from: `%s`", user.getName(), TimeConvertingService.millisecondsToHHMMSS(positionMs))), audioPlayback.getChannel());

                    VoiceUpdateListener.sendEvent("timeUpdate", client, new JSONObject().put("positionMs", positionMs));
                };
            }
        },
        ACCESS {
            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();
                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                JSONObject data = new JSONObject();
                data.put("positionMs", audioPlayback.getCurrentPositionMs());
                data.put("position", audioPlayback.getAudioQueue().getPosition());

                ScheduledTask task = client.getScheduledTask();
                if (audioPlayback.isPlaying() && !task.isInProgress()) task.startScheduledTask(audioPlayback, client);

                return () -> {
                    try {
                        handleQueueMutatorEvent("stationUpdate", user.getId(), audioPlayback, data);
                    } catch (UnavailableResourceException e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        },

        SKIP {
            public ChainableRunnable onDataEvent(Guild guild, AudioManager audioManager, AudioPlayback audioPlayback, User
                    user, MessageService messageService) throws Exception {
                return new ChainableRunnable() {
                    @Override
                    public void doRun() throws Exception {
                        AudioQueue audioQueue = audioPlayback.getAudioQueue();
                        MessageChannel channel = audioPlayback.getChannel();

                        if (!audioQueue.hasNext()) {
                            audioPlayback.clear(true);
                            audioPlayback.stop();
                        } else {
//                website version cannot skip more than one song per click
                            audioQueue.setPosition(audioQueue.getPosition() + 1);
                            audioManager.startPlaying(guild, false);
                        }

                        if (channel == null) return;
                        messageService.sendBold(Templates.music.skipped_song.formatFull(user.getName() + " skipped the current track!"), channel);
                    }
                };
            }

            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest
                    ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();
                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                return onDataEvent(guild, audioManager, audioPlayback, user, messageService)
                        .then(() -> {
                            try {
                                handleQueueMutatorEvent("stationUpdate", user.getId(), audioPlayback);
                            } catch (UnavailableResourceException e) {
//                                send error message back to client
//                                socketIOClient.sendEvent("queueMutatorError", data);
                            }
                        });
            }
        },

        PAUSE {
            public ChainableRunnable onDataEvent(AudioPlayback audioPlayback, JSONObject d, User user, MessageService
                    messageService) throws Exception {
                return new ChainableRunnable() {
                    @Override
                    public void doRun() throws Exception {
                        String message = user.getName();

//                return whether player is paused
                        if (d.getBoolean("d")) {
                            audioPlayback.resume();
                            message += " resumed";
                            message = Templates.music.resumed_queue.formatFull(message + " the player!");
                        } else {
                            audioPlayback.pause();
                            message += " paused";
                            message = Templates.music.paused_queue.formatFull(message + " the player!");
                        }

                        if (audioPlayback.getChannel() == null) return;
                        messageService.sendBold(message, audioPlayback.getChannel());
                    }
                };
            }

            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest
                    ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();
                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                return onDataEvent(audioPlayback, d, user, messageService)
                        .then(() -> handleTrackMutatorEvent("stationUpdate", user.getId(), audioPlayback));
            }
        },

        REPEAT {
            public ChainableRunnable onDataEvent(AudioPlayback audioPlayback, JSONObject d, User user, MessageService
                    messageService) throws Exception {
                return new ChainableRunnable() {
                    @Override
                    public void doRun() throws Exception {
                        String message = user.getName() + " set repeat ";

//                        rO: repeatOne, rA: repeatAll
                        if (d.getBoolean("rA")) {
                            audioPlayback.setRepeatAll(true);
                            audioPlayback.setRepeatOne(false);
                            message += "queue!";
                            message = Templates.music.repeat_queue.formatFull(message);
                        } else if (d.getBoolean("rO")) {
                            audioPlayback.setRepeatAll(false);
                            audioPlayback.setRepeatOne(true);
                            message += "track!";
                            message = Templates.music.repeat_song.formatFull(message);
                        } else {
                            audioPlayback.setRepeatAll(false);
                            audioPlayback.setRepeatOne(false);
                            message += "off!";
                            message = Templates.music.repeat_song.formatFull(message);
                        }

                        if (audioPlayback.getChannel() == null) return;
                        messageService.sendBold(message, audioPlayback.getChannel());
                    }
                };
            }

            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest
                    ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();
                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                return onDataEvent(audioPlayback, d, user, messageService)
                        .then(() -> handleTrackMutatorEvent("stationUpdate", user.getId(), audioPlayback));
            }
        },

        SHUFFLE {
            public ChainableRunnable onDataEvent(AudioPlayback audioPlayback, User user, MessageService messageService) throws
                    Exception {
                return new ChainableRunnable() {
                    @Override
                    public void doRun() throws Exception {
                        String message = user.getName();

//                return true if queue is shuffled
                        if (audioPlayback.isShuffled()) {
                            audioPlayback.setShuffled(false);
                            message += " normalized the queue!";
                        } else {
                            audioPlayback.setShuffled(true);
                            message += " shuffled the queue!";
                        }

                        if (audioPlayback.getChannel() == null) return;
                        messageService.sendBold(Templates.music.shuffle_queue.formatFull(message), audioPlayback.getChannel());
                    }
                };
            }

            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest
                    ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();
                AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);

                return onDataEvent(audioPlayback, user, messageService)
                        .then(() -> {
                            try {
                                handleQueueMutatorEvent("stationUpdate", user.getId(), audioPlayback);
                            } catch (UnavailableResourceException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        },

        PREVIOUS {
            public ChainableRunnable onDataEvent(AudioManager audioManager, JSONObject d, User user, Guild guild, MessageService messageService) throws
                    Exception {
                return new ChainableRunnable() {
                    @Override
                    public void doRun() throws Exception {
                        AudioPlayback audioPlayback = audioManager.getPlaybackForGuild(guild);
                        AudioQueue audioQueue = audioPlayback.getAudioQueue();

                        String message = user.getName() + " has started playing the ";

//                        true if previous track is requested. initial request sets current positionMs
//                        to 0 and sequential requests, with refreshing timers, moves the current position
//                        backwards to play "played" songs again
                        if (d.getBoolean("loadPrevious") && audioQueue.hasPrevious()) {
                            audioQueue.setPosition(audioQueue.getPosition() - 1);
                            message += "previous track!";
                            handleQueueMutatorEvent("stationUpdate", user.getId(), audioPlayback);
                        } else {
                            audioPlayback.setCurrentPositionMs(0);
                            message += "track from the beginning!";
                            handleTrackUpdateEvent("stationUpdate", user.getId(), audioPlayback);
                        }

                        audioManager.startPlaying(guild, true);

                        messageService.sendBold(Templates.command.blue_check_mark.formatFull(message), audioPlayback.getChannel());
                    }
                };
            }

            @Override
            public Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest
                    ackRequest, MessageService messageService) throws Exception {
                Guild guild = client.getGuild();
                AudioManager audioManager = JavKing.get().getAudioManager();

                return onDataEvent(audioManager, d, user, guild, messageService);
            }
        };

        public abstract Runnable onData(SocketIOClient socketIOClient, JSONObject d, StationClient client, User user, AckRequest ackRequest, MessageService messageService) throws Exception;
    }
}
