/*
 * Copyright (c) 2022 Silverwolfg11
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

package me.silverwolfg11.maptowny.platform.bluemap;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.marker.Shape;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.FetchMarkerOptionsOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.IconMarkerOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.MarkerOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.PolyMarkerOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.RemoveMarkerOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.RemoveMarkersOp;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.SetMarkerOptionsOp;
import me.silverwolfg11.maptowny.platform.bluemap.objects.WorldIdentifier;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlueMapLayerWrapper implements MapLayer {

    private final BlueMapMarkerProcessor markerProcessor;

    private final String layerKey;
    private final WorldIdentifier worldIdentifier;
    private final int zIndex;

    // Keep track of markers on the layer
    // Raw marker keys, not world keys.
    private final Set<String> markerIds = ConcurrentHashMap.newKeySet();

    // Simulate BlueMap having a MultiPolygon Marker
    // Marker Key -> Number of Polygons
    // Raw Marker Keys, not world keys
    private final Map<String, Integer> multiPolys = new ConcurrentHashMap<>();

    public BlueMapLayerWrapper(BlueMapMarkerProcessor markerProcessor, String layerKey, WorldIdentifier worldIdentifier, int zIndex) {
        this.markerProcessor = markerProcessor;
        this.layerKey = layerKey;
        this.worldIdentifier = worldIdentifier;
        this.zIndex = zIndex;
    }

    // Transform marker keys into per-world marker keys
    private String toWorldKey(String markerKey) {
        return markerKey + "_" + worldIdentifier.getWorldName();
    }

    // Create a new remove op with layer info given
    private MarkerOp newRemoveOp(String markerId) {
        return new RemoveMarkerOp(layerKey, markerId);
    }

    private MarkerOp newPolyOp(String markerId, Shape shape, MarkerOptions markerOptions) {
        return new PolyMarkerOp(layerKey, markerId, shape, worldIdentifier, zIndex, markerOptions);
    }

    private Vector2d pointToVec(Point2D point) {
        return new Vector2d(point.x(), point.z());
    }

    private Vector2d[] pointsToVecs(Collection<Point2D> points) {
        return points.stream()
                .map(this::pointToVec)
                .toArray(Vector2d[]::new);
    }

    @Override
    public void addMultiPolyMarker(@NotNull String markerKey, @NotNull List<Polygon> polygons, @NotNull MarkerOptions markerOptions) {
        // Try a ShapeMarker otherwise use an Extrude Marker
        // Label = Name
        // SetDetail = SetHTML
        final String worldKey = toWorldKey(markerKey);

        List<MarkerOp> markerOps = new ArrayList<>();

        // Delete markers if current multipoly size is less than old multipoly size
        if (multiPolys.containsKey(markerKey)) {
            int oldPolySize = multiPolys.get(markerKey);
            int currPolySize = polygons.size();
            // Delete all old markers
            for (int i = currPolySize; i < oldPolySize; ++i) {
                markerOps.add(newRemoveOp(worldKey + i));
            }
        }
        for (int i = 0; i < polygons.size(); i++) {
            final String polyId = worldKey + i;
            final Polygon polygon = polygons.get(i);

            final Shape shape = new Shape(pointsToVecs(polygon.getPoints()));
            markerOps.add(newPolyOp(polyId, shape, markerOptions));
        }

        if (!markerOps.isEmpty())
            markerProcessor.queueMarkerOps(markerOps);

        multiPolys.put(markerKey, polygons.size());
        markerIds.add(markerKey);
    }

    @Override
    public void addIconMarker(@NotNull String markerKey, @NotNull String iconKey, @NotNull Point2D iconLoc, int sizeX, int sizeY, @NotNull MarkerOptions markerOptions) {
        // Use a POI Marker
        // POI Markers have no descriptions
        final String worldKey = toWorldKey(markerKey);
        markerProcessor.queueMarkerOp(new IconMarkerOp(layerKey, worldKey, iconKey, worldIdentifier, zIndex,
                iconLoc, markerOptions, sizeX, sizeY));
        markerIds.add(markerKey);
    }

    @Override
    public boolean hasMarker(@NotNull String markerKey) {
        // May not be the most accurate, but it's the best option w/o having to delay the result.
        return markerIds.contains(markerKey);
    }

    @Override
    public boolean removeMarker(@NotNull String markerKey) {
        final String worldKey = toWorldKey(markerKey);
        // Remove all multipolygon representations
        if (multiPolys.containsKey(markerKey)) {
            List<MarkerOp> removeOps = new ArrayList<>();
            int polySize = multiPolys.get(markerKey);
            for (int i = 0; i < polySize; ++i) {
                removeOps.add(newRemoveOp(worldKey + i));
            }

            markerProcessor.queueMarkerOps(removeOps);

            multiPolys.remove(markerKey);
            markerIds.remove(markerKey);
            return true;
        }

        markerProcessor.queueMarkerOp(newRemoveOp(worldKey));
        return markerIds.remove(markerKey);
    }

    @Override
    public void removeMarkers(@NotNull Predicate<String> markerKeyFilter) {
        markerProcessor.queueMarkerOp(new RemoveMarkersOp(layerKey, markerKeyFilter));
    }

    @Override
    public @NotNull CompletableFuture<MarkerOptions> getMarkerOptions(@NotNull String markerKey) {
        if (!markerIds.contains(markerKey)) {
            return CompletableFuture.completedFuture(null);
        }

        final String worldKey = toWorldKey(markerKey);
        final CompletableFuture<MarkerOptions> future = new CompletableFuture<>();

        markerProcessor.queueMarkerOp(new FetchMarkerOptionsOp(layerKey, worldKey, future::complete));

        return future;
    }

    @Override
    public void setMarkerOptions(@NotNull String markerKey, @NotNull MarkerOptions markerOptions) {
        if (!markerIds.contains(markerKey))
            return;
        final String worldKey = toWorldKey(markerKey);

        markerProcessor.queueMarkerOp(new SetMarkerOptionsOp(layerKey, worldKey, markerOptions));
    }
}
