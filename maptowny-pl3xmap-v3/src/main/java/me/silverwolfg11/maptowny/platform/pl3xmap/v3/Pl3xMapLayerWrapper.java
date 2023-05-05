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

package me.silverwolfg11.maptowny.platform.pl3xmap.v3;

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Icon;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.MultiPolygon;
import net.pl3x.map.core.markers.marker.Polyline;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class Pl3xMapLayerWrapper implements MapLayer {
    private final SimpleLayer layer;

    private Pl3xMapLayerWrapper(@NotNull SimpleLayer layer) {
        this.layer = layer;
    }

    public static Pl3xMapLayerWrapper from(SimpleLayer layer) {
        return new Pl3xMapLayerWrapper(layer);
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

    private Color toAlphaColor(java.awt.Color awtColor, double alphaPercent) {
        // Use explicit constructor to avoid worrying about how the ints are packed.
        return new Color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), (int) (alphaPercent * 255));
    }

    private java.awt.Color fromAlpha(Color alphaColor) {
        return new java.awt.Color(alphaColor.getRed(), alphaColor.getGreen(), alphaColor.getBlue(), 255);
    }

    private double getOpacityFromColor(java.awt.Color color) {
        int alpha = color.getAlpha();
        // Opacity is a percentage
        return alpha / 255d;
    }

    private Options buildOptions(MarkerOptions markerOptions) {
        var builder = Options.builder()
                .popupContent(markerOptions.clickTooltip())
                .tooltipContent(markerOptions.hoverTooltip());

        if (markerOptions.fill()) {
            builder.fill(true)
                    .fillType(
                            markerOptions.fillRule() == MarkerOptions.FillRule.EVENODD ? Fill.Type.EVENODD : Fill.Type.NONZERO
                    );

            if (markerOptions.fillColor() != null) {
                builder.fillColor(toAlphaColor(markerOptions.fillColor(), markerOptions.fillOpacity()).getRGB());
            }
        }

        if (markerOptions.stroke()) {
            builder.stroke(true)
                    .strokeColor(toAlphaColor(markerOptions.strokeColor(), markerOptions.strokeOpacity()).getRGB())
                    .strokeWeight(markerOptions.strokeWeight());
        }

        return builder.build();
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        List<net.pl3x.map.core.markers.marker.Polygon> parts = new ArrayList<>(polygons.size());
        for (int polyIdx = 0; polyIdx < polygons.size(); polyIdx++) {
            Polygon polygon = polygons.get(polyIdx);
            List<List<Point2D>> negSpace = polygon.getNegativeSpace();

            List<Polyline> polyLines = new ArrayList<>(1 + negSpace.size());
            polyLines.add(
                    new Polyline(String.valueOf(markerKey + "_" + polyIdx + "_0"),
                                    toPoints(polygon.getPoints()))
            );

            for (int i = 0; i < negSpace.size(); i++) {
                String lineKey = markerKey + "_" + polyIdx + "_" + (i + 1);
                polyLines.add(new Polyline(String.valueOf(lineKey), toPoints(negSpace.get(i))));
            }

            parts.add(new net.pl3x.map.core.markers.marker.Polygon((markerKey + "_" + polyIdx)));
        }


        MultiPolygon multiPolygon = MultiPolygon.multiPolygon(String.valueOf((markerKey)), parts);

        var options = buildOptions(markerOptions);
        multiPolygon.setOptions(options);

        layer.addMarker(Marker.multiPolygon(((markerKey))));
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        Icon icon = Marker.icon((markerKey), toPoint(iconLoc), (iconKey), sizeX, sizeY);
        // Convert marker options
        Options iconOptions = Options.builder()
                .popupContent(markerOptions.clickTooltip())
                .tooltipContent(markerOptions.hoverTooltip())
                .build();

        icon.setOptions(iconOptions);

        layer.addMarker(icon);
    }

    @Override
    public boolean hasMarker(@NotNull String markerKey) {
        return layer.hasMarker((markerKey));
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        return layer.removeMarker(String.valueOf(markerKey)) != null;
    }

    @Override
    public void removeMarkers(@NotNull Predicate<String> markerKeyFilter) {
        List<String> markersToRemove = layer.registeredMarkers().keySet().stream()
                .filter(markerKeyFilter)
                .toList();

        for (String key : markersToRemove) {
            layer.removeMarker(key);
        }
    }

    @Override
    public @NotNull CompletableFuture<MarkerOptions> getMarkerOptions(@NotNull String markerKey) {
        Marker<?> marker = layer.registeredMarkers().get((markerKey));

        if (marker == null)
            return CompletableFuture.completedFuture(null);

        Options pl3xOptions = marker.getOptions();

        if (pl3xOptions == null)
            return CompletableFuture.completedFuture(null);

        MarkerOptions.Builder markerOptionsBuilder = MarkerOptions.builder();

        if (pl3xOptions.getPopup() != null) {
            markerOptionsBuilder.clickTooltip(pl3xOptions.getPopup().getContent());
        }

        if (pl3xOptions.getTooltip() != null) {
             markerOptionsBuilder.hoverTooltip(pl3xOptions.getTooltip().getContent());
        }

        if (pl3xOptions.getFill() != null && pl3xOptions.getFill().isEnabled()) {
            var fill = pl3xOptions.getFill();
            var fillColor = new Color(fill.getColor());

            markerOptionsBuilder.fill(true);
            markerOptionsBuilder.fillColor(fromAlpha(fillColor));
            markerOptionsBuilder.fillOpacity(getOpacityFromColor(fillColor));
        }

        if (pl3xOptions.getStroke() != null && pl3xOptions.getStroke().isEnabled()) {
            var strokeColor = new Color(pl3xOptions.getStroke().getColor());

            markerOptionsBuilder.stroke(true);
            markerOptionsBuilder.strokeColor(fromAlpha(strokeColor));
            markerOptionsBuilder.strokeOpacity(getOpacityFromColor(strokeColor));
            markerOptionsBuilder.strokeWeight(pl3xOptions.getStroke().getWeight());
        }

        return CompletableFuture.completedFuture(markerOptionsBuilder.build());
    }

    @Override
    public void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions) {
        Marker<?> marker = layer.registeredMarkers().get((markerKey));

        if (marker == null)
            return;

        Options pl3xOptions = buildOptions(markerOptions);
        marker.setOptions(pl3xOptions);
    }
}
