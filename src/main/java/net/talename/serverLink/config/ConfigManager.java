package net.talename.serverLink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.talename.serverLink.Main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Main plugin;
    private final Gson gson;
    private final Path configPath;
    private ServerLinkConfig config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configPath = plugin.getDataDirectory().resolve("serverlink.json");
    }

    public void loadConfig() {
        try {
            Files.createDirectories(plugin.getDataDirectory());

            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                config = gson.fromJson(json, ServerLinkConfig.class);
            } else {
                config = new ServerLinkConfig();
                saveConfig();
            }
        } catch (IOException e) {
            plugin.getPluginLogger().warning("Failed to load config: " + e.getMessage());
            config = new ServerLinkConfig();
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, gson.toJson(config));
        } catch (IOException e) {
            plugin.getPluginLogger().warning("Failed to save config: " + e.getMessage());
        }
    }

    public boolean isLinked() {
        return config != null && 
               config.serverToken != null && 
               !config.serverToken.isEmpty() &&
               config.serverId != null;
    }

    public void setLinkData(String serverToken, Long serverId) {
        config.serverToken = serverToken;
        config.serverId = serverId;
        config.linkedAt = System.currentTimeMillis();
        saveConfig();
    }

    public void clearLinkData() {
        config.serverToken = null;
        config.serverId = null;
        config.linkedAt = null;
        saveConfig();
    }

    public String getServerToken() {
        return config != null ? config.serverToken : null;
    }

    public Long getServerId() {
        return config != null ? config.serverId : null;
    }

    public boolean isDevMode() {
        return config != null && config.devMode;
    }

    public String getApiBaseUrl() {
        if (config == null) {
            return ServerLinkConfig.PROD_API_URL;
        }
        return config.devMode ? ServerLinkConfig.DEV_API_URL : ServerLinkConfig.PROD_API_URL;
    }

    public int getHeartbeatInterval() {
        return config != null ? config.heartbeatIntervalSeconds : ServerLinkConfig.DEFAULT_HEARTBEAT_INTERVAL;
    }

    public static class ServerLinkConfig {
        public static final String PROD_API_URL = "https://api.talename.net";
        public static final String DEV_API_URL = "https://api.talename.local";
        public static final int DEFAULT_HEARTBEAT_INTERVAL = 300;

        public String serverToken;
        public Long serverId;
        public Long linkedAt;
        public boolean devMode = false;
        public int heartbeatIntervalSeconds = DEFAULT_HEARTBEAT_INTERVAL;
    }
}
