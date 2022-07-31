/*
 * Copyright (c) 2022 Silverwolfg11
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

package me.silverwolfg11.maptowny.platform.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BlueMapPlatform implements MapPlatform {

    private final JavaPlugin plugin;
    private final BlueMapMarkerProcessor markerProcessor;
    private final BlueMapIconMapper iconMapper;
    private final Collection<String> registeredGlobalLayers = ConcurrentHashMap.newKeySet();

    public BlueMapPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iconMapper = new BlueMapIconMapper(plugin.getLogger());
        this.markerProcessor = new BlueMapMarkerProcessor(plugin, iconMapper);

        // Allow scheduling marker processing operations when the API has loaded.
        BlueMapAPI.onEnable(api -> markerProcessor.enableScheduling());
    }

    @Override
    public @NotNull String getPlatformName() {
        return "bluemap";
    }

    @Override
    public void onFirstInitialize(Runnable callback) {
        // Use the future to unregister the listener
        CompletableFuture<Void> future = new CompletableFuture<>();
        Consumer<BlueMapAPI> apiConsumer = (api) -> {
            callback.run();
            // Have to delay it, otherwise it will end up concurrently modifying
            // the onEnablers list.
            Bukkit.getScheduler().runTask(plugin, () -> future.complete(null));
        };
        future.thenRun(() -> BlueMapAPI.unregisterListener(apiConsumer));

        BlueMapAPI.onEnable(apiConsumer);
    }

    @Override
    public void onInitialize(final Runnable callback) {
        // BlueMap API asynchronously initializes after the server starts ticking.
        BlueMapAPI.onEnable(api -> callback.run());
    }

    @Override
    public void startShutdown() {
        markerProcessor.disableScheduling();
    }

    @Override
    public boolean isWorldEnabled(@NotNull World world) {
        BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
        if (api == null)
            return false;

        return api.getWorld(world.getUID()).isPresent();
    }

    @Override
    public @Nullable MapWorld getWorld(@NotNull World world) {
        WorldIdentifier worldId = WorldIdentifier.from(world);
        return new BlueMapWorldWrapper(worldId, plugin.getLogger(), registeredGlobalLayers, markerProcessor);
    }

    @Override
    public void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width) {
        iconMapper.registerIcon(iconKey, icon, height, width);
    }

    @Override
    public boolean hasIcon(@NotNull String iconKey) {
        return iconMapper.isRegistered(iconKey);
    }

    @Override
    public boolean unregisterIcon(@NotNull String iconKey) {
        return iconMapper.unregisterIcon(iconKey);
    }
}
