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

import static org.junit.jupiter.api.Assertions.*;
import static me.silverwolfg11.maptowny.TestHelpers.*;

import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import me.silverwolfg11.maptowny.util.PolygonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;

// A class that tests PolygonUtil

// Expectations: PolygonUtil needs to return the correct outline
// of the provided cluster as well as in a connected order.

// Manual Testing: Manually create a cluster and a set of points to validate against the class.
public class PolygonTest {

    // Validate that the output list contains all the expected points in a sequential order.
    void polygonHasPoints(List<Point2D> output, List<Point2D> expected) {
        assertNotNull(output, "Polygon output was null!");

        // Quickly validate size
        assertFalse(output.size() < expected.size(),
                String.format("Polygon output has less points than expected. %d expected, but %d found.",
                        expected.size(), output.size())
        );

        if (output.isEmpty()) {
            return;
        }

        int startingIndex = output.indexOf(expected.get(0));

        // Couldn't find starting point
        assertNotEquals(startingIndex, -1,
                () -> {
                    Point2D startingPoint = expected.get(0);
                    int x = (int) startingPoint.x();
                    int z = (int) startingPoint.z();
                    return String.format("Could not find first expected point (%d, %d) in Polygon output!", x, z);
                });

        int outputIndex = (startingIndex + 1) % output.size();
        int expectedIndex = 1;

        while (outputIndex != startingIndex && expectedIndex < expected.size()) {
            if (output.get(outputIndex).equals(expected.get(expectedIndex)))
                expectedIndex++;

            outputIndex = (outputIndex + 1) % output.size();
        }

        final int missingIndex = expectedIndex;
        assertEquals(expectedIndex, expected.size(),
                () -> String.format("Polygon output is missing point (%f, %f) in the correct order! Expected Index: %d",
                        expected.get(missingIndex).x(), expected.get(missingIndex).z(), missingIndex)
        );
    }

    // Test polygonHasPoints algorithm
    @Test
    @DisplayName("Verify: polygonHasPoints")
    void testPolyHasPoints() {
        // All these checks are done in a single test to avoid cluttering the test class for a verification check.

        // Test perfectly matched lists
        {
            List<Point2D> actualPts = pointsOf(0, 0, 1, 0, 1, 1);
            List<Point2D> expectedPts = pointsOf(0, 0, 1, 0, 1, 1);
            polygonHasPoints(actualPts, expectedPts);
        }

        // Test offset list
        {
            List<Point2D> actualPts = pointsOf(1, 1, 0, 0, 1, 0);
            List<Point2D> expectedPts = pointsOf(0, 0, 1, 0, 1, 1);
            polygonHasPoints(actualPts, expectedPts);
        }

        // Test incorrect order of points
        {
            List<Point2D> actualPts = pointsOf(1, 1, 1, 0, 0, 0);
            List<Point2D> expectedPts = pointsOf(0, 0, 1, 0, 1, 1);

            assertThrows(AssertionFailedError.class, () -> polygonHasPoints(actualPts, expectedPts));
        }

        // Test missing point points
        {
            List<Point2D> actualPts = pointsOf(1, 1, 1, 0);
            List<Point2D> expectedPts = pointsOf(0, 0, 1, 0, 1, 1);

            assertThrows(AssertionFailedError.class, () -> polygonHasPoints(actualPts, expectedPts));
        }
    }



