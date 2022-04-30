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

public class SetMarkerOptionsOp implements MarkerOp {

    private final String markerSetId;
    private final String markerId;

    private final MarkerOptions markerOptions;

    public SetMarkerOptionsOp(String markerSetId, String markerId, MarkerOptions markerOptions) {
        this.markerSetId = markerSetId;
        this.markerId = markerId;
        this.markerOptions = markerOptions;
    }

    private Color adjustAlpha(Color initialColor, double alpha) {
        int alphaNorm = (int) (alpha * 255);
        return new Color(initialColor.getRed(), initialColor.getBlue(), initialColor.getGreen(), alphaNorm);
    }

    @Override
    public void run(BlueMapAPI api, MarkerAPI markerAPI, BlueMapIconMapper iconMapper) {
        MarkerSet markerSet = markerAPI.getMarkerSet(markerSetId).orElse(null);

        if (markerSet == null) {
            return;
        }

        Marker marker = markerSet.getMarker(markerId).orElse(null);

        if (marker == null) {
            return;
        }

        marker.setLabel(markerOptions.name());

        if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;
            shapeMarker.setFillColor(adjustAlpha(markerOptions.fillColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineColor(adjustAlpha(markerOptions.strokeColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineWidth(markerOptions.strokeWeight());
            shapeMarker.setDetail(markerOptions.clickTooltip());
        }
    }
}
