package javking.commands.manager;

import javking.concurrent.ThreadExecutionQueue;
import javking.util.ISnowflakeMap;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CommandExecutionQueueManager {
    private final ISnowflakeMap<ThreadExecutionQueue> guildExecutionQueues;

    private static final int EXECUTION_QUEUE_SIZE = 3;
    private static final int EXECUTION_QUEUE_CONCURRENT_SIZE = 3;
    private static final int RATE_LIMIT_FOR_PERIOD = 4;
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofSeconds(5);
    private static final Duration RATE_LIMIT_VIOLATION_TIMEOUT = Duration.ofSeconds(5);

    private static final ThreadPoolExecutor GLOBAL_POOL = new ThreadPoolExecutor(3, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactory() {
                private final AtomicLong threadId = new AtomicLong(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("command-execution-queue-idle-thread-" + threadId.getAndIncrement());
//                    thread.setUncaughtExceptionHandler(new CommandUncaughtExceptionHandler(LoggerFactory.getLogger(Command.class)));
                    return thread;
                }
            });

    public CommandExecutionQueueManager() {
        this.guildExecutionQueues = new ISnowflakeMap<>();
    }

    public ISnowflakeMap<ThreadExecutionQueue> getGuildExecutionQueues() {
        return guildExecutionQueues;
    }

    public ThreadExecutionQueue getForGuild(Guild guild) {
        return guildExecutionQueues.computeIfAbsent(guild, g -> {
            ThreadExecutionQueue newQueue = new ThreadExecutionQueue(
                "command-execution-queue-" + guild.getId(),
                EXECUTION_QUEUE_CONCURRENT_SIZE,
                EXECUTION_QUEUE_SIZE,
                GLOBAL_POOL,
                "command-rate_limiter-guild-" + guild.getId(),
                RATE_LIMIT_FOR_PERIOD,
                RATE_LIMIT_PERIOD,
                RATE_LIMIT_VIOLATION_TIMEOUT
        );
            guildExecutionQueues.put(guild, newQueue);
            return newQueue;
        });
    }

    public void addGuild(Guild guild) {
        guildExecutionQueues.put(guild, new ThreadExecutionQueue(
                        "command-execution-queue-" + guild.getId(),
                        EXECUTION_QUEUE_CONCURRENT_SIZE,
                        EXECUTION_QUEUE_SIZE,
                        GLOBAL_POOL,
                        "command-rate_limiter-guild-" + guild.getId(),
                        RATE_LIMIT_FOR_PERIOD,
                        RATE_LIMIT_PERIOD,
                        RATE_LIMIT_VIOLATION_TIMEOUT
        ));
    }

    public void removeGuild(Guild guild) {
        guildExecutionQueues.remove(guild);
    }

    public void closeAll() {
        guildExecutionQueues.values().forEach(ThreadExecutionQueue::close);
    }
}
