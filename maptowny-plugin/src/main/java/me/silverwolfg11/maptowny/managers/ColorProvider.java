/*
 * Copyright (c) 2023 Silverwolfg11
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
import me.silverwolfg11.maptowny.objects.ColorSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

// Provide the colors and sources for a town's fill and stroke.
public class ColorProvider {
    private final List<ColorSource> fillSources;
    private final List<ColorSource> strokeSources;

    private final Color defaultFillColor, defaultStrokeColor;
    private final Logger pluginLogger;

    public ColorProvider(Logger pluginLogger,
                         List<ColorSource> fillSources, List<ColorSource> strokeSources,
                         Color defaultFill, Color defaultStroke) {
        this.pluginLogger = pluginLogger;

        // Ensure uniqueness of sources
        this.fillSources = new ArrayList<>(new LinkedHashSet<>(fillSources));
        this.strokeSources = new ArrayList<>(new LinkedHashSet<>(strokeSources));

        this.defaultFillColor = defaultFill;
        this.defaultStrokeColor = defaultStroke;
    }

    @Nullable
    private Color decideColorFromTown(ColorSource source, Town town) {
        if (source == ColorSource.TOWN) {
            return getTownColor(town);
        } else if (source == ColorSource.NATION) {
            return getNationColor(town);
        }

        return null;
    }

    public TownColorSource getTownColorSource(Town town) {
        boolean useTBColorFill = false;
        Color fillColor = null;

        for (final ColorSource source : this.fillSources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                useTBColorFill = true;
                continue;
            }

            final Color decidedColor = decideColorFromTown(source, town);

            if (decidedColor != null) {
                fillColor = decidedColor;
                break;
            }
        }

        if (fillColor == null)
            fillColor = defaultFillColor;

        boolean useTBColorStroke = false;
        Color strokeColor = null;

        for (final ColorSource source : this.strokeSources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                useTBColorStroke = true;
                continue;
            }

            final Color decidedColor = decideColorFromTown(source, town);

            if (decidedColor != null) {
                strokeColor = decidedColor;
                break;
            }
        }

        if (strokeColor == null)
            strokeColor = defaultStrokeColor;

        return new TownColorSource(useTBColorFill, useTBColorStroke, strokeColor, fillColor);
    }

    // Convert given town hex code to color with error handling.
    @Nullable
    private Color convertTownHexCodeToColor(String hex, Town town) {
        if (!hex.isEmpty()) {
            if (hex.charAt(0) != '#')
                hex = "#" + hex;

            try {
                return Color.decode(hex);
            } catch (NumberFormatException ex) {
                String name = town.getName();
                pluginLogger.warning("Error loading town " + name + "'s map color: " + hex + "!");
            }
        }

        return null;
    }

    // Gets the nation color from a town if:
    // config set to use nation colors and town has a valid nation color.
    @Nullable
    private Color getNationColor(@NotNull Town town) {
        final String hex = town.getNationMapColorHexCode();
        return hex == null ? null : convertTownHexCodeToColor(hex, town);
    }

    // Gets the town color from a town if:
    // config set to use town colors
    @Nullable
    private Color getTownColor(@NotNull Town town) {
        final String hex = town.getMapColorHexCode();
        return hex == null ? null : convertTownHexCodeToColor(hex, town);
    }

    public static class TownColorSource {

        // These booleans indicate whether
        // the townblock type should determine
        // the fill color and/or stroke color.
        public final boolean useTBFill, useTBStroke;
        @Nullable
        public final Color fillColor;

        @Nullable
        public final Color strokeColor;

        private TownColorSource(boolean useTBFill, boolean useTBStroke,
                                @Nullable Color strokeColor,
                                @Nullable Color fillColor) {
            this.useTBFill = useTBFill;
            this.useTBStroke = useTBStroke;
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
        }
    }
}
