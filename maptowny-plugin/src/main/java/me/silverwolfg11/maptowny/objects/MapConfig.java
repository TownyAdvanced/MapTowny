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

import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.ConfigVersion;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.serialization.ClassDeserializer;
import me.Silverwolfg11.CommentConfig.serialization.ClassSerializer;
import me.Silverwolfg11.CommentConfig.serialization.NodeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SerializableConfig
@ConfigVersion(2.0)
public class MapConfig {

    @Comment("Worlds that should display town claims.")
    @Node("enabled-worlds")
    private List<String> enabledWorlds = Collections.singletonList("world");

    @Comment({"", "How often should the plugin render all towns? (in minutes)"})
    @Node("update-period")
    private double updatePeriod = 5;

    @Comment({"", "Layer Properties"})
    @Node("layer")
    private LayerInfo layerInfo = new LayerInfo();

    @Comment({"", "Fill Style.", "Properties about how claims should look on the map."})
    @Node("fill-style")
    private FillStyle fillStyle = new FillStyle();

    @Comment({"", "Icon Properties:", "Icons are placed at the center of a town's homeblock."})
    @Node("icon-info")
    private IconInfo iconInfo = new IconInfo();

    @Comment({"", "Default townblock types can be found on " +
            "https://github.com/TownyAdvanced/Towny/blob/master/Towny/src/main/java/com/palmergames/bukkit/towny/object/TownBlockType.java.",
            "Each type requires a \"fill-color\" and \"stroke-color\".",
            "If the color is not being used, set its value to \"none\"."})
    @Node("townblock-colors")
    private Map<String, TownBlockColor> townblockTypeColors = new HashMap<>();

    @SerializableConfig
    private static class LayerInfo {
        @Comment("Name of the layer")
        private String name = "Towny";

        @Comment("Should the layer be toggleable by users?")
        @Node("show-controls")
        private boolean showControls = true;

        @Comment("Should the layer be invisible by default?")
        @Node("default-hidden")
        private boolean defaultHidden = false;

        @Comment({"Layer Priority.", "Don't need to touch this unless other web-map add-ons are interfering with the layer."})
        @Node("layer-priority")
        private int layerPriority = 5;

        @Comment({"The z-index on which the layer will display.", "Decrease if you want the layer to be more blended in with the map."})
        @Node("z-index")
        private int zIndex = 250;
    }

    @SerializableConfig
    private static class FillStyle {
        @Comment("Whether to fill the claim with color")
        private boolean fill = true;

        @Node("fill-color")
        private String fillColor = "#3388ff";

        @Node("fill-opacity")
        private double fillOpacity = 0.2;

        @Comment({"", "Priorities for how color for claims should be applied.",
                "The valid list options are \"NATION\", \"TOWN\", and \"TOWNBLOCK_TYPE\".",
                "If one option doesn't have a color for the respective area, then it will move onto the next option.",
                "Claims will be separated by townblock types if \"TOWNBLOCK_TYPE\" is listed as a priority.",
                "The default fill color is last priority."})
        @Node("fill-priorities")
        private List<ColorSource> fillPriorities = new ArrayList<>();

        @Comment({"", "Whether to draw a stroke along the claim path."})
        private boolean stroke = true;

        @Comment("Stroke width in pixels")
        @Node("stroke-weight")
        private int strokeWeight = 3;

        @Node("stroke-color")
        private String strokeColor = "#3388ff";

        @Node("stroke-opacity")
        private double strokeOpacity = 1.0;

        @Comment({"", "Priorities for how color for claims should be applied.",
                "See fill-priorities for more information."})
        @Node("stroke-priorities")
        private List<ColorSource> strokePriorities = new ArrayList<>();

        private transient Color awtFillColor, awtStrokeColor;
    }

    @SerializableConfig
    private static class IconInfo {
        @Comment({"Icon for the town's homeblock. Icon must be a valid image URL.",
                "Java URLs support https, http, jar, and file protocols.",
                "Special values: 'built-in' to use the built-in icon; 'empty' to not use an icon.",
                "Built-in icon was created by Giraffeshroom."})
        @Node("town-icon")
        private String townIconImage = "built-in";

