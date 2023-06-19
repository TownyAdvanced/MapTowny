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
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Event called when a town is about to be rendered on the web-map for a specific world.
 *
 * Cancelling this event will prevent this town from being rendered in the world on the Pl3xMap.
 * This includes not rendering any homeblock or outpost icons for the town in the world.
 *
 * This event is called from an asynchronous context!
 */
public class WorldRenderTownEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final String worldName;
    private final String townName;
    private final UUID townUUID;
    private final List<Polygon> polygons;
    private final MarkerOptions.Builder markerOptionsBuilder;

    private String homeBlockIconKey;

    private String outpostIconKey;

    private boolean cancelled = false;

    public WorldRenderTownEvent(String worldName, String townName, UUID townUUID, String homeBlockIconKey, String outpostIconKey,
                                List<Polygon> polys, MarkerOptions.Builder markerOptionsBuilder) {
        super(!Bukkit.isPrimaryThread());
        this.worldName = worldName;
        this.townName = townName;
        this.townUUID = townUUID;
        this.homeBlockIconKey = homeBlockIconKey;
        this.outpostIconKey = outpostIconKey;
        this.polygons = polys;
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
     * NOTE: This list is directly modifiable.
     *
     * @return list of polygons that make up town outline.
     */
    @NotNull
    public List<Polygon> getMultiPolygon() {
        return polygons;
    }

    /**
     * Get the marker options for the town outline.
     *
     * NOTE: These options are directly modifiable.
     *
     * @return marker options for the town outline.
     */
    @NotNull
    public MarkerOptions.Builder getMarkerOptions() {
        return markerOptionsBuilder;
    }

    /**
     * Get the icon key for the homeblock.
     * This may vary depending on if the homeblock belongs to a capital nation or not.
     *
     * @return the icon key for the town's homeblock.
     *
     * @since 2.2.0
     */
    @NotNull
    public String getHomeBlockIconKey() {
        return homeBlockIconKey;
    }

    /**
     * Set the icon key for the homeblock icon.
     * If the icon key is not valid, the respective icon will not be rendered.
     *
     * @param homeBlockIconKey Icon key for the town's homeblock.
     *
     * @since 2.2.0
     */
    public void setHomeBlockIconKey(@NotNull String homeBlockIconKey) {
        this.homeBlockIconKey = homeBlockIconKey;
    }

    /**
     * Get the icon key for the outpost icon.
     *
     * @return the icon key for the town's outposts.
     *
     * @since 2.2.0
     */
    @NotNull
    public String getOutpostIconKey() {
        return outpostIconKey;
    }

    /**
     * Set the icon key for the outpost icon.
     * If the icon key is not valid, the respective icon will not be rendered.
     *
     * @param outpostIconKey icon key for the town's outposts.
     *
     * @since 2.2.0
     */
    public void setOutpostIconKey(@NotNull String outpostIconKey) {
        this.outpostIconKey = outpostIconKey;
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
