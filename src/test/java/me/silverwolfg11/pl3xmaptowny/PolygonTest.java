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

import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import me.silverwolfg11.pl3xmaptowny.util.PolygonUtil;
import net.pl3x.map.api.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

// A class that tests PolygonUtil

// Expectations: PolygonUtil needs to return the correct outline
// of the provided cluster as well as in a connected order.

// Manual Testing: Manually create a cluster and a set of points to validate against the class.
public class PolygonTest {

    // Compare two lists of points finding the first element
    void polygonHasPoints(List<Point> output, List<Point> expected) {
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

        Point ur = cornerPoint(tb, CORNER.UPPER_RIGHT);
        Point lr = cornerPoint(tb, CORNER.LOWER_RIGHT);
        Point ll = cornerPoint(tb, CORNER.LOWER_LEFT);
        Point ul = cornerPoint(tb, CORNER.UPPER_LEFT);
        List<Point> expected = list(ur, lr, ll, ul);
        List<Point> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

    // Test a single 3 x 1 row of townblocks
    @Test
    @DisplayName("Polygon: Single Row")
    void testPolygonRowCluster() {
        StaticTB originTB = tb(0, 0);
        StaticTB endingTB = tb(2, 0);

        TBCluster cluster = clusterOf(originTB, tb(1,0), endingTB);

        Point ulOrigin = cornerPoint(originTB, CORNER.UPPER_LEFT);
        Point urEnding = cornerPoint(endingTB, CORNER.UPPER_RIGHT);
        Point lrEnding = cornerPoint(endingTB, CORNER.LOWER_RIGHT);
        Point llOrigin = cornerPoint(originTB, CORNER.LOWER_LEFT);

        List<Point> expected = list(ulOrigin, urEnding, lrEnding, llOrigin);
        List<Point> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

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

        Point startingPt = cornerPoint(topTB, CORNER.UPPER_RIGHT);

        Point rightCorner1 = cornerPoint(rightTB, CORNER.UPPER_LEFT);
        Point rightCorner2 = cornerPoint(rightTB, CORNER.UPPER_RIGHT);
        Point rightCorner3 = cornerPoint(rightTB, CORNER.LOWER_RIGHT);

        Point bottomCorner1 = cornerPoint(bottomTB, CORNER.UPPER_RIGHT);
        Point bottomCorner2 = cornerPoint(bottomTB, CORNER.LOWER_RIGHT);
        Point bottomCorner3 = cornerPoint(bottomTB, CORNER.LOWER_LEFT);

        Point leftCorner1 = cornerPoint(leftTB, CORNER.LOWER_RIGHT);
        Point leftCorner2 = cornerPoint(leftTB, CORNER.LOWER_LEFT);
        Point leftCorner3 = cornerPoint(leftTB, CORNER.UPPER_LEFT);

        Point topCorner1 = cornerPoint(topTB, CORNER.LOWER_LEFT);

        Point endingPt = cornerPoint(topTB, CORNER.UPPER_LEFT);

        List<Point> expected = list(startingPt, rightCorner1, rightCorner2, rightCorner3,
                bottomCorner1, bottomCorner2, bottomCorner3, leftCorner1, leftCorner2, leftCorner3,
                topCorner1, endingPt);

        List<Point> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

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
        Point ul = cornerPoint(originCornerTB, CORNER.UPPER_LEFT);
        Point ur = cornerPoint(urCornerTB, CORNER.UPPER_RIGHT);
        Point lr = cornerPoint(lrCornerTB, CORNER.LOWER_RIGHT);
        Point ll = cornerPoint(llCornerTB, CORNER.LOWER_LEFT);

        List<Point> expected = list(ul, ur, lr, ll);
        List<Point> output = PolygonUtil.formPolyFromCluster(cluster, TILE_SIZE);

        polygonHasPoints(output, expected);
    }

}
