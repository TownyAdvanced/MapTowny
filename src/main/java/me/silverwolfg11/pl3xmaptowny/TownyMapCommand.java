package me.silverwolfg11.pl3xmaptowny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.pl3xmaptowny.objects.TownRenderEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TownyMapCommand implements TabExecutor {

    private final Pl3xMapTowny plugin;

    private final String PERM_PREFIX = "pl3xmaptowny.";
    private final String PERM_DENY_MSG = ChatColor.RED + "You do not have permission to use this command!";

    private final String PLUGIN_PREFIX = ChatColor.WHITE + "[" + ChatColor.GREEN + "Pl3xMap-Towny" + ChatColor.WHITE + "] ";

    public TownyMapCommand(Pl3xMapTowny plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final String usageMsg = ChatColor.RED + "Usage: /" + label + " [reload/render/unrender]";

        if (args.length < 1) {
            sendMsg(sender, usageMsg);
            return true;
        }

        final String subCmd = args[0].toLowerCase(Locale.ROOT);

        switch (subCmd) {
            case "reload":
                reload(sender);
                break;
            case "render":
                renderTown(sender, label, args);
                break;
            case "unrender":
                unrenderTown(sender, label, args);
                break;
            default:
                sendMsg(sender, usageMsg);
                break;
        }

        return true;
    }

    private void renderTown(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_PREFIX + "render")) {
            sendMsg(sender, PERM_DENY_MSG);
            return;
        }

        if (args.length != 2) {
            sendMsg(sender, PLUGIN_PREFIX + ChatColor.RED + "Usage: /" + label + "render [town name]");
            return;
        }

        final String townName = args[1];

        Town town = TownyAPI.getInstance().getTown(townName);

        if (town == null) {
            sendMsg(sender, ChatColor.RED + "Not a valid town!");
            return;
        }

        final TownRenderEntry tre = plugin.getLayerManager().buildTownEntry(town);
        // Run processing async
        plugin.async(() -> {
            plugin.getLayerManager().renderTown(tre);
            sendMsg(sender, ChatColor.GREEN + "Rendered town " + town.getName() + "!");
        });
    }

    private void unrenderTown(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_PREFIX + "unrender")) {
            sendMsg(sender, PERM_DENY_MSG);
            return;
        }

        if (args.length != 2) {
            sendMsg(sender, ChatColor.RED + "Usage: /" + label + "unrender [town name]");
            return;
        }

        UUID townUUID = null;

        final String townName = args[1];

        Town town = TownyAPI.getInstance().getTown(townName);

        if (town == null) {
            // Check if name is a UUID instead
            try {
                townUUID = UUID.fromString(townName);
            } catch (IllegalArgumentException ex) {
                sendMsg(sender, ChatColor.RED + "Not a valid town!");
                return;
            }
        }
        else {
            townUUID = town.getUUID();
        }

        boolean unrendered = plugin.getLayerManager().removeTownMarker(townUUID);
        if (unrendered)
            sendMsg(sender, ChatColor.GREEN + "Unrendered town " + townName + "!");
        else
            sendMsg(sender, ChatColor.RED + "No town " + townName + " was displayed!");
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(PERM_PREFIX + "reload")) {
            sendMsg(sender, PERM_DENY_MSG);
            return;
        }

        try {
            plugin.reload();
            sendMsg(sender, ChatColor.GREEN + "Successfully reloaded all plugin data!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading config!", e);
            sendMsg(sender, ChatColor.RED + "Error reloading plugin: could not load config! Please see error message in console!");
        }
    }

    private void sendMsg(CommandSender sender, String message) {
        sender.sendMessage(PLUGIN_PREFIX + message);
    }

    // Tab Completion

    private final List<String> SUB_CMDS = Arrays.asList("reload", "render", "unrender");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0)
            return null;

        String subCmdArg = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            return SUB_CMDS.stream()
                    .filter(s -> s.startsWith(subCmdArg))
                    .collect(Collectors.toList());
        }
        else if (args.length == 2) {
            if (subCmdArg.equals("render") || subCmdArg.equals("unrender"))
                return TownyUniverse.getInstance().getTownsTrie().getStringsFromKey(args[1]);
        }

        return null;
    }
}
