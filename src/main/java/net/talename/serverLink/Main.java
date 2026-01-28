package net.talename.serverLink;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.talename.serverLink.command.TaleNameCommand;
import net.talename.serverLink.config.ConfigManager;
import net.talename.serverLink.service.HeartbeatService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.slf4j.Logger;

public class Main extends JavaPlugin {

    private static Main instance;
    private Logger logger;
    private ConfigManager configManager;
    private HeartbeatService heartbeatService;

    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    public void onEnable() {
        this.logger = getLogger();
        logger.info("TaleName ServerLink is starting...");

        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        TaleNameCommand taleNameCommand = new TaleNameCommand(this);
        getServer().getCommandManager().registerCommand(taleNameCommand);

        this.heartbeatService = new HeartbeatService(this);
        
        if (configManager.isLinked()) {
            logger.info("Server is linked. Starting heartbeat service...");
            heartbeatService.start();
        } else {
            logger.info("Server not linked. Use /talename link <code> to link.");
        }

        logger.info("TaleName ServerLink enabled!");
    }

    @Override
    public void onDisable() {
        if (heartbeatService != null) {
            heartbeatService.stop();
        }
        if (configManager != null) {
            configManager.saveConfig();
        }
        logger.info("TaleName ServerLink disabled!");
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
        return logger;
    }
}
