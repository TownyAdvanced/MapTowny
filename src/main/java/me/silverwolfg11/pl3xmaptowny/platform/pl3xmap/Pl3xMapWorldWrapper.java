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

package me.silverwolfg11.pl3xmaptowny.platform.pl3xmap;

import me.silverwolfg11.pl3xmaptowny.objects.LayerOptions;
import me.silverwolfg11.pl3xmaptowny.platform.MapLayer;
import me.silverwolfg11.pl3xmaptowny.platform.MapWorld;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.SimpleLayerProvider;
import org.jetbrains.annotations.NotNull;

public class Pl3xMapWorldWrapper implements MapWorld {
    private final net.pl3x.map.api.MapWorld mapWorld;

    private Pl3xMapWorldWrapper(@NotNull net.pl3x.map.api.MapWorld pMapWorld) {
        this.mapWorld = pMapWorld;
    }

    public static Pl3xMapWorldWrapper from(net.pl3x.map.api.MapWorld pMapWorld) {
        return new Pl3xMapWorldWrapper(pMapWorld);
    }

    @Override
    public @NotNull MapLayer registerLayer(@NotNull String layerKey, @NotNull LayerOptions options) {
        net.pl3x.map.api.SimpleLayerProvider layerProvider = SimpleLayerProvider.builder(options.getName())
                .defaultHidden(options.isDefaultHidden())
                .layerPriority(options.getLayerPriority())
                .zIndex(options.getzIndex())
                .showControls(options.showControls())
                .build();

        mapWorld.layerRegistry().register(Key.of(layerKey), layerProvider);
        return Pl3xMapLayerWrapper.from(layerProvider);
    }

    @Override
    public void unregisterLayer(@NotNull String layerKey) {
        mapWorld.layerRegistry().unregister(Key.of(layerKey));
    }
}
