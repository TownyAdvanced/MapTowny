/*
 * Copyright (c) 2025 Silverwolfg11
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

package me.silverwolfg11.maptowny.objects.groups;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * A grouping strategy that only accepts townblocks
 * of a particular {@link com.palmergames.bukkit.towny.object.TownBlockType}.
 *
 * The strategy applies custom colors for the townblock type when rendering.
 */
public class TownblockTypeStrategy extends GroupingStrategy {
    private final String townblockTypeName;
    private final Color fillColor;
    private final Color strokeColor;

    public TownblockTypeStrategy(String townblockTypeName, Color fillColor, Color strokeColor) {
        this.townblockTypeName = townblockTypeName;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
    }

    @Override
    public boolean accept(@NotNull TownBlock townblock) {
        final TownBlockType type = townblock.getType();
        return (type != null) && (type.getName().equalsIgnoreCase(townblockTypeName));
    }

    @Override
    public @NotNull Consumer<MarkerOptions.Builder> postGroupingStyling() {
        return builder -> {
            // Apply townblock type colors if they are set
            if (fillColor != null) {
                builder.fillColor(fillColor);
            }
            if (strokeColor != null) {
                builder.strokeColor(strokeColor);
            }
        };
    }
}
