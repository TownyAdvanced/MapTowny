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

package me.silverwolfg11.maptowny.objects;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper class that stores a polygon as a list of points and some enclosed space.
 * <br>
 * All points in a polygon are connected and stored in sequence of connection.
 * <br>
 * Negative space is subtractive where sub-polygons represent the area
 * in which the polygon does not exist (one or more holes).<br>
 * Segmented space consists of sub-polygons
 * that cumulatively make up the area of the original polygon.
 * <br>
 *
 * @since 2.0.0
 */
public class Polygon {

    private final List<Point2D> polygon;
    private final List<List<Point2D>> negativeSpace;

    /**
     * Create a new polygon.
     *
     * @param polygon List of points that form the outline of the polygon.
     * @param negativeSpace Collection of list of points that outline each negative space polygon.
     *
     * @since 2.0.0
     */
    public Polygon(@NotNull List<Point2D> polygon, @NotNull List<List<Point2D>> negativeSpace) {
        this.polygon = polygon;
        this.negativeSpace = negativeSpace;
    }

    /**
     * Get a list of {@link Point2D} that make up the polygon.
     *
     * The points are all connected and stored in a specific order.
     *
     * This list can be directly modified to affect the shape of the polygon.
     *
     * @return list of {@link Point2D} that make up the polygon.
     */
    @NotNull
    public List<Point2D> getPoints() {
        return polygon;
    }

    /**
     * Check if the polygon has negative space (holes) inside it.
     *
     * @return if the polygon has negative space (holes) inside it.
     */
    public boolean hasNegativeSpace() {
        return !negativeSpace.isEmpty();
    }

    /**
     * Get the negative space inside the polygon.
     *
     * @return a list of polygons that make up the holes inside the original polygon.
     *         Each hole polygon is represented as list of {@link Point2D}.
     */
    @NotNull
    public List<List<Point2D>> getNegativeSpace() {
        return negativeSpace;
    }
}
