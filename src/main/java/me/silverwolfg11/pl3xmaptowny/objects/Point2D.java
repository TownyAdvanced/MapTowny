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


import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// Copied from https://github.com/pl3xgaming/Pl3xMap/blob/master/api/src/main/java/net/pl3x/map/api/Point.java.
// Credit for this class belongs to the respective owners and contributors of the original class.

// Represents a point in 2D space
public class Point2D {
    private final double x;
    private final double z;

    private Point2D(final double x, final double z) {
        this.x = x;
        this.z = z;
    }

    /**
     * Get the x position of this point
     *
     * @return x
     */
    public double x() {
        return this.x;
    }

    /**
     * Get the z position of this point
     *
     * @return z
     */
    public double z() {
        return this.z;
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NotNull Point2D of(final double x, final double z) {
        return new Point2D(x, z);
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NotNull Point2D point(final double x, final double z) {
        return new Point2D(x, z);
    }

    public static @NotNull Point2D fromLocation(Location loc) {
        return point(loc.getX(), loc.getZ());
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final @Nullable Point2D point = (Point2D) o;
        return Double.compare(point.x, this.x) == 0 && Double.compare(point.z, this.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }
}
