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
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.platform.bluemap.BlueMapIconMapper;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;

import java.awt.Color;

public class PolyMarkerOp implements MarkerOp {

    private final String markerSetId;
    private final String markerId;

    private final Shape shape;

    private final WorldIdentifier worldIdentifier;
    private final int zIdx;

    private final MarkerOptions markerOptions;

    public PolyMarkerOp(String markerSetId, String markerId, Shape shape, WorldIdentifier worldIdentifier, int zIdx, MarkerOptions markerOptions) {
        this.markerSetId = markerSetId;
        this.markerId = markerId;
        this.shape = shape;
        this.worldIdentifier = worldIdentifier;
        this.zIdx = zIdx;
        this.markerOptions = markerOptions;
    }

    private Color adjustAlpha(Color initialColor, double alpha) {
        int alphaNorm = (int) (alpha * 255);

        return new Color(initialColor.getRed(), initialColor.getBlue(), initialColor.getGreen(), alphaNorm);
    }

    @Override
    public void run(BlueMapAPI api, MarkerAPI markerAPI, BlueMapIconMapper iconMapper) {
        MarkerSet markerSet = markerAPI.getMarkerSet(markerSetId).orElse(null);

        if (markerSet == null)
            return;

        BlueMapMap bmp = worldIdentifier.getWorldMap(api);

        if (bmp == null)
            return;

        ShapeMarker shapeMarker = markerSet.createShapeMarker(markerId, bmp, 0, zIdx, 0, shape, zIdx);

        if (markerOptions.fillColor() != null)
            shapeMarker.setFillColor(adjustAlpha(markerOptions.fillColor(), markerOptions.fillOpacity()));

        if (markerOptions.strokeColor() != null)
            shapeMarker.setLineColor(adjustAlpha(markerOptions.strokeColor(), markerOptions.strokeOpacity()));

        shapeMarker.setLineWidth(markerOptions.strokeWeight());

        shapeMarker.setLabel(markerOptions.name());
        shapeMarker.setDetail(markerOptions.clickTooltip());
    }
}
