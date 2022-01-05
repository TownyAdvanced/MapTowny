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

package me.silverwolfg11.pl3xmaptowny.objects;

import org.jetbrains.annotations.NotNull;

import java.util.List;

// A simple wrapper class that stores a polygon as a list of points and negative space.
public class Polygon {

    private final List<Point2D> polygon;
    private final List<List<Point2D>> negativeSpace;

    public Polygon(@NotNull List<Point2D> polygon, @NotNull List<List<Point2D>> negativeSpace) {
        this.polygon = polygon;
        this.negativeSpace = negativeSpace;
    }

    public List<Point2D> getPoints() {
        return polygon;
    }

    public boolean hasNegativeSpace() {
        return !negativeSpace.isEmpty();
    }

    public List<List<Point2D>> getNegativeSpace() {
        return negativeSpace;
    }
}
