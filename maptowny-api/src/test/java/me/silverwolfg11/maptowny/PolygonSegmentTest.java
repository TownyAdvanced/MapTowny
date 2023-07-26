package me.silverwolfg11.maptowny;

import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import me.silverwolfg11.maptowny.util.PolygonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static me.silverwolfg11.maptowny.TestHelpers.*;

public class PolygonSegmentTest {
    // Test the segmentation of a polygon.

    private Polygon pointsToJavaPoly(List<Point2D> points) {
        int[] xpoints = new int[points.size() + 1];
        int[] zpoints = new int[points.size() + 1];

        for (int i = 0; i < points.size(); i++) {
            xpoints[i] = (int) points.get(i).x();
            zpoints[i] = (int) points.get(i).z();
        }

        return new Polygon(xpoints, zpoints, points.size());
    }

    private Point2D tbCenter(StaticTB tb, int tbSize) {
        Point2D llPoint = tb.getLL(tbSize);
        return point(llPoint.x() + (tbSize / 2), llPoint.z() + (tbSize / 2));
    }
    // ++
    // ++
    @Test
    @DisplayName("Polygon Segment: Segment Unneccessary")
    void segmentUnnecessary() {
        TBCluster cluster = clusterOf(tb(0, 0), tb(0, 1), tb(1,0), tb(1,1));

        List<List<Point2D>> polygonList = PolygonUtil.segmentPolygon(cluster, new ArrayList<>(), TILE_SIZE);
        assertEquals(polygonList.size(), 1);

        List<Point2D> polygonOutline = polygonList.get(0);
        List<Point2D> expectedOutline = PolygonUtil.getPolyInfoFromCluster(cluster, TILE_SIZE, false).getPolygonPoints();

        assertEquals(polygonOutline, expectedOutline);
    }

    // +++
    // +-+
    // +++
    @Test
    @DisplayName("Polygon Segment: Segment 3x3")
    void segment3x3() {
        TBCluster cluster = clusterOf(
                            tb(0, 0), tb(0, 1), tb(0,2),
                            tb(1,0),            tb(1,2),
                            tb(2,0),  tb(2,1),  tb(2,2)
        );

        TBCluster negSpaceCluster = clusterOf(tb(1,1));
        Point2D negSpaceCenter = tbCenter(tb(1, 1), TILE_SIZE);

        List<List<Point2D>> polygonList = PolygonUtil.segmentPolygon(cluster, list(negSpaceCluster), TILE_SIZE);
        assertTrue(polygonList.size() > 1);

        for (List<Point2D> polyOutline : polygonList) {
            assertFalse(polyOutline.isEmpty());
            Polygon javaPoly = pointsToJavaPoly(polyOutline);
            assertFalse(javaPoly.contains(negSpaceCenter.x(), negSpaceCenter.z()));
        }
    }

    // +++
    // +-+
    // +++
    // +-+
    // +++
    @Test
    @DisplayName("Polygon Segment: Segment 8-figure")
    void segment8Figure() {
        TBCluster cluster = clusterOf(
                tb(0, 0), tb(0, 1), tb(0,2),
                tb(1,0),            tb(1,2),
                tb(2,0),  tb(2,1),  tb(2,2),
                tb(3,0),            tb(3,2),
                tb(4,0),  tb(4,1),  tb(4,2)
        );

        List<TBCluster> negSpaceClusters = clustersOf(tb(1,1), tb(3,1));
        List<Point2D> negSpaceCenters = list(tbCenter(tb(1, 1), TILE_SIZE), tbCenter(tb(3, 1), TILE_SIZE));

        List<List<Point2D>> polygonList = PolygonUtil.segmentPolygon(cluster, negSpaceClusters, TILE_SIZE);
        assertTrue(polygonList.size() > 1);

        for (List<Point2D> polyOutline : polygonList) {
            assertFalse(polyOutline.isEmpty());
            Polygon javaPoly = pointsToJavaPoly(polyOutline);
            for (Point2D negSpaceCenter : negSpaceCenters) {
                assertFalse(javaPoly.contains(negSpaceCenter.x(), negSpaceCenter.z()));
            }
        }
    }
}
