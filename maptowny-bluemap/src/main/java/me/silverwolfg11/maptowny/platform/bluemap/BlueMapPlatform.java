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
import me.silverwolfg11.maptowny.platform.MapPlatformObserver;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public class BlueMapPlatform implements MapPlatform {

    private final BlueMapIconMapper iconMapper;
    private final BlueMapObserverHandler observerHandler;

    public BlueMapPlatform(JavaPlugin plugin) {
        this.iconMapper = new BlueMapIconMapper(plugin.getLogger());
        observerHandler = new BlueMapObserverHandler();
    }

    @Override
    public @NotNull String getPlatformName() {
        return "bluemap";
    }

    @Override
    public void onFirstInitialize(Runnable callback) {
        observerHandler.registerObserver(new MapPlatformObserver() {
            @Override
            public void onObserverSetup() {
                callback.run();
            }
        });
    }

    @Override
    public void onInitialize(final Runnable callback) {
        // Implement both setup and enabled to maintain old behavior
        observerHandler.registerObserver(new MapPlatformObserver() {
            @Override
            public void onObserverSetup() {
                callback.run();
            }

            @Override
            public void onPlatformEnabled() {
                callback.run();
            }
        });
    }

    @Override
    public boolean registerObserver(@NotNull MapPlatformObserver observer) {
        return observerHandler.registerObserver(observer);
    }

    @Override
    public boolean unregisterObserver(@NotNull MapPlatformObserver observer) {
        return observerHandler.unregisterObserver(observer);
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
        return new BlueMapWorldWrapper(worldId, iconMapper);
    }

    @Override
    public boolean usesSegmentedPolygons() {
        return true;
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

    @Override
    public void shutdown() {
        observerHandler.disableObservers();
    }
}
