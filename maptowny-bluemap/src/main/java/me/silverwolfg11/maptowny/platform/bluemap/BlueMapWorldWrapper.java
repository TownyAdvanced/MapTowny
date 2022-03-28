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
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlueMapWorldWrapper implements MapWorld {

    private final WorldIdentifier worldIdentifier;
    private final Logger errorLogger;
    private final boolean registerMarkerSet;
    private final BlueMapMarkerProcessor markerProcessor;

    public BlueMapWorldWrapper(WorldIdentifier worldIdentifier, Logger errorLogger, boolean registerMarkerSet, BlueMapMarkerProcessor markerProcessor) {
        this.worldIdentifier = worldIdentifier;
        this.errorLogger = errorLogger;
        this.registerMarkerSet = registerMarkerSet;
        this.markerProcessor = markerProcessor;
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        // Layers in BlueMap are global (independent of world)
        // BlueMap Marker Ops require I/O, so to avoid this, keep track if the Towny layer has already been registered.
        if (registerMarkerSet) {
            BlueMapAPI api = BlueMapAPI.getInstance().get();
            try {
                MarkerAPI markerAPI = api.getMarkerAPI();

                MarkerSet markerSet = markerAPI.createMarkerSet(layerKey);
                if (markerSet != null) {
                    markerSet.setLabel(options.getName());
                    markerSet.setDefaultHidden(options.isDefaultHidden());
                    markerSet.setToggleable(options.showControls());
                    markerAPI.save();
                }

            } catch (IOException e) {
                errorLogger.log(Level.SEVERE, String.format("Error creating layer '%s' for BlueMap!", layerKey), e);
            }
        }

        return new BlueMapLayerWrapper(markerProcessor, layerKey, worldIdentifier, options.getZIndex());
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        // Only unregister the marker set once.
        if (registerMarkerSet) {
            BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
            if (api == null)
                return;

            try {
                MarkerAPI markerAPI = api.getMarkerAPI();
                markerAPI.removeMarkerSet(layerKey);
                markerAPI.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
