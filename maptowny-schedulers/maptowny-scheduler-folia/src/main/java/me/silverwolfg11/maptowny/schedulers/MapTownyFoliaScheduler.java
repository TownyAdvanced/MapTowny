package me.silverwolfg11.maptowny.schedulers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

class MapTownyFoliaScheduler extends MapTownyScheduler {

    MapTownyFoliaScheduler(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }

    @Override
    public void scheduleTask(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    @Override
    public void scheduleAsyncTask(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public void scheduleRepeatingTask(Runnable task, long delay, long period) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period);
    }
}
