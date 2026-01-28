package net.talename.serverLink.service;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.talename.serverLink.Main;
import net.talename.serverLink.api.TaleNameAPI;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HeartbeatService {

    private final Main plugin;
    private final TaleNameAPI api;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatTask;
    private int consecutiveFailures = 0;

    public HeartbeatService(Main plugin) {
        this.plugin = plugin;
        this.api = new TaleNameAPI(plugin);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TaleName-Heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) return;
        if (!plugin.getConfigManager().isLinked()) return;

        int interval = plugin.getConfigManager().getHeartbeatInterval();
        plugin.getPluginLogger().info("Starting heartbeat service (interval: " + interval + "s)");

        sendHeartbeat();
        heartbeatTask = scheduler.scheduleAtFixedRate(this::sendHeartbeat, interval, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    public boolean isRunning() {
        return heartbeatTask != null && !heartbeatTask.isCancelled();
    }

    private void sendHeartbeat() {
        String token = plugin.getConfigManager().getServerToken();
        if (token == null || token.isEmpty()) {
            stop();
            return;
        }

        api.sendHeartbeat(token, collectHeartbeatData())
                .thenAccept(response -> {
                    if (response.success()) {
                        consecutiveFailures = 0;
                    } else {
                        consecutiveFailures++;
                        plugin.getPluginLogger().warning("Heartbeat failed: " + response.message());
                        if (response.message().contains("Invalid server token")) {
                            plugin.getConfigManager().clearLinkData();
                            stop();
                        }
                    }
                });
    }

    private TaleNameAPI.HeartbeatData collectHeartbeatData() {
        Universe universe = Universe.get();
        HytaleServer server = HytaleServer.get();
        List<PlayerRef> players = universe.getPlayers();
        
        return new TaleNameAPI.HeartbeatData(
                universe.getPlayerCount(),
                server.getConfig().getMaxPlayers(),
                server.getConfig().getMotd(),
                players.stream().map(PlayerRef::getUuid).collect(Collectors.toList())
        );
    }

    public void sendHeartbeatNow() {
        scheduler.execute(this::sendHeartbeat);
    }

    public TaleNameAPI getApi() {
        return api;
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
    }
}
