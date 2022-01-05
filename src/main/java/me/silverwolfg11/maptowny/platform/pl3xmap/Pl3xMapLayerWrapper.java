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

package me.silverwolfg11.maptowny.platform.pl3xmap;

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.SimpleLayerProvider;
import net.pl3x.map.api.marker.Icon;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MultiPolygon;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Pl3xMapLayerWrapper implements MapLayer {
    private final SimpleLayerProvider layerProvider;

    private Pl3xMapLayerWrapper(@NotNull SimpleLayerProvider layerProvider) {
        this.layerProvider = layerProvider;
    }

    public static Pl3xMapLayerWrapper from(SimpleLayerProvider layerProvider) {
        return new Pl3xMapLayerWrapper(layerProvider);
    }

    private Point toPoint(Point2D point2D) {
        return Point.of(point2D.x(), point2D.z());
    }

    private List<Point> toPoints(List<Point2D> point2Ds) {
        List<Point> points = new ArrayList<>(point2Ds.size());
        for (Point2D point2D : point2Ds) {
            points.add(toPoint(point2D));
        }

        return points;
    }

    private net.pl3x.map.api.marker.MarkerOptions buildOptions(MarkerOptions markerOptions) {
        net.pl3x.map.api.marker.MarkerOptions.Builder builder = net.pl3x.map.api.marker.MarkerOptions
                .builder()
                .clickTooltip(markerOptions.clickTooltip())
                .hoverTooltip(markerOptions.hoverTooltip())
                .fill(markerOptions.fill())
                .fillOpacity(markerOptions.fillOpacity())
                .stroke(markerOptions.stroke())
                .strokeColor(markerOptions.strokeColor())
                .strokeWeight(markerOptions.strokeWeight())
                .strokeOpacity(markerOptions.strokeOpacity());

        // Maintain parameter contract
        if (markerOptions.fillColor() != null)
            builder.fillColor(markerOptions.fillColor());

        return builder.build();
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        List<MultiPolygon.MultiPolygonPart> parts = new ArrayList<>(polygons.size());
        for (Polygon polygon : polygons) {
            List<Point> polyPoints = toPoints(polygon.getPoints());
            List<List<Point>> negSpace = polygon.getNegativeSpace().stream()
                                                                    .map(this::toPoints)
                                                                    .collect(Collectors.toList());
            parts.add(MultiPolygon.part(polyPoints, negSpace));
        }

        MultiPolygon multiPolygon = MultiPolygon.multiPolygon(parts);
        multiPolygon.markerOptions(buildOptions(markerOptions));

        layerProvider.addMarker(Key.of(markerKey), multiPolygon);
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        Icon icon = Marker.icon(toPoint(iconLoc), Key.of(iconKey), sizeX, sizeY);
        // Convert marker options
        net.pl3x.map.api.marker.MarkerOptions iconOptions = net.pl3x.map.api.marker.MarkerOptions.builder()
                .clickTooltip(markerOptions.clickTooltip())
                .hoverTooltip(markerOptions.hoverTooltip())
                .build();

        icon.markerOptions(iconOptions);
        layerProvider.addMarker(Key.of(markerKey), icon);
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        return layerProvider.removeMarker(Key.of(markerKey)) != null;
    }

    @Override
    public void removeMarkers(@NotNull Predicate<String> markerKeyFilter) {
        List<Key> markersToRemove = layerProvider.registeredMarkers().keySet().stream()
                .filter(k -> markerKeyFilter.test(k.getKey()))
                .collect(Collectors.toList());

        for (Key key : markersToRemove) {
            layerProvider.removeMarker(key);
        }
    }
}
