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

    /**
     * Complex algorithm to detect negative spaces in a cluster.
     *
     * @param cluster static townblock cluster.
     * @return a list of static townblocks that are negative space.
     */
    @NotNull
    public static List<StaticTB> findNegativeSpace(TBCluster cluster) {
        // There can be no negative space if a cluster has less than 8 townblocks
        if (cluster.size() < 8)
            return Collections.emptyList();
        Set<Long> freeSpaces = new HashSet<>();

        Deque<Long> potentialNSpace = sortClusterIntoSpaces(cluster, freeSpaces);

        List<StaticTB> negativeSpace = propagateFreeSpaces(cluster, freeSpaces, potentialNSpace);

        // Clear up free spaces
        freeSpaces.clear();

        return negativeSpace;
    }

    // Sort the townblock cluster into free space and potential negative space.
    // It is potential negative space because some negative spaces may be connected to free spaces.
    // Mutates both freeSpace parameters.
    private static Deque<Long> sortClusterIntoSpaces(TBCluster cluster,
                                                     Collection<Long> freeSpace) {
        Deque<Long> potentialNSpace = new ArrayDeque<>();

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
                            // Check if the potential space matches one of the following conditions to be a free space:
                            // - On the Z-border
                            // - Above an unclaimed wild-space
                            // - Diagonally touching an unclaimed space on the Z-border
                            // - Surrounded by a free space.
                            if (posZ == maxZ || posZ == minZ
                                    || negSpaceX < firstLastEncounteredTBX || negSpaceX > lastLastEncounteredTBX
                                    || touchingDiagonalBorders(negSpaceX, posZ, maxZ, minZ, cluster)
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

        return potentialNSpace;
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

        // Potential Negative Spaces are ordered from min X min Z to max X max Z.

        Set<Long> negSpaceSet = new HashSet<>(potentialNSpace.size());
        Deque<StaticTB> newFreeSpaces = new ArrayDeque<>();

        // First pass, check if any potential negative space is adjacent to a free space.
        // If it is, convert it to a free space, and add it to the list to be propagated
        // in the second pass.
        while (!potentialNSpace.isEmpty()) {
            // Pop the negative space with min X min Z (going from -z -> z, -x -> x)
            // Poll Last removes from the tail of the queue (pop() actually removes from the head)
            long hash = potentialNSpace.pollLast();
            int x = StaticTB.rawX(hash);
            int z = StaticTB.rawZ(hash);
            // Check if the potential negative space
            // is adjacent to a free space
            // or is above an empty space (hits the min Z border)
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

                // Do a surrounding check on the specificed TB
                for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                    for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                        if (xOffset == 0 && zOffset == 0)
                            continue;

                        addAdjacentNeighbor.accept(x + xOffset, z + zOffset);
                    }
                }
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

    // Checks if the specific TB is surrounded by any free space
    // +++
    // +x+
    // +++
    private static boolean isNextToHashedCoord(Collection<Long> hashCoords, int posX, int posZ) {
        if (hashCoords.isEmpty())
            return false;

        for (int xOffset = -1; xOffset <= 1; ++xOffset) {
            for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                if (xOffset == 0 && zOffset == 0)
                    continue;

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

    // Check if a TB is diagonal to an unclaimed TB on the Z borders.
    // Any TBs on the Z-borders are guaranteed to not be negative space
    // Thus if any TB passes this check, it should be considered a free-space / empty-space.
    private static boolean touchingDiagonalBorders(int posX, int posZ, int maxZ, int minZ, TBCluster cluster) {
        int newZ;

        // Check if Z is one below the upper Z-border
        if ((posZ + 1) == maxZ) {
            newZ = posZ + 1;
        }
        // Check if Z is one above the bottom Z-border
        else if ((posZ - 1) == minZ) {
            newZ = posZ - 1;
        }
        // If neither are true, return false.
        else {
            return false;
        }

        long leftDiag = StaticTB.hashPos(posX - 1, newZ);
        long rightDiag = StaticTB.hashPos(posX + 1, newZ);

        return !cluster.has(leftDiag) || !cluster.has(rightDiag);
    }


}
