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

package me.silverwolfg11.pl3xmaptowny.managers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.pl3xmaptowny.Pl3xMapTowny;
import me.silverwolfg11.pl3xmaptowny.objects.MapConfig;
import me.silverwolfg11.pl3xmaptowny.objects.StaticTB;
import me.silverwolfg11.pl3xmaptowny.objects.TBCluster;
import me.silverwolfg11.pl3xmaptowny.objects.TownRenderEntry;
import me.silverwolfg11.pl3xmaptowny.util.NegativeSpaceFinder;
import me.silverwolfg11.pl3xmaptowny.util.PolygonUtil;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.MapWorld;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.Registry;
import net.pl3x.map.api.SimpleLayerProvider;
import net.pl3x.map.api.marker.Icon;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.api.marker.MultiPolygon;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TownyLayerManager {

    private final Pl3xMapTowny plugin;
    private final TownInfoManager townInfoManager;

    private final Map<String, SimpleLayerProvider> worldProviders = new ConcurrentHashMap<>();
    private final Collection<UUID> renderedTowns = ConcurrentHashMap.newKeySet();

    private final Key LAYER_KEY = Key.of("towny");

    // Icon Registry Keys
    private final Key TOWN_ICON = Key.of("towny_town_icon");
    private final Key CAPITAL_ICON = Key.of("towny_capital_icon");
    private final Key OUTPOST_ICON = Key.of("towny_outpost_icon");

    private final String TOWN_KEY_PREFIX = "town_";
    private final String TOWN_ICON_KEY_PREFIX = "town_icon_";
    private final String TOWN_OUTPOST_ICON_KEY_PREFIX = "town_outpost_icon_";

    // Quick Indicators
    private final boolean usingOutposts;

    public TownyLayerManager(Pl3xMapTowny plugin) {
        this.plugin = plugin;
        this.townInfoManager = new TownInfoManager(plugin.getDataFolder(), plugin.getLogger());

        // Load world providers
        Pl3xMap api = Pl3xMapProvider.get();
        for (String worldName : plugin.config().getEnabledWorlds()) {
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().severe("Error accessing world " + worldName + "!");
                continue;
            }

            MapWorld mapWorld = api.getWorldIfEnabled(world).orElse(null);

            if (mapWorld == null) {
                plugin.getLogger().severe(worldName + " is not an enabled world for Pl3xMap!");
                continue;
            }

            SimpleLayerProvider layerProvider = plugin.config().buildLayerProvider();
            mapWorld.layerRegistry().register(LAYER_KEY, layerProvider);
            worldProviders.put(world.getName(), layerProvider);
        }

        // Load icons
        BufferedImage townIcon = plugin.config().loadTownIcon(plugin.getLogger());
        if (townIcon != null)
            api.iconRegistry().register(TOWN_ICON, townIcon);

        BufferedImage capitalIcon = plugin.config().loadCapitalIcon(plugin.getLogger());
        if (capitalIcon != null)
            api.iconRegistry().register(CAPITAL_ICON, capitalIcon);

        BufferedImage outpostIcon = plugin.config().loadOutpostIcon(plugin.getLogger());
        if (outpostIcon != null) {
            api.iconRegistry().register(OUTPOST_ICON, outpostIcon);
            usingOutposts = true;
        }
        else {
            usingOutposts = false;
        }
    }

    // Only ran synchronous
    @NotNull
    public TownRenderEntry buildTownEntry(Town town) {
        Color nationColor = getNationColor(town).orElse(null);
        Logger logger = plugin.getLogger();
        String clickText = townInfoManager.getClickTooltip(town, logger);
        String hoverText = townInfoManager.getHoverTooltip(town, logger);

        return new TownRenderEntry(town, usingOutposts, nationColor, clickText, hoverText);
    }

    // Thread-Safe
    public void renderTown(TownRenderEntry tre) {
        final int townblockSize = TownySettings.getTownBlockSize();

        // Fast-return if no enabled worlds
        if (worldProviders.isEmpty())
            return;

        // Fast-return if there are no townblocks
        if (tre.hasWorldBlocks())
            return;

        // Single-reference in case config reloads during method
        MapConfig config = plugin.config();

        // Universal Town Data
        Key townKey = Key.of(TOWN_KEY_PREFIX + tre.getTownUUID());
        Key townIconKey = Key.of(TOWN_ICON_KEY_PREFIX + tre.getTownUUID());

        String homeBlockWorld = tre.getHomeBlockWorld();
        Optional<Color> nationColor = tre.getNationColor();

        String clickTooltip = tre.getClickText();
        String hoverTooltip = tre.getHoverText();

        // Actual Rendering
        Map<String, ? extends Collection<StaticTB>> worldBlocks = tre.getWorldBlocks();

        for (Map.Entry<String, ? extends Collection<StaticTB>> entry : worldBlocks.entrySet()) {
            final String worldName = entry.getKey();
            final Collection<StaticTB> blocks = entry.getValue();

            SimpleLayerProvider worldProvider = worldProviders.get(worldName);

            // If no world provider, then we can discard rendering
            if (worldProvider == null)
                continue;

            List<TBCluster> clusters = TBCluster.findClusters(blocks);
            List<MultiPolygon.MultiPolygonPart> parts = new ArrayList<>();

            for (TBCluster cluster : clusters) {
                // Check if the cluster has negative space
                List<StaticTB> negativeSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
                List<List<Point>> negSpacePolys = Collections.emptyList();

                // If the cluster does have negative space, get the outlines of the negative space polygons
                if (!negativeSpace.isEmpty()) {
                    List<TBCluster> negSpaceClusters = TBCluster.findClusters(negativeSpace);

                    negSpacePolys = negSpaceClusters.stream()
                            .map(tbclust -> PolygonUtil.formPolyFromCluster(tbclust, townblockSize))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }

                // Form the main polygon
                List<Point> poly = PolygonUtil.formPolyFromCluster(cluster, townblockSize);

                if (poly != null) {
                    MultiPolygon.MultiPolygonPart part = MultiPolygon.part(poly, negSpacePolys);
                    parts.add(part);
                }
                else {
                    plugin.getLogger().warning("Error rendering part of town " + tre.getTownName());
                }
            }

            if (!parts.isEmpty()) {
                MultiPolygon multiPoly = MultiPolygon.multiPolygon(parts);
                MarkerOptions.Builder optionsBuilder = config.buildMarkerOptions()
                        .clickTooltip(clickTooltip)
                        .hoverTooltip(hoverTooltip);

                // Set nation fill color if there is one
                nationColor.ifPresent(optionsBuilder::fillColor);

                multiPoly.markerOptions(optionsBuilder.build());

                worldProvider.addMarker(townKey, multiPoly);

                // Check if this is the proper world provider to add the town icon
                if (homeBlockWorld.equals(worldName)) {
                    // Add icon markers
                    Optional<Point> homeblockPoint = tre.getHomeBlockPoint();
                    Key iconKey = tre.isCapital() ? CAPITAL_ICON : TOWN_ICON;
                    // Check if icon exists
                    if (homeblockPoint.isPresent() && Pl3xMapProvider.get().iconRegistry().hasEntry(iconKey)) {
                        Icon iconMarker = Marker.icon(homeblockPoint.get(), iconKey, config.getIconSizeX(), config.getIconSizeY());

                        iconMarker.markerOptions(
                                MarkerOptions.builder()
                                .clickTooltip(clickTooltip)
                                .hoverTooltip(hoverTooltip)
                        );

                        worldProvider.addMarker(townIconKey, iconMarker);
                    }
                }
            }
        }

        // Add outpost markers
        renderOutpostMarkers(tre, config.getIconSizeX(), config.getIconSizeY());

        renderedTowns.add(tre.getTownUUID());
    }

    private void renderOutpostMarkers(TownRenderEntry tre, int iconSizeX, int iconSizeZ) {
        final String townUUID = tre.getTownUUID().toString();
        // Delete previous town outpost icons
        for (Map.Entry<String, SimpleLayerProvider> entry : worldProviders.entrySet()) {
            final String worldName = entry.getKey();
            final SimpleLayerProvider provider = entry.getValue();

            int outpostNum = 1;
            while (
                    provider.removeMarker(
                            Key.of(TOWN_OUTPOST_ICON_KEY_PREFIX + worldName + "_" + townUUID + "_" + outpostNum)
                    ) != null
            ) {
                outpostNum++;
            }
        }

        // Add new town outpost icons
        if (tre.hasOutpostSpawns()) {
            for (Map.Entry<String, List<Point>> entry : tre.getOutpostSpawnPoints().entrySet()) {
                final String worldName = entry.getKey();
                SimpleLayerProvider provider = worldProviders.get(worldName);

                if (provider == null)
                    continue;

                final List<Point> outpostPoints = entry.getValue();
                int outpostNum = 1;
                for (Point outpostPoint : outpostPoints) {
                       Icon icon = Marker.icon(outpostPoint, OUTPOST_ICON, iconSizeX, iconSizeZ);
                       icon.markerOptions(
                               MarkerOptions.builder()
                               .clickTooltip(tre.getClickText())
                               .hoverTooltip(tre.getHoverText())
                       );

                       Key outpostKey = Key.of(TOWN_OUTPOST_ICON_KEY_PREFIX + worldName
                               + "_" + tre.getTownUUID() + "_" + outpostNum);
                       provider.addMarker(outpostKey, icon);
                }
            }
        }
    }

    // Gets the nation color from a town if:
    // config set to use nation colors and town has a valid nation color.
    @NotNull
    private Optional<Color> getNationColor(Town town) {
        if (plugin.config().useNationFillColor() && town.hasNation()) {
            Nation nation = TownyAPI.getInstance().getTownNationOrNull(town);
            String hex = nation.getMapColorHexCode();
            if (!hex.isEmpty()) {
                if (hex.charAt(0) != '#')
                    hex = "#" + hex;

                try {
                    return Optional.of(Color.decode(hex));
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Error loading nation " + nation.getName() + "'s map color: " + hex + "!");
                }
            }
        }

        return Optional.empty();
    }

    // Thread-safe
    @NotNull
    public Collection<UUID> getRenderedTowns() {
        return Collections.unmodifiableCollection(renderedTowns);
    }


    public void removeAllMarkers() {
        // Remove town markers based on key string comparison
        // This will also make sure to get rid of any deleted town markers
        for (Map.Entry<String, SimpleLayerProvider> entry : worldProviders.entrySet()) {
            final SimpleLayerProvider worldProvider = entry.getValue();
            Collection<Key> registeredTownKeys = new ArrayList<>();

            for (Map.Entry<Key, Marker> markerEntry : worldProvider.registeredMarkers().entrySet()) {
                final Key markerKey = markerEntry.getKey();
                final String markerKeyStr = markerKey.getKey();
                if (markerKeyStr.contains(TOWN_KEY_PREFIX)
                        || markerKeyStr.contains(TOWN_ICON_KEY_PREFIX))
                    registeredTownKeys.add(markerKey);
            }

            registeredTownKeys.forEach(worldProvider::removeMarker);
        }
    }

    public boolean removeTownMarker(Town town) {
        return removeTownMarker(town.getUUID());
    }

    // Returns if the town was successfully unrendered
    public boolean removeTownMarker(UUID townUUID) {
        boolean unrendered = false;
        Key townKey = Key.of(TOWN_KEY_PREFIX + townUUID);
        Key townIconKey = Key.of(TOWN_ICON_KEY_PREFIX + townUUID);

        for (Map.Entry<String, SimpleLayerProvider> entry : worldProviders.entrySet()) {
            final SimpleLayerProvider worldProvider = entry.getValue();
            unrendered |= worldProvider.removeMarker(townKey) != null;
            worldProvider.removeMarker(townIconKey);
        }
        renderedTowns.remove(townUUID);
        return unrendered;
    }

    // Closes the layer manager
    // Only run synchronously (uses Bukkit API)
    public void close() {
        // Remove all town markers
        removeAllMarkers();

        // Unregister world providers and clear
        Pl3xMap api = Pl3xMapProvider.get();
        for (Map.Entry<String, SimpleLayerProvider> entry : worldProviders.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null)
                continue;

            MapWorld mapWorld = api.getWorldIfEnabled(world).orElse(null);
            if (mapWorld == null)
                continue;

            mapWorld.layerRegistry().unregister(LAYER_KEY);
        }

        worldProviders.clear();

        // Unregister icons
        Registry<BufferedImage> iconReg = api.iconRegistry();
        if (iconReg.hasEntry(TOWN_ICON))
            iconReg.unregister(TOWN_ICON);

        if (iconReg.hasEntry(CAPITAL_ICON))
            iconReg.unregister(CAPITAL_ICON);

        if (iconReg.hasEntry(OUTPOST_ICON))
            iconReg.unregister(OUTPOST_ICON);
    }
}
