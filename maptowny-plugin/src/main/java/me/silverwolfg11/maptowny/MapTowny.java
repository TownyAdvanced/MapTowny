/*
 * Copyright (c) 2021 Silverwolfg11
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

package me.silverwolfg11.maptowny;

import com.palmergames.bukkit.towny.Towny;
import me.silverwolfg11.maptowny.events.MapReloadEvent;
import me.silverwolfg11.maptowny.listeners.TownClaimListener;
import me.silverwolfg11.maptowny.managers.TownyLayerManager;
import me.silverwolfg11.maptowny.objects.MapConfig;
import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.tasks.RenderTownsTask;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class MapTowny extends JavaPlugin implements MapTownyPlugin {

    private TownyLayerManager layerManager;
    private MapPlatform mapPlatform;
    private MapConfig config;
    private RenderTownsTask renderTownsTask;

		private static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer") || classExists("io.papermc.paper.threadedregions.RegionizedServerInitEvent");

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
            // IOException caused by creating new file usually, so disabling is a valid
            // option
            getLogger().log(Level.SEVERE, "Error loading config. Disabling plugin...", e);
            setEnabled(false);
            return;
        }

        // Try to load the map platform
        mapPlatform = loadPlatform();

        // No map platform found so disable
        if (mapPlatform == null) {
            getLogger().severe("Error no valid map plugin found! Valid map plugins: Pl3xMap, squaremap, dynmap");
            setEnabled(false);
            return;
        }

        getLogger().info("Using web-map plugin: " + mapPlatform.getPlatformName());

        // Load layer manager
        layerManager = new TownyLayerManager(this, mapPlatform);

        // Register command
        TownyMapCommand commandExec = new TownyMapCommand(this);
        PluginCommand command = getCommand("maptowny");
        command.setExecutor(commandExec);
        command.setTabCompleter(commandExec);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new TownClaimListener(this), this);

        renderTownsTask = new RenderTownsTask(this);

        // If towny is in safe-mode, do not attempt to render towns.
        if (!Towny.getPlugin().isError()) {
            // Schedule render task when the layer manager is initialized.
            if (IS_FOLIA) {
                layerManager.onInitialize(
                        () -> Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(this,
                                value -> renderTownsTask.run(), 20L, config.getUpdatePeriod() * 20L * 60L));
            } else {
                layerManager.onInitialize(
                        () -> new RenderTownsTask(this).runTaskTimer(this, 20, config.getUpdatePeriod() * 20L * 60));
            }
        }
    }

    @Override
    public void onDisable() {
        // Initiaize map platform shutdown
        if (mapPlatform != null)
            mapPlatform.startShutdown();

        // Plugin shutdown logic
        if (layerManager != null)
            layerManager.close();

        // Finalize map platform shutdown
        if (mapPlatform != null)
            mapPlatform.shutdown();
    }

    // Load the appropriate map platform or the map plugin that Pl3xMapTowny should
    // use.
    // For right now this only supports squaremap and Pl3xMap
    @Nullable
    private MapPlatform loadPlatform() {
        Predicate<String> pluginEnabled = (pluginName) -> Bukkit.getPluginManager().isPluginEnabled(pluginName);

        if (pluginEnabled.test("Pl3xMap")) {
            // Differentiate versions based on
            // available classes.
            String version;
            if (classExists("net.pl3x.map.core.Pl3xMap")) {
                version = "v3";
            } else if (classExists("net.pl3x.map.Pl3xMap")) {
                version = "v2";
            } else {
                version = "v1";
            }

            return loadPlatformClass("pl3xmap." + version + ".Pl3xMapPlatform");
        } else if (pluginEnabled.test("squaremap")) {
            return loadPlatformClass("squaremap.SquareMapPlatform");
        } else if (pluginEnabled.test("dynmap")) {
            return loadPlatformClass("dynmap.DynmapPlatform");
        } else if (pluginEnabled.test("BlueMap")) {
            return loadPlatformClass("bluemap.BlueMapPlatform", this);
        }

        return null;
    }

    private MapPlatform loadPlatformClass(@NotNull String abridgedPath) {
        return loadPlatformClass(abridgedPath, null);
    }

    // Load class using reflection because some classes are compiled on different
    // Java versions
    @Nullable
    private MapPlatform loadPlatformClass(@NotNull String abridgedPath, @Nullable JavaPlugin plugin) {
        String platformClassPrefix = "me.silverwolfg11.maptowny.platform.";
        String platformClassPath = platformClassPrefix + abridgedPath;
        try {
            Class<?> platformClass = Class.forName(platformClassPath);
            if (plugin != null) {
                return (MapPlatform) platformClass.getConstructor(JavaPlugin.class).newInstance(plugin);
            } else {
                return (MapPlatform) platformClass.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException ex) {
            String msg = String.format("Error trying to load class '%s'", platformClassPath);
            getLogger().log(Level.SEVERE, msg, ex);
            return null;
        }
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

    @NotNull
    public MapConfig config() {
        return config;
    }

    @Override
    @NotNull
    public TownyLayerManager getLayerManager() {
        return layerManager;
    }

    @Override
    @Nullable
    public MapPlatform getPlatform() {
        return mapPlatform;
    }

    public void reload() throws IOException {
        config = MapConfig.loadConfig(getDataFolder(), getLogger());
        if (IS_FOLIA) {
            Bukkit.getServer().getGlobalRegionScheduler().cancelTasks(this);
        } else {
            Bukkit.getScheduler().cancelTasks(this);
        }

        layerManager.close();
        layerManager = new TownyLayerManager(this, mapPlatform);
        Bukkit.getPluginManager().callEvent(new MapReloadEvent()); // API Event
        Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(this,
                value -> renderTownsTask.run(), 1L, config.getUpdatePeriod() * 20L * 60L);
    }

    public void async(Runnable run) {
        if (IS_FOLIA) {
            Bukkit.getServer().getAsyncScheduler().runNow(this, value -> run.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, run);
        }
    }
}
