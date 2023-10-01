package me.silverwolfg11.maptowny.objects;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An extension of the Polygon class that contains
 * sub-polygons that add up together to fill the area
 * of the original polygon.<br>
 * These sub-polygons are notated as segments.
 *
 * @since 3.0.0
 */
public class SegmentedPolygon extends Polygon {
    private final List<List<Point2D>> subSegments;

    public SegmentedPolygon(@NotNull List<Point2D> polygon, @NotNull List<List<Point2D>> negativeSpace, @NotNull List<List<Point2D>> segmentedSpace) {
        super(polygon, negativeSpace);
        this.subSegments = segmentedSpace;
    }

    /**
     * Get the segmented sub-polygons that cumulatively represent the original polygon.
     *
     * @return a list of polygons that added together represent the area
     *         of the original polygon.
     * @since 3.0.0
     */
    public List<List<Point2D>> getSegments() {
        return subSegments;
    }
}
