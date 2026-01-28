package net.talename.serverLink.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.talename.serverLink.Main;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TaleNameAPI {

    private final Main plugin;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;

    public TaleNameAPI(Main plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.baseUrl = plugin.getConfigManager().getApiBaseUrl();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CompletableFuture<LinkResponse> linkServer(String linkCode, ServerInfo serverInfo) {
        JsonObject body = new JsonObject();
        body.addProperty("linkCode", linkCode.toUpperCase());
        
        JsonObject serverInfoJson = new JsonObject();
        serverInfoJson.addProperty("software", serverInfo.software);
        serverInfoJson.addProperty("motd", serverInfo.motd);
        serverInfoJson.addProperty("maxPlayers", serverInfo.maxPlayers);
        body.add("serverInfo", serverInfoJson);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/serverlinker/link"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (response.statusCode() == 200) {
                        return new LinkResponse(true,
                                json.has("serverToken") ? json.get("serverToken").getAsString() : null,
                                json.has("serverId") ? json.get("serverId").getAsLong() : null,
                                json.has("message") ? json.get("message").getAsString() : "Success");
                    }
                    return new LinkResponse(false, null, null,
                            json.has("error") ? json.get("error").getAsString() : "Unknown error");
                })
                .exceptionally(e -> new LinkResponse(false, null, null, "Connection failed: " + e.getMessage()));
    }

    public CompletableFuture<HeartbeatResponse> sendHeartbeat(String serverToken, HeartbeatData data) {
        JsonObject body = new JsonObject();
        body.addProperty("playersOnline", data.playersOnline);
        body.addProperty("maxPlayers", data.maxPlayers);
        body.addProperty("motd", data.motd);
        if (data.playerList != null && !data.playerList.isEmpty()) {
            body.add("playerList", gson.toJsonTree(data.playerList));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/serverlinker/heartbeat"))
                .header("Content-Type", "application/json")
                .header("X-Server-Token", serverToken)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return new HeartbeatResponse(true, "OK");
                    } else if (response.statusCode() == 401) {
                        return new HeartbeatResponse(false, "Invalid server token");
                    }
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    return new HeartbeatResponse(false, json.has("error") ? json.get("error").getAsString() : "Error");
                })
                .exceptionally(e -> new HeartbeatResponse(false, "Connection failed"));
    }

    public CompletableFuture<UnlinkResponse> unlinkServer(String serverToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/serverlinker/unlink"))
                .header("Content-Type", "application/json")
                .header("X-Server-Token", serverToken)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return new UnlinkResponse(true, "Unlinked");
                    }
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    return new UnlinkResponse(false, json.has("error") ? json.get("error").getAsString() : "Error");
                })
                .exceptionally(e -> new UnlinkResponse(false, "Connection failed"));
    }

    // Response classes
    public record LinkResponse(boolean success, String serverToken, Long serverId, String message) {}
    public record HeartbeatResponse(boolean success, String message) {}
    public record UnlinkResponse(boolean success, String message) {}

    // Data classes
    public record ServerInfo(String name, String software, String motd, int maxPlayers) {}
    public record HeartbeatData(int playersOnline, int maxPlayers, String motd, List<UUID> playerList) {}
}
