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

package me.silverwolfg11.maptowny.platform.dynmap;

import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapWorld;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

public class DynmapWorldWrapper implements MapWorld {
    private final DynmapAPI dynmapAPI;
    private final String worldName;

    public DynmapWorldWrapper(DynmapAPI dynmapAPI, String worldName) {
        this.dynmapAPI = dynmapAPI;
        this.worldName = worldName;
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        // Dynmap has global layers, and not per-world layers.
        // So only register the marker set once for all the worlds.
        MarkerSet markerSet = dynmapAPI.getMarkerAPI().getMarkerSet(layerKey);
        if (markerSet == null) {
            markerSet = dynmapAPI.getMarkerAPI().createMarkerSet(layerKey, options.getName(), null, false);
            markerSet.setLayerPriority(options.getLayerPriority());
            markerSet.setHideByDefault(options.isDefaultHidden());
            markerSet.setMarkerSetLabel(options.getName());
        }

        return new DynmapLayerWrapper(dynmapAPI, worldName, markerSet, options.getZIndex());
    }

    @Override
    public boolean hasLayer(@NotNull String layerKey) {
        return dynmapAPI.getMarkerAPI().getMarkerSet(layerKey) != null;
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        MarkerSet markerSet = dynmapAPI.getMarkerAPI().getMarkerSet(layerKey);
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }
    }
}
