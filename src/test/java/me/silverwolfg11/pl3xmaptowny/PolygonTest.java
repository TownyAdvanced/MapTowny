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

package me.silverwolfg11.pl3xmaptowny;

import static org.junit.jupiter.api.Assertions.*;
import static me.silverwolfg11.pl3xmaptowny.TestHelpers.*;

import me.silverwolfg11.pl3xmaptowny.objects.Point2D;
import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import me.silverwolfg11.pl3xmaptowny.util.PolygonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

// A class that tests PolygonUtil

// Expectations: PolygonUtil needs to return the correct outline
// of the provided cluster as well as in a connected order.

// Manual Testing: Manually create a cluster and a set of points to validate against the class.
public class PolygonTest {

    // Compare two lists of points finding the first element
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
        assertNotEquals(startingIndex, -1, "Could not find first expected point in Polygon output!");

        int outputIndex = startingIndex + 1;
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

        Point2D startingPt = cornerPoint(topTB, CORNER.UPPER_RIGHT);

        Point2D rightCorner1 = cornerPoint(rightTB, CORNER.UPPER_LEFT);
        Point2D rightCorner2 = cornerPoint(rightTB, CORNER.UPPER_RIGHT);
        Point2D rightCorner3 = cornerPoint(rightTB, CORNER.LOWER_RIGHT);

        Point2D bottomCorner1 = cornerPoint(bottomTB, CORNER.UPPER_RIGHT);
        Point2D bottomCorner2 = cornerPoint(bottomTB, CORNER.LOWER_RIGHT);
        Point2D bottomCorner3 = cornerPoint(bottomTB, CORNER.LOWER_LEFT);

        Point2D leftCorner1 = cornerPoint(leftTB, CORNER.LOWER_RIGHT);
        Point2D leftCorner2 = cornerPoint(leftTB, CORNER.LOWER_LEFT);
        Point2D leftCorner3 = cornerPoint(leftTB, CORNER.UPPER_LEFT);

        Point2D topCorner1 = cornerPoint(topTB, CORNER.LOWER_LEFT);

        Point2D endingPt = cornerPoint(topTB, CORNER.UPPER_LEFT);

        List<Point2D> expected = list(startingPt, rightCorner1, rightCorner2, rightCorner3,
                bottomCorner1, bottomCorner2, bottomCorner3, leftCorner1, leftCorner2, leftCorner3,
                topCorner1, endingPt);

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

}
