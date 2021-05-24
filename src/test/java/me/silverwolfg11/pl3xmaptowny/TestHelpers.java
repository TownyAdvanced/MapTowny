package me.silverwolfg11.pl3xmaptowny;

import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import net.pl3x.map.api.Point;

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

    enum CORNER { LOWER_LEFT, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT }
    static Point cornerPoint(StaticTB tb, CORNER corner) {
        int xOffset = 0;
        if (corner == CORNER.LOWER_RIGHT || corner == CORNER.UPPER_RIGHT)
            xOffset = (TILE_SIZE - 1);

        int zOffset = 0;
        if (corner == CORNER.UPPER_LEFT || corner == CORNER.UPPER_RIGHT)
            zOffset = (TILE_SIZE - 1);

        return Point.of((tb.x() * TILE_SIZE) + xOffset, (tb.z() * TILE_SIZE) + zOffset);
    }

}
