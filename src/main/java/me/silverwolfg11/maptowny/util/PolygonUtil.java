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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class PolygonUtil {

    private enum DIRECTION { RIGHT, DOWN, UP, LEFT };

    // Forms a polygon given a townblock cluster
    // Returns
    @Nullable
    public static List<Point2D> formPolyFromCluster(TBCluster cluster, final int tbSize) {

        StaticTB rightMostBlock = findRightMost(cluster);

        // Path algorithm inserts unnecessary duplicate points
        Set<Point2D> poly = new LinkedHashSet<>();

        // Origin point is the upper left of the townblock
        Point2D originPoint = rightMostBlock.getUL(tbSize);

        Queue<Long> townBlocksToVisit = new ArrayDeque<>(1);
        townBlocksToVisit.add(rightMostBlock.toLong());

        DIRECTION currDir = DIRECTION.RIGHT;

        boolean isOriginPoint = true;
        while(!townBlocksToVisit.isEmpty()) {

            long tbHash = townBlocksToVisit.poll();
            StaticTB townBlock = cluster.at(tbHash);

            switch (currDir) {
                case RIGHT: {
                    Point2D upperLeft = townBlock.getUL(tbSize);
                    // We approach the origin from the right so verify we are not back at the origin
                    if (!isOriginPoint && upperLeft.equals(originPoint))
                        continue;
                    else if (isOriginPoint)
                        isOriginPoint = false;

                    long offsetHash;
                    // Check if there's a townblock above us
                    // Theoretically can only happen if we are going towards the origin, not away
                    // We have hit a corner!
                    if (cluster.has(offsetHash = townBlock.offsetLong(0, 1))) {
                        townBlocksToVisit.add(offsetHash);
                        currDir = DIRECTION.UP;
                        // Mark UL
                        poly.add(upperLeft);
                    }
                    // Check if there's a townblock to the right of us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(1, 0))) {
                        // Keep Going Right
                        // Don't mark anything
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the rightmost, so switch direction to down and queue the same block
                        currDir = DIRECTION.DOWN;
                        townBlocksToVisit.add(tbHash);
                        // Mark UR
                        poly.add(townBlock.getUR(tbSize));
                    }
                    break;
                }
                case LEFT: {
                    long offsetHash;
                    // Check if there's a townblock below us (This happens at corners)
                    if (cluster.has(offsetHash = townBlock.offsetLong(0, -1))) {
                        // Queue the block below
                        townBlocksToVisit.add(offsetHash);
                        // Set direction as down
                        currDir = DIRECTION.DOWN;
                        // Mark LR
                        poly.add(townBlock.getLR(tbSize));
                    }
                    // Check if there's a townblock to the left of us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(-1, 0))) {
                        // Keep Going Left
                        // Don't mark anything
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the leftmost, so switch direction to up
                        currDir = DIRECTION.UP;
                        townBlocksToVisit.add(tbHash);
                        // Mark LL
                        poly.add(townBlock.getLL(tbSize));
                    }

                    break;
                }
                case DOWN: {
                    long offsetHash;
                    // Check if there's a townblock to the right us (We have hit a corner)
                    if (cluster.has(offsetHash = townBlock.offsetLong(1, 0))) {
                        // Queue the block right
                        townBlocksToVisit.add(offsetHash);
                        // Set direction as right
                        currDir = DIRECTION.RIGHT;
                        // Mark UR
                        poly.add(townBlock.getUR(tbSize));
                    }
                    // Check if there's a townblock below us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(0, -1))) {
                        // Keep Going Down
                        // Don't mark anything
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the bottom most, so make a left turn
                        currDir = DIRECTION.LEFT;
                        townBlocksToVisit.add(tbHash);
                        // Mark LR
                        poly.add(townBlock.getLR(tbSize));
                    }

                    break;
                }
                case UP: {
                    long offsetHash;
                    // Check if there's a townblock to the left of us (We have hit a corner)
                    if (cluster.has(offsetHash = townBlock.offsetLong(-1, 0))) {
                        townBlocksToVisit.add(offsetHash);
                        currDir = DIRECTION.LEFT;
                        // Mark LL
                        poly.add(townBlock.getLL(tbSize));
                    }
                    // Check if there's a townblock above us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(0, 1))) {
                        // Keep Going up
                        // Don't mark anything
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the top most, so make a right turn
                        currDir = DIRECTION.RIGHT;
                        townBlocksToVisit.add(tbHash);
                        // Mark UL
                        poly.add(townBlock.getUL(tbSize));
                    }

                    break;
                }
            }
        }

        List<Point2D> polyList = new ArrayList<>(poly);

        // Help GC
        poly.clear();
        townBlocksToVisit.clear();

        return polyList;
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
