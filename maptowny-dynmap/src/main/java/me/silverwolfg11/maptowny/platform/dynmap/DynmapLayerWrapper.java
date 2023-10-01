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
import me.silverwolfg11.maptowny.objects.SegmentedPolygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    private final Set<String> segmentedPolys = ConcurrentHashMap.newKeySet();

    public DynmapLayerWrapper(DynmapAPI dynmapAPI, String worldName, MarkerSet markerSet, int zIndex) {
        this.dynmapAPI = dynmapAPI;
        this.worldName = worldName;
        this.markerSet = markerSet;
        this.zIndex = zIndex;
    }

    // Transform marker keys into per-world marker keys
    private String toWorldKey(String markerKey) {
        return worldName + "_" + markerKey;
    }

    // Convert color to dynmap's weird RGB format.
    private int toDynmapRGB(Color color) {
        // Color Format: 0xAARRGGBB
        // Dynmap Format: 0x00RRGGBB
        // Just force the alpha value to 0
        return color.getRGB() & 0x00FFFFFF;
    }

    private Color fromDynmapRGB(int rgb) {
        // Dynmap Format: 0x00RRGGBB
        // Color Format: 0xAARRGGBB
        return new Color(rgb, false);
    }

    private void createAreaPoly(String markerKey, List<Point2D> points, MarkerOptions markerOptions) {
        final String worldKey = toWorldKey(markerKey);

        double[] x = new double[points.size()];
        double[] z = new double[points.size()];

        for (int j = 0; j < points.size(); j++) {
            Point2D point = points.get(j);
            x[j] = point.x();
            z[j] = point.z();
        }

        AreaMarker areaMarker = markerSet.createAreaMarker(worldKey, markerOptions.name(), false, worldName, x, z, false);
        if (markerOptions.clickTooltip() != null) {
            areaMarker.setDescription(markerOptions.clickTooltip());
        }

        if (markerOptions.fill() && markerOptions.fillColor() != null) {
            areaMarker.setFillStyle(markerOptions.fillOpacity(), toDynmapRGB(markerOptions.fillColor()));
        }

        if (markerOptions.stroke() && markerOptions.strokeColor() != null) {
            areaMarker.setLineStyle(markerOptions.strokeWeight(), markerOptions.strokeOpacity(), toDynmapRGB(markerOptions.strokeColor()));
        }
        else {
            areaMarker.setLineStyle(0, 0, 0);
        }

        areaMarker.setRangeY(zIndex, zIndex);
    }

    private void createLineMarker(String markerKey, List<Point2D> points, boolean joinEnds, MarkerOptions markerOptions) {
        final String worldKey = toWorldKey(markerKey);

        // Validate if ends need to be joined
        if (joinEnds && !points.isEmpty()
                && points.get(0).equals(points.get(points.size() - 1))) {
            joinEnds = false;
        }

        // Create line poly first
        final int lineSize = points.size() + (joinEnds ? 1 : 0);
        double[] x = new double[lineSize];
        double[] z = new double[lineSize];
        double[] y = new double[lineSize];
        Arrays.fill(y, zIndex);

        for (int j = 0; j < points.size(); j++) {
            Point2D point = points.get(j);
            x[j] = point.x();
            z[j] = point.z();
        }

        if (joinEnds) {
            // Set last point in array to first point in array
            // to join the ends.
            x[points.size()] = points.get(0).x();
            z[points.size()] = points.get(0).z();
        }

        PolyLineMarker lineMarker = markerSet.createPolyLineMarker(worldKey, markerOptions.name(), false, worldName, x, y, z, false);
        lineMarker.setDescription(markerOptions.clickTooltip());
        lineMarker.setLineStyle(markerOptions.strokeWeight(), markerOptions.strokeOpacity(), toDynmapRGB(markerOptions.strokeColor()));
    }

    private void createSegmentedPolygon(String markerKey, SegmentedPolygon poly, MarkerOptions markerOptions) {
        // A segmented polygon consists of two or more line markers representing the border of the polygon and
        // several area markers representing the interior of the polygon.

        // Create exterior border line
        createLineMarker(markerKey + "_line0", poly.getPoints(), true, markerOptions);

        // Create negative space outlines
        if (poly.hasNegativeSpace()) {
            int segmentIdx = 1;
            for (List<Point2D> negSpacePts : poly.getNegativeSpace()) {
                final String lineMarkerKey = markerKey + "_line" + segmentIdx;
                createLineMarker(lineMarkerKey, negSpacePts, true, markerOptions);
                segmentIdx++;
            }
        }

        MarkerOptions areaOptions = markerOptions.asBuilder()
                .strokeOpacity(0)
                .build();

        segmentedPolys.add(markerKey);

        for(int i = 0; i < poly.getSegments().size(); i++) {
            createAreaPoly(markerKey + i, poly.getSegments().get(i), areaOptions);
        }

        // Segmented polys are represented as a multipolygon and a line marker.
        multiPolys.put(markerKey, poly.getSegments().size());
    }

    private void addSinglePolyMarker(String markerKey, Polygon poly, MarkerOptions markerOptions) {
        if (poly instanceof SegmentedPolygon) {
            createSegmentedPolygon(markerKey, (SegmentedPolygon) poly, markerOptions);
        }
        else {
            createAreaPoly(markerKey, poly.getPoints(), markerOptions);
        }
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        // Dynmap has no concept of multi-polygon markers, only single polygon markers.
        // To simulate a multi-polygon marker, just create multiple polygon markers.

        // Dynmap also doesn't allow overwriting markers
        // so have to delete all of them before creating new ones.
        removeMarker(markerKey);

        if (polygons.isEmpty())
            return;

        if (polygons.size() == 1) {
            addSinglePolyMarker(markerKey, polygons.get(0), markerOptions);
            return;
        }

        // Create individual area-markers (polygon)
        for (int i = 0; i < polygons.size(); i++) {
            Polygon poly = polygons.get(i);
            addSinglePolyMarker(markerKey + i, poly, markerOptions);
        }

        multiPolys.put(markerKey, polygons.size());
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        MarkerIcon markerIcon = dynmapAPI.getMarkerAPI().getMarkerIcon(iconKey);

        if (markerIcon == null) {
            DynmapPlatform.logError(String.format(
                    "Error adding icon marker with key '%s' to Dynmap because no icon with key '%s' could be found!",
                    markerKey, iconKey
            ));
            return;
        }

        final String worldKey = toWorldKey(markerKey);

        Marker marker = markerSet.findMarker(worldKey);

        // If marker exists, overwrite marker.
        if (marker != null) {
            marker.setLabel(markerOptions.name());
            marker.setLocation(worldName, iconLoc.x(), zIndex, iconLoc.z());
            marker.setDescription(markerOptions.clickTooltip());
            return;
        }

        marker = markerSet.createMarker(worldKey, markerOptions.name(), worldName,
                        iconLoc.x(), zIndex, iconLoc.z(), markerIcon, false);

        marker.setDescription(markerOptions.clickTooltip());
    }

    @Override
    public boolean hasMarker(@NotNull String markerKey) {
        return markerSet.findMarker(toWorldKey(markerKey)) != null;
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        final String worldKey = toWorldKey(markerKey);
        // Remove all segmented representations
        if (segmentedPolys.contains(markerKey)) {
            int segmentIdx = 0;
            PolyLineMarker marker;
            while ((marker = markerSet.findPolyLineMarker(worldKey + "_line" + segmentIdx))
                    != null) {
                marker.deleteMarker();
                segmentIdx++;
            }

            segmentedPolys.remove(markerKey);
        }

        // Remove all multipolygon representations
        if (multiPolys.containsKey(markerKey)) {
            int polySize = multiPolys.get(markerKey);
            for (int i = 0; i < polySize; ++i) {
                removeMarker(markerKey + i);
            }

            multiPolys.remove(markerKey);
            return true;
        }

        // Dynmap stores different types of markers separately

        // Check if it's an area marker
        AreaMarker areaMarker = markerSet.findAreaMarker(worldKey);
        if (areaMarker != null) {
            areaMarker.deleteMarker();
            return true;
        }

        // Check if it's a line marker
        PolyLineMarker lineMarker = markerSet.findPolyLineMarker(worldKey);
        if (lineMarker != null) {
            lineMarker.deleteMarker();
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
        List<GenericMarker> markersToRemove = new ArrayList<>();

        // Dynmap stores different types of markers separately
        for (GenericMarker marker : markerSet.getMarkers()) {
            if (markerKeyFilter.test(marker.getMarkerID())) {
                markersToRemove.add(marker);
            }
        }

        for (GenericMarker marker : markerSet.getAreaMarkers()) {
            if (markerKeyFilter.test(marker.getMarkerID())) {
                markersToRemove.add(marker);
            }
        }

        for (GenericMarker marker : markerSet.getPolyLineMarkers()) {
            if (markerKeyFilter.test(marker.getMarkerID())) {
                markersToRemove.add(marker);
            }
        }

        for (GenericMarker marker : markersToRemove) {
            marker.deleteMarker();
        }

        // Handle local data
        multiPolys.keySet().removeIf(markerKeyFilter);
        segmentedPolys.removeIf(markerKeyFilter);
    }

    @Override
    public @NotNull CompletableFuture<MarkerOptions> getMarkerOptions(@NotNull String markerKey) {
        final String worldKey = toWorldKey(markerKey);

        Marker marker;
        if (multiPolys.containsKey(markerKey)) {
            // Use first marker of the polygon to get the marker options for all polygons
            marker = markerSet.findMarker(worldKey + 0);
        }
        else {
            marker = markerSet.findMarker(worldKey);
        }

        if (marker == null)
            return CompletableFuture.completedFuture(null);

        MarkerOptions.Builder markerBuilder = MarkerOptions.builder()
                .name(marker.getLabel())
                .clickTooltip(marker.getDescription());

        if (marker instanceof AreaMarker) {
            AreaMarker areaMarker = (AreaMarker) marker;

            markerBuilder
                    .fillColor(fromDynmapRGB(areaMarker.getFillColor()))
                    .fillOpacity(areaMarker.getFillOpacity())
                    .strokeOpacity(areaMarker.getLineOpacity())
                    .strokeWeight(areaMarker.getLineWeight());
        }
        else {
            markerBuilder.fill(false)
                    .stroke(false);
        }

        return CompletableFuture.completedFuture(markerBuilder.build());
    }

    @Override
    public void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions) {
        final String worldKey = toWorldKey(markerKey);

        List<Marker> markers = Collections.emptyList();

        if (multiPolys.containsKey(markerKey)) {
            int polySize = multiPolys.get(markerKey);
            markers = new ArrayList<>(polySize);

            for (int polyIdx = 0; polyIdx < polySize; ++polyIdx) {
                Marker marker = markerSet.findMarker(worldKey + polyIdx);
                if (marker != null)
                    markers.add(marker);
            }
        }
        else {
            Marker marker = markerSet.findMarker(worldKey);
            if (marker != null)
                markers = Collections.singletonList(marker);
        }

        for (Marker marker : markers) {
            marker.setLabel(markerOptions.name());
            marker.setDescription(markerOptions.clickTooltip());

            if (marker instanceof AreaMarker) {
                AreaMarker areaMarker = (AreaMarker) marker;
                if (markerOptions.fillColor() != null) {
                    areaMarker.setFillStyle(markerOptions.fillOpacity(), toDynmapRGB(markerOptions.fillColor()));
                }

                areaMarker.setLineStyle(markerOptions.strokeWeight(), markerOptions.strokeOpacity(), toDynmapRGB(markerOptions.strokeColor()));
            }
        }
    }
}
