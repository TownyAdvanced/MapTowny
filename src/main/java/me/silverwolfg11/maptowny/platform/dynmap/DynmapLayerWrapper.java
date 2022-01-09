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

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class DynmapLayerWrapper implements MapLayer {
    private final DynmapAPI dynmapAPI;
    private final String worldName;
    private final MarkerSet markerSet;
    private final int zIndex;

    // Simulate Dynmap having a MultiPolygon Marker
    // Marker Key -> Number of Polygons
    private final Map<String, Integer> multiPolys = new ConcurrentHashMap<>();

    public DynmapLayerWrapper(DynmapAPI dynmapAPI, String worldName, MarkerSet markerSet, int zIndex) {
        this.dynmapAPI = dynmapAPI;
        this.worldName = worldName;
        this.markerSet = markerSet;
        this.zIndex = zIndex;
    }

    // Transform marker keys into per-world marker keys
    private String toWorldKey(String markerKey) {
        return markerKey + "_" + worldName;
    }

    // Convert color to dynmap's weird RGB format.
    private int toDynmapRGB(Color color) {
        // 0xRRGGBBFF
        return (color.getRGB() << 8) | 0x0FF;
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        // Dynmap has no concept of multi-polygon markers, only single polygon markers.
        // To simulate a multi-polygon marker, just create multiple polygon markers.
        final String worldKey = toWorldKey(markerKey);

        // Dynmap also doesn't allow overwriting markers
        // so have to delete all of them before creating new ones.
        if (multiPolys.containsKey(markerKey)) {
            int oldPolySize = multiPolys.get(markerKey);
            // Delete all old markers
            for (int i = 0; i < oldPolySize; ++i) {
                AreaMarker marker = markerSet.findAreaMarker(worldKey + i);
                if (marker != null) {
                    marker.deleteMarker();
                }
            }
        }

        // Create individual area-markers (polygon)
        for (int i = 0; i < polygons.size(); i++) {
            Polygon poly = polygons.get(i);
            List<Point2D> points = poly.getPoints();
            double[] x = new double[points.size()];
            double[] z = new double[points.size()];

            for (int j = 0; j < points.size(); j++) {
                Point2D point = points.get(j);
                x[j] = point.x();
                z[j] = point.z();
            }

            AreaMarker areaMarker = markerSet.createAreaMarker(worldKey + i, markerOptions.name(), false, worldName, x, z, false);
            areaMarker.setDescription(markerOptions.clickTooltip());
            if (markerOptions.fillColor() != null) {
                areaMarker.setFillStyle(markerOptions.fillOpacity(), toDynmapRGB(markerOptions.fillColor()));
            }

            areaMarker.setLineStyle(markerOptions.strokeWeight(), markerOptions.strokeOpacity(), toDynmapRGB(markerOptions.strokeColor()));
        }

        multiPolys.put(markerKey, polygons.size());
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        MarkerIcon markerIcon = dynmapAPI.getMarkerAPI().getMarkerIcon(iconKey);

        markerSet.createMarker(toWorldKey(markerKey), markerOptions.name(), worldName,
                iconLoc.x(), zIndex, iconLoc.z(), markerIcon, false);
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        final String worldKey = toWorldKey(markerKey);
        // Remove all multipolygon representations
        if (multiPolys.containsKey(markerKey)) {
            int polySize = multiPolys.get(markerKey);
            for (int i = 0; i < polySize; ++i) {
                AreaMarker marker = markerSet.findAreaMarker(worldKey + i);
                if (marker != null) {
                    marker.deleteMarker();
                }
            }

            multiPolys.remove(markerKey);
            return true;
        }

        Marker marker = markerSet.findMarker(worldKey);
        if (marker != null) {
            marker.deleteMarker();
            return true;
        }
        return false;
    }

    @Override
    public void removeMarkers(@NotNull Predicate<String> markerKeyFilter) {
        List<Marker> markersToRemove = new ArrayList<>();
        for (Marker marker : markerSet.getMarkers()) {
            if (markerKeyFilter.test(marker.getMarkerID())) {
                markersToRemove.add(marker);
            }
        }

        for (Marker marker : markersToRemove) {
            marker.deleteMarker();
        }
    }
}
