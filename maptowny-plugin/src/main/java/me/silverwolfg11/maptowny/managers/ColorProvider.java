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
import me.silverwolfg11.maptowny.objects.groups.TownblockTypeStrategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
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
    private final List<TownblockTypeStrategy> tbColorGroupingStrats;

    private final ColorGroup defaultGroup;
    private final Logger pluginLogger;

    public ColorProvider(Logger pluginLogger, MapConfig config) {
        this.pluginLogger = pluginLogger;

        // Ensure uniqueness of sources
        this.fillSources = uniqueList(config.getFillColorPriorities());
        this.strokeSources = uniqueList(config.getStrokeColorPriorities());

        this.defaultGroup = ColorGroup.of(config.getDefaultFillColor(),
                config.getDefaultStrokeColor());

        // Map townblock type -> color group
        Map<String, ColorGroup> tbColors = populateTypeMap(config);
        this.tbColorGroupingStrats = populateGroupingStrats(tbColors);
    }

    public TownColoring getTownColorSource(Town town) {
        Pair<Color, Boolean> fillResult = resolveColor(fillSources, town);
        Pair<Color, Boolean> strokeResult = resolveColor(strokeSources, town);

        Color finalFillColor = fillResult.left() != null ? fillResult.left() : defaultGroup.fillColor;
        Color finalStrokeColor = strokeResult.left() != null ? strokeResult.left() : defaultGroup.strokeColor;

        return new TownColoring(
                ColorGroup.of(finalFillColor, finalStrokeColor),
                fillResult.right(),
                strokeResult.right());
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
                            ColorGroup.of(fillColor, strokeColor));
                })
                .filter(entry -> entry.getValue().fillColor != null ||
                        entry.getValue().strokeColor != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    // Generate townblock type grouping strategies
    private List<TownblockTypeStrategy> populateGroupingStrats(Map<String, ColorGroup> tbColors) {
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

    // Identify two things:
    // - Whether the town should be colored with townblock type coloring
    // - Colors for the town stroke, fill
    private Pair<Color, Boolean> resolveColor(List<ColorSource> sources, Town town) {
        boolean usesTownblockType = false;
        Color resolvedColor = null;

        for (ColorSource source : sources) {
            if (source == ColorSource.TOWNBLOCK_TYPE) {
                usesTownblockType = true;
                // Fall-through for townblock type to
                // get the next color source in case
                // the town-block type colors are missing / not set.
                continue;
            }

            Color color = null;
            if (source == ColorSource.TOWN) {
                color = town.getMapColor();
            } else if (source == ColorSource.NATION && town.hasNation()) {
                color = town.getNationOrNull().getMapColor();
            }

            if (color != null) {
                resolvedColor = color;
                break;
            }
        }

        return Pair.pair(resolvedColor, usesTownblockType);
    }

    // Get the townblock grouping strategies based on the townblock types
    // that are configured to have separate colors.
    public List<TownblockTypeStrategy> getTownblockTypeStrategies() {
        return tbColorGroupingStrats;
    }

    // Modifies townblock grouping strategies to handle partial coloring.
    // i.e. if a town uses townblock type coloring for a single color source (e.g fill), but not both sources (e.g. stroke & fill).
    public List<TownblockTypeStrategy> getTownblockTypeStrategies(TownColoring townColoring) {
        if (townColoring.usesTownblockFillColors() && townColoring.usesTownblockStrokeColors()) {
            return getTownblockTypeStrategies();
        } else if (!townColoring.usesTownblockColors()) {
            return Collections.emptyList();
        }

        var baseStrategies = getTownblockTypeStrategies();
        List<TownblockTypeStrategy> strategies = new ArrayList<>(baseStrategies.size());
        // Partial coloring
        for (var tbStrategy : getTownblockTypeStrategies()) {
            strategies.add(new TownblockTypeStrategy(
                    tbStrategy.getTownblockTypeName(),
                    townColoring.usesTownblockFillColors() ? tbStrategy.getFillColor() : null,
                    townColoring.usesTownblockStrokeColors() ? tbStrategy.getStrokeColor() : null
            ));
        }

        return strategies;
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
            boolean usesTownblockStrokeColors) {
        public boolean usesTownblockColors() {
            return usesTownblockFillColors || usesTownblockStrokeColors;
        }
    }
}
