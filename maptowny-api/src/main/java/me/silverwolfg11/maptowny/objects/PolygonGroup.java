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
 * Polygon groups are a collection of {@link Polygon}s that belong to the same world (analogous to MultiPolygon markers).
 * Each polygon group is allowed to modify the default
 * town marker options to apply changes based on the group's properties.
 * <br><br>
 * This class is meant to be extended. The default version of this class does not modify the default
 * town marker options.
 *
 * @since 3.0.0
 */
public class PolygonGroup {
    final List<Polygon> polygons;

    public PolygonGroup(@NotNull List<Polygon> polygons) {
        this.polygons = polygons;
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
     * Modify the marker options builder based on the polygon group's properties.
     *
     * @param optionsBuilder Marker options builder that is modified.
     */
    public void modifyMarkerOptions(MarkerOptions.Builder optionsBuilder) {
    }
}
