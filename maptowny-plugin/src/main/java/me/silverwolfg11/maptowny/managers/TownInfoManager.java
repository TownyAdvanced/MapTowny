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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import me.silverwolfg11.maptowny.objects.TextReplacement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// This class handles loading the tooltip HTML files as well as filling in the placeholders
public class TownInfoManager {

    private final TextReplacement<Town> clickReplacements;
    private final TextReplacement<Town> hoverReplacements;

    // Used for %founded% replacements.
    private final SimpleDateFormat registeredTimeFormat =  new SimpleDateFormat("MMM d yyyy");

    public TownInfoManager(File dataFolder, Logger errorLogger) {
        final String CLICK_FILE_NAME = "click_tooltip.html";
        final String HOVER_FILE_NAME = "hover_tooltip.html";

        // Create the parent directory if it doesn't exist
        if (!dataFolder.exists())
            dataFolder.mkdir();

        // Read hover and click files
        File clickWindowFile = new File(dataFolder, CLICK_FILE_NAME);
        if (!clickWindowFile.exists())
            copyFile(errorLogger, CLICK_FILE_NAME, clickWindowFile);

        clickReplacements = readHTMLReplacementFile(clickWindowFile, errorLogger);

        File hoverWindowFile = new File(dataFolder, HOVER_FILE_NAME);
        if (!hoverWindowFile.exists())
            copyFile(errorLogger, HOVER_FILE_NAME, hoverWindowFile);

        hoverReplacements = readHTMLReplacementFile(hoverWindowFile, errorLogger);

        registerReplacements();
    }

    // Read the replacement HTML file and return the associated text replacement object.
    @NotNull
    private TextReplacement<Town> readHTMLReplacementFile(File file, Logger errorLogger) {
        String replacementFileRaw = null;
        try {
            replacementFileRaw = readFile(file);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Unable to read file " + file.getName(), e);
        }

        if (replacementFileRaw != null) {
            return TextReplacement.fromHTML(replacementFileRaw);
        }
        else {
            return TextReplacement.empty();
        }
    }

    // Replacement Registration Methods
    private void registerReplacements() {
        register("town", TownyObject::getName);
        register("mayor", t -> t.getMayor().getName());
        register("firespread", t -> String.valueOf(t.getPermissions().fire));
        register("pvp", t -> String.valueOf(t.getPermissions().pvp));
        register("explosion", t -> String.valueOf(t.getPermissions().explosion));
        register("mobs", t -> String.valueOf(t.getPermissions().mobs));
        register("residents", t -> {
            String residents = t.getResidents().stream().map(TownyObject::getName).collect(Collectors.joining(", "));
            return residents.isEmpty() ? "None" : residents;
        });
        register("residentdisplaynames", t -> {
            String residents = t.getResidents()
                    .stream()
                    .map(r -> {
                        Player resPlayer = r.getPlayer();
                        return resPlayer != null ? resPlayer.getDisplayName() : r.getFormattedName();
                    })
                    .collect(Collectors.joining(", "));

            return residents.isEmpty() ? "None" : residents;
        });
        register("residentcount", t -> String.valueOf(t.getNumResidents()));
        register("board", Government::getBoard);
        register("nationstatus", t -> {
            if (!t.hasNation())
                return "";

            final String nationName = TownyAPI.getInstance().getTownNationOrNull(t).getName();
            return t.isCapital() ? "Capital of " + nationName : "Member of " + nationName;
        });
        register("public", t -> String.valueOf(t.isPublic()));
        register("peaceful", t -> String.valueOf(t.isNeutral()));

        register("founded", t -> {
            long founded = t.getRegistered();
            if (founded == 0)
                return "Not Set";

            return registeredTimeFormat.format(new Date(founded));
        });

        registerParenthesesReplacement("nation",
                t-> t.hasNation() ? TownyAPI.getInstance().getTownNationOrNull(t).getName() : ""
        );

        // Register Economy Replacements
        if (TownyEconomyHandler.isActive() && TownySettings.isUsingEconomy()) {
            register("tax", t -> {
                if (t.isTaxPercentage()) {
                    return t.getTaxes() + "%";
                }

                return TownyEconomyHandler.getFormattedBalance(t.getTaxes());
            });
            register("upkeep", t -> {
                if (!t.hasUpkeep())
                    return "";

                return TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(t));
            });
            register("bank",
                    t -> TownyEconomyHandler.getFormattedBalance(t.getAccount().getCachedBalance())
            );
        }

