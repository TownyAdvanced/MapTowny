/*
 * Copyright (c) 2021 Silverwolfg11
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

package me.silverwolfg11.maptowny.objects;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

// Entry for a town that is going to be rendered
// Stores all data needed for async rendering
// All TownRenderEntries are immutable and contain immutable data.
public class TownRenderEntry {

    private final UUID townUUID;
    private final String townName;
    private final boolean nation;
    private final boolean capital;
    private final boolean ruined;

    private final Color nationColor;
    private final Color townColor;

    private final String clickText;
    private final String hoverText;

    private final String homeBlockWorld;
    private final Point2D homeBlockPoint;

    private final Map<String, ? extends Collection<StaticTB>> worldBlocks;
    private final Map<String, List<Point2D>> outpostSpawns;

    public TownRenderEntry(Town town, boolean findOutposts,
                           Color nationColor, Color townColor,
                           String clickText, String hoverText) {
        this.townUUID = town.getUUID();
        this.townName = town.getName();
        this.nation = town.hasNation();
        this.capital = town.isCapital();
        this.ruined = town.isRuined();

        this.clickText = clickText;
        this.hoverText = hoverText;

        this.nationColor = nationColor;
        this.townColor = townColor;

        this.homeBlockWorld = town.getHomeblockWorld().getName();
        this.homeBlockPoint = getHomeblockPoint(town).orElse(null);

        worldBlocks = townblockByWorlds(town);

        outpostSpawns = findOutposts ? getOutpostSpawns(town) : Collections.emptyMap();
    }

    @NotNull
    public UUID getTownUUID() {
        return townUUID;
    }

    @NotNull
    public String getTownName() {
        return townName;
    }

    public boolean hasNation() {
        return nation;
    }

    public boolean isCapital() {
        return capital;
    }

    public boolean isRuined() {
        return ruined;
    }

    @NotNull
    public Optional<Color> getNationColor() {
        return Optional.ofNullable(nationColor);
    }

    @NotNull
    public Optional<Color> getTownColor() {
        return Optional.ofNullable(townColor);
    }

    @NotNull
    public String getClickText() {
        return clickText;
    }

    @NotNull
    public String getHoverText() {
        return hoverText;
    }

    @NotNull
    public String getHomeBlockWorld() {
        return homeBlockWorld;
    }

    @NotNull
    public Optional<Point2D> getHomeBlockPoint() {
        return Optional.of(homeBlockPoint);
    }

    public boolean hasWorldBlocks() {
        return !worldBlocks.isEmpty();
    }

    @NotNull
    public Map<String, ? extends Collection<StaticTB>> getWorldBlocks() {
        return Collections.unmodifiableMap(worldBlocks);
    }

    public boolean hasOutpostSpawns() {
        return !outpostSpawns.isEmpty();
    }

    @NotNull
    public Map<String, List<Point2D>> getOutpostSpawnPoints() {
        return Collections.unmodifiableMap(outpostSpawns);
    }

    @NotNull
    private Optional<Point2D> getHomeblockPoint(Town town) {
        TownBlock homeblock;

        try {
            homeblock = town.getHomeBlock();
        } catch (TownyException e) {
            return Optional.empty();
        }

        int townblockSize = TownySettings.getTownBlockSize();

        int centerOffset = townblockSize / 2;
        int lowerX = homeblock.getX() * townblockSize;
        int lowerZ = homeblock.getZ() * townblockSize;

        return Optional.of(Point2D.of(lowerX + centerOffset, lowerZ + centerOffset));
    }


    @NotNull
    private Map<String, List<Point2D>> getOutpostSpawns(Town town) {
        if (!town.hasOutpostSpawn())
            return Collections.emptyMap();

        return sortByWorld(
                town.getAllOutpostSpawns(), Point2D::of,
                l -> {
                    World world = l.getWorld();

                    if (world == null)
                        return null;

                    return world.getName();
                }
        );
    }

    @NotNull
    private Map<String, ? extends Collection<StaticTB>> townblockByWorlds(Town town) {
        return sortByWorld(
                town.getTownBlocks(), tb -> StaticTB.from(tb.getX(), tb.getZ()),
                tb -> tb.getWorld().getName()
        );
    }

    @NotNull
    private <T, N> Map<String, List<N>> sortByWorld(Collection<T> items, Function<T, N> conversionFunc,
                                                    Function<T, String> worldFetchFunc) {
        if (items.isEmpty())
            return Collections.emptyMap();

        Map<String, List<N>> worldItems = new HashMap<>();

        final Function<String, List<N>> absentFunc = s -> new ArrayList<>();

        List<N> currWorldItems = null;
        String lastWorld = null;

        for (T item : items) {
            String worldName = worldFetchFunc.apply(item);

            // Skip invalid world names
            if (worldName == null)
                continue;

            if (lastWorld == null || !lastWorld.equals(worldName)) {
                lastWorld = worldName;
                currWorldItems = worldItems.computeIfAbsent(lastWorld, absentFunc);
            }

            final N convertedItem = conversionFunc.apply(item);

            currWorldItems.add(convertedItem);
        }

        return worldItems;
    }



}
