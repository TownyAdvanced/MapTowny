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

package me.silverwolfg11.maptowny.platform;

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * An abstract layer on top of a web-map that can display markers (icons or shapes).
 * A layer is associated with a specific world.
 *
 * @since 2.0.0
 */
public interface MapLayer {

    /**
     * Add polygon marker to the layer.
     * If a marker with the marker key already exists, it will be overwritten.
     *
     * The platform is tasked with rendering the marker.
     *
     * @param markerKey Unique marker key to associate with the marker.
     * @param polygon Polygon to associate with the marker.
     * @param markerOptions Options influencing the style of the marker.
     *
     * @since 3.0.0
     */
    void addPolyMarker(@NotNull String markerKey, @NotNull Polygon polygon, @NotNull MarkerOptions markerOptions);

    /**
     * Add a multi-polygon marker to the layer.
     * If a marker with the marker key already exists, it will be overwritten.
     *
     * The platform is tasked with rendering the marker.
     *
     * @param markerKey Unique marker key to associate with the marker.
     * @param polygons List of polygons to associate with the marker.
     * @param markerOptions Options influencing the style of the marker.
     */
    void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions);

    /**
     * Add line marker to the layer.
     * If a marker with the marker key already exists, it will be overwritten.
     *
     * The platform is tasked with rendering the marker.
     *
     * @param markerKey Unique marker key to associate with the marker.
     * @param line List of points rendered as a line.
     * @param markerOptions Options influencing the style of the marker.
     *
     * @since 3.0.0
     */
    void addLineMarker(@NotNull String markerKey, @NotNull List<Point2D> line, @NotNull MarkerOptions markerOptions);

    /**
     * Add an icon marker to the layer.
     * If a marker with the marker key already exists, it will be overwritten.
     *
     * NOTE: Not all platforms will obey the icon size arguments.
     *
     * @param markerKey Unique marker key to associate with the marker.
     * @param iconKey Unique icon key to tell the platform to render a specific image associated with the icon key.
     * @param iconLoc Location to place the icon on the map.
     * @param sizeX Icon width.
     * @param sizeY Icon height.
     * @param markerOptions Options influencing the style of the marker.
     */
    void addIconMarker(@NotNull String markerKey, @NotNull String iconKey,
                       @NotNull Point2D iconLoc, int sizeX, int sizeY,
                       @NotNull MarkerOptions markerOptions);

    /**
     * Check if the layer has a marker matching the key.
     * <br>
     * On some platforms, this will only be accurate for markers registered through this API.
     *
     * @param markerKey Unique marker key.
     *
     * @return whether the layer has a marker matching the key.
     */
    boolean hasMarker(@NotNull String markerKey);

    /**
     * Remove a marker associated with the specific marker key.
     *
     * @param markerKey Unique marker key.
     * @return whether a marker matching the marker key was removed.
     */
    boolean removeMarker(@NotNull String markerKey);

    /**
     * Remove all markers that have keys that pass the filter.
     *
     * @param markerKeyFilter Filter acting upon the marker's unique key.
     */
    void removeMarkers(@NotNull Predicate<String> markerKeyFilter);

    /**
     * Get the marker options for a specific existing marker.
     *
     * The future may complete with a null value if no marker matching marker key is found.
     * The future has no guarantees about what kind of thread it completes in.
     *
     * @param markerKey Unique marker key.
     * @return completable future with the marker options or null for that marker.
     */
    @NotNull
    CompletableFuture<@Nullable MarkerOptions> getMarkerOptions(@NotNull String markerKey);

    /**
     * Set marker options for a specific existing marker.
     *
     * @param markerKey Unique marker key.
     * @param markerOptions Options influencing the style of the marker.
     */
    void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions);
}
