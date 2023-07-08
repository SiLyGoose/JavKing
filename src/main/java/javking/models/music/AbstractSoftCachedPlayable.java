package javking.models.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.annotation.Nullable;
import java.lang.ref.SoftReference;

public abstract class AbstractSoftCachedPlayable implements Playable {
    private SoftReference<AudioTrack> cachedTrack;

    @Nullable
    @Override
    public AudioTrack getCached() {
        if (cachedTrack != null) {
            return cachedTrack.get();
        }

        return null;
    }

    @Override
    public void setCached(AudioTrack audioTrack) {
        cachedTrack = new SoftReference<>(audioTrack);
    }
}
