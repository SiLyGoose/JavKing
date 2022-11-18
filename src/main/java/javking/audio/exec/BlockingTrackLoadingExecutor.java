package javking.audio.exec;

// blocks progress of current thread until runnable is completed
// necessary for when data needs to be fetched for completion
public class BlockingTrackLoadingExecutor implements TrackLoadingExecutor {
    @Override
    public void execute(Runnable trackLoadingRunnable) {
        trackLoadingRunnable.run();
    }
}