        @Comment({"Icon for a town if they are the capital of the nation. Icon must be a valid image URL.",
                "Special values: 'default' to use town icon image; 'built-in' to use the built-in icon; 'empty' to not use an icon.",
                "Built-in icon was created by Giraffeshroom."
        })
        @Node("capital-icon")
        private String capitalIconImage = "built-in";

        @Comment({"Icon for an outpost claim that will appear at the location of outpost spawns. Icon must be a valid image URL.",
                "Special values: 'default' to use town icon image; 'built-in' to use the built-in icon; 'empty' to not place an icon.",
                "Built-in icon was created by Giraffeshroom."})
        @Node("outpost-icon")
        private String outpostIconImage = "built-in";

        @Comment("Height of the icon")
        @Node("icon-height")
        private int iconSizeX = 35;

        @Comment("Width of the icon")
        @Node("icon-width")
        private int iconSizeY = 35;
    }

    @SerializableConfig
    private static class TownBlockColor {
        @Node("fill-color")
        private String fillColorStr = "none";

        @Node("stroke-color")
        private String strokeColorStr = "#3388ff";

        private transient Color fillColor = null, strokeColor = null;

        // Call to validate that the colors are cached
        void cache() {
            if (fillColorStr != null) {
                fillColor = parseColorHex(fillColorStr);
                strokeColor = parseColorHex(strokeColorStr);

                fillColorStr = null;
                strokeColorStr = null;
            }
        }
    }

    // Sets default values for the config.
    public MapConfig() {
        townblockTypeColors.put("Shop", new TownBlockColor());

        List<ColorSource> defaultPriorities = Arrays.asList(
                ColorSource.TOWN, ColorSource.NATION
        );
        fillStyle.strokePriorities.addAll(defaultPriorities);
        fillStyle.fillPriorities.addAll(defaultPriorities);
    }


    public List<String> getEnabledWorlds() {
        return Collections.unmodifiableList(enabledWorlds);
    }

    public double getUpdatePeriod() {
        return updatePeriod;
    }

    @NotNull
    public LayerOptions getLayerOptions() {
        return new LayerOptions(
                layerInfo.name,
                layerInfo.showControls,
                layerInfo.defaultHidden,
                layerInfo.layerPriority,
                layerInfo.zIndex
        );
    }

