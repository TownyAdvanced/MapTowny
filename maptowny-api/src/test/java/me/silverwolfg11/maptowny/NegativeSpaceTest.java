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

import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import me.silverwolfg11.maptowny.util.NegativeSpaceFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static me.silverwolfg11.maptowny.TestHelpers.*;

public class NegativeSpaceTest {

    // Test shapes that should not have negative spaces first
    // The algorithm has a higher risk of producing negative spaces when it is not supposed to
    // Thus, there are more "No Negative Space" tests to verify that it functions correctly.

    // Shapes test are based on production data that revealed previous bugs.

    @Test
    @DisplayName("No Negative Space: Single Block")
    void testNoNSpaceSingleBlock() {
        TBCluster cluster = clusterOf(tb(0,0));
        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // +++
    // + +
    // + +
    // +
    @Test
    @DisplayName("No Negative Space: Cane")
    void testNoNSpaceCane() {
        TBCluster cluster = clusterOf(
                tb(-1, 0),
                tb(-1, 1), tb(1, 1),
                tb(-1, 2), tb(1, 2),
                tb(-1, 3), tb(0, 3), tb(1, 3)
        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    //     ++++
    //  ++++  +
    //  +     +
    // ++     +
    // +      +
    //        +
    //        +
    @Test
    @DisplayName("No Negative Space: Scythe")
    void testNoNSpaceFlippedCane() {
        TBCluster cluster = clusterOf(
                tb(0, -4), tb(1, -4), tb(2, -4), tb(3, -4),
                tb(-3, -3), tb(-2, -3), tb(-1, -3), tb(0, -3), tb(3, -3),
                tb(-3, -2), tb(3, -2),
                tb(-4, -1), tb(-3, -1), tb(3, -1),
                tb(-4, 0), tb(3, 0),
                tb(3, 1),
                tb(3, 2)
        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // +
    // + +
    // + +
    // +++
    @Test
    @DisplayName("No Negative Space: U")
    void testNoNSpaceU() {
        TBCluster cluster = clusterOf(
                tb(-1, 0), tb(0, 0), tb(1, 0),
                tb(-1, 1), tb(1, 1),
                tb(-1, 2), tb(1, 2),
                tb(-1, 3)
        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // ++++
    //    +
    //  +++
    @Test
    @DisplayName("No Negative Space: 3-figure")
    void testNoNSpace3() {
        TBCluster cluster = clusterOf(
                tb(1, 0), tb(2, 0), tb(3,0),
                tb(3, 1),
                tb(0, 2), tb(1, 2), tb(2, 2), tb(3, 2)
        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // +++
    // +
    // ++++
    @Test
    @DisplayName("No Negative Space: C-figure")
    void testNoNSpaceC() {
        TBCluster cluster = clusterOf(
                tb(0, 0), tb(1, 0), tb(2, 0), tb(3,0),
                tb(0, 1),
                tb(0, 2), tb(1, 2), tb(2, 2)
        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // + +++
    // + + +
    // +   +
    // +++++
    @Test
    @DisplayName("No Negative Space: Sideway's G")
    void testNoNSpaceSidewayG() {
        TBCluster cluster = clusterOf(
                tb(0,0 ), tb(2, 0), tb(3, 0), tb(4, 0),
                tb(0, 1), tb(2, 1), tb(4, 1),
                tb(0, 2), tb(4, 2),
                tb(0, 3), tb(1, 3), tb(2, 3), tb(3, 3), tb(4, 3)

        );

        Collection<StaticTB> negSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertTrue(negSpace.isEmpty());
    }

    // Test shapes that do have negative spaces

    // +++
    // + +
    // +++
    @Test
    @DisplayName("Negative Space: 3x3")
    void testNSpace3x3() {
        TBCluster cluster = clusterOf(
                tb(-1, 1), tb(0, 1), tb(1, 1),
                tb(-1, 0), tb(1, 0),
                tb(-1, -1), tb(0, -1), tb(1, -1)
        );

        List<StaticTB> output = NegativeSpaceFinder.findNegativeSpace(cluster);
        assertEquals(output.size(), 1);

        StaticTB missingTB = tb(0, 0);
        assertEquals(output.get(0), missingTB);
    }

    // +++
    // + +
    // +++
    // + +
    // +++
    @Test
    @DisplayName("Negative Space: 8-figure")
    void testNSpace8() {
        TBCluster cluster = clusterOf(
                tb(-1, 2), tb(0, 2), tb(1, 2),
                tb(-1, 1), tb(1, 1),
                tb(-1, 0), tb(0, 0), tb(1, 0),
                tb(-1, -1), tb(1, -1),
                tb(-1, 2), tb(0, -2), tb(1, -2)
        );

        List<StaticTB> output = NegativeSpaceFinder.findNegativeSpace(cluster);
        Collection<StaticTB> expectedMissing = list(tb(0, 1), tb(0, -1));

        assertEquals(output.size(), expectedMissing.size());
        assertTrue(output.containsAll(expectedMissing));
    }

    // +++
    // + +
    // +++
    // + +
    // + +
    @Test
    @DisplayName("Negative Space: A-figure")
    void testNSpaceA() {
        TBCluster cluster = clusterOf(
                tb(-1, 2), tb(0, 2), tb(1, 2),
                tb(-1, 1), tb(1, 1),
                tb(-1, 0), tb(0, 0), tb(1, 0),
                tb(-1, -1), tb(1, -1),
                tb(-1, 2), tb(1, -2)
        );

        List<StaticTB> output = NegativeSpaceFinder.findNegativeSpace(cluster);
        Collection<StaticTB> expectedMissing = list(tb(0, 1));

        assertEquals(output.size(), expectedMissing.size());
        assertTrue(output.containsAll(expectedMissing));
    }

}
