package me.silverwolfg11.pl3xmaptowny.util;

import com.palmergames.bukkit.towny.TownySettings;
import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import net.pl3x.map.api.Point;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class PolygonUtil {

    private enum DIRECTION { RIGHT, DOWN, UP, LEFT };

    // Forms a polygon given a townblock cluster
    // Returns
    @Nullable
    public static List<Point> formPolyFromCluster(TBCluster cluster, final int tbSize) {

        StaticTB rightMostBlock = findRightMost(cluster);

        // Path algorithm inserts unnecessary duplicate points
        Set<Point> poly = new LinkedHashSet<>();

        // Origin point is the upper left of the townblock
        Point originPoint = rightMostBlock.getUL(tbSize);

        Queue<Long> townBlocksToVisit = new ArrayDeque<>(1);
        townBlocksToVisit.add(rightMostBlock.toLong());

        // Fast-fail infinite loops by checking that we don't have too many iterations
        // There should only be 4 iterations per townblock + an extra iteration to check if back at origin.
//        final int pointsErrorBound = (cluster.size() * 4) + 1;

        DIRECTION currDir = DIRECTION.RIGHT;

        int pathIterations = 0;
        boolean isOriginPoint = true;
        while(!townBlocksToVisit.isEmpty()) {
            pathIterations++;

//            // Fast fail
//            if (pathIterations > pointsErrorBound) {
//                return null;
//            }

            long tbHash = townBlocksToVisit.poll();
            StaticTB townBlock = cluster.at(tbHash);

            switch (currDir) {
                case RIGHT: {
                    Point upperLeft = townBlock.getUL(tbSize);
                    // We approach the origin from the right so verify we are not back at the origin
                    if (!isOriginPoint && upperLeft.equals(originPoint))
                        continue;
                    else if (isOriginPoint)
                        isOriginPoint = false;

                    boolean corner = false;
                    long offsetHash;
                    // Check if there's a townblock above us
                    // Theoretically can only happen if we are going towards the origin, not away
                    // We have hit a corner!
                    if (cluster.has(offsetHash = townBlock.offsetLong(0, 1))) {
                        townBlocksToVisit.add(offsetHash);
                        currDir = DIRECTION.UP;
                        corner = true;
                    }
                    // Check if there's a townblock to the right of us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(1, 0))) {
                        // Keep Going Right
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the rightmost, so switch direction to down and queue the same block
                        currDir = DIRECTION.DOWN;
                        townBlocksToVisit.add(tbHash);
                        corner = true;
                    }

                    // Add upper left and upper right points to the poly (ORDER MATTERS)
                    poly.add(upperLeft); // Upper Left
                    if (!corner)
                        poly.add(townBlock.getUR(tbSize)); // Upper Right

                    break;
                }
                case LEFT: {
                    boolean corner = false;
                    long offsetHash;
                    // Check if there's a townblock below us (This happens at corners)
                    if (cluster.has(offsetHash = townBlock.offsetLong(0, -1))) {
                        // Queue the block below
                        townBlocksToVisit.add(offsetHash);
                        // Set direction as down
                        currDir = DIRECTION.DOWN;
                        corner = true;
                    }
                    // Check if there's a townblock to the left of us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(-1, 0))) {
                        // Keep Going Left
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the leftmost, so switch direction to up
                        currDir = DIRECTION.UP;
                        townBlocksToVisit.add(tbHash);
                        corner = true;
                    }

                    // Add lower right and lower left (ORDER MATTERS)
                    poly.add(townBlock.getLR(tbSize)); // Lower Right
                    if (!corner)
                        poly.add(townBlock.getLL(tbSize)); // Lower Left
                    break;
                }
                case DOWN: {
                    boolean corner = false;
                    long offsetHash;
                    // Check if there's a townblock to the right us (We have hit a corner)
                    if (cluster.has(offsetHash = townBlock.offsetLong(1, 0))) {
                        // Queue the block right
                        townBlocksToVisit.add(offsetHash);
                        // Set direction as right
                        currDir = DIRECTION.RIGHT;
                        corner = true;
                    }
                    // Check if there's a townblock below us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(0, -1))) {
                        // Keep Going Down
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the bottom most, so make a left turn
                        currDir = DIRECTION.LEFT;
                        townBlocksToVisit.add(tbHash);
                        corner = true;
                    }

                    // Add upper right and lower right points to the poly (ORDER MATTERS)
                    poly.add(townBlock.getUR(tbSize)); // Upper Right
                    if (!corner)
                        poly.add(townBlock.getLR(tbSize)); // Lower Right

                    break;
                }
                case UP: {
                    boolean corner = false;
                    long offsetHash;
                    // Check if there's a townblock to the left of us (We have hit a corner)
                    if (cluster.has(offsetHash = townBlock.offsetLong(-1, 0))) {
                        townBlocksToVisit.add(offsetHash);
                        currDir = DIRECTION.LEFT;
                        corner = true;
                    }
                    // Check if there's a townblock above us
                    else if (cluster.has(offsetHash = townBlock.offsetLong(0, 1))) {
                        // Keep Going up
                        townBlocksToVisit.add(offsetHash);
                    }
                    else {
                        // We're the top most, so make a right turn
                        currDir = DIRECTION.RIGHT;
                        townBlocksToVisit.add(tbHash);
                        corner = true;
                    }

                    // Add lower left and upper left points to the poly (ORDER MATTERS)
                    poly.add(townBlock.getLL(tbSize)); // Lower left
                    if (!corner)
                        poly.add(townBlock.getUL(tbSize)); // Upper Left
                    break;
                }
            }
        }

        List<Point> polyList = new ArrayList<>(poly);

        // Help GC
        poly.clear();
        townBlocksToVisit.clear();


        return polyList;
    }

    // Find the upper right-most town block (prioritize right-most over upper)
    private static StaticTB findRightMost(TBCluster cluster) {
        StaticTB rightMostBlock = cluster.findAny();

        for (StaticTB tb : cluster.getBlocks()) {
            if (tb.x() > rightMostBlock.x()
                    || (tb.x() == rightMostBlock.x() && tb.z() > rightMostBlock.z())) {
                rightMostBlock = tb;
            }
        }

        return rightMostBlock;
    }
    
}
