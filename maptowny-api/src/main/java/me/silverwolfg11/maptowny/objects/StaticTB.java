/*
 * Copyright (c) 2021 Silverwolfg11
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;

/**
 * Immutable wrapper class for a townblock.
 *
 * @since 2.0.0
 */
public class StaticTB {

    private final int x, z;
    private final Map<String, Object> properties;

    private StaticTB(int x, int z) {
        this(x, z, null);
    }

    private StaticTB(int x, int z, Map<String, Object> properties) {
        this.x = x;
        this.z = z;
        this.properties = properties;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }


    private int getLLX(int tbSize) {
        return x() * tbSize;
    }

    private int getLLZ(int tbSize) {
        return z() * tbSize;
    }

    // Get lower left
    @NotNull
    public Point2D getLL(int tbSize) {
        return Point2D.of(getLLX(tbSize), getLLZ(tbSize));
    }

    // Get lower right
    @NotNull
    public Point2D getLR(int tbSize) {
        return Point2D.of(getLLX(tbSize) + (tbSize - 1), getLLZ(tbSize));
    }

    // Get upper left
    @NotNull
    public Point2D getUL(int tbSize) {
        return Point2D.of(getLLX(tbSize), getLLZ(tbSize) + (tbSize - 1));
    }

    // Get upper right
    @NotNull
    public Point2D getUR(int tbSize) {
        final int offset = tbSize - 1;
        return Point2D.of(getLLX(tbSize) + offset, getLLZ(tbSize) + offset);
    }

    public long toLong() {
        return getPairToLong(x, z);
    }

    public long offsetLong(int xOffset, int zOffset) {
        return getPairToLong(x + xOffset, z + zOffset);
    }

    // Properties Methods

    /**
     * Get the current map of properties for this static TB.
     * If there are no properties set, an empty map is returned.
     *
     * @return an immutable map of properties for this TB.
     *
     * @since 3.0.0
     */
    @UnmodifiableView
    @NotNull
    public Map<@NotNull String, @NotNull Object> getProperties() {
        if (properties == null)
            return Collections.emptyMap();

        return Collections.unmodifiableMap(properties);
    }

    /**
     * Check if the static TB has some property.
     *
     * @param propertyKey Unique key for a static TB property.
     * @return true if the static TB has a property with the key.
     *
     * @since 3.0.0
     */
    public boolean hasProperty(@NotNull String propertyKey) {
        return properties != null && properties.containsKey(propertyKey);
    }

    /**
     * Get the property for the static TB matching the property key.
     *
     * @param propertyKey Unique key for the property.
     * @return the property value or {@code null} if the property doesn't exist.
     *
     * @since 3.0.0
     */
    @Nullable
    public Object getProperty(@NotNull String propertyKey) {
        if (properties == null)
            return null;

        return properties.get(propertyKey);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticTB staticTB = (StaticTB) o;
        return x == staticTB.x && z == staticTB.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    public static StaticTB from(int x, int z) {
        return new StaticTB(x, z);
    }

    /**
     * Create a static TB
     * @param x X-Coord
     * @param z Z-Coord
     * @param properties Properties that the static TB has.
     *
     * @return the newly created static TB.
     *
     * @since 3.0.0
     */
    public static StaticTB from(int x, int z,
                                @Nullable Map<@NotNull String, @NotNull Object> properties) {
        return new StaticTB(x, z, properties);
    }

    public static StaticTB fromHashed(long hashed) {
        return new StaticTB(rawX(hashed), rawZ(hashed));
    }

    public static long toLong(StaticTB tb) {
        return tb.toLong();
    }

    public static long hashOffset(StaticTB tb, int xOffset, int zOffset) {
        return tb.offsetLong(xOffset, zOffset);
    }

    // Math Helper Methods

    public static int rawX(long hash) {
        return (int) hash;
    }

    public static int rawZ(long hash) {
        return (int) (hash >> 32);
    }

    public static long hashPos(int x, int z) {
        return getPairToLong(x, z);
    }

    private static long getPairToLong(int x, int z) {
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }

    // Collector Class
    // Used to get the edge corners of
    public static class Edges {
        private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        private int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        private Edges() {}

        private void accept(StaticTB tb) {
            if (tb.x() < minX)
                minX = tb.x();

            if (tb.x() > maxX)
                maxX = tb.x();

            if (tb.z() < minZ)
                minZ = tb.z();

            if (tb.z() > maxZ)
                maxZ = tb.z();
        }

        private Edges combine(Edges that) {
            if (that.minX < this.minX)
                this.minX = that.minX;

            if (that.maxX > this.maxX)
                this.maxX = that.maxX;

            if (that.minZ < this.minZ)
                this.minZ = that.minZ;

            if (that.maxZ > this.maxZ)
                this.maxZ = that.maxZ;

            return this;
        }

        public int getMinX() {
            return minX;
        }

        public int getMaxX() {
            return maxX;
        }

        public int getMinZ() {
            return minZ;
        }

        public int getMaxZ() {
            return maxZ;
        }

        public static Collector<StaticTB, Edges, Edges> collect() {
            return Collector.of(
                    Edges::new,
                    Edges::accept,
                    Edges::combine,
                    Collector.Characteristics.IDENTITY_FINISH, Collector.Characteristics.UNORDERED
            );
        }
    }

}
