package me.petterim1.hiddencommands;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.network.protocol.AvailableCommandsPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import me.seetch.format.Format;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.user.User;

import java.util.ArrayList;
import java.util.List;

public class Main extends PluginBase implements Listener {

    private LuckPerms luckPerms;

    private boolean ignoreOPs;
    private List<String> hiddenCommands;
    private List<String> disabledCommands;

    public void onEnable() {
        if (this.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            this.getLogger().error(TextFormat.RED + "LuckPerms not found! Disabling...");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            this.luckPerms = LuckPermsProvider.get();
        }

        saveDefaultConfig();
        ignoreOPs = getConfig().getBoolean("ignoreOPs");
        hiddenCommands = getConfig().getStringList("hiddenCommands");
        disabledCommands = getConfig().getStringList("disabledCommands");

        getServer().getPluginManager().registerEvents(this, this);

        EventBus eventBus = this.luckPerms.getEventBus();
        eventBus.subscribe(this, NodeAddEvent.class, this::onNodeAdd);
        eventBus.subscribe(this, NodeRemoveEvent.class, this::onNodeRemove);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSendCommandData(DataPacketSendEvent e) {
        if (e.getPacket() instanceof AvailableCommandsPacket && !(ignoreOPs && e.getPlayer().isOp())) {
            AvailableCommandsPacket pk = (AvailableCommandsPacket) e.getPacket();
            List<String> remove = new ArrayList<>();
            for (String cmd : pk.commands.keySet()) {
                if (hiddenCommands.contains(cmd)) {
                    remove.add(cmd);
                }
            }
            for (String cmd : remove) {
                pk.commands.remove(cmd);
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage().toLowerCase().replaceAll("\\s+", "");
        for (String str : disabledCommands) {
            if (cmd.startsWith("/" + str) && !(ignoreOPs && e.getPlayer().isOp())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Format.RED.colorize("\uE112", "У Вас недостаточно прав для выполнения этой команды."));
                return;
            }
        }
    }

    public void onNodeAdd(NodeAddEvent event) {
        if (!event.isUser()) {
            return;
        }

        User target = (User) event.getTarget();

        // LuckPerms events are posted async, we want to process on the server thread!
        this.getServer().getScheduler().scheduleTask(this, () -> {
            Player player = this.getServer().getPlayer(target.getUsername());
            if (player == null) {
                return; // Player not online.
            }

            player.sendCommandData();
        });
    }

    public void onNodeRemove(NodeRemoveEvent event) {
        if (!event.isUser()) {
            return;
        }

        User target = (User) event.getTarget();

        // LuckPerms events are posted async, we want to process on the server thread!
        this.getServer().getScheduler().scheduleTask(this, () -> {
            Player player = this.getServer().getPlayer(target.getUsername());
            if (player == null) {
                return; // Player not online.
            }

            player.sendCommandData();
        });
    }
}
