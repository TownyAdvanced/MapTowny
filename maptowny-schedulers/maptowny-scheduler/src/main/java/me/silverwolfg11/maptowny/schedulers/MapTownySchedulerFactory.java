/*
 * Copyright (c) 2023 Silverwolfg11
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
