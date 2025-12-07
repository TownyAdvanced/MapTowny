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
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A grouping strategy is used to determine
 * how to form {@link TBGroup}. Particularly, it defines
 * the criteria for accepting townblocks into a group.
 * Every {@link TBGroup} will be associated with a grouping strategy.
 */
public abstract class GroupingStrategy {
    /**
     * Whether to accept a townblock into the townblock group.
     * @param townblock Townblock under evaluation.
     * @return true if townblock was accepted to the group, false otherwise.
     */
    public abstract boolean accept(@NotNull TownBlock townblock);


    /**
     * Provide a post-grouping styling function that can modify
     * styling of the resulting {@link me.silverwolfg11.maptowny.objects.PolygonGroup}
     * after default styling is provided.
     */
    @NotNull
    public abstract Consumer<MarkerOptions.Builder> postGroupingStyling();

    /**
     * A default grouping strategy that accepts
     * all townblocks.
     *
     * @return a default strategy.
     */
    @NotNull
    public static GroupingStrategy defaultStrategy() {
        return new GroupingStrategy() {
            @Override
            public boolean accept(@NotNull TownBlock townblock) {
                return true;
            }

            @Override
            public @NotNull Consumer<MarkerOptions.Builder> postGroupingStyling() {
                return builder -> {}; // No-op styling function
            }
        };
    }
}
