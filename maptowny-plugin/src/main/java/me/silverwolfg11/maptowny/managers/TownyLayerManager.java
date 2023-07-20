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

package me.silverwolfg11.maptowny.managers;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.maptowny.MapTowny;
import me.silverwolfg11.maptowny.events.WorldRenderTownEvent;
import me.silverwolfg11.maptowny.objects.LayerOptions;
import me.silverwolfg11.maptowny.objects.MapConfig;
import me.silverwolfg11.maptowny.objects.MarkerOptions;
import me.silverwolfg11.maptowny.objects.Point2D;
import me.silverwolfg11.maptowny.objects.Polygon;
import me.silverwolfg11.maptowny.objects.PolygonGroup;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import me.silverwolfg11.maptowny.objects.TownRenderEntry;
import me.silverwolfg11.maptowny.objects.groups.TBGroup;
import me.silverwolfg11.maptowny.objects.groups.TBTypeTBGroup;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.util.NegativeSpaceFinder;
import me.silverwolfg11.maptowny.util.PolygonUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TownyLayerManager implements LayerManager {

    private final MapTowny plugin;
    private final TownInfoManager townInfoManager;
    private final MapPlatform mapPlatform;
    private final ColorProvider colorProvider;

    private final Map<String, MapLayer> worldProviders = new ConcurrentHashMap<>();
    private final Collection<UUID> renderedTowns = ConcurrentHashMap.newKeySet();

    private Collection<Runnable> initializerCallbacks = new ArrayList<>();

    private final String LAYER_KEY = "towny";

    // Icon Registry Keys
    private final String TOWN_ICON = "towny_town_icon";
    private final String CAPITAL_ICON = "towny_capital_icon";
    private final String OUTPOST_ICON = "towny_outpost_icon";

    private final String TOWN_KEY_PREFIX = "town_";
    private final String TOWN_ICON_KEY_PREFIX = "town_icon_";
    private final String TOWN_OUTPOST_ICON_KEY_PREFIX = "town_outpost_icon_";

    // Quick Indicators
    private boolean usingOutposts;
    private boolean isInitialized = false;

    public TownyLayerManager(MapTowny plugin, MapPlatform platform) {
        this.plugin = plugin;
        this.townInfoManager = new TownInfoManager(plugin.getDataFolder(), plugin.getLogger());
        this.mapPlatform = platform;
        this.colorProvider = new ColorProvider(plugin.getLogger(),
                plugin.config().getFillColorPriorities(), plugin.config().getStrokeColorPriorities(),
                plugin.config().getDefaultFillColor(), plugin.config().getDefaultStrokeColor());

        // Schedule initialization
        mapPlatform.onFirstInitialize(this::initialize);
    }

    // Callbacks execute when the LayerManager completes initialization.
    // If the LayerManager is already initialized, callback runs in the calling thread.
    // Callbacks do not persist through MapTowny reloads.
    public void onInitialize(Runnable runnable) {
        if (isInitialized)
            runnable.run();
        else {
            this.initializerCallbacks.add(runnable);
        }
    }

    private void initialize() {
        MapPlatform platform = mapPlatform;

        // Load world providers
        for (String worldName : plugin.config().getEnabledWorlds()) {
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().severe("Error accessing world " + worldName + "!");
                continue;
            }

            if (!platform.isWorldEnabled(world)) {
                plugin.getLogger().severe(worldName + " is not an enabled world for " + platform.getPlatformName() + "!");
                continue;
            }

            LayerOptions layerOptions = plugin.config().getLayerOptions();
            MapLayer mapLayer = platform.getWorld(world).registerLayer(LAYER_KEY, layerOptions);
            worldProviders.put(world.getName(), mapLayer);
        }

        // Load icons
        int iconWidth = plugin.config().getIconSizeX();
        int iconHeight = plugin.config().getIconSizeY();

        BufferedImage townIcon = plugin.config().loadTownIcon(plugin.getLogger());
        if (townIcon != null)
            platform.registerIcon(TOWN_ICON, townIcon, iconHeight, iconWidth);

        BufferedImage capitalIcon = plugin.config().loadCapitalIcon(plugin.getLogger());
        if (capitalIcon != null)
            platform.registerIcon(CAPITAL_ICON, capitalIcon, iconHeight, iconWidth);

        BufferedImage outpostIcon = plugin.config().loadOutpostIcon(plugin.getLogger());
        if (outpostIcon != null) {
            platform.registerIcon(OUTPOST_ICON, outpostIcon, iconHeight, iconWidth);
            usingOutposts = true;
        }
        else {
            usingOutposts = false;
        }

        // Run initialization callbacks
        isInitialized = true;
        for (Runnable callback : initializerCallbacks) {
            callback.run();
        }

        // Allow GC
        initializerCallbacks.clear();
        initializerCallbacks = null;
    }

    private List<TBGroup> buildGeneralTBGroups(boolean useTBTypeFill, boolean useTBTypeStroke) {
        final List<TBGroup> generalGroups = new ArrayList<>();

        final MapConfig pluginConfig = plugin.config();

        // Check if grouping by townblock type is necessary
        if (useTBTypeFill || useTBTypeStroke) {
            // Only group by townblock types that have configured stroke / fill changes.
            for (final String tbTypeName : pluginConfig.getConfigTownBlockTypeNames()) {
                final Color strokeColor = useTBTypeStroke ? pluginConfig.getStrokeColor(tbTypeName) : null;
                final Color fillColor = useTBTypeFill ? pluginConfig.getFillColor(tbTypeName) : null;

                if (strokeColor == null && fillColor == null)
                    continue;

                generalGroups.add(new TBTypeTBGroup(tbTypeName, fillColor, strokeColor));
            }
        }

        // Add a default TB group at the end
        generalGroups.add(new TBGroup());

        return generalGroups;
    }

    // Only ran synchronous
    @NotNull
    public TownRenderEntry buildTownEntry(Town town) {
        // Get all objects that must be fetched synchronously
        // This includes anything that uses the Towny or Bukkit API.
        ColorProvider.TownColorSource colorSource = colorProvider.getTownColorSource(town);
        Color fillColor = colorSource.fillColor;
        Color strokeColor = colorSource.strokeColor;

        Logger logger = plugin.getLogger();
        String clickText = townInfoManager.getClickTooltip(town, logger);
        String hoverText = townInfoManager.getHoverTooltip(town, logger);

        return new TownRenderEntry(town, usingOutposts,
                                   buildGeneralTBGroups(colorSource.useTBFill, colorSource.useTBStroke),
                                   strokeColor, fillColor, clickText, hoverText);
    }

    private List<Polygon> getPolygonsFromGroup(String townName, TBGroup tbGroup, int tbSize) {
        List<TBCluster> clusters = TBCluster.findClusters(tbGroup.getTownblocks());
        List<Polygon> parts = new ArrayList<>();

        for (TBCluster cluster : clusters) {
            // Check if the cluster has negative space
            List<StaticTB> negativeSpace = NegativeSpaceFinder.findNegativeSpace(cluster);
            List<List<Point2D>> negSpacePolys = Collections.emptyList();

            // If the cluster does have negative space, get the outlines of the negative space polygons
            if (!negativeSpace.isEmpty()) {
                List<TBCluster> negSpaceClusters = TBCluster.findClusters(negativeSpace);

                negSpacePolys = negSpaceClusters.stream()
                        .map(tbclust -> PolygonUtil.formPolyFromCluster(tbclust, tbSize))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            // Form the main polygon
            List<Point2D> poly = PolygonUtil.formPolyFromCluster(cluster, tbSize);

            if (poly != null) {
                Polygon part = new Polygon(poly, negSpacePolys);
                parts.add(part);
            }
            else {
                plugin.getLogger().warning("Error rendering part of town " + townName);
            }
        }

        return parts;
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

        plugin.getLogger().info("Rendering town " + tre.getTownName()); // FIXME DEBUG

        // Single-reference in case config reloads during method
        MapConfig config = plugin.config();

        // Universal Town Data
        final String townKey = TOWN_KEY_PREFIX + tre.getTownUUID();
        final String townIconKey = TOWN_ICON_KEY_PREFIX + tre.getTownUUID();

        String homeBlockWorld = tre.getHomeBlockWorld();
        Optional<Color> strokeColor = tre.getStrokeColor();
        Optional<Color> fillColor = tre.getFillColor();

        String clickTooltip = tre.getClickText();
        String hoverTooltip = tre.getHoverText();

        // Actual Rendering
        Map<String, List<TBGroup>> worldGroups = tre.getWorldGroups();

        for (Map.Entry<String, List<TBGroup>> entry : worldGroups.entrySet()) {
            final String worldName = entry.getKey();
            final List<TBGroup> tbGroups = entry.getValue();

            MapLayer worldProvider = worldProviders.get(worldName);

            // If no world provider, then we can discard rendering
            if (worldProvider == null)
                continue;

            // Unrender previous town markers
            worldProvider.removeMarkers(
                    (markerKey) -> markerKey.contains(TOWN_KEY_PREFIX + tre.getTownName())
                                    || markerKey.contains(TOWN_ICON_KEY_PREFIX + tre.getTownName())
            );

            // Create polygons from each townblock group
            List<PolygonGroup> polygonGroups = new ArrayList<>();
            for (TBGroup tbGroup : tbGroups) {
                List<Polygon> polyParts = getPolygonsFromGroup(tre.getTownName(), tbGroup, townblockSize);

                if (polyParts.isEmpty())
                    continue;

                polygonGroups.add(tbGroup.buildPolygonGroup(polyParts));
            }

            if (polygonGroups.isEmpty())
                continue;

            MarkerOptions.Builder optionsBuilder = config.buildMarkerOptions()
                    .name(tre.getTownName())
                    .clickTooltip(clickTooltip)
                    .hoverTooltip(hoverTooltip);

            strokeColor.ifPresent(optionsBuilder::strokeColor);
            fillColor.ifPresent(optionsBuilder::fillColor);

            // Call event
            WorldRenderTownEvent event = new WorldRenderTownEvent(worldName, tre.getTownName(), tre.getTownUUID(), polygonGroups, optionsBuilder);
            Bukkit.getPluginManager().callEvent(event);

            // Skip rendering the town for the world if it is cancelled.
            if (event.isCancelled())
                continue;

            // Render each polygon group
            for (int i = 0; i < event.getPolygonGroups().size(); i++) {
                final String groupKey = townKey + "_" + (i + 1);
                PolygonGroup group = event.getPolygonGroups().get(i);

                MarkerOptions.Builder groupBuilder = optionsBuilder.clone();
                group.modifyMarkerOptions(groupBuilder);

                worldProvider.addMultiPolyMarker(groupKey, group.getPolygons(), groupBuilder.build());
            }

            // Add outpost markers for the current world
            renderOutpostMarker(tre, worldName, worldProvider, config.getIconSizeX(), config.getIconSizeY());

            // Check if this is the proper world provider to add the town icon
            if (homeBlockWorld.equals(worldName)) {
                // Add icon markers
                Optional<Point2D> homeblockPoint = tre.getHomeBlockPoint();
                final String iconKey = tre.isCapital() ? CAPITAL_ICON : TOWN_ICON;
                // Check if icon exists
                if (homeblockPoint.isPresent() && mapPlatform.hasIcon(iconKey)) {
                    MarkerOptions iconOptions = MarkerOptions.builder()
                            .name(tre.getTownName())
                            .clickTooltip(clickTooltip)
                            .hoverTooltip(hoverTooltip)
                            .build();

                    worldProvider.addIconMarker(townIconKey, iconKey, homeblockPoint.get(),
                            config.getIconSizeX(), config.getIconSizeY(),
                            iconOptions);
                }
            }
        }

        renderedTowns.add(tre.getTownUUID());
    }

    private void renderOutpostMarker(TownRenderEntry tre, String worldName, MapLayer worldProvider, int iconSizeX, int iconSizeZ) {
        // Delete previous town outpost icons
        unrenderOutpostMarkers(worldProvider, worldName, tre.getTownUUID());

        final String keyPrefix = TOWN_OUTPOST_ICON_KEY_PREFIX + worldName + "_" + tre.getTownUUID() + "_";
        // Add new town outpost icons
        if (tre.hasOutpostSpawns()) {
            final List<Point2D> outpostPoints = tre.getOutpostSpawnPoints().get(worldName);

            if (outpostPoints == null)
                return;

            int outpostNum = 1;
            for (Point2D outpostPoint : outpostPoints) {
                MarkerOptions iconOptions = MarkerOptions.builder()
                                                         .name(tre.getTownName())
                                                         .clickTooltip(tre.getClickText())
                                                         .hoverTooltip(tre.getHoverText())
                                                         .build();

                worldProvider.addIconMarker(keyPrefix + outpostNum, OUTPOST_ICON, outpostPoint,
                                            iconSizeX, iconSizeZ, iconOptions);
            }
        }
    }

    private void unrenderOutpostMarkers(MapLayer mapLayer, String worldName, UUID townUUID) {
        final String keyPrefix = TOWN_OUTPOST_ICON_KEY_PREFIX + worldName + "_" + townUUID + "_";
        int outpostNum = 1;
        while (mapLayer.removeMarker(keyPrefix + outpostNum)) {
            outpostNum++;
        }
    }


    // Thread-safe
    @NotNull
    public Collection<UUID> getRenderedTowns() {
        return Collections.unmodifiableCollection(renderedTowns);
    }


    public void removeAllMarkers() {
        // Remove town markers based on key string comparison
        // This will also make sure to get rid of any deleted town's markers
        for (Map.Entry<String, MapLayer> entry : worldProviders.entrySet()) {
            final MapLayer worldProvider = entry.getValue();
            worldProvider.removeMarkers(
                    (markerKey) -> markerKey.contains(TOWN_KEY_PREFIX)  || markerKey.contains(TOWN_ICON_KEY_PREFIX)
            );
        }
    }

    @Override
    public boolean removeTownMarker(@NotNull Town town) {
        return removeTownMarker(town.getUUID());
    }

    // Returns if the town was successfully unrendered
    @Override
    public boolean removeTownMarker(@NotNull UUID townUUID) {
        boolean unrendered = false;
        final String townKey = TOWN_KEY_PREFIX + townUUID;
        final String townIconKey = TOWN_ICON_KEY_PREFIX + townUUID;

        for (Map.Entry<String, MapLayer> entry : worldProviders.entrySet()) {
            final String worldName = entry.getKey();
            final MapLayer mapLayer = entry.getValue();
            unrendered |= mapLayer.removeMarker(townKey);
            mapLayer.removeMarker(townIconKey);
            unrenderOutpostMarkers(mapLayer, worldName, townUUID);
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
        for (Map.Entry<String, MapLayer> entry : worldProviders.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null)
                continue;

            final MapWorld mapWorld = mapPlatform.getWorld(world);

            if (mapWorld == null)
                continue;

            mapWorld.unregisterLayer(LAYER_KEY);
        }

        worldProviders.clear();

        // Unregister icons
        if (mapPlatform.hasIcon(TOWN_ICON))
            mapPlatform.unregisterIcon(TOWN_ICON);

        if (mapPlatform.hasIcon(CAPITAL_ICON))
            mapPlatform.unregisterIcon(CAPITAL_ICON);

        if (mapPlatform.hasIcon(OUTPOST_ICON))
            mapPlatform.unregisterIcon(OUTPOST_ICON);
    }

    // API Methods

    @Override
    public void renderTown(final @NotNull Town town) {
        Runnable renderTown = () -> {
            final TownRenderEntry tre = buildTownEntry(town);
            plugin.async(() -> renderTown(tre));
        };

        if (!Bukkit.isPrimaryThread()) {
            // Can only get TRE from sync thread
            Bukkit.getScheduler().runTask(plugin, renderTown);
        }
        else {
            renderTown.run();
        }
    }

    @Override
    public void renderTowns(@NotNull Collection<Town> towns) {
        Runnable renderTowns = () -> {
            final Collection<TownRenderEntry> tres = towns.stream()
                    .map(this::buildTownEntry)
                    .collect(Collectors.toList());

            plugin.async(() -> tres.forEach(this::renderTown));
        };

        if (!Bukkit.isPrimaryThread()) {
            // Can only get TRE from sync thread
            Bukkit.getScheduler().runTask(plugin, renderTowns);
        }
        else {
            renderTowns.run();
        }
    }

    @Override
    @Nullable
    public MapLayer getTownyWorldLayerProvider(@NotNull String worldName) {
        Validate.notNull(worldName);

        return worldProviders.get(worldName);
    }

    // Pass-through to TownInfoManager
    @Override
    public void registerReplacement(@NotNull String key, @NotNull Function<Town, String> function) {
        Validate.notNull(key);
        Validate.notNull(function);

        this.townInfoManager.registerReplacement(key, function);
    }

    @Override
    public void unregisterReplacement(@NotNull String key) {
        Validate.notNull(key);

        this.townInfoManager.unregisterReplacement(key);
    }

    @Override
    public @NotNull MarkerOptions getTownInfoMarker(@NotNull Town town) {
        Validate.notNull(town);

        String clickText = townInfoManager.getClickTooltip(town, plugin.getLogger());
        String hoverText = townInfoManager.getHoverTooltip(town, plugin.getLogger());

        return MarkerOptions.builder()
                            .name(town.getName())
                            .clickTooltip(clickText)
                            .hoverTooltip(hoverText)
                            .build();
    }

    // =====
}
