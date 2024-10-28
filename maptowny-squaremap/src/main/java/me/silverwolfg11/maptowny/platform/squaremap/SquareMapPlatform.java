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

package me.silverwolfg11.maptowny.platform.squaremap;

import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapPlatformObserver;
import me.silverwolfg11.maptowny.platform.MapWorld;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SquareMapPlatform implements MapPlatform {

    private final List<MapPlatformObserver> platformObservers = new ArrayList<>();

    @Override
    public @NotNull String getPlatformName() {
        return "squaremap";
    }

    @Override
    public boolean registerObserver(@NotNull MapPlatformObserver observer) {
        if (platformObservers.contains(observer)) {
            return false;
        }

        // SquareMap doesn't implement a way to monitor enable/disable.
        observer.onObserverSetup();
        platformObservers.add(observer);
        return true;
    }

    @Override
    public boolean unregisterObserver(@NotNull MapPlatformObserver observer) {
        // Not keeping track of observers
        return platformObservers.remove(observer);
    }

    @Override
    public boolean isWorldEnabled(@NotNull World world) {
        final WorldIdentifier worldId = BukkitAdapter.worldIdentifier(world);
        return SquaremapProvider.get().getWorldIfEnabled(worldId).isPresent();
    }

    @Override
    public @Nullable MapWorld getWorld(@NotNull World world) {
        final WorldIdentifier worldId = BukkitAdapter.worldIdentifier(world);
        final xyz.jpenilla.squaremap.api.MapWorld sqMapWorld = SquaremapProvider.get().getWorldIfEnabled(worldId).orElse(null);
        if (sqMapWorld == null)
            return null;

        return SquareMapWorldWrapper.from(sqMapWorld);
    }

    @Override
    public boolean usesSegmentedPolygons() {
        return false;
    }

    @Override
    public void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width) {
        SquaremapProvider.get().iconRegistry().register(Key.of(iconKey), icon);
    }

    @Override
    public boolean hasIcon(@NotNull String iconKey) {
        return SquaremapProvider.get().iconRegistry().hasEntry(Key.of(iconKey));
    }

    @Override
    public boolean unregisterIcon(@NotNull String iconKey) {
        Key key = Key.of(iconKey);
        boolean hasKey = SquaremapProvider.get().iconRegistry().hasEntry(key);
        SquaremapProvider.get().iconRegistry().unregister(key);
        return hasKey;
    }

    @Override
    public void shutdown() {
        platformObservers.clear();
    }
}
