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

package me.silverwolfg11.maptowny.util;

import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class PolygonUtil {

    private enum DIRECTION {
        // Order matters for traversal
        // Direction to the left indicates the next traversed direction.
        // Direction to the right indicates default direction.
        DOWN(0, 0, -1), LEFT(1, -1, 0), UP(2, 0, 1), RIGHT(3, 1, 0);

        final int idx;
        final int xOffset, zOffset;
        DIRECTION(int idx, int xOffset, int zOffset) {
            this.idx = idx;
            this.xOffset = xOffset;
            this.zOffset = zOffset;
        }
    };

    public static class PolyFormResult {
        private final List<Point2D> polygonPoints;
        private final List<TBCluster> negativeSpaceClusters;

        private PolyFormResult(List<Point2D> polygonPoints, List<TBCluster> negativeSpaceClusters) {
            this.polygonPoints = polygonPoints;
            this.negativeSpaceClusters = negativeSpaceClusters;
        }

        /**
         * Get the list of points that outline the polygon.
         * @return list of points that outline the polygon.
         */
        @NotNull
        public List<Point2D> getPolygonPoints() {
            return polygonPoints;
        }

        /**
         * @return a list of clusters that encapsulate the negative space (holes) within the polygon.
         */
        @NotNull
        public List<TBCluster> getNegativeSpaceClusters() {
            return negativeSpaceClusters;
        }
    }

    /**
     * Get polygon information from the cluster such as:<br>
     * - Set of points that outline the polygon.<br>
     * - List of negative space clusters that represent the holes in the polygon.
     *
     * @param cluster Cluster to get polygon information from.
     * @param tbSize Size of each townblock.
     * @return Information about the polygon formation from the cluster.
     *
     * @since 3.0.0
     */
    @NotNull
    public static PolyFormResult getPolyInfoFromCluster(@NotNull TBCluster cluster, final int tbSize) {
        return getPolyInfoFromCluster(cluster, tbSize, true);
    }


    /**
     * Get polygon information from the cluster such as:<br>
     * - Set of points that outline the polygon.<br>
     * - List of negative space clusters that represent the holes in the polygon.<br>
     *
     * @param cluster Cluster to get polygon information from.
     * @param tbSize Size of each townblock.
     * @param findHoles Whether to find holes within the cluster.
     * @return Information about the polygon formation from the cluster.
     *
     * @since 3.0.0
     */
    // Convert a TB cluster to a polygon with border points and appropriate negative space points
    // Idea is adapted from https://github.com/Mark-225/NeincraftPlugin/blob/main/src/main/java/de/neincraft/neincraftplugin/modules/plots/util/PlotUtils.java
    @NotNull
    public static PolyFormResult getPolyInfoFromCluster(@NotNull TBCluster cluster, final int tbSize, boolean findHoles) {
        Objects.requireNonNull(cluster);

        if (cluster.isEmpty()) {
            return new PolyFormResult(new ArrayList<>(), new ArrayList<>());
        }

        StaticTB rightMostBlock = findRightMost(cluster);
        // Idea: Any hole within a cluster will have boundaries of its own (i.e. a north boundary).
        // Hence, if we filter out all boundaries of the outermost polygon, we will only be left with the boundaries
        // for the holes.
        EnumMap<DIRECTION, Set<Long>> borderMap = new EnumMap<>(DIRECTION.class);

        DIRECTION[] dirValues = DIRECTION.values(); // Each values call allocates memory

        // Initialize border map
        for (DIRECTION dir : dirValues) {
            borderMap.put(dir, new HashSet<>());
        }

        // Only clusters with at least 8 blocks can have holes.
        if (findHoles && cluster.size() >= 8) {
            for (StaticTB tb : cluster.getBlocks()) {
                // Find the top/bottom borders
                for (DIRECTION dir : dirValues) {
                    if (!cluster.has(tb.offsetLong(dir.xOffset, dir.zOffset))) {
                        borderMap.get(dir).add(tb.toLong());
                    }
                }
            }
        }

        // Path algorithm inserts unnecessary duplicate points
        Set<Point2D> poly = new LinkedHashSet<>();

        // Keep track of origin block
        long startHash = rightMostBlock.toLong();

        Queue<Long> townBlocksToVisit = new ArrayDeque<>(1);
        townBlocksToVisit.add(rightMostBlock.toLong());

        DIRECTION currDir = DIRECTION.RIGHT;

        boolean isOriginPoint = true;
        while(!townBlocksToVisit.isEmpty()) {
            long tbHash = townBlocksToVisit.poll();
            StaticTB townBlock = cluster.at(tbHash);

            // Check if back at origin point (always approaches from the right)
            if (!isOriginPoint && (currDir == DIRECTION.RIGHT) && (tbHash == startHash))
                break;
            else if (isOriginPoint) {
                isOriginPoint = false;
            }

            // Remove relevant borders
            // For example, if the direction is right, then target north (up) borders
            DIRECTION borderDir = dirValues[(currDir.idx + 3) % 4]; // alternative for (idx - 1) % 4
            borderMap.get(borderDir).remove(tbHash);

            DIRECTION nextDir = null;
            // Check left adjacent and current direction for existing blocks
            for (int i = -1; i < 1; ++i) {
                int newDirIdx = currDir.idx + i;
                // Wrap index around
                newDirIdx = newDirIdx < 0 ? 3 : newDirIdx;
                DIRECTION checkDir = dirValues[newDirIdx];
                long offsetHash = townBlock.offsetLong(checkDir.xOffset, checkDir.zOffset);
                if (cluster.has(offsetHash)) {
                    townBlocksToVisit.add(offsetHash);
                    nextDir = checkDir;
                    break;
                }
            }

            // If none of the other directions are valid, requeue the townblock
            // but try the adjacent direction.
            if (nextDir == null) {
                nextDir = dirValues[(currDir.idx + 1) % 4];
                townBlocksToVisit.add(tbHash);
            }

            if (nextDir != currDir) {
                boolean lower = (currDir != DIRECTION.RIGHT) && (nextDir != DIRECTION.RIGHT);
                boolean left = (currDir == DIRECTION.UP) || (nextDir == DIRECTION.UP);
                poly.add(townBlock.getCorner(tbSize, lower, left));
            }

            currDir = nextDir;
        }

        townBlocksToVisit = null;

        List<TBCluster> negativeSpaceClusters = new ArrayList<>();
        if (!borderMap.isEmpty()) {
            negativeSpaceClusters = findNegSpaceFromBorders(cluster, borderMap);
        }

        return new PolyFormResult(new ArrayList<>(poly), negativeSpaceClusters);
    }

    private static List<TBCluster> findNegSpaceFromBorders(TBCluster cluster, Map<DIRECTION, Set<Long>> borderMap) {
        // Compute upper bound for negative space calculations
        StaticTB.Edges edges = cluster.getBlocks().stream().collect(StaticTB.Edges.collect());
        int maxChunks = (Math.abs(edges.getMinX() - edges.getMaxX()) + 1) * (Math.abs(edges.getMinZ() - edges.getMaxZ()) + 1);

        List<TBCluster> negSpaceClusters = new ArrayList<>();

        // Compile a list of negative space blocks to start BFS on
        Set<StaticTB> negSpaceStarters = new HashSet<>();
        for (Map.Entry<DIRECTION, Set<Long>> entryMap : borderMap.entrySet()) {
            DIRECTION borderDir = entryMap.getKey();
            for (Long tbHash : entryMap.getValue()) {
                negSpaceStarters.add(StaticTB.fromHashed(tbHash).add(borderDir.xOffset, borderDir.zOffset));
            }
        }

        // Go through each negative space block

        while (!negSpaceStarters.isEmpty()) {
            StaticTB startingBlock = negSpaceStarters.stream().findAny().get();
            negSpaceStarters.remove(startingBlock);

            // Verify that starting block not outside the edges
            if (startingBlock.x() < edges.getMinX() || startingBlock.x() > edges.getMaxX() ||
                    startingBlock.z() < edges.getMinZ() || startingBlock.z() > edges.getMaxZ()) {
                continue;
            }

            TBCluster negSpaceCluster = TBCluster.findNegativeSpaceCluster(cluster, startingBlock, maxChunks);
            // Remove negative space  from border. Negative space would be a block below the border
            negSpaceCluster.getBlocks().forEach(negSpaceStarters::remove);
            negSpaceClusters.add(negSpaceCluster);
        }

        return negSpaceClusters;
    }

    /**
     * Segment a polygon into several polygons.
     * The segmentation algorithm requires the cluster that makes up the polygon, and the cluster
     * that represents the negative space of the polygon.<br>
     * See {@link PolygonUtil#getPolyInfoFromCluster(TBCluster, int)} on how to generate that information.
     * <br><br>
     * The polygon is segmented at locations adjacent to the negative space. The algorithm does not guarantee the optimal number
     * of partitions, but does guarantee that the resulting partitioned polygons contain no holes (no negative space).
     *
     * @param polyCluster Cluster of townblocks that make up the polygon.
     * @param negSpaceClusters Cluster<b>s</b> of townblocks that make up the negative space (holes).
     * @param tbSize Size of the townblock.
     * @return a list of polygons (represented as a list of points that outline each polygon) that when aggregated make up the area of
     *         the original provided polygon.
     */
    // Idea is adapted from https://github.com/Mark-225/NeincraftPlugin/blob/main/src/main/java/de/neincraft/neincraftplugin/modules/plots/util/PlotUtils.java
    public static List<List<Point2D>> segmentPolygon(@NotNull TBCluster polyCluster, @NotNull List<TBCluster> negSpaceClusters, int tbSize) {
        Objects.requireNonNull(polyCluster);
        Objects.requireNonNull(negSpaceClusters);

        List<List<Point2D>> polyList = new ArrayList<>();

        if (negSpaceClusters.isEmpty()) {
            polyList.add(getPolyInfoFromCluster(polyCluster, tbSize, false).getPolygonPoints());
            return polyList;
        }

        // Choose a random negative space and the min-z axis. Separate the poly cluster based on the y-line.
        StaticTB.Edges clusterEdges =  negSpaceClusters.get(0).getBlocks().stream().collect(StaticTB.Edges.collect());
        final List<StaticTB> topBlocks = new ArrayList<>();
        final List<StaticTB> bottomBlocks = new ArrayList<>();

        for (StaticTB tb : polyCluster.getBlocks()) {
            if (tb.z() < clusterEdges.getMinZ())
                topBlocks.add(tb);
            else
                bottomBlocks.add(tb);
        }

        List<TBCluster> separatedClusters = TBCluster.findClusters(topBlocks);
        separatedClusters.addAll(TBCluster.findClusters(bottomBlocks));

        for (TBCluster cluster : separatedClusters) {
            PolyFormResult result = getPolyInfoFromCluster(cluster, tbSize);
            // Recursively segment
            if (!result.negativeSpaceClusters.isEmpty()) {
                polyList.addAll(segmentPolygon(cluster, result.negativeSpaceClusters, tbSize));
            }
            else {
                polyList.add(result.getPolygonPoints());
            }
        }

        return polyList;
    }

    // Forms a polygon given a townblock cluster
    // Returns a list of points that outline the polygon.

    /**
     * Forms a polygon from a cluster of townblocks.
     * @param cluster Cluster of connected townblocks.
     * @param tbSize Number of blocks in the 2d area of a single townblock.
     * @return a list of points that outline the polygon.
     *
     * @deprecated Use {@link #getPolyInfoFromCluster(TBCluster, int)} instead.
     */
    @Nullable
    @Deprecated
    public static List<Point2D> formPolyFromCluster(TBCluster cluster, final int tbSize) {
        return getPolyInfoFromCluster(cluster, tbSize, false).getPolygonPoints();
    }

    // Find the upper right-most town block (prioritize right-most over upper)
    private static StaticTB findRightMost(TBCluster cluster) {
        StaticTB rightMostBlock = cluster.findAny();

        for (StaticTB tb : cluster.getBlocks()) {
            if (tb.x() > rightMostBlock.x()
                    || (tb.x() == rightMostBlock.x() && tb.z() > rightMostBlock.z())) {
                rightMostBlock = tb;
            }
        }

        return rightMostBlock;
    }
    
}
