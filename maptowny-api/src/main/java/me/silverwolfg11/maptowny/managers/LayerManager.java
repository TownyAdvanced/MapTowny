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

package me.silverwolfg11.maptowny.managers;

import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.maptowny.platform.MapLayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abstract layer manager that renders towns on the web-map.
 *
 * @since 2.0.0
 */
public interface LayerManager {

    /**
     * Render a single town on the web-map.
     * Do not use this method to render multiple towns as it will impact server performance.
     *
     * This method is thread-safe.
     *
     * @param town town to render.
     */
    void renderTown(@NotNull Town town);

    /**
     * Render multiple towns on the web-map.
     *
     * This method is thread-safe.
     *
     * @param towns towns to render.
     */
    void renderTowns(@NotNull Collection<Town> towns);

    /**
     * Remove a town from the web-map.
     *
     * @param town town to remove.
     * @return whether the town was successfully un-rendered from the web-map.
     */
    boolean removeTownMarker(@NotNull Town town);

    /**
     * Remove a town from the web-map.
     *
     * @param townUUID uuid of the town to remove.
     * @return whether the town was successfully un-rendered from the web-map.
     */
    boolean removeTownMarker(@NotNull UUID townUUID);

    /**
     * Get the {@link MapLayer} that the MapTowny plugin uses for a specific world.
     *
     * @param worldName Name of the world.
     * @return the provider for the world if there is one, or {@code null} if there is not.
     */
    MapLayer getTownyWorldLayerProvider(@NotNull String worldName);

    /**
     * Register a replacement for the town tooltips (both the click and hover tooltips).
     *
     * Note: Replacements are not persistent through plugin reload.
     * Must be registered again everytime the plugin is reloaded.
     *
     * @param key The specific string to be replaced.
     * @param function The function to produce a valid replacement.
     */
    void registerReplacement(@NotNull String key, @NotNull Function<Town, String> function);

    /**
     * Unregister a replacement for the town tooltips.
     *
     * @param key Replacement string.
     */
    void unregisterReplacement(@NotNull String key);

}
