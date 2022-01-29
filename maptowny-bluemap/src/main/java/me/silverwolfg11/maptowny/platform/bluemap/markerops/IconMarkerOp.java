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
import de.bluecolored.bluemap.api.marker.POIMarker;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.platform.bluemap.BlueMapIconMapper;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;

public class IconMarkerOp implements MarkerOp {

    private final String markerSetId;
    private final String markerId;
    private final String iconKey;

    private final WorldIdentifier worldId;
    private final int zIdx;

    private final Point2D point;
    private final MarkerOptions markerOptions;

    private final int sizeX, sizeY;

    public IconMarkerOp(String markerSetId, String markerId, String iconKey, WorldIdentifier worldId, int zIdx, Point2D point, MarkerOptions markerOptions, int sizeX, int sizeY) {
        this.markerSetId = markerSetId;
        this.markerId = markerId;
        this.iconKey = iconKey;
        this.worldId = worldId;
        this.zIdx = zIdx;
        this.point = point;
        this.markerOptions = markerOptions;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    @Override
    public void run(BlueMapAPI api, MarkerAPI markerAPI, BlueMapIconMapper iconMapper) {
        MarkerSet markerSet = markerAPI.getMarkerSet(markerSetId).orElse(null);
        if (markerSet == null)
            return;

        BlueMapMap bmp = worldId.getWorldMap(api);
        if (bmp == null)
            return;

        // POIMarker is a Point-Of-Interest Marker that is shown at a specific location
        POIMarker poiMarker = markerSet.createPOIMarker(markerId, bmp, point.x(), zIdx, point.z());
        poiMarker.setLabel(markerOptions.name());

        String bluePath = iconMapper.getBlueMapAddress(iconKey);

        if (bluePath == null) {
            return;
        }

        poiMarker.setIcon(bluePath, (int) point.x(), (int) point.z());
    }
}
