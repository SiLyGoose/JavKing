package javking.database;


import javking.JavKing;
import javking.audio.exec.BlockingTrackLoadingExecutor;
import javking.util.function.CheckedRunnable;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoManager {
    private final MongoService mongoService;
    private final Logger logger;

    public MongoManager() {
        mongoService = new MongoService();
        logger = LoggerFactory.getLogger(getClass());
    }

    public MongoService getMongoService() {
        return mongoService;
    }

    public void manageSession(JavKing instance, Guild guild, CheckedRunnable runnable) {
        new BlockingTrackLoadingExecutor().execute(runnable);
    }
}
