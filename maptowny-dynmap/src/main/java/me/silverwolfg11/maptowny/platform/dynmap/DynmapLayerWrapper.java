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
import org.dynmap.markers.MarkerDescription;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DynmapLayerWrapper implements MapLayer {
    private final DynmapAPI dynmapAPI;
    private final String worldName;
    private final MarkerSet markerSet;
    private final int zIndex;

    // Simulate Dynmap having a MultiPolygon Marker
    // Marker key -> Child Polys
    // Parent keys should not be actual markers.
    private final Map<String, @Unmodifiable List<String>> parentPolys = new ConcurrentHashMap<>();

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
    private void addNonSegmentedPoly(@NotNull String markerKey, @NotNull List<Point2D> points,
                              @NotNull MarkerOptions markerOptions) {
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

    private void addSegmentedPolygon(String markerKey, SegmentedPolygon poly, MarkerOptions markerOptions) {
        // A segmented polygon consists of two or more line markers representing the border of the polygon and
        // several area markers representing the interior of the polygon.

        List<String> childKeys = new ArrayList<>();
        // Create exterior border line
        addLineMarker(markerKey + "_line0", poly.getPoints(), true, markerOptions);
        childKeys.add(markerKey + "_line0");

        // Segmented polygons will always have negative space.
        int segmentIdx = 1;
        for (List<Point2D> negSpacePts : poly.getNegativeSpace()) {
            final String lineMarkerKey = markerKey + "_line" + segmentIdx;
            addLineMarker(lineMarkerKey, negSpacePts, true, markerOptions);
            childKeys.add(lineMarkerKey);
            segmentIdx++;
        }

        MarkerOptions areaOptions = markerOptions.asBuilder()
                .strokeOpacity(0)
                .build();

        for (int i = 0; i < poly.getSegments().size(); i++) {
            final String segmentKey = markerKey + i;
            addNonSegmentedPoly(segmentKey, poly.getSegments().get(i), areaOptions);
            childKeys.add(segmentKey);
        }

        // Segmented polys are represented as a multipolygon and a line marker.
        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }

    @Override
    public void addPolyMarker(@NotNull String markerKey, @NotNull Polygon polygon,
                              @NotNull MarkerOptions markerOptions) {
        if (polygon instanceof SegmentedPolygon) {
            addSegmentedPolygon(markerKey, (SegmentedPolygon) polygon, markerOptions);
        } else {
            addNonSegmentedPoly(markerKey, polygon.getPoints(), markerOptions);
        }
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons,
                                   @NotNull MarkerOptions markerOptions) {
        // Dynmap has no concept of multi-polygon markers, only single polygon markers.
        // To simulate a multi-polygon marker, just create multiple polygon markers.

        // Dynmap also doesn't allow overwriting markers
        // so have to delete all of them before creating new ones.
        removeMarker(markerKey);

        if (polygons.isEmpty())
            return;

        if (polygons.size() == 1) {
            addPolyMarker(markerKey, polygons.get(0), markerOptions);
            return;
        }

        // Create individual area-markers (polygon)
        List<String> childKeys = new ArrayList<>(polygons.size());
        for (int i = 0; i < polygons.size(); i++) {
            Polygon poly = polygons.get(i);
            final String polyKey = markerKey + i;
            addPolyMarker(polyKey, poly, markerOptions);
            childKeys.add(polyKey);
        }

        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }

    @Override
    public void addLineMarker(@NotNull String markerKey, @NotNull List<Point2D> line, @NotNull MarkerOptions markerOptions) {
        addLineMarker(markerKey, line, false, markerOptions);
    }

    private void addLineMarker(@NotNull String markerKey, @NotNull List<Point2D> line,
                               boolean joinEnds, @NotNull MarkerOptions markerOptions) {
        final String worldKey = toWorldKey(markerKey);

        // Validate if ends need to be joined
        if (joinEnds &&
                (line.isEmpty() || line.get(0).equals(line.get(line.size() - 1)))) {
            joinEnds = false;
        }

        // Create line poly first
        final int lineSize = line.size() + (joinEnds ? 1 : 0);;

        double[] x = new double[lineSize];
        double[] z = new double[lineSize];
        double[] y = new double[lineSize];
        Arrays.fill(y, zIndex);

        for (int j = 0; j < line.size(); j++) {
            Point2D point = line.get(j);
            x[j] = point.x();
            z[j] = point.z();
        }

        if (joinEnds) {
            // Set last point in array to first point in array
            // to join the ends.
            x[line.size()] = line.get(0).x();
            z[line.size()] = line.get(0).z();
        }

        PolyLineMarker lineMarker = markerSet.createPolyLineMarker(worldKey, markerOptions.name(), false, worldName, x, y, z, false);
        lineMarker.setDescription(markerOptions.clickTooltip());
        lineMarker.setLineStyle(markerOptions.strokeWeight(), markerOptions.strokeOpacity(), toDynmapRGB(markerOptions.strokeColor()));
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

    private MarkerDescription findDynmapMarker(@NotNull String worldMarkerKey) {
        MarkerDescription marker;

        // Dynmap stores different types of markers separately
        if ((marker = markerSet.findAreaMarker(worldMarkerKey)) != null) {
            return marker;
        }

        if ((marker = markerSet.findPolyLineMarker(worldMarkerKey)) != null) {
            return marker;
        }

        if ((marker = markerSet.findMarker(worldMarkerKey)) != null) {
            return marker;
        }

        return null;
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        // Check if marker key is a parent
        List<String> childrenKeys = parentPolys.get(markerKey);
        if (childrenKeys != null) {
            for (String childrenKey : childrenKeys) {
                removeMarker(childrenKey);
            }
            parentPolys.remove(markerKey);
            return true;
        }

        final String worldKey = toWorldKey(markerKey);
        GenericMarker marker = findDynmapMarker(worldKey);
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
        parentPolys.keySet().removeIf(markerKeyFilter);
    }

    @Override
    public @NotNull CompletableFuture<MarkerOptions> getMarkerOptions(@NotNull String markerKey) {
        String worldKey = toWorldKey(markerKey);

        if (parentPolys.containsKey(markerKey)) {
            // Use first child marker of the parent if it exists
            worldKey = toWorldKey(parentPolys.get(markerKey).get(0));
        }

        final MarkerDescription marker = findDynmapMarker(worldKey);

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
        List<MarkerDescription> markers = Collections.emptyList();

        if (parentPolys.containsKey(markerKey)) {
            markers = parentPolys.get(markerKey)
                    .stream()
                    .map(this::toWorldKey)
                    .map(this::findDynmapMarker)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        else {
            final String worldKey = toWorldKey(markerKey);
            MarkerDescription marker = findDynmapMarker(worldKey);
            if (marker != null)
                markers = Collections.singletonList(marker);
        }

        for (MarkerDescription marker : markers) {
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
