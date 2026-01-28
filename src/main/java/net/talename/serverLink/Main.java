package net.talename.serverLink;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.talename.serverLink.command.TaleNameCommand;
import net.talename.serverLink.config.ConfigManager;
import net.talename.serverLink.service.HeartbeatService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("TaleName-ServerLink");

    private static Main instance;
    private ConfigManager configManager;
    private HeartbeatService heartbeatService;

    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.info("TaleName ServerLink is starting...");

        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        TaleNameCommand taleNameCommand = new TaleNameCommand(this);
        getCommandRegistry().registerCommand(taleNameCommand);

        this.heartbeatService = new HeartbeatService(this);

        if (configManager.isLinked()) {
            LOGGER.info("Server is linked. Starting heartbeat service...");
            heartbeatService.start();
        } else {
            LOGGER.info("Server not linked. Use /talename link <code> to link.");
        }

        LOGGER.info("TaleName ServerLink enabled!");
    }

    @Override
    protected void shutdown() {
        if (heartbeatService != null) {
            heartbeatService.stop();
        }
        if (configManager != null) {
            configManager.saveConfig();
        }
        LOGGER.info("TaleName ServerLink disabled!");
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HeartbeatService getHeartbeatService() {
        return heartbeatService;
    }

    public Logger getPluginLogger() {
        return LOGGER;
    }
}
