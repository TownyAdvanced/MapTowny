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

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.LineMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ObjectMarker;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Line;
import de.bluecolored.bluemap.api.math.Shape;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.objects.SegmentedPolygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class BlueMapLayerWrapper implements MapLayer {

    private final MarkerSet markerSet;

    private final int zIndex;

    private final BlueMapIconMapper iconMapper;

    // Allow multi-marker representation from one marker key.
    // Marker Key -> Children Markers
    // Parent keys are not actual markers.
    private final Map<String, @Unmodifiable List<String>> parentPolys = new ConcurrentHashMap<>();

    public BlueMapLayerWrapper(BlueMapIconMapper iconMapper, MarkerSet markerSet, int zIndex) {
        this.iconMapper = iconMapper;
        this.markerSet = markerSet;
        this.zIndex = zIndex;
    }

    private Vector2d pointToVec2d(Point2D point) {
        return new Vector2d(point.x(), point.z());
    }

    private Vector2d[] pointsToVecs2d(Collection<Point2D> points) {
        return points.stream()
                .map(this::pointToVec2d)
                .toArray(Vector2d[]::new);
    }

    private Color toBMColor(java.awt.Color awtColor, double alphaPercent) {
        // Use explicit constructor to avoid worrying about how the ints are packed.
        // BlueMap Color RGB values need to be between 0-255
        // while alpha is a percentage.
        return new Color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), (float) alphaPercent);
    }

    private java.awt.Color fromBMColor(Color bmColor) {
        // Java Color requires everything as percentage OR everything between 0-255.
        int convertedAlpha = (int)((double)(bmColor.getAlpha() * 255.0F) + 0.5);
        return new java.awt.Color(bmColor.getRed(), bmColor.getGreen(), bmColor.getBlue(), convertedAlpha);
    }

    private double getOpacityFromColor(java.awt.Color color) {
        int alpha = color.getAlpha();
        // Opacity is a percentage
        return alpha / 255d;
    }

    @Contract(pure = true, value = "_, _ -> new")
    private Line toLine(@NotNull List<Point2D> points, boolean joinEnds) {
        if (points == null || points.size() == 0)
            return new Line();

        Line.Builder lineBuilder = Line.builder();

        for (Point2D point : points) {
            // BlueMap uses the y-axis as the vertical axis
            lineBuilder.addPoint(new Vector3d(point.x(), zIndex, point.z()));
        }

        if (joinEnds && (points.size() > 1) &&
                !points.get(0).equals(points.get(points.size() - 1))) {
            var startingPoint = points.get(0);
            lineBuilder.addPoint(new Vector3d(startingPoint.x(), zIndex, startingPoint.z()));
        }

        return lineBuilder.build();
    }

    private void addNonSegmentedPoly(@NotNull String markerKey, @NotNull List<Point2D> polygon,
                                     @NotNull MarkerOptions markerOptions) {
        final Shape shape = new Shape(pointsToVecs2d(polygon));

        final ShapeMarker shapeMarker = ShapeMarker.builder()
                .label(markerOptions.name())
                .shape(shape, zIndex)
                .lineColor(toBMColor(markerOptions.strokeColor(), markerOptions.strokeOpacity()))
                .lineWidth(markerOptions.strokeWeight())
                .fillColor(toBMColor(markerOptions.fillColor(), markerOptions.fillOpacity()))
                .detail(markerOptions.clickTooltip())
                .build();

        markerSet.getMarkers().put(markerKey, shapeMarker);
    }

    private void addSegmentedPoly(@NotNull String markerKey, @NotNull SegmentedPolygon polygon,
                                  @NotNull MarkerOptions markerOptions) {
        final List<String> childKeys = new ArrayList<>(polygon.getNegativeSpace().size()
                                                        + polygon.getSegments().size() + 1);

        // Add polygon outline
        addLineMarker(markerKey + "_line0", toLine(polygon.getPoints(), true), markerOptions);
        childKeys.add(markerKey + "_line0");

        // Add negative space outlines
        for (int i = 0; i < polygon.getNegativeSpace().size(); i++) {
            final String negSpaceKey = markerKey + "_line" + (i + 1);
            addLineMarker(negSpaceKey, toLine(polygon.getNegativeSpace().get(i), true), markerOptions);
            childKeys.add(negSpaceKey);
        }

        // Add segmented areas
        MarkerOptions segAreaOptions = markerOptions.asBuilder()
                .strokeOpacity(0)
                .strokeWeight(0)
                .build();

        for (int i = 0; i < polygon.getSegments().size(); i++) {
            final String segKey = markerKey + "_seg" + i;
            addNonSegmentedPoly(segKey, polygon.getSegments().get(i), segAreaOptions);
            childKeys.add(segKey);
        }

        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }
    public void addPolyMarker(@NotNull String markerKey, @NotNull Polygon polygon,
                              @NotNull MarkerOptions markerOptions) {
        Objects.requireNonNull(markerKey);
        Objects.requireNonNull(polygon);
        Objects.requireNonNull(markerOptions);

        if (polygon instanceof SegmentedPolygon) {
            addSegmentedPoly(markerKey, (SegmentedPolygon) polygon, markerOptions);
        }
        else {
            addNonSegmentedPoly(markerKey, polygon.getPoints(), markerOptions);
        }
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons,
                                   @NotNull MarkerOptions markerOptions) {
        // Delete old markers
        removeMarker(markerKey);

        // Avoid layer of indirection
        if (polygons.size() == 1) {
            addPolyMarker(markerKey, polygons.get(0), markerOptions);
            return;
        }

        final List<String> childKeys = new ArrayList<>(polygons.size());
        for (int i = 0; i < polygons.size(); i++) {
            final String polyKey = markerKey + i;
            addPolyMarker(polyKey, polygons.get(i), markerOptions);
            childKeys.add(polyKey);
        }

        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }

    @Override
    public void addLineMarker(@NotNull String markerKey, @NotNull List<Point2D> line, @NotNull MarkerOptions markerOptions) {
        addLineMarker(markerKey, toLine(line, false), markerOptions);
    }

    private void addLineMarker(@NotNull String markerKey, @NotNull Line line, @NotNull MarkerOptions markerOptions) {
        var lineMarker = de.bluecolored.bluemap.api.markers.LineMarker.builder()
                .label(markerOptions.name())
                .line(line)
                .lineColor(toBMColor(markerOptions.strokeColor(), markerOptions.strokeOpacity()))
                .lineWidth(markerOptions.strokeWeight())
                .detail(markerOptions.clickTooltip())
                .build();

        markerSet.getMarkers().put(markerKey, lineMarker);
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc,
                              int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        // Use a POI Marker
        // POI Markers have no descriptions
        String bmIconAdress = iconMapper.getBlueMapAddress(iconKey);

        if (bmIconAdress == null)
            return;

        POIMarker poiMarker = POIMarker.builder()
                .label(markerOptions.name())
                .icon(bmIconAdress, sizeX / 2, sizeY / 2)
                .position(iconLoc.x(), zIndex, iconLoc.z())
                .build();

        markerSet.getMarkers().put(markerKey, poiMarker);
    }

    @Override
    public boolean hasMarker(@NotNull String markerKey) {
        return parentPolys.containsKey(markerKey) ||
                markerSet.getMarkers().containsKey(markerKey);
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        // Remove all multipolygon representations
        if (parentPolys.containsKey(markerKey)) {
            List<String> childKeys = parentPolys.get(markerKey);
            for (String childKey : childKeys) {
                removeMarker(childKey);
            }

            parentPolys.remove(markerKey);
            return true;
        }

        return markerSet.getMarkers().remove(markerKey) != null;
    }

    @Override
    public void removeMarkers(@NotNull Predicate<String> markerKeyFilter) {
        new ArrayList<>(markerSet.getMarkers().keySet()).stream()
                        .filter(markerKeyFilter)
                        .forEach(removeKey -> markerSet.getMarkers().remove(removeKey));
    }

    private MarkerOptions optionsFromMarker(Marker marker) {

        MarkerOptions.Builder options = MarkerOptions.builder()
                .name(marker.getLabel());

        if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;

            if (shapeMarker.getFillColor() != null) {
                var fillColor = fromBMColor(shapeMarker.getFillColor());

                options.fillColor(fillColor);
                options.fillOpacity(getOpacityFromColor(fillColor));
            }

            if (shapeMarker.getLineColor() != null) {
                var strokeColor = fromBMColor(shapeMarker.getLineColor());

                options.strokeColor(strokeColor);
                options.strokeOpacity(getOpacityFromColor(strokeColor));
            }

            options.strokeWeight(shapeMarker.getLineWidth());
            options.clickTooltip(shapeMarker.getDetail());
        }

        return options.build();
    }

    @Override
    public @NotNull CompletableFuture<MarkerOptions> getMarkerOptions(@NotNull String markerKey) {
        Marker marker = null;
        if (parentPolys.containsKey(markerKey)) {
            List<String> childKeys = parentPolys.get(markerKey);
            for (String childKey : childKeys) {
                marker = markerSet.getMarkers().get(childKey);
                // Try to find ShapeMarker since they have the most details
                if (marker instanceof ShapeMarker) {
                    break;
                }
            }
        }
        else {
            marker = markerSet.getMarkers().get(markerKey);
        }

        if (marker == null) {
            return CompletableFuture.completedFuture(null);
        }

        MarkerOptions markerOptions = optionsFromMarker(marker);

        return CompletableFuture.completedFuture(markerOptions);
    }

    private void updateMarkerOptions(Marker marker, MarkerOptions markerOptions) {
        marker.setLabel(markerOptions.name());

        if (marker instanceof ObjectMarker) {
            final ObjectMarker objMarker = (ObjectMarker) marker;
            objMarker.setDetail(markerOptions.clickTooltip());
            objMarker.setLabel(markerOptions.name());
        }

        if (marker instanceof LineMarker) {
            LineMarker lineMarker = (LineMarker) marker;
            lineMarker.setLineColor(toBMColor(markerOptions.strokeColor(), markerOptions.fillOpacity()));
            lineMarker.setLineWidth(markerOptions.strokeWeight());
        }
        else if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;
            shapeMarker.setFillColor(toBMColor(markerOptions.fillColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineColor(toBMColor(markerOptions.strokeColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineWidth(markerOptions.strokeWeight());
        }
    }

    @Override
    public void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions) {
        Collection<String> markerKeys = parentPolys.get(markerKey);
        if (markerKeys == null) {
            markerKeys = Collections.singleton(markerKey);
        }

        for (String keys : markerKeys) {
            Marker marker = markerSet.getMarkers().get(keys);
            if (marker != null) {
                updateMarkerOptions(marker, markerOptions);
            }
        }
    }
}
