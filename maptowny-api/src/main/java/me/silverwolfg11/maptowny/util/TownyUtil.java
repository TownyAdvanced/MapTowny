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

package me.silverwolfg11.maptowny.util;

import com.palmergames.bukkit.towny.object.Coord;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility helper class to assist with Towny specific uses of the plugin.
 *
 * @since 2.0.0
 */
public class TownyUtil {

    /**
     * Convert a collection of {@link Coord} objects to a list of {@link Polygon}.
     * The collection of Coords can be in any order.
     *
     * All Coord objects should be of the same world.
     *
     * @param coords Collection of coord objects.
     * @param findNegativeSpace Should the polygons have accurate negative space?
     *                          This will be more computationally expensive.
     * @param coordScale What is the scale between a coord and a block?
     *                   In other words, how many blocks make up one coord?
     *                   E.g. if the coords are townblocks, and the townblocks are each a chunk, the scale is 16.
     *
     * @return a list of {@link Polygon}s.
     */
    @NotNull
    public List<Polygon> coordsToPolys(@NotNull Collection<Coord> coords, boolean findNegativeSpace, int coordScale) {
        Objects.requireNonNull(coords);

        // Convert coords to StaticTBs
        Collection<StaticTB> staticTBs = coords.stream()
                                                .map(c -> StaticTB.from(c.getX(), c.getZ()))
                                                .collect(Collectors.toList());

        // Find clusters (connected groups of townblocks)
        List<TBCluster> clusters = TBCluster.findClusters(staticTBs);

        List<Polygon> polygons = new ArrayList<>(clusters.size());

        // Convert each cluster to a polygon
        for (TBCluster cluster : clusters) {
            List<Point2D> polygon = PolygonUtil.formPolyFromCluster(cluster, coordScale);

            if (polygon == null)
                continue;

            List<List<Point2D>> negativeSpace = Collections.emptyList();

            if (findNegativeSpace) {
                List<StaticTB> negSpaceTbs = NegativeSpaceFinder.findNegativeSpace(cluster);
                List<TBCluster> negSpaceClusters = TBCluster.findClusters(negSpaceTbs);

                negativeSpace = negSpaceClusters.stream()
                                                .map(nsc -> PolygonUtil.formPolyFromCluster(nsc, coordScale))
                                                .collect(Collectors.toList());

            }

            polygons.add(new Polygon(polygon, negativeSpace));
        }

        return polygons;
    }

}