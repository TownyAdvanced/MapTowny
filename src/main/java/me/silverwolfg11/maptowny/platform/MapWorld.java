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

import me.silverwolfg11.maptowny.objects.LayerOptions;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for a specific world on a web-map.
 *
 * @since 2.0.0
 */
public interface MapWorld {

    /**
     * Register a specific layer on top of the world.
     *
     * @param layerKey Unique layer key to associate with the layer.
     * @param options Options on how the layer should be set up.
     * @return a layer wrapper to the specific layer.
     */
    @NotNull
    MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options);

    /**
     * Unregister a layer associated with the layer key.
     *
     * @param layerKey Unique layer key.
     */
    void unregisterLayer(@NotNull String layerKey);

}
