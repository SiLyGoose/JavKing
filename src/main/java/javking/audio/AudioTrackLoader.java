package javking.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javking.exceptions.NoResultsFoundException;
import javking.templates.Templates;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.*;

public class AudioTrackLoader implements Serializable {
    private static final long serialVersionUID = 13L;

    private transient final AudioPlayerManager audioPlayerManager;
    private transient final Logger logger;

    public AudioTrackLoader(AudioPlayerManager audioPlayerManager) {
        this.audioPlayerManager = audioPlayerManager;
        logger = LoggerFactory.getLogger(getClass());
    }

    @Nullable
    public AudioItem loadByIdentifier(String identifier) {
        CompletableFuture<AudioItem> result = new CompletableFuture<>();
        audioPlayerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                result.complete(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                result.complete(audioPlaylist);
            }

            @Override
            public void noMatches() {
                result.cancel(false);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                result.completeExceptionally(e);
                if (e.severity != FriendlyException.Severity.COMMON) {
                    logger.error("Exception thrown while loading track " + identifier, e);
                }
            }
        });

        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause != null) {
                throw new RuntimeException(cause);
            }

            throw new RuntimeException(e);
        } catch (CancellationException e) {
            e.printStackTrace();
            throw new NoResultsFoundException("No results found for: " + identifier, e);
        } catch (TimeoutException e) {
            return null;
        }
    }
}
