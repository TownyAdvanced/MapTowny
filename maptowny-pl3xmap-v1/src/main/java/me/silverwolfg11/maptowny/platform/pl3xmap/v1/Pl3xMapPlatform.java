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

package me.silverwolfg11.maptowny.platform.pl3xmap.v1;

import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapWorld;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.Pl3xMapProvider;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.image.BufferedImage;

public class Pl3xMapPlatform implements MapPlatform {
    @Override
    public @NotNull String getPlatformName() {
        return "Pl3xMap";
    }

    @Override
    public boolean isWorldEnabled(@NotNull World world) {
        return Pl3xMapProvider.get().getWorldIfEnabled(world).isPresent();
    }

    @Override
    public @Nullable MapWorld getWorld(@NotNull World world) {
        final net.pl3x.map.api.MapWorld pMapWorld = Pl3xMapProvider.get().getWorldIfEnabled(world).orElse(null);
        if (pMapWorld == null)
            return null;

        return Pl3xMapWorldWrapper.from(pMapWorld);
    }

    @Override
    public boolean usesSegmentedPolygons() {
        return false;
    }

    @Override
    public void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width) {
        Pl3xMapProvider.get().iconRegistry().register(Key.of(iconKey), icon);
    }

    @Override
    public boolean hasIcon(@NotNull String iconKey) {
        return Pl3xMapProvider.get().iconRegistry().hasEntry(Key.of(iconKey));
    }

    @Override
    public boolean unregisterIcon(@NotNull String iconKey) {
        Key key = Key.of(iconKey);
        boolean hasKey = Pl3xMapProvider.get().iconRegistry().hasEntry(key);
        Pl3xMapProvider.get().iconRegistry().unregister(key);
        return hasKey;
    }
}
