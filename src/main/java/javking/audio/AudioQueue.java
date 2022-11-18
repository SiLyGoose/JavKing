package javking.audio;

import com.google.common.collect.Lists;
import javking.exceptions.CommandExecutionException;
import javking.exceptions.NoResultsFoundException;
import javking.exceptions.UnavailableResourceException;
import javking.models.music.Playable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class AudioQueue implements Serializable {
    private static final long serialVersionUID = 12L;

    private final List<Playable> currentQueue = Lists.newArrayList();
//    for when bot lags
//    keep static currentQueue and iterate through randomizedQueue
//    private final List<Integer> randomizedQueue = Lists.newArrayList();

    private int currentTrack = 0;
    private boolean repeatOne = false;
    private boolean repeatAll = false;
    private long totalDuration = 0L;

    public AudioQueue() {

    }

    public void add(Playable playable) {
        add(currentQueue.size(), playable);
    }

    public void add(int index, Playable playable) {
        currentQueue.add(index, playable);
        increaseTotalDuration(playable.durationMs());
    }

    public Playable remove(int index) {
        return currentQueue.remove(index);
    }

    public void iterate() {
        Playable playable = remove(0);
        if (repeatAll) add(playable);

        try {
            decreaseTotalDuration(playable.getDurationMs());
        } catch (UnavailableResourceException e) {
            decreaseTotalDuration(playable.durationMs());
        }
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

        return currentQueue.get(nextPosition());
    }

    private int nextPosition() {
        if (currentTrack < currentQueue.size() - 1) {
            return currentTrack + 1;
        } else return 0;
    }

    public List<Playable> getTracks() {
        return currentQueue;
    }

    public Playable getTrack(int index) {
        if (size() == 0) throw new CommandExecutionException("No songs in current queue!");
        return getTracks().get(index);
    }

    public Playable getPlaying() {
        return getTrack(currentTrack);
    }

    public Playable getFirst() {
        return getTrack(nextPosition());
    }

    public Playable getLast() {
        return getTrack(getTracks().size() - 1);
    }

    public int getPosition() {
        return currentTrack;
    }

    public void setPosition(int position) {
        currentTrack = position;
    }

    public int getCurrentTrackNumber() {
        return currentTrack;
    }

    public Playable getCurrent() {
        if (isEmpty()) {
            throw new NoResultsFoundException("Queue is empty");
        }
        return currentQueue.get(currentTrack);
    }

    public boolean isEmpty() {
        return currentQueue.isEmpty();
    }

    public int size() {
        return currentQueue.size();
    }

    /**
     * quicker to shuffle ints and match with currentQueue
     * range starts at 1 to protect currently playing song
     */
    public void shuffle() {
        Playable item = getPlaying();

        currentQueue.remove(item);
        Collections.shuffle(currentQueue);
        currentQueue.add(0, item);
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
            setTotalDuration(getCurrent().durationMs());
        } else setTotalDuration(0);
        reset();
    }

    public void forceClear() {
        currentQueue.clear();
        setTotalDuration(0);
        reset();
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void increaseTotalDuration(long totalDuration) {
        this.totalDuration += totalDuration;
    }

    public void decreaseTotalDuration(long totalDuration) {
        this.totalDuration -= totalDuration;
    }
}
