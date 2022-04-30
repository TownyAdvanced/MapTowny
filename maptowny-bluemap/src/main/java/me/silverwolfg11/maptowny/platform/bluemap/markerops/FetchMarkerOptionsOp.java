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

package me.silverwolfg11.maptowny.platform.bluemap.markerops;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.Marker;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.platform.bluemap.BlueMapIconMapper;

import java.awt.Color;
import java.util.function.Consumer;

public class FetchMarkerOptionsOp implements MarkerOp {

    private final String markerSetId;
    private final String markerId;

    private final Consumer<MarkerOptions> callback;

    public FetchMarkerOptionsOp(String markerSetId, String markerId, Consumer<MarkerOptions> callback) {
        this.markerSetId = markerSetId;
        this.markerId = markerId;
        this.callback = callback;
    }

    private int getOpacityFromColor(Color color) {
        int alpha = color.getAlpha();
        return alpha / 255;
    }


    @Override
    public void run(BlueMapAPI api, MarkerAPI markerAPI, BlueMapIconMapper iconMapper) {
        MarkerSet markerSet = markerAPI.getMarkerSet(markerSetId).orElse(null);

        if (markerSet == null) {
            callback.accept(null);
            return;
        }


        Marker marker = markerSet.getMarker(markerId).orElse(null);

        if (marker == null) {
            callback.accept(null);
            return;
        }

        MarkerOptions.Builder options = MarkerOptions.builder()
                .name(marker.getLabel());

        if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;

            if (shapeMarker.getFillColor() != null) {
                options.fillColor(shapeMarker.getFillColor());
                options.fillOpacity(getOpacityFromColor(shapeMarker.getFillColor()));
            }

            if (shapeMarker.getLineColor() != null) {
                options.strokeColor(shapeMarker.getLineColor());
                options.strokeOpacity(getOpacityFromColor(shapeMarker.getLineColor()));
            }

            options.strokeWeight(shapeMarker.getLineWidth());
            options.clickTooltip(shapeMarker.getDetail());
        }

        callback.accept(options.build());
    }
}
