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

package me.silverwolfg11.maptowny.objects.groups;

import com.palmergames.bukkit.towny.object.TownBlock;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.objects.PolygonGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;

// TBGroups that group by townblock type
// Each townblock type may have a special fill and stroke color.
public class TBTypeTBGroup extends TBGroup {
    private final String typeName;
    private final Color fillColor;
    private final Color strokeColor;

    public TBTypeTBGroup(@NotNull String tbTypeName,
                         @Nullable Color fillColor, @Nullable Color strokeColor) {
        this.typeName = tbTypeName;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
    }

    protected void copyInternals(TBTypeTBGroup clone) {
        super.copyInternals(clone);
    }

    @Override
    public boolean canAddTownBlock(TownBlock tb) {
        return tb.getType() != null && tb.getType().getName().equalsIgnoreCase(typeName);
    }

    @Override
    public PolygonGroup buildPolygonGroup(List<Polygon> polygons) {
        return new ColorPolygonGroup(polygons, strokeColor, fillColor);
    }

    @Override
    public TBTypeTBGroup clone() {
        TBTypeTBGroup clone = new TBTypeTBGroup(typeName, fillColor, strokeColor);
        copyInternals(clone);
        return clone;
    }
}
