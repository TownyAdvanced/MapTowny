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
import me.silverwolfg11.maptowny.platform.bluemap.BlueMapIconMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RemoveMarkersOp implements MarkerOp {

    private final String markerSetId;
    private final Predicate<String> markerFilter;

    public RemoveMarkersOp(String markerSetId, Predicate<String> markerFilter) {
        this.markerSetId = markerSetId;
        this.markerFilter = markerFilter;
    }

    @Override
    public void run(BlueMapAPI api, MarkerAPI markerAPI, BlueMapIconMapper iconMapper) {
        MarkerSet markerSet = markerAPI.getMarkerSet(markerSetId).orElse(null);

        if (markerSet == null)
            return;

        List<String> markerIdsToRemove = new ArrayList<>();
        for (Marker marker : markerSet.getMarkers()) {
            String id = marker.getId();
            if (markerFilter.test(id)) {
                markerIdsToRemove.add(id);
            }
        }

        for (String markerId : markerIdsToRemove) {
            markerSet.removeMarker(markerId);
        }
    }
}
