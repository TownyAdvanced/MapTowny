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

package me.silverwolfg11.maptowny.platform;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Simple interface to connect to an arbitrary web-map plugin.
 *
 * @since 2.0.0
 */
public interface MapPlatform {
    /**
     * Get the platform name which is most likely the name of the web-map plugin.
     *
     * @return platform name.
     */
    @NotNull String getPlatformName();

    /**
     * Execute a callback the first time the platform's API is initialized.
     * <br><br>
     * Callback Behavior:
     * <ul>
     *  <li> Platforms already initialized will execute the callback immediately on the calling thread.
     *  <li> Platforms may execute the callback asynchronously.
     * </ul>
     *
     * @param callback Callback to execute
     * @deprecated 3.0.0+ - Use {@link MapPlatformObserver} instead.
     */
    default void onFirstInitialize(Runnable callback) {
        callback.run();
    }

    /**
     * Execute a callback when the platform's API has initialized.
     * <br><br>
     * Callback Behavior:
     * <ul>
     *  <li> Platforms that are already initialized will execute the callback immediately on the calling thread.
     *  <li> Platforms may execute the callbacks asynchronously.
     *  <li> Platforms may execute the callback again if the platform is reloaded.
     * </ul>
     *
     * @param callback Callback to execute.
     * @deprecated 3.0.0+ - Use {@link MapPlatformObserver} instead.
     */
    default void onInitialize(Runnable callback) {
        callback.run();
    }

    /**
     * Register an observer to listen to platform events.
     * Registering an already registered observer has no effect
     * and will report as a registration failure.
     *
     * @return if registration was successful.
     * @since 3.0.0
     */
    boolean registerObserver(@NotNull MapPlatformObserver observer);

    /**
     * Unregister an observer.
     * @return if unregistering was successful.
     * @since 3.0.0
     */
    boolean unregisterObserver(@NotNull MapPlatformObserver observer);

    /**
     * Check if the map plugin renders the specific world.
     *
     * @param world Bukkit world to check.
     * @return whether the world is enabled to rendered.
     */
    boolean isWorldEnabled(@NotNull World world);

    /**
     * Get a world wrapper associated by a specific platform.
     * @param world Bukkit world
     * @return MapWorld associated with the Bukkit world.
     */
    @Nullable
    MapWorld getWorld(@NotNull World world);

    /**
     * Check if the platform uses segmented polygons in rendering claim markers.
     * See {@link me.silverwolfg11.maptowny.objects.Polygon} for more info.
     *
     * @return {@code true} if the platform uses and handles segmented polygons, {@code false} otherwise.
     *
     * @since 3.0.0
     */
    boolean usesSegmentedPolygons();

    // Icon Related Functions

    /**
     * Associate a specific icon key with an image.
     *
     * NOTE: Some platforms may only accept specific image types and/or
     * may not abide by the image size parameters.
     *
     * @param iconKey Unique string specific to the image icon.
     * @param icon Image to associate with.
     * @param height Height of the image (in pixels).
     * @param width Width of the image (in pixels).
     */
    void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width);

    /**
     * Check if the platform has an icon matching the specific string.
     *
     * @param iconKey unique icon string.
     * @return if platform has an icon associated with the icon key.
     */
    boolean hasIcon(@NotNull String iconKey);

    /**
     * Unregister an icon.
     *
     * @param iconKey Unique string representing the icon key.
     * @return whether the platform successfully unregistered an icon matching a string.
     *         If no icon was associated with the icon string, then this will return false.
     */
    boolean unregisterIcon(@NotNull String iconKey);

    /**
     * Indicate to the platform that it should switch to shutdown mode.
     *
     * The platform will still be able to process all operations during this period.
     * @deprecated 3.0.0 - Platform cleanup does not need to be split into two phases.
     * 
     */
    default void startShutdown() {
    }

    /**
     * Runs any platform-dependent cleanup code when the MapTowny plugin is disabling.
     *
     * After this method is executed, there is no guarantee that the platform will be able to process any operations.
     */
    default void shutdown() {
    }

}
