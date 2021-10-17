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

package me.silverwolfg11.pl3xmaptowny.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

// A basic text replacement class that performs
// replacing text based an object passed in.

// The replacements are cached to speed up performance when fetching the replaced text.
public class TextReplacement<T> {

    private final String replacementText;
    private final List<KVPair<String, Function<T, String>>> replacements = new ArrayList<>();

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

        replacements.add(new KVPair<>(key, replacementFunc));

        return true;
    }

    public void unregisterReplacement(@NotNull String key) {
        Objects.requireNonNull(key);
        replacements.removeIf(p -> p.hasKey() && p.key.equals(key));
    }

    @NotNull
    public String getReplacedText(T appliedObj, BiConsumer<String, Exception> exceptionHandler) {
        if (replacementText == null || replacementText.isEmpty())
            return "";

        String text = replacementText;

        for (KVPair<String, Function<T, String>> replacement : replacements) {
            String replacementKey = replacement.key;

            String applied = null;
            // Yes, it's bad to catch general exceptions.
            // However, if there were any expected exceptions, it wouldn't be called an exception.
            try {
                applied = replacement.value.apply(appliedObj);
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(replacementKey, e);
                }
                applied = "[Error]";
            }

            // Replacements are allowed to return a null value.
            if (applied == null)
                applied = "";

            // Use replace because we want to match exactly, not based on regex.
            text = text.replace(replacementKey, applied);
        }

        return text;
    }

    /**
     * Get an empty text replacement object with no replacement text.
     *
     * @param <V> Context-dependent object class that is used to get the replaced text.
     * @return    An empty text replacement object.
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
     * @return              a text replacement object for that specific replacement text.
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
     * @return              a text replacement object for HTML content.
     */
    @NotNull
    public static <V> TextReplacement<V> fromHTML(@NotNull String htmlContent) {
        Objects.requireNonNull(htmlContent);
        return new TextReplacement<>(removeComments(htmlContent));
    }


    private static class KVPair<L, R> {
        final L key;
        final R value;

        KVPair(L key, R value) {
            this.key = key;
            this.value = value;
        }

        boolean hasKey() {
            return key != null;
        }

        boolean hasRight() {
            return value != null;
        }
    }
}
