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

package me.silverwolfg11.pl3xmaptowny.objects;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A cluster represents a group of townblocks which are all connected
// Functions mostly as an easy-to-use wrapper to generate polygons
public class TBCluster {

    private final Map<Long, StaticTB> townblocks = new HashMap<>();

    private TBCluster() {}

    public boolean isEmpty() {
        return townblocks.isEmpty();
    }

    public int size() {
        return townblocks.size();
    }

    public boolean has(StaticTB tb) {
        return townblocks.containsKey(tb.toLong());
    }

    public boolean has(long hash) {
        return townblocks.containsKey(hash);
    }

    public StaticTB at(long hash) {
        return townblocks.get(hash);
    }

    public void add(StaticTB tb) {
        townblocks.put(tb.toLong(), tb);
    }

    public void add(long hash, StaticTB tb) {
        townblocks.put(hash, tb);
    }

    public StaticTB findAny() {
        return townblocks.values().stream().findAny().orElse(null);
    }

    public Collection<StaticTB> getBlocks() {
        return Collections.unmodifiableCollection(townblocks.values());
    }

    /*
    // Debug Method:
    // This method is kept to visualize a cluster like it would be displayed on the towny map in-game.
    // This method uses sysout statements so it can be used at runtime and during static testing.
    public void print() {
        if (isEmpty()) {
            System.out.println("[PTD] EMPTY CLUSTER");
            return;
        }

        java.util.Comparator<StaticTB> comparator = (l, r) -> {
          if (l.z() < r.z())
              return -1;

          if (l.z() > r.z())
              return 1;

            return Integer.compare(l.x(), r.x());
        };

        // Sort by z, and then x
        List<StaticTB> blocks = this.getBlocks()
                .stream()
                .sorted(comparator)
                .collect(java.util.stream.Collectors.toList());

        // Find corners
        StaticTB.Edges edges = blocks.stream().collect(StaticTB.Edges.collect());

        int minX = edges.getMinX(), maxX = edges.getMaxX();
        int minZ = edges.getMinZ(), maxZ = edges.getMaxZ();

        System.out.printf("(%d, %d) - (%d, %d)%n", minX, minZ, maxX, maxZ);
        int currIndex = 0;
        StaticTB currTB = blocks.get(0);
        for (int posZ = minZ; posZ <= maxZ; posZ++) {
            StringBuilder line = new StringBuilder(maxX - minX + 1);
            for (int posX = minX; posX <= maxX; posX++) {
                if (currIndex < blocks.size() &&
                        currTB.x() == posX && currTB.z() == posZ) {
                    line.append('+');
                    currIndex++;
                    currTB = currIndex < blocks.size() ? blocks.get(currIndex) : null;
                }
                else
                    line.append('-');
            }
            System.out.println(line);
        }

    }
    */

    private static final int[] DIRECTIONS = { -1, 1 };

    // Get all clusters of connected townblocks
    @NotNull
    public static List<TBCluster> findClusters(Collection<StaticTB> townBlocks) {
        if (townBlocks == null || townBlocks.isEmpty())
            return Collections.emptyList();

        Map<Long, StaticTB> hashedMap = collectionToMap(townBlocks);
        List<TBCluster> clusters = new ArrayList<>();

        while (!hashedMap.isEmpty()) {
            TBCluster cluster = new TBCluster();
            Deque<Long> visited = new ArrayDeque<>();
            // Push the first entry key of the map onto the stackk
            visited.push(hashedMap.entrySet().stream().findFirst().get().getKey());

            while (!visited.isEmpty()) {
                long hash = visited.pop();

                StaticTB townBlock = hashedMap.remove(hash);

                // Townblock may have already been visited
                if (townBlock == null)
                    continue;

                cluster.add(hash, townBlock);

                for (int i = 0; i < 2; ++i) {
                    for (int dir : DIRECTIONS) {
                        int xOffset = i == 0 ? dir : 0;
                        int zOffset = i == 1 ? dir : 0;

                        long offSetHash = townBlock.offsetLong(xOffset, zOffset);
                        if (hashedMap.containsKey(offSetHash))
                            visited.push(offSetHash);
                    }
                }
            }

            if (!cluster.isEmpty())
                clusters.add(cluster);
        }

        return clusters;
    }

    // Would use streams, but unfortunately they don't handle duplicate values
    private static Map<Long, StaticTB> collectionToMap(Collection<StaticTB> townBlocks) {
        Map<Long, StaticTB> tbMap = new HashMap<>((4 * townBlocks.size()) / 3);
        for (StaticTB tb : townBlocks) {
            tbMap.put(tb.toLong(), tb);
        }
        return tbMap;
    }
}
