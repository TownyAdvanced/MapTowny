package me.silverwolfg11.pl3xmaptowny.util;

import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NegativeSpaceFinder {

    @NotNull
    public static List<StaticTB> findNegativeSpace(TBCluster cluster) {
        // There can be no negative space if a cluster has less than 8 townblocks
        if (cluster.size() < 8)
            return Collections.emptyList();

        // Find the edge corners of the cluster (corners don't need to be in cluster)
        StaticTB.Edges edges = cluster.getBlocks().stream().collect(StaticTB.Edges.collect());
        int minX = edges.getMinX(), maxX = edges.getMaxX();
        int minZ = edges.getMinZ(), maxZ = edges.getMaxZ();

        // Use a stack for negative space, so we can start from the bottom row for back propagation
        Deque<Long> negativeSpace = new ArrayDeque<>(); // Possible Negative space

        Set<Long> freeSpace = new HashSet<>(); // Free space that is used to check
        int firstLastEncounteredTBX = minX; // Represents the X pos of the first encountered TB of the LAST ROW
        int lastLastEncounteredTBX = maxX; // Represents the X pos of the last encountered TB of the LAST ROW

        // Start from upper left corner and go row by row
        // This will mark our free spaces and possible negative spaces
        for (int posZ = maxZ; posZ >= minZ; posZ--) {
            boolean encounteredTB = false;
            int encounteredTBX = minX;

            int startNegativeSpace = minX;
            int endNegativeSpace = minX;

            for (int posX = minX; posX <= maxX; posX++) {
                long hash = StaticTB.hashPos(posX, posZ);
                boolean isInCluster = cluster.has(hash);

                // These are just townblocks on the outline of the polygon
                if (!encounteredTB && !isInCluster)
                    continue;

                if (isInCluster) {
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
                                    || isNextToFreeSpace(freeSpace, negSpaceX, posZ))
                                freeSpace.add(currHash);
                            else {
                                // Mark it as a possible negative space
                                negativeSpace.push(currHash);
                            }
                        }
                    }

                    startNegativeSpace = posX;
                    endNegativeSpace = posX;
                }
                else {
                    endNegativeSpace++;
                }
            }
            firstLastEncounteredTBX = encounteredTBX;
            lastLastEncounteredTBX = Math.max(firstLastEncounteredTBX, startNegativeSpace);
        }

        List<StaticTB> allNegativeSpaces = null;
        Set<Long> negSpaceSet = new HashSet<>();

        // Backpropogate free spaces to negative space
        if (!negativeSpace.isEmpty()) {
            allNegativeSpaces = new ArrayList<>();
            while (!negativeSpace.isEmpty()) {
                long hash = negativeSpace.pop();
                int x = StaticTB.rawX(hash);
                int z = StaticTB.rawZ(hash);
                if (isNextToFreeSpace(freeSpace, x, z)
                    || isEmptyBelow(negSpaceSet, cluster, x, z)) {
                    freeSpace.add(hash);
                } else {
                    negSpaceSet.add(hash);
                    allNegativeSpaces.add(StaticTB.from(x, z));
                }
            }
        }

        freeSpace.clear();

        return allNegativeSpaces == null ? Collections.emptyList() : allNegativeSpaces;
    }

    private static final int[] DIRECTIONS = { -1, 1 };
    private static boolean isNextToFreeSpace(Collection<Long> freeSpaces, int posX, int posZ) {
        if (freeSpaces.isEmpty())
            return false;

        for (int i = 0; i < 2; ++i) {
            for (int dir : DIRECTIONS) {
                int xOffset = i == 0 ? dir : 0;
                int zOffset = i == 1 ? dir : 0;

                long offSetHash = StaticTB.hashPos(posX + xOffset, posZ + zOffset);
                if (freeSpaces.contains(offSetHash))
                    return true;
            }
        }

        return false;
    }

    // Used for back propogation
    private static boolean isEmptyBelow(Collection<Long> negSpace, TBCluster cluster, int posX, int posZ) {
        long belowHash = StaticTB.hashPos(posX, posZ - 1);
        return !negSpace.contains(belowHash) && !cluster.has(belowHash);
    }


}
