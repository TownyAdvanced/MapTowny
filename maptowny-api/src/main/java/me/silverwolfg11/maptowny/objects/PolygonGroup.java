/*
 * Copyright (c) 2023 Silverwolfg11
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

package me.silverwolfg11.maptowny.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

/**
 * Polygon groups are a collection of {@link Polygon}s that belong to
 * the same world (analogous to MultiPolygon markers).
 * <br><br>
 * Each polygon group contains its own {@link MarkerOptions} to dictate the rendered
 * styling.
 *
 * @since 3.0.0
 */
public class PolygonGroup {
    protected MarkerOptions markerOptions;
    protected final List<Polygon> polygons;

    public PolygonGroup(@NotNull List<Polygon> polygons,
                        @NotNull MarkerOptions markerOptions) {
        this.polygons = polygons;
        this.markerOptions = markerOptions;
    }

    /**
     * Get the polygons in this group.
     *
     * @return the polygons in this group.
     */
    @UnmodifiableView
    @NotNull
    public List<Polygon> getPolygons() {
        return polygons;
    }


    /**
     * Get the marker options (styling options) for this polygon group.
     * <br><br>
     * Use {@link MarkerOptions#asBuilder()} to modify the marker options, and
     * {@link #setMarkerOptions(MarkerOptions)} to set this group's marker options.
     *
     * @return the marker options for this polygon group, or null if not set.
     */
    @NotNull
    public MarkerOptions getMarkerOptions() {
        return markerOptions;
    }

    /**
     * Set the marker options (styling options) for this polygon group.
     *
     * @param markerOptions the marker options to apply to this polygon group.
     */
    public void setMarkerOptions(@NotNull MarkerOptions markerOptions) {
        if (markerOptions == null) {
            return;
        }

        this.markerOptions = markerOptions;
    }
}
