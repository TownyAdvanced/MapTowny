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

package me.silverwolfg11.maptowny.platform.pl3xmap.v3;

import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapWorld;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.image.IconImage;
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
        var mapworld = Pl3xMap.api().getWorldRegistry().get(world.getName());
        return mapworld != null && mapworld.isEnabled();
    }

    @Override
    public @Nullable MapWorld getWorld(@NotNull World world) {
        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(world.getName());

        if (mapworld == null || !mapworld.isEnabled())
            return null;

        return Pl3xMapWorldWrapper.from(mapworld);
    }

    @Override
    public void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width) {
        // Assume that all images are PNGs for right now.
        // Maybe in the future, there will be an API addition to keep track of the image type.
        Pl3xMap.api().getIconRegistry().register(new IconImage(iconKey, icon, "png"));
    }

    @Override
    public boolean hasIcon(@NotNull String iconKey) {
        return Pl3xMap.api().getIconRegistry().has(iconKey);
    }

    @Override
    public boolean unregisterIcon(@NotNull String iconKey) {
        return Pl3xMap.api().getIconRegistry().unregister(iconKey) != null;
    }
}
