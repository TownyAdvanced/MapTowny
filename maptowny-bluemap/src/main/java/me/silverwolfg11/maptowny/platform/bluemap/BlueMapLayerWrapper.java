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
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class BlueMapLayerWrapper implements MapLayer {

    private final MarkerSet markerSet;

    private final int zIndex;

    private final BlueMapIconMapper iconMapper;

    // Simulate BlueMap having a MultiPolygon Marker
    // Marker Key -> Number of Polygons
    // Raw Marker Keys, not world keys
    private final Map<String, Integer> multiPolys = new ConcurrentHashMap<>();

    public BlueMapLayerWrapper(BlueMapIconMapper iconMapper, MarkerSet markerSet, int zIndex) {
        this.iconMapper = iconMapper;
        this.markerSet = markerSet;
        this.zIndex = zIndex;
    }

    private String toMarkerSetKey(String markerKey) {
        if (multiPolys.containsKey(markerKey)) {
            return markerKey + 0;
        }

        return markerKey;
    }

    private Vector2d pointToVec(Point2D point) {
        return new Vector2d(point.x(), point.z());
    }

    private Vector2d[] pointsToVecs(Collection<Point2D> points) {
        return points.stream()
                .map(this::pointToVec)
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

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        // Delete markers if current multipoly size is less than old multipoly size
        if (multiPolys.containsKey(markerKey)) {
            int oldPolySize = multiPolys.remove(markerKey);
            int currPolySize = polygons.size();

            // Delete all old markers
            for (int i = currPolySize; i < oldPolySize; ++i) {
                markerSet.getMarkers().remove(markerKey + i);
            }
        }

        for (int i = 0; i < polygons.size(); i++) {
            final String polyId = markerKey + i;
            final Polygon polygon = polygons.get(i);

            final Shape shape = new Shape(pointsToVecs(polygon.getPoints()));

            final ShapeMarker shapeMarker = ShapeMarker.builder()
                    .label(markerOptions.name())
                    .shape(shape, zIndex)
                    .lineColor(toBMColor(markerOptions.strokeColor(), markerOptions.strokeOpacity()))
                    .lineWidth(markerOptions.strokeWeight())
                    .fillColor(toBMColor(markerOptions.fillColor(), markerOptions.fillOpacity()))
                    .detail(markerOptions.clickTooltip())
                    .build();

            markerSet.getMarkers().put(polyId, shapeMarker);
        }

        multiPolys.put(markerKey, polygons.size());
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
        final String targetKey = toMarkerSetKey(markerKey);

        return markerSet.getMarkers().containsKey(targetKey);
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        // Remove all multipolygon representations
        if (multiPolys.containsKey(markerKey)) {
            int polySize = multiPolys.get(markerKey);
            boolean markerExists = false;

            for (int i = 0; i < polySize; ++i) {
                final String finalKey = markerKey + i;
                markerExists |= markerSet.getMarkers().remove(finalKey) != null;
            }

            multiPolys.remove(markerKey);
            return markerExists;
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
        String targetKey = toMarkerSetKey(markerKey);

        Marker marker = markerSet.getMarkers().get(targetKey);

        if (marker == null) {
            return CompletableFuture.completedFuture(null);
        }

        MarkerOptions markerOptions = optionsFromMarker(marker);

        return CompletableFuture.completedFuture(markerOptions);
    }

    private void updateMarkerOptions(Marker marker, MarkerOptions markerOptions) {
        marker.setLabel(markerOptions.name());

        if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;
            shapeMarker.setFillColor(toBMColor(markerOptions.fillColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineColor(toBMColor(markerOptions.strokeColor(), markerOptions.fillOpacity()));
            shapeMarker.setLineWidth(markerOptions.strokeWeight());
            shapeMarker.setDetail(markerOptions.clickTooltip());
        }
    }

    @Override
    public void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions) {
        if (multiPolys.containsKey(markerKey)) {
            int polySize = multiPolys.get(markerKey);
            for (int i = 0; i < polySize; ++i) {
                Marker marker = markerSet.getMarkers().get(markerKey + i);
                updateMarkerOptions(marker, markerOptions);
            }
        }
        else {
            Marker marker = markerSet.getMarkers().get(markerKey);
            updateMarkerOptions(marker, markerOptions);
        }
    }
}
