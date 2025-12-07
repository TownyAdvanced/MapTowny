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
import com.palmergames.util.Pair;
import me.silverwolfg11.maptowny.objects.ColorSource;
import me.silverwolfg11.maptowny.objects.MapConfig;
import me.silverwolfg11.maptowny.objects.groups.GroupingStrategy;
import me.silverwolfg11.maptowny.objects.groups.TBGroup;
import me.silverwolfg11.maptowny.objects.groups.TownblockTypeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Provide the colors and sources for a town's fill and stroke.
public class ColorProvider {
    private final List<ColorSource> fillSources;
    private final List<ColorSource> strokeSources;
    private final Map<String, ColorGroup> tbColors;
    private final List<GroupingStrategy> tbColorGroupingStrats;

    private final ColorGroup defaultGroup;
    private final Logger pluginLogger;

    public ColorProvider(Logger pluginLogger, MapConfig config) {
        this.pluginLogger = pluginLogger;

        // Ensure uniqueness of sources
        this.fillSources = uniqueList(config.getFillColorPriorities());
        this.strokeSources = uniqueList(config.getStrokeColorPriorities());

        this.defaultGroup = ColorGroup.of(config.getDefaultFillColor(),
                config.getDefaultStrokeColor());

        this.tbColors = populateTypeMap(config);
        this.tbColorGroupingStrats = populateGroupingStrats(this.tbColors);
    }

    private Map<String, ColorGroup> populateTypeMap(MapConfig config) {
        // Check if needed
        if (!config.clusterByTownBlockType()) {
            return Collections.emptyMap();
        }

        // Map townblock type name -> associated fill and stroke color
        return config.getConfigTownBlockTypeNames()
                .stream()
                .map(tbType -> {
                    final Color fillColor = config.getFillColor(tbType);
                    final Color strokeColor = config.getStrokeColor(tbType);
                    return new AbstractMap.SimpleEntry<>(
                            tbType,
                            ColorGroup.of(fillColor, strokeColor)
                    );
                })
                .filter(entry -> entry.getValue().fillColor != null ||
                        entry.getValue().strokeColor != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    // Generate townblock type grouping strategies
    private List<GroupingStrategy> populateGroupingStrats(Map<String, ColorGroup> tbColors) {
        if (tbColors.isEmpty()) {
            return Collections.emptyList();
        }

        return tbColors.entrySet().stream()
                .map(entry -> {
                    String tbTypeName = entry.getKey();
                    ColorGroup colors = entry.getValue();
                    return new TownblockTypeStrategy(tbTypeName, colors.fillColor, colors.strokeColor);
                })
                .collect(Collectors.toUnmodifiableList());
    }

    public TownColoring getTownColorSource(Town town) {
        Pair<Color, Boolean> fillResult = resolveColor(fillSources, town);
        Pair<Color, Boolean> strokeResult = resolveColor(strokeSources, town);

        Color finalFillColor = fillResult.left() != null ?
                fillResult.left() : defaultGroup.fillColor;
        Color finalStrokeColor = strokeResult.left() != null ?
                strokeResult.left() : defaultGroup.strokeColor;

        return new TownColoring(
                ColorGroup.of(finalFillColor, finalStrokeColor),
                fillResult.right(),
                strokeResult.right()
        );
    }

    // Identify two things:
    // - Whether the town should be colored with townblock type coloring
    // - Colors for the town stroke, fill
    private Pair<Color, Boolean> resolveColor(List<ColorSource> sources, Town town) {
        boolean usesTownblockType = false;
        Color resolvedColor = null;

        for (ColorSource source : sources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                usesTownblockType = true;
                continue;
            }

            Color color = fetchColorFromTown(source, town);
            if (color != null) {
                resolvedColor = color;
                break;
            }
        }

        return Pair.pair(resolvedColor, usesTownblockType);
    }

    // Get the townblock grouping strategies based on the townblock types
    // that are configured to have separate colors.
    public List<GroupingStrategy> getTownblockTypeStrategies() {
        return tbColorGroupingStrats;
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

    public record TownColoring(
            @NotNull ColorGroup colors,
            boolean usesTownblockFillColors,
            boolean usesTownblockStrokeColors
    ) {
        public boolean usesTownblockColors() {
            return usesTownblockFillColors || usesTownblockStrokeColors;
        }
    }
}
