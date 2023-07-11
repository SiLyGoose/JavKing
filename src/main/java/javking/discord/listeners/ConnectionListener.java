package javking.discord.listeners;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;

public class ConnectionListener extends ListenerAdapter {
    private final ShardManager shardManager;

    public ConnectionListener(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Shard " + event.getJDA().getShardInfo().getShardId() + " connected to Discord WebSocket!");
    }

    @Override
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        System.out.println("Shard " + event.getJDA().getShardInfo().getShardId() + " disconnected from Discord WebSocket!");
        // Implement reconnection logic here
        shardManager.restart(event.getJDA().getShardInfo().getShardId());
    }
}
