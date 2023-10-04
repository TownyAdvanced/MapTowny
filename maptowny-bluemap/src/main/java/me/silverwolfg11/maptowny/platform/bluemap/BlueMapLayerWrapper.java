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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        return new Color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), (int) alphaPercent * 255);
    }

    private java.awt.Color fromBMColor(Color bmColor) {
        return new java.awt.Color(bmColor.getRed(), bmColor.getGreen(), bmColor.getBlue(), bmColor.getAlpha());
    }

    private double getOpacityFromColor(java.awt.Color color) {
        int alpha = color.getAlpha();
        // Opacity is a percentage
        return alpha / 255d;
    }

    private void addShapeMarker(@NotNull String markerKey, List<Point2D> points, MarkerOptions markerOptions) {
        final Shape shape = new Shape(pointsToVecs2d(points));

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

    private void addLineMarker(@NotNull String markerKey, List<Point2D> points, boolean joinEnds, MarkerOptions markerOptions) {
        Line.Builder lineBuilder = Line.builder();

        // Check if really need to join ends
        if (joinEnds && ((points.size() < 2) || (points.get(0).equals(points.get(points.size() - 1))))) {
            joinEnds = false;
        }

        for (Point2D point : points) {
            lineBuilder.addPoint(new Vector3d(point.x(), point.z(), zIndex));
        }

        if (joinEnds) {
            final Point2D firstPoint = points.get(0);
            lineBuilder.addPoint(new Vector3d(firstPoint.x(), firstPoint.z(), zIndex));
        }

        LineMarker lineMarker = LineMarker.builder()
                .label(markerOptions.name())
                .line(lineBuilder.build())
                .lineColor(toBMColor(markerOptions.strokeColor(), markerOptions.strokeOpacity()))
                .lineWidth(markerOptions.strokeWeight())
                .detail(markerOptions.clickTooltip())
                .build();

        markerSet.getMarkers().put(markerKey, lineMarker);
    }

    private void addSegmentedPoly(@NotNull String markerKey, SegmentedPolygon segPoly, MarkerOptions markerOptions) {
        final List<String> childKeys = new ArrayList<>(segPoly.getNegativeSpace().size() + segPoly.getSegments().size() + 1);

        // Add polygon outline
        addLineMarker(markerKey + "_line0", segPoly.getPoints(), true, markerOptions);
        childKeys.add(markerKey + "_line0");

        // Add negative space outlines
        for (int i = 0; i < segPoly.getNegativeSpace().size(); i++) {
            final String negSpaceKey = markerKey + "_line" + (i + 1);
            addLineMarker(negSpaceKey, segPoly.getNegativeSpace().get(i), true, markerOptions);
            childKeys.add(negSpaceKey);
        }

        // Add segmented areas
        MarkerOptions segAreaOptions = markerOptions.asBuilder()
                                                    .strokeOpacity(0)
                                                    .strokeWeight(0)
                                                    .build();

        for (int i = 0; i < segPoly.getSegments().size(); i++) {
            final String segKey = markerKey + "_seg" + i;
            addShapeMarker(segKey, segPoly.getSegments().get(i), segAreaOptions);
            childKeys.add(segKey);
        }

        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }

    private void addPolyMarker(@NotNull String markerKey, Polygon polygon, MarkerOptions markerOptions) {
        if (polygon instanceof SegmentedPolygon) {
            addSegmentedPoly(markerKey, (SegmentedPolygon) polygon, markerOptions);
        }
        else {
            addShapeMarker(markerKey, polygon.getPoints(), markerOptions);
        }
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        // Delete old markers
        removeMarker(markerKey);

        final List<String> childKeys = new ArrayList<>(polygons.size());
        for (int i = 0; i < polygons.size(); i++) {
            final String polyKey = markerKey + i;
            addPolyMarker(polyKey, polygons.get(i), markerOptions);
            childKeys.add(polyKey);
        }

        parentPolys.put(markerKey, Collections.unmodifiableList(childKeys));
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        // Use a POI Marker
        // POI Markers have no descriptions

        String bmIconAdress = iconMapper.getBlueMapAddress(iconKey);

        if (bmIconAdress == null)
            return;

        POIMarker poiMarker = POIMarker.toBuilder()
                .label(markerOptions.name())
                .icon(bmIconAdress, sizeX / 2, sizeY / 2)
                .position((int) iconLoc.x(), zIndex, (int) iconLoc.z())
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
