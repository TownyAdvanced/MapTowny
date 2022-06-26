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
     * Execute a callback when the platform's API has initialized.
     *
     * Platforms that are already initialized will execute the callback immediately on the calling thread.
     * Platforms may execute the callbacks asynchronously.
     *
     * @param callback Callback to execute.
     */
    default void onInitialize(Runnable callback) {
        callback.run();
    }

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
