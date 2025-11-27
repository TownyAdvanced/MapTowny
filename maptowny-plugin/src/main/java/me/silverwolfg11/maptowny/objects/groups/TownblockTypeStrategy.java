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
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A grouping strategy that only accepts townblocks
 * of a particular {@link com.palmergames.bukkit.towny.object.TownBlockType}.
 *
 * The group data contains the townblock type name.
 */
public class TownblockTypeStrategy extends GroupingStrategy {
    private final String dataKey;
    private final String townblockTypeName;

    public TownblockTypeStrategy(String dataKey, String townblockTypeName) {
        this.dataKey = dataKey;
        this.townblockTypeName = townblockTypeName;
    }

    @Override
    public boolean accept(@NotNull TownBlock townblock) {
        final TownBlockType type = townblock.getType();
        return (type != null) && (type.getName().equalsIgnoreCase(townblockTypeName));
    }

    @Override
    public @NotNull Map<String, Object> provideGroupData() {
        return Map.of(dataKey, townblockTypeName);
    }
}
