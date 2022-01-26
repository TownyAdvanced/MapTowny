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

package me.silverwolfg11.maptowny.platform.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// A simple mapper that maps the icon keys to bluemap addresses which are required by bluemap to render icons.
public class BlueMapIconMapper {

    private final Logger errorLogger;
    // Icon Key -> BlueMap Address
    private final Map<String, String> keyPaths = new ConcurrentHashMap<>();

    public BlueMapIconMapper(Logger errorLogger) {
        this.errorLogger = errorLogger;
    }

    public void registerIcon(String iconKey, BufferedImage img) {
        BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
        if (api == null)
            return;

        try {
            String imgPath = api.createImage(img, iconKey);
            keyPaths.put(iconKey, imgPath);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, String.format("Error creating image for icon keyed '%s'!", iconKey), e);
        }
    }

    public boolean unregisterIcon(String iconKey) {
        return keyPaths.remove(iconKey) != null;
    }

    public boolean isRegistered(String iconKey) {
        return keyPaths.containsKey(iconKey);
    }

    @Nullable
    public String getBlueMapAddress(String iconKey) {
        return keyPaths.get(iconKey);
    }

}
