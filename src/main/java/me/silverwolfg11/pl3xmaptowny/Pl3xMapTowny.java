package me.silverwolfg11.pl3xmaptowny;

import me.silverwolfg11.pl3xmaptowny.listeners.TownClaimListener;
import me.silverwolfg11.pl3xmaptowny.objects.MapConfig;
import me.silverwolfg11.pl3xmaptowny.tasks.RenderTownsTask;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;

public final class Pl3xMapTowny extends JavaPlugin {

    private TownyLayerManager layerManager;
    private MapConfig config;

    @Override
    public void onEnable() {
        // Check that Towny is enabled
        // While the plugin.yml makes sure Towny is installed, Towny can be disabled
        if (!Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            setEnabled(false);
            return;
        }

        // Plugin startup logic
        try {
            config = MapConfig.loadConfig(getDataFolder(), getLogger());
        } catch (IOException e) {
            // IOException caused by creating new file usually, so disabling is a valid option
            getLogger().log(Level.SEVERE, "Error loading config. Disabling plugin...", e);
            setEnabled(false);
            return;
        }

        // Load layer manager
        layerManager = new TownyLayerManager(this);

        // Register command
        TownyMapCommand commandExec = new TownyMapCommand(this);
        PluginCommand command = getCommand("pl3xmaptowny");
        command.setExecutor(commandExec);
        command.setTabCompleter(commandExec);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new TownClaimListener(this), this);

        // Schedule render task
        new RenderTownsTask(this).runTaskTimer(this, 0, config.getUpdatePeriod() * 20L * 60);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (layerManager != null)
            layerManager.close();
    }

    @NotNull
    public MapConfig config() {
        return config;
    }

    @NotNull
    public TownyLayerManager getLayerManager() {
        return layerManager;
    }

    public void reload() throws IOException {
        config = MapConfig.loadConfig(getDataFolder(), getLogger());
        Bukkit.getScheduler().cancelTasks(this);
        layerManager.close();
        layerManager = new TownyLayerManager(this);
        new RenderTownsTask(this).runTaskTimer(this, 0, config.getUpdatePeriod() * 20L * 60);
    }

    public void async(Runnable run) {
        Bukkit.getScheduler().runTaskAsynchronously(this, run);
    }
}
