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

package me.silverwolfg11.maptowny.platform.dynmap;

import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DynmapPlatform implements MapPlatform {
    private final DynmapAPI dynmapAPI;

    public DynmapPlatform() {
        dynmapAPI = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");
    }


    @Override
    public @NotNull String getPlatformName() {
        return "dynmap";
    }

    @Override
    public boolean isWorldEnabled(@NotNull World world) {
        // TODO How would this even be implemented for Dynmap?
        return true;
    }

    @Override
    public @Nullable MapWorld getWorld(@NotNull World world) {
        return new DynmapWorldWrapper(dynmapAPI, world.getName());
    }

    @Override
    public void registerIcon(@NotNull String iconKey, @NotNull BufferedImage icon, int height, int width) {
        // Delete icon if it's been previously registered
        final MarkerIcon oldMarkerIcon = dynmapAPI.getMarkerAPI().getMarkerIcon(iconKey);
        if (oldMarkerIcon != null) {
            oldMarkerIcon.deleteIcon();
        }

        // Oh dynmap, why can't you just take a BufferedImage directly.'
        // Convert BufferedImage to inputstream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(icon, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        MarkerIcon markerIcon = dynmapAPI.getMarkerAPI().createMarkerIcon(iconKey, iconKey, bais);
        if (markerIcon == null) {
            logError(String.format("Error registering icon '%s' on dynmap!", iconKey));
        }
    }

    @Override
    public boolean hasIcon(@NotNull String iconKey) {
        return dynmapAPI.getMarkerAPI().getMarkerIcon(iconKey) != null;
    }

    @Override
    public boolean unregisterIcon(@NotNull String iconKey) {
        MarkerIcon markerIcon = dynmapAPI.getMarkerAPI().getMarkerIcon(iconKey);
        if (markerIcon != null) {
            markerIcon.deleteIcon();
            return true;
        }

        return false;
    }

    // Logger Accessibility
    static void logError(String errorMsg) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MapTowny");
        if (plugin == null)
            return;

        plugin.getLogger().severe(errorMsg);
    }
}
