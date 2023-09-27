package me.silverwolfg11.maptowny;

import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.StaticTB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static me.silverwolfg11.maptowny.TestHelpers.*;

public class StaticTBTest {

    private static final int TB_SIZE = 16;

    @Test
    @DisplayName("Static TB: Basic Getters")
    void simpleGetterTest() {
        StaticTB tb = StaticTB.from(1, -1);

        assertEquals(1, tb.x());
        assertEquals(-1, tb.z());
    }

    @Test
    @DisplayName("Static TB: To Hash")
    void toHashTest() {
        StaticTB tb = StaticTB.from(1, -1);

        // Long hash should pack the coords together in format z,x
        // 1 =  0x00000001
        // -1 = 0xFF FF FF FF
        // Combined: 0xFFFFFFFF000000001
        long hash = 0xFFFFFFFF00000001L;
        assertEquals(hash, tb.toLong());
    }

    @Test
    @DisplayName("Static TB: From Hash")
    void fromHashTest() {

        // Hash represents x:1, z:-1
        long hash = 0xFFFFFFFF00000001L;

        StaticTB tb = StaticTB.fromHashed(hash);

        assertEquals(1, tb.x());
        assertEquals(-1, tb.z());
        assertEquals(hash, tb.toLong());
    }

    @Test
    @DisplayName("Static TB: Individual corner methods")
    void indvCornerTest() {
        StaticTB tb = StaticTB.from(0, 0);
        // Use 16 blocks as the TB size

        assertEquals(cornerPoint(tb, CORNER.LOWER_LEFT), tb.getLL(TB_SIZE), "Incorrect lower left");
        assertEquals(cornerPoint(tb, CORNER.LOWER_RIGHT), tb.getLR(TB_SIZE), "Incorrect lower right");
        assertEquals(cornerPoint(tb, CORNER.UPPER_LEFT), tb.getUL(TB_SIZE), "Incorrect upper left");
        assertEquals(cornerPoint(tb, CORNER.UPPER_RIGHT), tb.getUR(TB_SIZE), "Incorrect upper right");
    }

    @Test
    @DisplayName("Static TB: Negative TB Corners")
    void negTBCornerTest() {
        StaticTB tb = StaticTB.from(-1, -1);
        // Use 16 blocks as the TB size

        assertEquals(cornerPoint(tb, CORNER.LOWER_LEFT), tb.getCorner(TB_SIZE, true, true), "Incorrect lower left");
        assertEquals(cornerPoint(tb, CORNER.LOWER_RIGHT), tb.getCorner(TB_SIZE, true, false), "Incorrect lower right");
        assertEquals(cornerPoint(tb, CORNER.UPPER_LEFT), tb.getCorner(TB_SIZE, false, true), "Incorrect upper left");
        assertEquals(cornerPoint(tb, CORNER.UPPER_RIGHT), tb.getCorner(TB_SIZE, false, false), "Incorrect upper right");
    }


    @Test
    @DisplayName("Static TB: Positive TB Corners")
    void getCornerTest() {
        StaticTB tb = StaticTB.from(0, 0);
        // Use 16 blocks as the TB size

        assertEquals(cornerPoint(tb, CORNER.LOWER_LEFT), tb.getCorner(TB_SIZE, true, true), "Incorrect lower left");
        assertEquals(cornerPoint(tb, CORNER.LOWER_RIGHT), tb.getCorner(TB_SIZE, true, false), "Incorrect lower right");
        assertEquals(cornerPoint(tb, CORNER.UPPER_LEFT), tb.getCorner(TB_SIZE, false, true), "Incorrect upper left");
        assertEquals(cornerPoint(tb, CORNER.UPPER_RIGHT), tb.getCorner(TB_SIZE, false, false), "Incorrect upper right");
    }

    @Test
    @DisplayName("Static TB: TB Block Corners")
    void getBlockCornerTest() {
        StaticTB tb = StaticTB.from(0, 0);
        // Use 16 blocks as the TB size

        assertEquals(Point2D.of(0, 0), tb.getBlockCorner(TB_SIZE, true, true), "Incorrect lower left");
        assertEquals(Point2D.of(15, 0), tb.getBlockCorner(TB_SIZE, true, false), "Incorrect lower right");
        assertEquals(Point2D.of(0, 15), tb.getBlockCorner(TB_SIZE, false, true), "Incorrect upper left");
        assertEquals(Point2D.of(15, 15), tb.getBlockCorner(TB_SIZE, false, false), "Incorrect upper right");
    }

}
