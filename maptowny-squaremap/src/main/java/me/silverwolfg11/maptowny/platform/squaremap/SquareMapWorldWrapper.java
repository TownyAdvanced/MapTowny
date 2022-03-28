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

package me.silverwolfg11.maptowny.platform.squaremap;

import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapWorld;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;

public class SquareMapWorldWrapper implements MapWorld {
    private final xyz.jpenilla.squaremap.api.MapWorld mapWorld;

    private SquareMapWorldWrapper(@NotNull xyz.jpenilla.squaremap.api.MapWorld sqMapWorld) {
        this.mapWorld = sqMapWorld;
    }

    public static SquareMapWorldWrapper from(xyz.jpenilla.squaremap.api.MapWorld sqMapWorld) {
        return new SquareMapWorldWrapper(sqMapWorld);
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        SimpleLayerProvider layerProvider = SimpleLayerProvider.builder(options.getName())
                .defaultHidden(options.isDefaultHidden())
                .layerPriority(options.getLayerPriority())
                .zIndex(options.getZIndex())
                .showControls(options.showControls())
                .build();

        mapWorld.layerRegistry().register(Key.of(layerKey), layerProvider);
        return SquareMapLayerWrapper.from(layerProvider);
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        mapWorld.layerRegistry().unregister(Key.of(layerKey));
    }
}
