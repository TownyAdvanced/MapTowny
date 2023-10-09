package me.silverwolfg11.maptowny.schedulers;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Generic scheduler for the MapTowny plugin.
 * Assume all scheduling is done is on a global region.
 */
public abstract class MapTownyScheduler {
    final protected JavaPlugin plugin;

    protected MapTownyScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    abstract public void cancelAllTasks();

    abstract public void scheduleTask(Runnable task);

    abstract public void scheduleAsyncTask(Runnable task);

    abstract public void scheduleRepeatingTask(Runnable task, long delay, long period);
}