        registerRanks();
    }

    // Register a replacement that will replace parenthesis if empty.
    // E.g. For the key "nation", the replacement "(%nation%)" or "%nation%" would be valid.
    private void registerParenthesesReplacement(String key, Function<Town, String> func) {
        String wrappedKey = "%" + key + "%";
        String pKey = "(" + wrappedKey + ")";
        Function<Town, String> pFunc = t -> {
            String result = func.apply(t);
            return (result != null && !result.isEmpty()) ? "(" + result + ")" : "";
        };

        if (!clickReplacements.registerReplacement(pKey, pFunc)) {
            clickReplacements.registerReplacement(wrappedKey, func);
        }

        if (!hoverReplacements.registerReplacement(pKey, pFunc)) {
            hoverReplacements.registerReplacement(wrappedKey, func);
        }
    }

    private void register(String key, Function<Town, String> func) {
        String wrappedKey = "%" + key + "%";
        registerReplacement(wrappedKey, func);
    }


    // API Methods

    public void registerReplacement(@NotNull String key, @NotNull Function<Town, String> func) {
        clickReplacements.registerReplacement(key, func);
        hoverReplacements.registerReplacement(key, func);
    }

    public void unregisterReplacement(@NotNull String key) {
        clickReplacements.unregisterReplacement(key);
        hoverReplacements.unregisterReplacement(key);
    }

    // Does not validate ranks.
    private void registerRanks() {
        // Pattern gets anything matching "%rank_<rank name>%"
        Pattern pattern = Pattern.compile("%rank_.+%");
        registerRanks(clickReplacements, pattern);
        registerRanks(hoverReplacements, pattern);
    }

    private void registerRanks(TextReplacement<Town> textReplacement, Pattern rankPattern) {
        String textToReplace = textReplacement.getTextToReplace();

        if (textToReplace == null)
            return;

        Matcher matcher = rankPattern.matcher(textToReplace);

        while (matcher.find()) {
            String rankReplacement = textToReplace.substring(matcher.start(), matcher.end());
            // %rank_test% - Just need "test" part so substring at 6 and go up to the %.
            final String rank = rankReplacement.substring(6, rankReplacement.length() - 1);
            textReplacement.registerReplacement(rankReplacement, t -> {
                String rankResidents = t.getRank(rank).stream().map(TownyObject::getName).collect(Collectors.joining(", "));
                return rankResidents.isEmpty() ? "None" : rankResidents;
            });
        }
    }

    public String getClickTooltip(Town town, Logger errorLogger) {
        final String townName = town.getName();
        return clickReplacements.getReplacedText(town, (replacementKey, ex) -> {
            errorLogger.log(
                    Level.SEVERE,
                    String.format("Error applying the replacement '%s' for click information on town '%s'!", replacementKey, townName),
                    ex
            );
        });
    }

    public String getHoverTooltip(Town town, Logger errorLogger) {
        final String townName = town.getName();
        return hoverReplacements.getReplacedText(town, (replacementKey, ex) -> {
            errorLogger.log(
                    Level.SEVERE,
                    String.format("Error applying the replacement '%s' for hover information on town '%s'!", replacementKey, townName),
                    ex
            );
        });
    }


    // Utility Methods

    private void copyFile(Logger errorLogger, String resource, File dest) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resource);

        if (input == null) {
            errorLogger.severe("The resource file '"  + resource + "' could not be found!");
            return;
        }

        try {
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            errorLogger.log(Level.SEVERE, "Unable to copy \"" + resource + "\" to plugin directory!");
        }
    }

    // Read file as string
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    private static String readFile(File file)
            throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }

}