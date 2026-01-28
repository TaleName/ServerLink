package net.talename.serverLink.command;

import com.hypixel.hytale.server.core.Server;
import com.hypixel.hytale.server.core.command.Command;
import com.hypixel.hytale.server.core.command.CommandContext;
import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.annotation.*;
import com.hypixel.hytale.server.core.entity.player.Player;
import com.hypixel.hytale.server.core.permission.Permission;
import com.hypixel.hytale.server.core.text.Text;
import com.hypixel.hytale.server.core.text.TextColor;
import net.talename.serverLink.Main;
import net.talename.serverLink.api.TaleNameAPI;

@CommandInfo(name = "talename", description = "TaleName server linking", aliases = {"tn"})
public class TaleNameCommand implements Command {

    private final Main plugin;
    private static final String PERM = "talename.admin";

    public TaleNameCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.getSender();
        sender.sendMessage(Text.of("=== TaleName Commands ===").color(TextColor.GOLD));
        sender.sendMessage(Text.of("/talename link <code>").color(TextColor.YELLOW).append(Text.of(" - Link server").color(TextColor.GRAY)));
        sender.sendMessage(Text.of("/talename unlink").color(TextColor.YELLOW).append(Text.of(" - Unlink server").color(TextColor.GRAY)));
        sender.sendMessage(Text.of("/talename status").color(TextColor.YELLOW).append(Text.of(" - Check status").color(TextColor.GRAY)));
    }

    @Subcommand("link")
    @Description("Link server to TaleName")
    @Permission(value = PERM, defaultValue = Permission.Default.OP)
    public void linkCommand(CommandContext context, @Argument("code") String linkCode) {
        CommandSender sender = context.getSender();

        if (plugin.getConfigManager().isLinked()) {
            sender.sendMessage(Text.of("Server already linked! Use /talename unlink first.").color(TextColor.RED));
            return;
        }

        if (linkCode == null || linkCode.length() != 6) {
            sender.sendMessage(Text.of("Invalid code! Get your 6-char code from talename.net/settings").color(TextColor.RED));
            return;
        }

        sender.sendMessage(Text.of("Linking...").color(TextColor.YELLOW));

        Server server = plugin.getServer();
        TaleNameAPI.ServerInfo info = new TaleNameAPI.ServerInfo(
                server.getIp(), server.getPort(), server.getVersion(),
                "Hytale", server.getMotd(), server.getMaxPlayers()
        );

        plugin.getHeartbeatService().getApi().linkServer(linkCode.toUpperCase(), info)
                .thenAccept(response -> {
                    server.getScheduler().runTask(() -> {
                        if (response.success()) {
                            plugin.getConfigManager().setLinkData(response.serverToken(), response.serverId());
                            sender.sendMessage(Text.of("Server linked! ID: " + response.serverId()).color(TextColor.GREEN));
                            plugin.getHeartbeatService().start();
                        } else {
                            sender.sendMessage(Text.of("Failed: " + response.message()).color(TextColor.RED));
                        }
                    });
                });
    }

    @Subcommand("unlink")
    @Description("Unlink server from TaleName")
    @Permission(value = PERM, defaultValue = Permission.Default.OP)
    public void unlinkCommand(CommandContext context) {
        CommandSender sender = context.getSender();

        if (!plugin.getConfigManager().isLinked()) {
            sender.sendMessage(Text.of("Server is not linked!").color(TextColor.RED));
            return;
        }

        sender.sendMessage(Text.of("Unlinking...").color(TextColor.YELLOW));
        String token = plugin.getConfigManager().getServerToken();
        Server server = plugin.getServer();

        plugin.getHeartbeatService().getApi().unlinkServer(token)
                .thenAccept(response -> {
                    server.getScheduler().runTask(() -> {
                        plugin.getHeartbeatService().stop();
                        plugin.getConfigManager().clearLinkData();
                        sender.sendMessage(Text.of("Server unlinked!").color(TextColor.GREEN));
                    });
                });
    }

    @Subcommand("status")
    @Description("Check TaleName link status")
    @Permission(value = PERM, defaultValue = Permission.Default.OP)
    public void statusCommand(CommandContext context) {
        CommandSender sender = context.getSender();

        sender.sendMessage(Text.of("=== TaleName Status ===").color(TextColor.GOLD));

        if (plugin.getConfigManager().isLinked()) {
            sender.sendMessage(Text.of("Status: ").color(TextColor.WHITE).append(Text.of("LINKED").color(TextColor.GREEN)));
            sender.sendMessage(Text.of("Server ID: " + plugin.getConfigManager().getServerId()).color(TextColor.GRAY));
            sender.sendMessage(Text.of("Heartbeat: ").color(TextColor.WHITE)
                    .append(Text.of(plugin.getHeartbeatService().isRunning() ? "RUNNING" : "STOPPED")
                            .color(plugin.getHeartbeatService().isRunning() ? TextColor.GREEN : TextColor.RED)));
        } else {
            sender.sendMessage(Text.of("Status: ").color(TextColor.WHITE).append(Text.of("NOT LINKED").color(TextColor.RED)));
            sender.sendMessage(Text.of("Use /talename link <code>").color(TextColor.GRAY));
        }
    }

    @Subcommand("heartbeat")
    @Description("Force send heartbeat")
    @Permission(value = PERM, defaultValue = Permission.Default.OP)
    public void heartbeatCommand(CommandContext context) {
        CommandSender sender = context.getSender();

        if (!plugin.getConfigManager().isLinked()) {
            sender.sendMessage(Text.of("Server not linked!").color(TextColor.RED));
            return;
        }

        plugin.getHeartbeatService().sendHeartbeatNow();
        sender.sendMessage(Text.of("Heartbeat sent!").color(TextColor.GREEN));
    }
}
