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

package me.silverwolfg11.maptowny.objects;

public class LayerOptions {

    private final String name;
    private final boolean showControls;
    private final boolean defaultHidden;
    private final int layerPriority;
    private final int zIndex;

    public LayerOptions(String name, boolean showControls, boolean defaultHidden, int layerPriority, int zIndex) {
        this.name = name;
        this.showControls = showControls;
        this.defaultHidden = defaultHidden;
        this.layerPriority = layerPriority;
        this.zIndex = zIndex;
    }

    public String getName() {
        return name;
    }

    public boolean showControls() {
        return showControls;
    }

    public boolean isDefaultHidden() {
        return defaultHidden;
    }

    public int getLayerPriority() {
        return layerPriority;
    }

    public int getzIndex() {
        return zIndex;
    }
}
