package me.silverwolfg11.maptowny.schedulers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

class MapTownyBukkitScheduler extends MapTownyScheduler {

    MapTownyBukkitScheduler(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public void scheduleTask(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void scheduleAsyncTask(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void scheduleRepeatingTask(Runnable task, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }
}
