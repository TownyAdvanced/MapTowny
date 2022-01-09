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
 * Event called when a town is about to be rendered on the Pl3xMap for a specific world.
 *
 * Cancelling this event will prevent this town from being rendered in the world on the Pl3xMap.
 * This includes not rendering any homeblock or outpost icons for the town in the world.
 */
public class WorldRenderTownEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final String worldName;
    private final String townName;
    private final UUID townUUID;
    private final List<Polygon> polygons;
    private final MarkerOptions.Builder markerOptionsBuilder;

    private boolean cancelled = false;

    public WorldRenderTownEvent(String worldName, String townName, UUID townUUID, List<Polygon> polys, MarkerOptions.Builder markerOptionsBuilder) {
        super(!Bukkit.isPrimaryThread());
        this.worldName = worldName;
        this.townName = townName;
        this.townUUID = townUUID;
        this.polygons = polys;
        this.markerOptionsBuilder = markerOptionsBuilder;
    }

    @NotNull
    public String getTownName() {
        return townName;
    }

    @NotNull
    public UUID getTownUUID() {
        return townUUID;
    }

    @NotNull
    public String getWorldName() {
        return worldName;
    }

    @NotNull
    public List<Polygon> getMultiPolygon() {
        return polygons;
    }

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