    @Nullable
    private static Color parseColorHex(String colorStr) {
        if (colorStr == null || colorStr.isEmpty() || colorStr.equalsIgnoreCase("none"))
            return null;

        try {
            return Color.decode(colorStr);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @NotNull
    public MarkerOptions.Builder buildMarkerOptions() throws NumberFormatException {
        if (fillStyle.awtFillColor == null)
            fillStyle.awtFillColor = Color.decode(fillStyle.fillColor);

        if (fillStyle.awtStrokeColor == null)
            fillStyle.awtStrokeColor = Color.decode(fillStyle.strokeColor);

        return MarkerOptions.builder()
                .fill(fillStyle.fill)
                .fillColor(fillStyle.awtFillColor)
                .fillOpacity(fillStyle.fillOpacity)
                .stroke(fillStyle.stroke)
                .strokeWeight(fillStyle.strokeWeight)
                .strokeColor(fillStyle.awtStrokeColor)
                .strokeOpacity(fillStyle.strokeOpacity);
    }

    public Color getDefaultFillColor() {
        return fillStyle.awtFillColor;
    }

    public Color getDefaultStrokeColor() {
        return fillStyle.awtStrokeColor;
    }

    @Nullable
    public BufferedImage loadTownIcon(Logger errorLogger) {
        return loadIcon("town", iconInfo.townIconImage, errorLogger);
    }

    @Nullable
    public BufferedImage loadCapitalIcon(Logger errorLogger) {
        String url = iconInfo.capitalIconImage;

        if (url.equalsIgnoreCase("default"))
            url = iconInfo.townIconImage;

        return loadIcon("capital", url, errorLogger);
    }

    @Nullable
    public BufferedImage loadOutpostIcon(Logger errorLogger) {
        String url = iconInfo.outpostIconImage;

        if (url.equalsIgnoreCase("default"))
            url = iconInfo.townIconImage;

        return loadIcon("outpost", url, errorLogger);
    }

    private BufferedImage loadBuiltInIcon(String type, Logger errorLogger) {
        String iconName = null;
        switch (type) {
            case "town":
                iconName = "town_icon.png";
                break;
            case "capital":
                iconName = "capital_icon.png";
                break;
            case "outpost":
                iconName = "outpost_icon.png";
                break;
        }

        if (iconName == null) {
            errorLogger.log(Level.SEVERE, "No built-in icon for type: " + type);
            return null;
        }

        try (InputStream is = getClass().getResourceAsStream("/icons/" + iconName)) {
            if (is == null) {
                errorLogger.log(Level.SEVERE, "Couldn't find built-in icon in JAR resource: icons/" + iconName);
                return null;
            }

            return ImageIO.read(is);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Error while loading built-in " + type + " image icon!", e);
            return null;
        }
    }

    private BufferedImage loadIcon(String type, String urlStr, Logger errorLogger) {
        if (urlStr == null || "empty".equals(urlStr) || urlStr.isEmpty())
            return null;

        if (urlStr.equalsIgnoreCase("built-in")) {
            return loadBuiltInIcon(type, errorLogger);
        }

        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException ex) {
            errorLogger.log(Level.SEVERE, "Cannot load " + type + " icon due to an invalid URL!", ex);
            return null;
        }

        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Error while loading " + type + " image icon!", e);
            return null;
        }
    }

    public int getIconSizeX() {
        return iconInfo.iconSizeX;
    }

    public int getIconSizeY() {
        return iconInfo.iconSizeY;
    }

    public boolean clusterByTownBlockType() {
        return getFillColorPriorities().contains(ColorSource.TOWNBLOCK_TYPE) ||
                getStrokeColorPriorities().contains(ColorSource.TOWNBLOCK_TYPE);
    }

    public Collection<String> getConfigTownBlockTypeNames() {
        return townblockTypeColors.keySet();
    }

    private TownBlockColor getTownBlockTypeColors(String typeName) {
        if (townblockTypeColors == null || townblockTypeColors.isEmpty())
            return null;

        TownBlockColor tbColor = townblockTypeColors.getOrDefault(typeName, null);

        if (tbColor == null) {
            return null;
        }

        tbColor.cache();

        return tbColor;
    }

    @Nullable
    public Color getFillColor(String townblockTypeName) {
        TownBlockColor tbColor = getTownBlockTypeColors(townblockTypeName);
        return tbColor != null ? tbColor.fillColor : null;
    }

    @Nullable
    public Color getStrokeColor(String townblockTypeName) {
        TownBlockColor tbColor = getTownBlockTypeColors(townblockTypeName);
        return tbColor != null ? tbColor.strokeColor : null;
    }

    public List<ColorSource> getStrokeColorPriorities() {
        return Collections.unmodifiableList(fillStyle.strokePriorities);
    }

    public List<ColorSource> getFillColorPriorities() {
        return Collections.unmodifiableList(fillStyle.fillPriorities);
    }


    public static MapConfig loadConfig(File directory, Logger errorLogger) throws IOException {

        if (!directory.exists())
            directory.mkdir();

        File configFile = new File(directory, "config.yml");

        NodeSerializer serializer = new NodeSerializer();
        if (!configFile.exists()) {
            configFile.createNewFile();
            // Use save config mapping
            MapConfig config = new MapConfig();
            ParentConfigNode node = ClassSerializer.serializeClass(config);
            serializer.serializeToFile(configFile, node);
            return config;
        } else {
            ClassDeserializer deserializer = new ClassDeserializer();
            deserializer.setErrorLogger(errorLogger);
            return deserializer.deserializeClassAndUpdate(configFile, MapConfig.class, serializer);
        }

    }

}
