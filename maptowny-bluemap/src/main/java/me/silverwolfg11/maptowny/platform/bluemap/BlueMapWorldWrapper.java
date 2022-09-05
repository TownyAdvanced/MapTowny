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
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.jetbrains.annotations.NotNull;

public class BlueMapWorldWrapper implements MapWorld {

    private final BlueMapIconMapper iconMapper;
    private final BlueMapWorld bmWorld;

    public BlueMapWorldWrapper(WorldIdentifier worldIdentifier, BlueMapIconMapper iconMapper) {
        this.iconMapper = iconMapper;
        this.bmWorld = worldIdentifier.getBlueMapWorld(BlueMapAPI.getInstance().get());
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        MarkerSet markerSet = new MarkerSet(options.getName());
        markerSet.setDefaultHidden(options.isDefaultHidden());
        markerSet.setToggleable(options.showControls());

        for (BlueMapMap map : bmWorld.getMaps()) {
            map.getMarkerSets().put(layerKey, markerSet);
        }

        return new BlueMapLayerWrapper(iconMapper, markerSet, options.getZIndex());
    }

    @Override
    public boolean hasLayer(@NotNull String layerKey) {
        for (BlueMapMap map : bmWorld.getMaps()) {
            if (map.getMarkerSets().containsKey(layerKey))
                return true;
        }

        return false;
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        for (BlueMapMap map : bmWorld.getMaps()) {
            map.getMarkerSets().remove(layerKey);
        }
    }
}
