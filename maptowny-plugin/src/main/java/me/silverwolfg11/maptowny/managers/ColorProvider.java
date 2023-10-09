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
import me.silverwolfg11.maptowny.objects.MapConfig;
import me.silverwolfg11.maptowny.objects.groups.TBGroup;
import me.silverwolfg11.maptowny.objects.groups.TBTypeTBGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// Provide the colors and sources for a town's fill and stroke.
public class ColorProvider {
    private final List<ColorSource> fillSources;
    private final List<ColorSource> strokeSources;
    private final Map<String, ColorGroup> tbColors;

    private final ColorGroup defaultGroup;
    private final Logger pluginLogger;

    public ColorProvider(Logger pluginLogger, MapConfig config) {
        this.pluginLogger = pluginLogger;

        // Ensure uniqueness of sources
        this.fillSources = uniqueList(config.getFillColorPriorities());
        this.strokeSources = uniqueList(config.getStrokeColorPriorities());

        this.defaultGroup = ColorGroup.of(config.getDefaultFillColor(),
                                          config.getDefaultStrokeColor());

        this.tbColors = setupTypeMap(config);
    }

    private Map<String, ColorGroup> setupTypeMap(MapConfig config) {
        // Check if needed
        if (fillSources.stream().noneMatch(c -> c == ColorSource.TOWNBLOCK_TYPE) &&
                strokeSources.stream().noneMatch(c -> c == ColorSource.TOWNBLOCK_TYPE)) {
            return Collections.emptyMap();
        }

        Map<String, ColorGroup> colorMap = new HashMap<>();

        for (String tbType : config.getConfigTownBlockTypeNames()) {
            final Color fillColor = config.getFillColor(tbType);
            final Color strokeColor = config.getStrokeColor(tbType);

            if (fillColor != null || strokeColor != null) {
                colorMap.put(tbType, ColorGroup.of(fillColor, strokeColor));
            }
        }

        return colorMap;
    }

    public TownColorSource getTownColorSource(Town town) {
        boolean useTBColorFill = false;
        Color fillColor = null;

        for (final ColorSource source : this.fillSources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                useTBColorFill = true;
                continue;
            }

            final Color decidedColor = fetchColorFromTown(source, town);

            if (decidedColor != null) {
                fillColor = decidedColor;
                break;
            }
        }

        if (fillColor == null)
            fillColor = defaultGroup.fillColor;

        boolean useTBColorStroke = false;
        Color strokeColor = null;

        for (final ColorSource source : this.strokeSources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                useTBColorStroke = true;
                continue;
            }

            final Color decidedColor = fetchColorFromTown(source, town);

            if (decidedColor != null) {
                strokeColor = decidedColor;
                break;
            }
        }

        if (strokeColor == null)
            strokeColor = defaultGroup.strokeColor;

        return new TownColorSource(ColorGroup.of(fillColor, strokeColor),
                                   getDynamicColorGroups(useTBColorFill, useTBColorStroke));
    }

    private List<TBGroup> getDynamicColorGroups(boolean useTBFill, boolean useTBStroke) {
        if (!useTBFill && !useTBStroke) {
            return Collections.emptyList();
        }

        List<TBGroup> dynamicGroups = new ArrayList<>(tbColors.size());
        for (Map.Entry<String, ColorGroup> entry : tbColors.entrySet()) {
            final Color fillColor = useTBFill ? entry.getValue().fillColor : null;
            final Color strokeColor = useTBStroke ? entry.getValue().strokeColor : null;
            dynamicGroups.add(new TBTypeTBGroup(entry.getKey(), fillColor, strokeColor));
        }

        return dynamicGroups;
    }

    @Nullable
    private Color fetchColorFromTown(ColorSource source, Town town) {
        if (source == ColorSource.TOWN) {
            return getTownColor(town);
        } else if (source == ColorSource.NATION) {
            return getNationColor(town);
        }

        return null;
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

    // Create an ordered list guaranteeing unique elements
    // Duplicate elements in the original list are not inserted.
    private static <T> List<T> uniqueList(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    public static class ColorGroup {
        @Nullable
        public final Color fillColor, strokeColor;

        private ColorGroup(@Nullable Color fillColor,
                           @Nullable Color strokeColor) {
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
        }

        private static ColorGroup of(@Nullable Color fillColor,
                                     @Nullable Color strokeColor) {
            return new ColorGroup(fillColor, strokeColor);
        }
    }

    public static class TownColorSource {

        @NotNull
        public final ColorGroup defaultColors;
        @NotNull
        public final List<TBGroup> dynamicColorGroups;

        private TownColorSource(@NotNull ColorGroup defaultGroup,
                                @Nullable List<TBGroup> dynamicColorGroups) {
            this.defaultColors = defaultGroup;

            if (dynamicColorGroups == null || dynamicColorGroups.isEmpty()) {
                this.dynamicColorGroups = new ArrayList<>();
            }
            else {
                this.dynamicColorGroups = dynamicColorGroups;
            }
        }
    }
}