    // Test a single townblock in a cluster
    @Test
    @DisplayName("Polygon: Single Block")
    void testPolygonSingleBlock() {
        StaticTB tb = tb(0, 0);
        TBCluster cluster = clusterOf(tb);

        Point2D ur = cornerPoint(tb, CORNER.UPPER_RIGHT);
        Point2D lr = cornerPoint(tb, CORNER.LOWER_RIGHT);
        Point2D ll = cornerPoint(tb, CORNER.LOWER_LEFT);
        Point2D ul = cornerPoint(tb, CORNER.UPPER_LEFT);
        List<Point2D> expected = list(ur, lr, ll, ul);
        List<Point2D> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

    // Test a single 3 x 1 row of townblocks
    @Test
    @DisplayName("Polygon: Single Row")
    void testPolygonRowCluster() {
        StaticTB originTB = tb(0, 0);
        StaticTB endingTB = tb(2, 0);

        TBCluster cluster = clusterOf(originTB, tb(1,0), endingTB);

        Point2D ulOrigin = cornerPoint(originTB, CORNER.UPPER_LEFT);
        Point2D urEnding = cornerPoint(endingTB, CORNER.UPPER_RIGHT);
        Point2D lrEnding = cornerPoint(endingTB, CORNER.LOWER_RIGHT);
        Point2D llOrigin = cornerPoint(originTB, CORNER.LOWER_LEFT);

        List<Point2D> expected = list(ulOrigin, urEnding, lrEnding, llOrigin);
        List<Point2D> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

    // Test a plus-shaped cluster of town blocks. E.g.
    //  +
    // +++
    //  +
    @Test
    @DisplayName("Polygon: Plus Shape")
    void testPolygonPlusShapeCluster() {
        StaticTB topTB = tb(0, 1);
        StaticTB centerTB = tb(0, 0);
        StaticTB bottomTB = tb(0, -1);
        StaticTB leftTB = tb(-1, 0);
        StaticTB rightTB = tb(1, 0);

        TBCluster cluster = clusterOf(topTB, centerTB, bottomTB, leftTB, rightTB);

        // The path of points
        Point2D startingPt = cornerPoint(topTB, CORNER.UPPER_RIGHT);
        Point2D centerUR = cornerPoint(centerTB, CORNER.UPPER_RIGHT);
        Point2D rightTop = cornerPoint(rightTB, CORNER.UPPER_RIGHT);
        Point2D rightBottom = cornerPoint(rightTB, CORNER.LOWER_RIGHT);
        Point2D centerLR = cornerPoint(centerTB, CORNER.LOWER_RIGHT);
        Point2D bottomLR = cornerPoint(bottomTB, CORNER.LOWER_RIGHT);
        Point2D bottomLL = cornerPoint(bottomTB, CORNER.LOWER_LEFT);
        Point2D centerLL = cornerPoint(centerTB, CORNER.LOWER_LEFT);
        Point2D leftLL = cornerPoint(leftTB, CORNER.LOWER_LEFT);
        Point2D leftUL = cornerPoint(leftTB, CORNER.UPPER_LEFT);
        Point2D centerUL = cornerPoint(centerTB, CORNER.UPPER_LEFT);
        Point2D topUL = cornerPoint(topTB, CORNER.UPPER_LEFT);

        List<Point2D> expected = list(startingPt, centerUR, rightTop, rightBottom,
                centerLR, bottomLR, bottomLL, centerLL, leftLL, leftUL, centerUL, topUL);

        List<Point2D> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

    // +++
    // +++
    // +++
    @Test
    @DisplayName("Polygon: 3x3")
    void testPolygon3x3Outline() {
        StaticTB originCornerTB = tb(-1, 1);
        StaticTB urCornerTB = tb(1, 1);
        StaticTB lrCornerTB = tb(1, -1);
        StaticTB llCornerTB = tb(-1, -1);

        TBCluster cluster = clusterOf(originCornerTB, tb(0, 1), urCornerTB,
                tb(-1, 0), tb(0, 0), tb(1, 0), lrCornerTB, tb(0, -1), llCornerTB);

        // Should only generate 4 points that we care about
        // Upper Left, Upper Right, Lower Left, Lower Right points (not in that order)
        Point2D ul = cornerPoint(originCornerTB, CORNER.UPPER_LEFT);
        Point2D ur = cornerPoint(urCornerTB, CORNER.UPPER_RIGHT);
        Point2D lr = cornerPoint(lrCornerTB, CORNER.LOWER_RIGHT);
        Point2D ll = cornerPoint(llCornerTB, CORNER.LOWER_LEFT);

        List<Point2D> expected = list(ul, ur, lr, ll);
        List<Point2D> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

    // --+++-
    // +++-+-
    // ---+++
    @Test
    @DisplayName("Polygon: 3x3")
    void TestDuplicateCorners() {
        TBCluster cluster = ClusterBuilder.builder()
                .row(tb(2, 2), 2)
                .row(tb(0, 1), 2).add(tb(4, 1))
                .row(tb(3, 0), 2)
                .buildCluster();

        // Should only generate 4 points that we care about
        // Upper Left, Upper Right, Lower Left, Lower Right points (not in that order)
        List<Point2D> expected = PointList.builder()
                .add(tb(4, 2), CORNER.UPPER_RIGHT)
                .add(tb(4, 0), CORNER.UPPER_RIGHT)
                .add(tb(5, 0), CORNER.UPPER_RIGHT)
                .add(tb(5, 0), CORNER.LOWER_RIGHT)
                .add(tb(3, 0), CORNER.LOWER_LEFT)
                .add(tb(3, 0), CORNER.UPPER_LEFT)
                .add(tb(3, 0), CORNER.UPPER_RIGHT)
                .add(tb(4, 2), CORNER.LOWER_LEFT)
                .add(tb(2, 2), CORNER.LOWER_RIGHT)
                .add(tb(2, 1), CORNER.LOWER_RIGHT) // Doesn't get added in failed tests
                .add(tb(0, 1), CORNER.LOWER_LEFT)
                .add(tb(0, 1), CORNER.UPPER_LEFT)
                .add(tb(2, 1), CORNER.UPPER_LEFT)
                .add(tb(2, 2), CORNER.UPPER_LEFT)
                .build();

        List<Point2D> output = PolygonUtil.getPolyInfoFromCluster(cluster, TILE_SIZE, false)
                .getPolygonPoints();

        polygonHasPoints(output, expected);
    }

}
