package javking.audio;

import com.google.common.collect.Lists;
import javking.discord.listeners.VoiceUpdateListener;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.NoResultsFoundException;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;
import javking.util.YouTube.HollowYouTubeVideo;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AudioQueue implements Serializable {
    private static final long serialVersionUID = 12L;

    private final List<Playable> currentQueue = Lists.newArrayList();
    //    for when bot lags
//    keep static currentQueue and iterate through randomizedQueue
    private final List<Integer> shuffledQueue = Lists.newArrayList();

    private int currentTrack = 0;
    private boolean shuffled = false;
    private boolean repeatOne = false;
    private boolean repeatAll = false;

    public AudioQueue() {

    }

    public void add(Playable playable) {
        add(currentQueue.size(), playable);
    }

    public void add(int index, Playable playable) {
        currentQueue.add(index, playable);
        if (shuffled) shuffledQueue.add(index);
    }

    public Playable remove(int index) {
        Playable playable = currentQueue.remove(index);
//        adds to previousQueue if user wants to backtrack songs
//        previousQueue.add(playable);
        if (shuffled && !shuffledQueue.isEmpty()) shuffledQueue.remove(shuffledQueue.get(index));

        return playable;
    }

    public void iterate() {
        if (!hasNext()) return;

//        if (!repeatAll) {
//            remove(currentTrack);
//            currentTrack = 0;
//        } else {
        currentTrack = nextPosition();
//        }
    }

    public void reverse() {
        if (!hasPrevious()) return;

        currentTrack = previousPosition();
    }

    public boolean hasPrevious() {
        if (isEmpty()) return false;

        boolean inBound = currentTrack > 0;
        return inBound || isRepeatOne() || isRepeatAll();
    }

    public boolean hasNext() {
        if (isEmpty()) return false;
        return (currentTrack < currentQueue.size() - 1) || isRepeatOne() || isRepeatAll();
    }

    /**
     * @return next playable track in queue without triggering iteration
     */
    public Playable getNext() {
        if (!hasNext()) return null;
        if (isRepeatOne()) return getCurrent();

        return getTrack(nextPosition());
    }

    private int previousPosition() {
        if (currentTrack > 0) {
            return currentTrack - 1;
        } else {
            return isRepeatAll() ? getTracks().size() - 1 : 0;
        }
    }

    private int nextPosition() {
        if (currentTrack < currentQueue.size() - 1) {
            return currentTrack + 1;
        } else {
            return 0;
        }
    }

    public List<Playable> getTracks() {
        return currentQueue;
    }

    public Playable getTrack(int index) {
        if (size() == 0) throw new CommandExecutionException("No songs in current queue!");

        if (shuffled) return currentQueue.get(shuffledQueue.get(index));
        return currentQueue.get(index);
    }

    public int getPosition() {
        return currentTrack;
    }

    //    handles user skipping multiple songs at once
    public void setPosition(int position) {
//        for (int i = currentTrack; i < position; i++) {
//            Playable playable = remove(currentTrack);
//            if (repeatAll) add(playable);
//        }
        currentTrack = position;
    }

    public Playable getCurrent() {
        if (isEmpty()) {
            return null;
//            throw new NoResultsFoundException("Queue is empty");
        }

        return currentQueue.get(getCurrentTrackNumber());
    }

//    currentTrackNumber only used when getting current playable
//    to differentiate between shuffled or not. AudioQueue#getPosition returns
//    cursor that allows traversal of normalized/shuffled array.
    public int getCurrentTrackNumber() {
        if (shuffled) {
            return shuffledQueue.get(currentTrack);
        } else {
            return currentTrack;
        }
    }

    public boolean isEmpty() {
        return currentQueue.isEmpty();
    }

    public int size() {
        return currentQueue.size();
    }

    //    by default, protect currentTrack to ensure only songs ALREADY in queue are shuffled
//    queue must be reshuffled if additional are added
    public void shuffle() {
        if (currentTrack > 0) {
            List<Integer> indices = IntStream.range(0, currentTrack).boxed().collect(Collectors.toList());
            Collections.shuffle(indices);
            shuffledQueue.addAll(indices);
        }
        shuffledQueue.add(currentTrack);
        if (currentTrack < currentQueue.size() - 1) {
            List<Integer> indices = IntStream.range(currentTrack + 1, currentQueue.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(indices);
            shuffledQueue.addAll(indices);
        }
    }

    public boolean isShuffled() {
        return shuffled;
    }

    public void setShuffled(boolean shuffled) {
        if (shuffled) {
            shuffle();
        } else if (this.shuffled) {
//            when setting queue back to normal, the current track has to be adjusted
//            since currentTrack is the cursor rather than actual queue position
            currentTrack = shuffledQueue.get(currentTrack);
            shuffledQueue.clear();
        }

        this.shuffled = shuffled;
    }

    public boolean isRepeatOne() {
        return repeatOne;
    }

    public void setRepeatOne(boolean repeatOne) {
        this.repeatOne = repeatOne;
    }

    public boolean isRepeatAll() {
        return repeatAll;
    }

    public void setRepeatAll(boolean repeatAll) {
        this.repeatAll = repeatAll;
    }

    public void reset() {
        currentTrack = 0;
    }

    public void clear() {
        if (getCurrent() != null) {
            currentQueue.retainAll(Collections.singleton(getCurrent()));
        } else {
            currentQueue.clear();
        }
        shuffledQueue.clear();
        reset();
    }

    public void forceClear() {
        currentQueue.clear();
        shuffledQueue.clear();
        reset();
    }

    public long getTotalDuration() {
        long totalDuration = 0L;

        int startIndex = currentTrack;
        int endIndex = shuffled ? shuffledQueue.size() : currentQueue.size();

        for (int i = startIndex; i < endIndex; i++) {
            Playable playable = getTrack(i);
            try {
                totalDuration += playable.getDurationMs();
            } catch (UnavailableResourceException ignored) {
                totalDuration += playable.durationMs();
            }
        }

        return totalDuration;
    }
}

