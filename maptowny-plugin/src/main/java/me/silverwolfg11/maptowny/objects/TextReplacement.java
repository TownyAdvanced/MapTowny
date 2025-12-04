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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

// A basic text replacement class that performs
// replacing text based an object passed in.

// The replacements are cached to speed up performance when fetching the replaced text.
// This class is not thread-safe.
public class TextReplacement<T> {

    private final @Nullable String replacementText;
    private final Map<String, Function<T, String>> replacements = new HashMap<>();
    private List<ReplacementLoc<T>> sortedReplacements = null;

    private TextReplacement(@Nullable String replacementText) {
        this.replacementText = replacementText;
    }

    public boolean hasTextToReplace() {
        return replacementText != null;
    }

    @Nullable
    public String getTextToReplace() {
        return replacementText;
    }

    public boolean registerReplacement(@NotNull String key, @NotNull Function<T, String> replacementFunc) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(replacementFunc);
        if (replacementText == null || !replacementText.contains(key)) {
            return false;
        }

        replacements.put(key, replacementFunc);
        sortedReplacements = null;

        return true;
    }

    public void unregisterReplacement(@NotNull String key) {
        Objects.requireNonNull(key);
        boolean removed = replacements.remove(key) != null;

        // We can avoid re-sorting replacements since
        // the positions won't shift during removal.
        if (removed && sortedReplacements != null) {
            sortedReplacements.removeIf(loc -> loc.key().equals(key));
        }
    }

    // Sort the replacements based on the index
    // of their keys in the original text.
    // This builds a list of ALL occurrences of ALL registered keys.
    public void sortReplacements() {
        if (sortedReplacements != null) {
            return;
        }

        if (replacementText == null) {
            sortedReplacements = Collections.emptyList();
            return;
        }

        List<ReplacementLoc<T>> locations = new ArrayList<>();

        // For each registered replacement key, find ALL occurrences in the text
        for (Map.Entry<String, Function<T, String>> entry : replacements.entrySet()) {
            String key = entry.getKey();
            int index = 0;

            // Find all occurrences of this key
            while ((index = replacementText.indexOf(key, index)) != -1) {
                locations.add(new ReplacementLoc<>(index, key, entry.getValue()));
                index += key.length();
            }
        }

        // Sort by position in text
        locations.sort(Comparator.comparingInt(ReplacementLoc::replacementTxtIdx));
        sortedReplacements = locations;
    }

    @NotNull
    public String getReplacedText(T appliedObj, BiConsumer<String, Exception> exceptionHandler) {
        if (replacementText == null || replacementText.isEmpty())
            return "";

        sortReplacements();

        StringBuilder sb = new StringBuilder(replacementText.length());

        int lastPos = 0;

        for (ReplacementLoc<T> replacement : sortedReplacements) {
            // Append text from last position up to this replacement
            sb.append(replacementText, lastPos, replacement.replacementTxtIdx());

            // Apply the replacement function
            String applied;
            try {
                applied = replacement.replacementFunc().apply(appliedObj);
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(replacement.key(), e);
                }
                applied = "[Error]";
            }

            // Replacements are allowed to return a null value
            if (applied == null)
                applied = "";

            sb.append(applied);
            lastPos = replacement.replacementTxtIdx() + replacement.key().length();
        }

        // Append remaining text after the last replacement
        sb.append(replacementText, lastPos, replacementText.length());

        return sb.toString();
    }

    /**
     * Get an empty text replacement object with no replacement text.
     *
     * @param <V> Context-dependent object class that is used to get the replaced text.
     * @return An empty text replacement object.
     */
    @NotNull
    public static <V> TextReplacement<V> empty() {
        return new TextReplacement<V>(null);
    }

    /**
     * Get a text replacement object for specific replacement text.
     *
     * @param textToReplace The base text to apply replacements on.
     * @param <V>           Context-dependent object class that is used to get the replaced text.
     * @return a text replacement object for that specific replacement text.
     */
    @NotNull
    public static <V> TextReplacement<V> fromString(@NotNull String textToReplace) {
        Objects.requireNonNull(textToReplace);
        return new TextReplacement<>(textToReplace);
    }

    // Removes HTML comments from a string.
    private static String removeComments(String htmlStr) {
        return Pattern.compile("<!--.*?-->", Pattern.DOTALL).matcher(htmlStr).replaceAll("");
    }

    /**
     * Get a text replacement object for specific HTML content.
     *
     * This object will remove all HTML comments from the given text.
     *
     * @param htmlContent   HTML content that should be the base text for replacements.
     * @param <V>           Context-dependent object class that is used to get the replaced text.
     * @return a text replacement object for HTML content.
     */
    @NotNull
    public static <V> TextReplacement<V> fromHTML(@NotNull String htmlContent) {
        Objects.requireNonNull(htmlContent);
        return new TextReplacement<>(removeComments(htmlContent));
    }

    // Represents a single replacement location in the text
    private record ReplacementLoc<T>(int replacementTxtIdx, String key, Function<T, String> replacementFunc) {
    }
}
