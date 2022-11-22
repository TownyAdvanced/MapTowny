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

package me.silverwolfg11.maptowny.platform.pl3xmap.v2;

import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapWorld;
import net.pl3x.map.Key;
import net.pl3x.map.markers.layer.SimpleLayer;
import net.pl3x.map.world.World;
import org.jetbrains.annotations.NotNull;

public class Pl3xMapWorldWrapper implements MapWorld {
    private final World mapWorld;

    private Pl3xMapWorldWrapper(@NotNull World pMapWorld) {
        this.mapWorld = pMapWorld;
    }

    public static Pl3xMapWorldWrapper from(World pMapWorld) {
        return new Pl3xMapWorldWrapper(pMapWorld);
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        SimpleLayer layer = new SimpleLayer(Key.of(layerKey), options::getName);

        layer.setDefaultHidden(options.isDefaultHidden());
        layer.setPriority(options.getLayerPriority());
        layer.setZIndex(options.getZIndex());
        layer.setShowControls(options.showControls());

        mapWorld.getLayerRegistry().register(layer);

        return Pl3xMapLayerWrapper.from(layer);
    }

    @Override
    public boolean hasLayer(@NotNull String layerKey) {
        return mapWorld.getLayerRegistry().has(Key.of(layerKey));
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        mapWorld.getLayerRegistry().unregister(Key.of(layerKey));
    }
}
