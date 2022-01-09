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

package me.silverwolfg11.maptowny;

import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TestHelpers {

    // Variable tile size
    static final int TILE_SIZE = 16;

    // Helper methods
    @SafeVarargs
    static final <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    static StaticTB tb(int x, int z) {
        return StaticTB.from(x, z);
    }

    static TBCluster clusterOf(StaticTB... tbs) {
        return clusterOf(Arrays.asList(tbs));
    }

    static TBCluster clusterOf(Collection<StaticTB> tbs) {
        return TBCluster.findClusters(tbs).get(0);
    }

    static List<TBCluster> clustersOf(StaticTB... tbs) {
        return clustersOf(Arrays.asList(tbs));
    }

    static List<TBCluster> clustersOf(Collection<StaticTB> tbs) {
        return TBCluster.findClusters(tbs);
    }

    static Point2D point(double x, double z) {
        return Point2D.of(x, z);
    }

    static List<Point2D> pointsOf(double...pts) {
        // Validate points list
        if (pts == null || (pts.length % 2 != 0))
            return null;

        List<Point2D> points = new ArrayList<>(pts.length / 2);

        for (int i = 0; i < pts.length; i += 2) {
            points.add(Point2D.of(pts[i], pts[i + 1]));
        }

        return points;
    }

    enum CORNER { LOWER_LEFT, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT }
    static Point2D cornerPoint(StaticTB tb, CORNER corner) {
        int xOffset = 0;
        if (corner == CORNER.LOWER_RIGHT || corner == CORNER.UPPER_RIGHT)
            xOffset = (TILE_SIZE - 1);

        int zOffset = 0;
        if (corner == CORNER.UPPER_LEFT || corner == CORNER.UPPER_RIGHT)
            zOffset = (TILE_SIZE - 1);

        return Point2D.of((tb.x() * TILE_SIZE) + xOffset, (tb.z() * TILE_SIZE) + zOffset);
    }

}
