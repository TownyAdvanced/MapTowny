package me.silverwolfg11.maptowny.schedulers;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class MapTownySchedulerFactory {
    // Scheduler determination
    public static MapTownyScheduler create(JavaPlugin plugin) {
        final String FOLIA_REGION_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";
        if (classExists(FOLIA_REGION_CLASS)) {
            return loadScheduler(plugin, "MapTownyFoliaScheduler", "Folia");
        }

        return loadScheduler(plugin, "MapTownyBukkitScheduler", "Bukkit");
    }

    // Check if a class is loaded.
    private static boolean classExists(@NotNull String classPath) {
        try {
            // If a class exists, but is not loaded, the static initializer will be called.
            Class.forName(classPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if a class is loaded.
    private static MapTownyScheduler loadScheduler(JavaPlugin plugin, @NotNull String schedulerClassName, String schedulerName) {
        try {
            Class<?> schedulerClass = Class.forName("me.silverwolfg11.maptowny.schedulers." + schedulerClassName);
            return (MapTownyScheduler) schedulerClass.getDeclaredConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + schedulerName + " scheduler!", ex);
            return null;
        }
    }
}
