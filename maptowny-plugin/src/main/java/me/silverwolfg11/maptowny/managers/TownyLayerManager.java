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
import me.silverwolfg11.maptowny.objects.SegmentedPolygon;
import me.silverwolfg11.maptowny.objects.StaticTB;
import me.silverwolfg11.maptowny.objects.TBCluster;
import me.silverwolfg11.maptowny.objects.TownRenderEntry;
import me.silverwolfg11.maptowny.platform.MapLayer;
import me.silverwolfg11.maptowny.platform.MapPlatform;
import me.silverwolfg11.maptowny.platform.MapPlatformObserver;
import me.silverwolfg11.maptowny.platform.MapWorld;
import me.silverwolfg11.maptowny.tasks.RenderTownsTask;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TownyLayerManager implements LayerManager {

    private final MapTowny plugin;
    private final TownInfoManager townInfoManager;
    private final MapPlatform mapPlatform;
    private final MapPlatformObserver layerPlatformObserver;

    private final Map<String, MapLayer> worldProviders = new ConcurrentHashMap<>();
    private final Collection<UUID> renderedTowns = ConcurrentHashMap.newKeySet();

    private Collection<Runnable> initializerCallbacks = new ArrayList<>();

    private final String LAYER_KEY = "towny";

    // Icon Registry Keys
    private final String TOWN_ICON = "towny_town_icon";
    private final String NATION_ICON = "towny_nation_icon";
    private final String CAPITAL_ICON = "towny_capital_icon";
    private final String OUTPOST_ICON = "towny_outpost_icon";
    private final String RUINED_ICON = "towny_ruined_icon";

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

        this.layerPlatformObserver = createLayerPlatformObserver();
        this.mapPlatform.registerObserver(layerPlatformObserver);
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

    private MapPlatformObserver createLayerPlatformObserver() {
        return new MapPlatformObserver() {
            @Override
            public void onObserverSetup() {
                initialize();
            }

            @Override
            public void onPlatformEnabled() {
                reloadPlatform();
            }
        };
    }

    private void initialize() {
        MapPlatform platform = mapPlatform;

        loadWorldProviders(platform);
        loadIcons(platform);

        isInitialized = true;
        for (Runnable callback : initializerCallbacks) {
            callback.run();
        }

        // Allow GC
        initializerCallbacks.clear();
        initializerCallbacks = null;
    }

    // Assumes platform's marker sets and icons are
    // empty.
    private void reloadPlatform() {
        renderedTowns.clear();
        loadIcons(mapPlatform);

        plugin.getScheduler().scheduleTask(() -> {
            loadWorldProviders(mapPlatform);
            new RenderTownsTask(plugin).run();
        });
    }

    // Must be called on the Bukkit thread.
    private void loadWorldProviders(MapPlatform platform) {
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
    }

    private void loadIcons(MapPlatform platform) {
        // Load icons
        int iconWidth = plugin.config().getIconSizeX();
        int iconHeight = plugin.config().getIconSizeY();

        BufferedImage townIcon = plugin.config().loadTownIcon(plugin.getLogger());
        if (townIcon != null)
            platform.registerIcon(TOWN_ICON, townIcon, iconHeight, iconWidth);

        BufferedImage nationIcon = plugin.config().loadNationIcon(plugin.getLogger());
        if (nationIcon != null)
            platform.registerIcon(NATION_ICON, nationIcon, iconHeight, iconWidth);

        BufferedImage capitalIcon = plugin.config().loadCapitalIcon(plugin.getLogger());
        if (capitalIcon != null)
            platform.registerIcon(CAPITAL_ICON, capitalIcon, iconHeight, iconWidth);

        BufferedImage ruinedIcon = plugin.config().loadRuinedIcon(plugin.getLogger());
        if (ruinedIcon != null)
            platform.registerIcon(RUINED_ICON, ruinedIcon, iconHeight, iconWidth);

        BufferedImage outpostIcon = plugin.config().loadOutpostIcon(plugin.getLogger());
        if (outpostIcon != null) {
            platform.registerIcon(OUTPOST_ICON, outpostIcon, iconHeight, iconWidth);
            usingOutposts = true;
        }
        else {
            usingOutposts = false;
        }
    }

    // Only ran synchronous
    @NotNull
    public TownRenderEntry buildTownEntry(Town town) {
        // Get all objects that must be fetched synchronously
        // This includes anything that uses the Towny or Bukkit API.

        Color nationColor = getNationColor(town).orElse(null);
        Color townColor = getTownColor(town).orElse(null);

        Logger logger = plugin.getLogger();
        String clickText = townInfoManager.getClickTooltip(town, logger);
        String hoverText = townInfoManager.getHoverTooltip(town, logger);

        return new TownRenderEntry(town, usingOutposts, nationColor, townColor, clickText, hoverText);
    }

    // Thread-Safe
    public void renderTown(TownRenderEntry tre) {
        final int townblockSize = TownySettings.getTownBlockSize();

        // Fast-return if no enabled worlds
        if (worldProviders.isEmpty())
            return;

        // Fast-return if there are no townblocks
        if (!tre.hasWorldBlocks())
            return;

        // Single-reference in case config reloads during method
        MapConfig config = plugin.config();

        // Universal Town Data
        final String townKey = TOWN_KEY_PREFIX + tre.getTownUUID();
        final String townIconKey = TOWN_ICON_KEY_PREFIX + tre.getTownUUID();

        String homeBlockWorld = tre.getHomeBlockWorld();
        Optional<Color> nationColor = tre.getNationColor();
        Optional<Color> townColor = tre.getTownColor();

        String clickTooltip = tre.getClickText();
        String hoverTooltip = tre.getHoverText();

        // Actual Rendering
        Map<String, ? extends Collection<StaticTB>> worldBlocks = tre.getWorldBlocks();

        for (Map.Entry<String, ? extends Collection<StaticTB>> entry : worldBlocks.entrySet()) {
            final String worldName = entry.getKey();
            final Collection<StaticTB> blocks = entry.getValue();

            MapLayer worldProvider = worldProviders.get(worldName);

            // If no world provider, then we can discard rendering
            if (worldProvider == null)
                continue;
            // Sort the townblocks into clusters
            List<TBCluster> clusters = TBCluster.findClusters(blocks);
            List<Polygon> parts = new ArrayList<>();

            for (TBCluster cluster : clusters) {
                PolygonUtil.PolyFormResult result = PolygonUtil.getPolyInfoFromCluster(cluster, townblockSize);

                List<List<Point2D>> negativeSpace = new ArrayList<>();
                List<List<Point2D>> segmentedSpace = new ArrayList<>();

                // Handle enclosed space within the polygon
                if (!result.getNegativeSpaceClusters().isEmpty()) {
                    negativeSpace = result.getNegativeSpaceClusters().stream()
                            .map(negCluster -> PolygonUtil.getPolyInfoFromCluster(negCluster, townblockSize, false).getPolygonPoints())
                            .collect(Collectors.toList());

                    if (mapPlatform.usesSegmentedPolygons()) {
                        segmentedSpace = PolygonUtil.segmentPolygon(cluster, result.getNegativeSpaceClusters(), townblockSize);
                    }
                }

                // Form the main polygon
                List<Point2D> poly = result.getPolygonPoints();

                if (!poly.isEmpty()) {
                    Polygon part;

                    if (!segmentedSpace.isEmpty()) {
                        part = new SegmentedPolygon(poly, negativeSpace, segmentedSpace);
                    }
                    else {
                        part = new Polygon(poly, negativeSpace);
                    }

                    parts.add(part);
                }
                else {
                    plugin.getLogger().warning("Error rendering part of town " + tre.getTownName());
                }
            }

            if (!parts.isEmpty()) {
                MarkerOptions.Builder optionsBuilder = config.buildMarkerOptions()
                        .name(tre.getTownName())
                        .clickTooltip(clickTooltip)
                        .hoverTooltip(hoverTooltip);

                // Use nation color if present
                if (nationColor.isPresent()) {
                    if (config.useNationFillColor())
                        optionsBuilder.fillColor(nationColor.get());

                    if (config.useNationStrokeColor())
                        optionsBuilder.strokeColor(nationColor.get());
                }

                // Use town color if present.
                // Town color options will override nation colors
                if (townColor.isPresent()) {
                    if (config.useTownFillColor()) {
                        optionsBuilder.fillColor(townColor.get());
                    }

                    if (config.useTownStrokeColor()) {
                        optionsBuilder.strokeColor(townColor.get());
                    }
                }

                String homeBlockIconKey = TOWN_ICON;
                if (tre.hasNation()) {
                    if (tre.isCapital()) {
                        homeBlockIconKey = CAPITAL_ICON;
                    } else {
                        homeBlockIconKey = NATION_ICON;
                    }
                }
                if (tre.isRuined()) {
                    homeBlockIconKey = RUINED_ICON;
                }

                // Call event
                WorldRenderTownEvent event = new WorldRenderTownEvent(worldName, tre.getTownName(), tre.getTownUUID(),
                                                                      homeBlockIconKey, OUTPOST_ICON,
                                                                      parts, optionsBuilder);

                Bukkit.getPluginManager().callEvent(event);

                // Skip rendering the town for the world if it is cancelled.
                if (event.isCancelled())
                    continue;

                final MarkerOptions generalOptions = optionsBuilder.build();

                worldProvider.addMultiPolyMarker(townKey, parts, generalOptions);

                // Add outpost markers for the current world
                renderOutpostMarker(tre, worldName, worldProvider,
                                    event.getOutpostIconKey(), generalOptions,
                                    config.getIconSizeX(), config.getIconSizeY());

                // Check if this is the proper world provider to add the town icon
                if (homeBlockWorld.equals(worldName)) {
                    // Add icon markers
                    Optional<Point2D> homeblockPoint = tre.getHomeBlockPoint();
                    // Check if icon exists
                    if (homeblockPoint.isPresent() && mapPlatform.hasIcon(event.getHomeBlockIconKey())) {
                        MarkerOptions iconOptions = MarkerOptions.builder()
                                                                 .name(generalOptions.name())
                                                                 .clickTooltip(generalOptions.clickTooltip())
                                                                 .hoverTooltip(generalOptions.hoverTooltip())
                                                                 .build();

                        worldProvider.addIconMarker(townIconKey, event.getHomeBlockIconKey(), homeblockPoint.get(),
                                                    config.getIconSizeX(), config.getIconSizeY(),
                                                    iconOptions);
                    }
                }
            }
        }

        renderedTowns.add(tre.getTownUUID());
    }

    private void renderOutpostMarker(TownRenderEntry tre, String worldName, MapLayer worldProvider,
                                     String outpostIconKey, MarkerOptions generalOptions, int iconSizeX, int iconSizeZ) {
        // Delete previous town outpost icons
        unrenderOutpostMarkers(worldProvider, worldName, tre.getTownUUID());

        // Check that outpost icon exists.
        if (!mapPlatform.hasIcon(outpostIconKey))
            return;

        final String keyPrefix = TOWN_OUTPOST_ICON_KEY_PREFIX + worldName + "_" + tre.getTownUUID() + "_";
        // Add new town outpost icons
        if (tre.hasOutpostSpawns()) {
            final List<Point2D> outpostPoints = tre.getOutpostSpawnPoints().get(worldName);

            if (outpostPoints == null)
                return;

            for (int outpostIdx = 0; outpostIdx < outpostPoints.size(); outpostIdx++) {
                Point2D outpostPoint = outpostPoints.get(outpostIdx);
                MarkerOptions iconOptions = MarkerOptions.builder()
                        .name(generalOptions.name())
                        .clickTooltip(generalOptions.clickTooltip())
                        .hoverTooltip(generalOptions.hoverTooltip())
                        .build();

                worldProvider.addIconMarker(keyPrefix + (outpostIdx + 1), outpostIconKey, outpostPoint,
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

    // Convert given town hex code to color with error handling.
    @NotNull
    private Optional<Color> convertTownHexCodeToColor(String hex, Town town) {
        if (!hex.isEmpty()) {
            if (hex.charAt(0) != '#')
                hex = "#" + hex;

            try {
                return Optional.of(Color.decode(hex));
            } catch (NumberFormatException ex) {
                String name = town.getName();
                plugin.getLogger().warning("Error loading town " + name + "'s map color: " + hex + "!");
            }
        }

        return Optional.empty();
    }

    // Gets the nation color from a town if:
    // config set to use nation colors and town has a valid nation color.
    @NotNull
    private Optional<Color> getNationColor(Town town) {
        if (plugin.config().useNationFillColor() || plugin.config().useNationStrokeColor()) {
            String hex = town.getNationMapColorHexCode();
            return hex == null ? Optional.empty() : convertTownHexCodeToColor(hex, town);
        }

        return Optional.empty();
    }

    // Gets the town color from a town if:
    // config set to use town colors
    @NotNull
    private Optional<Color> getTownColor(Town town) {
        if (plugin.config().useTownFillColor() || plugin.config().useTownStrokeColor()) {
            String hex = town.getMapColorHexCode();
            return hex == null ? Optional.empty() : convertTownHexCodeToColor(hex, town);
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

        if (mapPlatform.hasIcon(NATION_ICON))
            mapPlatform.unregisterIcon(NATION_ICON);

        if (mapPlatform.hasIcon(CAPITAL_ICON))
            mapPlatform.unregisterIcon(CAPITAL_ICON);

        if (mapPlatform.hasIcon(RUINED_ICON))
            mapPlatform.unregisterIcon(RUINED_ICON);

        if (mapPlatform.hasIcon(OUTPOST_ICON))
            mapPlatform.unregisterIcon(OUTPOST_ICON);

        mapPlatform.unregisterObserver(layerPlatformObserver);
    }

    private void completeOnMainThread(CompletableFuture<Void> syncFutures) {
        if (Bukkit.isPrimaryThread()) {
            // Can only get TRE from sync thread
            syncFutures.complete(null);
        }
        else {
            plugin.getScheduler().scheduleTask(() -> syncFutures.complete(null));
        }
    }

    // API Methods

    @Override
    public void renderTown(final @NotNull Town town) {
        completeOnMainThread(
                new CompletableFuture<>()
                        .thenApply((Void) -> buildTownEntry(town))
                        .thenAcceptAsync(this::renderTown, plugin.getScheduler().getAsyncExecutor())
        );
    }

    @Override
    public void renderTowns(@NotNull Collection<Town> towns) {
        CompletableFuture<Void> mainThreadSync = new CompletableFuture<>()
                .thenApply((Void) -> towns.stream()
                        .map(this::buildTownEntry)
                        .collect(Collectors.toList()))
                .thenAcceptAsync((entries) -> entries.forEach(this::renderTown), plugin.getScheduler().getAsyncExecutor());

        completeOnMainThread(mainThreadSync);
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
