package net.talename.serverLink.command;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import net.talename.serverLink.Main;
import net.talename.serverLink.api.TaleNameAPI;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaleNameCommand extends CommandBase {

    private final Main plugin;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Nonnull
    private final RequiredArg<String> subArg =
            withRequiredArg("subcommand", "link/unlink/status/heartbeat", ArgTypes.STRING);

    @Nonnull
    private final OptionalArg<String> codeArg =
            withOptionalArg("code", "The 6-character TaleName link code.", ArgTypes.STRING);

    public TaleNameCommand(Main plugin) {
        super("talename", "TaleName server linking", false);
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@NonNull CommandContext commandContext) {
        String sub = subArg.get(commandContext).toLowerCase();

        switch (sub) {
            case "link":
                handleLink(commandContext);
                break;
            case "unlink":
                handleUnlink(commandContext);
                break;
            case "status":
                handleStatus(commandContext);
                break;
            case "heartbeat":
                handleHeartbeat(commandContext);
                break;
            default:
                sendHelp(commandContext);
                break;
        }
    }

    private void sendHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("=== TaleName Commands ==="));
        ctx.sendMessage(Message.raw("/talename link <code> - Link server"));
        ctx.sendMessage(Message.raw("/talename unlink - Unlink server"));
        ctx.sendMessage(Message.raw("/talename status - Check status"));
        ctx.sendMessage(Message.raw("/talename heartbeat - Force send heartbeat"));
    }

    private void handleLink(CommandContext ctx) {
        String linkCode = codeArg.get(ctx);

        if (plugin.getConfigManager().isLinked()) {
            ctx.sendMessage(Message.raw("Server already linked! Use /talename unlink first."));
            return;
        }

        if (linkCode == null || linkCode.length() != 6) {
            ctx.sendMessage(Message.raw("Invalid code! Get your 6-char code from talename.net/settings"));
            return;
        }

        ctx.sendMessage(Message.raw("Linking..."));

        Universe universe = Universe.get();
        HytaleServer server = HytaleServer.get();

        TaleNameAPI.ServerInfo info = new TaleNameAPI.ServerInfo(server.getServerName(),
                "Hytale", server.getConfig().getMotd(), server.getConfig().getMaxPlayers()
        );

        plugin.getHeartbeatService().getApi().linkServer(linkCode.toUpperCase(), info)
                .thenAccept(response -> {
                    // Use ScheduledExecutorService instead of server scheduler
                    scheduler.execute(() -> {
                        if (response.success()) {
                            plugin.getConfigManager().setLinkData(response.serverToken(), response.serverId());
                            ctx.sendMessage(Message.raw("Server linked! ID: " + response.serverId()));
                            plugin.getHeartbeatService().start();
                        } else {
                            ctx.sendMessage(Message.raw("Failed: " + response.message()));
                        }
                    });
                });
    }

    private void handleUnlink(CommandContext ctx) {
        // /talename unlink
        if (!plugin.getConfigManager().isLinked()) {
            ctx.sendMessage(Message.raw("Server is not linked!"));
            return;
        }

        ctx.sendMessage(Message.raw("Unlinking..."));

        String token = plugin.getConfigManager().getServerToken();

        plugin.getHeartbeatService().getApi().unlinkServer(token)
                .thenAccept(response -> {
                    scheduler.execute(() -> {
                        plugin.getHeartbeatService().stop();
                        plugin.getConfigManager().clearLinkData();
                        ctx.sendMessage(Message.raw("Server unlinked!"));
                    });
                });
    }

    private void handleStatus(CommandContext ctx) {
        // /talename status
        ctx.sendMessage(Message.raw("=== TaleName Status ==="));

        if (plugin.getConfigManager().isLinked()) {
            ctx.sendMessage(Message.raw("Status: LINKED"));
            ctx.sendMessage(Message.raw("Server ID: " + plugin.getConfigManager().getServerId()));
            ctx.sendMessage(Message.raw("Heartbeat: " +
                    (plugin.getHeartbeatService().isRunning() ? "RUNNING" : "STOPPED")));
        } else {
            ctx.sendMessage(Message.raw("Status: NOT LINKED"));
            ctx.sendMessage(Message.raw("Use /talename link <code>"));
        }
    }

    private void handleHeartbeat(CommandContext ctx) {
        // /talename heartbeat
        if (!plugin.getConfigManager().isLinked()) {
            ctx.sendMessage(Message.raw("Server not linked!"));
            return;
        }

        plugin.getHeartbeatService().sendHeartbeatNow();
        ctx.sendMessage(Message.raw("Heartbeat sent!"));
    }
}
