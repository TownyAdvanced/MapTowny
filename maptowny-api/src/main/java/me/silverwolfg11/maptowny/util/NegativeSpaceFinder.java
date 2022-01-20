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

import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class NegativeSpaceFinder {

    @NotNull
    public static List<StaticTB> findNegativeSpace(TBCluster cluster) {
        // There can be no negative space if a cluster has less than 8 townblocks
        if (cluster.size() < 8)
            return Collections.emptyList();

        Deque<Long> potentialNSpace = new ArrayDeque<>();
        Set<Long> freeSpaces = new HashSet<>();

        sortClusterIntoSpaces(cluster, freeSpaces, potentialNSpace);

        List<StaticTB> negativeSpace = propagateFreeSpaces(cluster, freeSpaces, potentialNSpace);

        // Clear up free spaces
        freeSpaces.clear();

        return negativeSpace;
    }

    // Sort the townblock cluster into free space and potential negative space.
    // It is potential negative space because some negative spaces may be connected to free spaces.
    // Mutates both freeSpace and potentialNSpace parameters.
    private static void sortClusterIntoSpaces(TBCluster cluster,
                                               Collection<Long> freeSpace, Deque<Long> potentialNSpace) {
        // Find the edge corners of the cluster (corners don't need to be in cluster)
        StaticTB.Edges edges = cluster.getBlocks().stream().collect(StaticTB.Edges.collect());
        int minX = edges.getMinX(), maxX = edges.getMaxX();
        int minZ = edges.getMinZ(), maxZ = edges.getMaxZ();

        int firstLastEncounteredTBX = minX; // Represents the X pos of the first encountered TB of the LAST ROW
        int lastLastEncounteredTBX = maxX; // Represents the X pos of the last encountered TB of the LAST ROW

        // Start from upper left corner and go row by row
        // This will mark our free spaces and possible negative spaces
        for (int posZ = maxZ; posZ >= minZ; posZ--) {
            boolean encounteredTB = false;
            int encounteredTBX = minX;

            // X-position of the start of potential negative space for a certain row
            int startNegativeSpace = minX;
            // X-position of the end of potential negative space for a certain row
            int endNegativeSpace = minX;

            for (int posX = minX; posX <= maxX; posX++) {
                long hash = StaticTB.hashPos(posX, posZ);
                boolean tbInCluster = cluster.has(hash);

                // These are just wilderness townblocks on the outline of the polygon
                if (!encounteredTB && !tbInCluster)
                    continue;

                if (tbInCluster) {
                    // Mark the townblock as the first in the row
                    if (!encounteredTB) {
                        encounteredTB = true;
                        encounteredTBX = posX;
                    }

                    if (startNegativeSpace != endNegativeSpace) {
                        for (int negSpaceX = startNegativeSpace + 1; negSpaceX <= endNegativeSpace; negSpaceX++) {
                            long currHash = StaticTB.hashPos(negSpaceX, posZ);
                            // Check if it is near the edge
                            if (posZ == maxZ || posZ == minZ
                                    || negSpaceX < firstLastEncounteredTBX || negSpaceX > lastLastEncounteredTBX
                                    || isNextToHashedCoord(freeSpace, negSpaceX, posZ))
                                freeSpace.add(currHash);
                            else {
                                // Mark it as a possible negative space
                                potentialNSpace.add(currHash);
                            }
                        }
                    }

                    startNegativeSpace = posX;
                    endNegativeSpace = posX;
                }
                else {
                    // The townblock is not in the cluster, so it may be potential negative space.
                    endNegativeSpace++;
                }
            }
            firstLastEncounteredTBX = encounteredTBX;
            lastLastEncounteredTBX = Math.max(firstLastEncounteredTBX, startNegativeSpace);
        }
    }

    /**
     * Applies propagation from free spaces to potential negative spaces.
     *
     * @param cluster         Townblock cluster.
     * @param freeSpace       Collection of hashed free spaces. Parameter is mutated.
     * @param potentialNSpace Deque of hashed potential negative space. Parameter is mutated.
     * @return list of actual negative space.
     */
    private static List<StaticTB> propagateFreeSpaces(TBCluster cluster,
                                                      Collection<Long> freeSpace, Deque<Long> potentialNSpace) {
        if (potentialNSpace.isEmpty())
            return Collections.emptyList();

        Set<Long> negSpaceSet = new HashSet<>(potentialNSpace.size());
        Deque<StaticTB> newFreeSpaces = new ArrayDeque<>();

        // First pass, check if any potential negative space is
        // adjacent to a free space.
        // If it is, convert it to a free space, and add it to the list to be propagated
        // in the second pass.
        while (!potentialNSpace.isEmpty()) {
            long hash = potentialNSpace.pop();
            int x = StaticTB.rawX(hash);
            int z = StaticTB.rawZ(hash);
            // Check if the potential negative space
            // is adjacent to a free space
            // or is above an empty space (the bottom border of the cluster).
            if (isNextToHashedCoord(freeSpace, x, z)
                    || isEmptyBelow(negSpaceSet, cluster, x, z)) {
                freeSpace.add(hash);
                newFreeSpaces.push(StaticTB.from(x, z));
            } else {
                negSpaceSet.add(hash);
            }
        }

        // Perform second pass only if there are new free spaces
        if (!newFreeSpaces.isEmpty()) {
            // Checks if a location is a negative space and convert it to a free space.
            BiConsumer<Integer, Integer> addAdjacentNeighbor = (x, z) -> {
                long upTB = StaticTB.hashPos(x, z);
                if (negSpaceSet.contains(upTB)) {
                    negSpaceSet.remove(upTB);
                    newFreeSpaces.push(StaticTB.from(x, z));
                }
            };

            // Second-pass
            // Propagate all the converted new free spaces to adjacent potential negative spaces.
            while (!newFreeSpaces.isEmpty()) {
                StaticTB freeTB = newFreeSpaces.pop();
                int x = freeTB.x();
                int z = freeTB.z();

                // Do a neighbor check in 4 directions
                addAdjacentNeighbor.accept(x, z + 1);
                addAdjacentNeighbor.accept(x, z - 1);
                addAdjacentNeighbor.accept(x - 1, z);
                addAdjacentNeighbor.accept(x + 1, z);
            }
        }

        List<StaticTB> negativeSpaceList = hashedCoordsToStaticTB(negSpaceSet);
        negSpaceSet.clear();

        return negativeSpaceList;
    }

    private static List<StaticTB> hashedCoordsToStaticTB(Collection<Long> hashedCoords) {
        if (!hashedCoords.isEmpty()) {
            ArrayList<StaticTB> tbList = new ArrayList<>(hashedCoords.size());
            for (Long hashedCoord : hashedCoords) {
                tbList.add(StaticTB.fromHashed(hashedCoord));
            }

            return tbList;
        }

        return Collections.emptyList();
    }

    private static final int[] DIRECTIONS = { -1, 1 };

    private static boolean isNextToHashedCoord(Collection<Long> hashCoords, int posX, int posZ) {
        if (hashCoords.isEmpty())
            return false;

        for (int i = 0; i < 2; ++i) {
            for (int dir : DIRECTIONS) {
                int xOffset = i == 0 ? dir : 0;
                int zOffset = i == 1 ? dir : 0;

                long offSetHash = StaticTB.hashPos(posX + xOffset, posZ + zOffset);
                if (hashCoords.contains(offSetHash))
                    return true;
            }
        }

        return false;
    }

    // Check if the position below the specified position is an empty space.
    // It is an empty space if it:
    // - Is beyond the bottom border of the townblock cluster
    // - Is not a potential negative space.
    private static boolean isEmptyBelow(Collection<Long> negSpace, TBCluster cluster, int posX, int posZ) {
        long belowHash = StaticTB.hashPos(posX, posZ - 1);
        return !negSpace.contains(belowHash) && !cluster.has(belowHash);
    }


}
