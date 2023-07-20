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

package me.silverwolfg11.maptowny.events;

import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.objects.PolygonGroup;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Event called when a town is about to be rendered on the web-map for a specific world.
 *
 * Cancelling this event will prevent this town from being rendered in the world on the Pl3xMap.
 * This includes not rendering any homeblock or outpost icons for the town in the world.
 */
public class WorldRenderTownEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final String worldName;
    private final String townName;
    private final UUID townUUID;
    private final List<PolygonGroup> polygonGroups;
    private final MarkerOptions.Builder markerOptionsBuilder;

    private boolean cancelled = false;

    public WorldRenderTownEvent(String worldName, String townName, UUID townUUID, List<PolygonGroup> polyGroups, MarkerOptions.Builder markerOptionsBuilder) {
        super(!Bukkit.isPrimaryThread());
        this.worldName = worldName;
        this.townName = townName;
        this.townUUID = townUUID;
        this.polygonGroups = polyGroups;
        this.markerOptionsBuilder = markerOptionsBuilder;
    }

    /**
     * Get the name of the town being rendered.
     *
     * @return name of town being rendered.
     */
    @NotNull
    public String getTownName() {
        return townName;
    }

    /**
     * Get the UUID of the town being rendered.
     *
     * @return uuid of town being rendered.
     */
    @NotNull
    public UUID getTownUUID() {
        return townUUID;
    }

    /**
     * Get the name of the world where the town is being rendered.
     *
     * @return name of world.
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }

    /**
     * Get the list of polygons that make up the outline of the town being rendered.
     *
     * NOTE: Modifying this list will have <b>no effect</b>.
     *
     * @return list of polygons that make up town outline.
     *
     * @deprecated Use {@link WorldRenderTownEvent#getPolygonGroups} instead. Slated for removal in 4.0.0.
     */
    @NotNull
    @Deprecated
    @Unmodifiable
    public List<Polygon> getMultiPolygon() {
        List<Polygon> polygons = new ArrayList<>();
        for (PolygonGroup polygonGroup : polygonGroups) {
            polygons.addAll(polygonGroup.getPolygons());
        }

        return polygons;
    }

    /**
     * Get the list of polygon groups that make up the outline of the town being rendered.
     *
     * NOTE: This list is directly modifiable.
     *
     * @return list of polygon groups that make up town outline.
     *
     * @since 3.0.0
     */
    @NotNull
    public List<PolygonGroup> getPolygonGroups() {
        return polygonGroups;
    }

    /**
     * Get the default marker options for the town outline.
     * {@link PolygonGroup}s are allowed to modify the marker options before rendering the final result.
     * <br><br>
     * NOTE: These options are directly modifiable.
     *
     * @return marker options for the town outline.
     */
    @NotNull
    public MarkerOptions.Builder getMarkerOptions() {
        return markerOptionsBuilder;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
