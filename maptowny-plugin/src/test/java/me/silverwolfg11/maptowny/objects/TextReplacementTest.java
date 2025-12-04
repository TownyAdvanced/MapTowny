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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextReplacement Tests")
class TextReplacementTest {

    // Test data record for replacement context
    record TestData(String name, int value) {
    }

    @Test
    @DisplayName("empty() creates empty TextReplacement")
    void testEmpty() {
        TextReplacement<String> replacement = TextReplacement.empty();

        assertNotNull(replacement);
        assertFalse(replacement.hasTextToReplace());
        assertNull(replacement.getTextToReplace());
    }

    @Test
    @DisplayName("fromString() creates TextReplacement with text")
    void testFromString() {
        String text = "Hello {name}!";
        TextReplacement<String> replacement = TextReplacement.fromString(text);

        assertNotNull(replacement);
        assertTrue(replacement.hasTextToReplace());
        assertEquals(text, replacement.getTextToReplace());
    }

    @Test
    @DisplayName("fromHTML() removes HTML comments")
    void testFromHTMLRemovesComments() {
        String htmlWithComments = "Hello <!-- comment --> world <!-- another comment -->";
        TextReplacement<String> replacement = TextReplacement.fromHTML(htmlWithComments);

        assertEquals("Hello  world ", replacement.getTextToReplace());
    }

    @Test
    @DisplayName("fromHTML() removes multiline HTML comments")
    void testFromHTMLRemovesMultilineComments() {
        String htmlWithComments = "Start <!-- \nmultiline\ncomment\n--> End";
        TextReplacement<String> replacement = TextReplacement.fromHTML(htmlWithComments);

        assertEquals("Start  End", replacement.getTextToReplace());
    }

    @Test
    @DisplayName("fromHTML() creates text from uncommented HTML")
    void testFromHTMLNoComments() {
        String html = "<div>Hello World</div>";
        TextReplacement<String> replacement = TextReplacement.fromHTML(html);

        assertEquals(html, replacement.getTextToReplace());
    }

    @Test
    @DisplayName("registerReplacement() succeeds when key is found")
    void testRegisterReplacementSuccess() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Name: {name}, Value: {value}");

        assertTrue(replacement.registerReplacement("{name}", data -> data.name));
    }

    @Test
    @DisplayName("registerReplacement() fails when key is not found")
    void testRegisterReplacementKeyNotFound() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello World");

        assertFalse(replacement.registerReplacement("{name}", data -> data.name));
    }

    @Test
    @DisplayName("registerReplacement() fails on empty TextReplacement")
    void testRegisterReplacementOnEmpty() {
        TextReplacement<TestData> replacement = TextReplacement.empty();

        assertFalse(replacement.registerReplacement("{name}", data -> data.name));
    }

    @Test
    @DisplayName("unregisterReplacement() removes registered replacement")
    void testUnregisterReplacement() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("{name}");
        replacement.registerReplacement("{name}", data -> data.name);

        replacement.unregisterReplacement("{name}");

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        // After unregistering, the key should not be replaced
        assertEquals("{name}", result);
    }

    @Test
    @DisplayName("getReplacedText() performs simple replacement")
    void testGetReplacedTextSimple() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello {name}!");
        replacement.registerReplacement("{name}", data -> data.name);

        TestData data = new TestData("World", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("Hello World!", result);
    }

    @Test
    @DisplayName("getReplacedText() performs multiple replacements")
    void testGetReplacedTextMultiple() {
        TextReplacement<TestData> replacement = TextReplacement.fromString(
                "Name: {name}, Value: {value}"
        );
        replacement.registerReplacement("{name}", data -> data.name);
        replacement.registerReplacement("{value}", data -> String.valueOf(data.value));

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("Name: Test, Value: 42", result);
    }

    @Test
    @DisplayName("getReplacedText() handles null replacement function result")
    void testGetReplacedTextNullResult() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello {name}!");
        replacement.registerReplacement("{name}", data -> null);

        TestData data = new TestData("World", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("Hello !", result);
    }

    @Test
    @DisplayName("getReplacedText() returns empty string for null replacement text")
    void testGetReplacedTextNullReplacementText() {
        TextReplacement<TestData> replacement = TextReplacement.empty();

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("", result);
    }

    @Test
    @DisplayName("getReplacedText() returns empty string for empty replacement text")
    void testGetReplacedTextEmptyReplacementText() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("");

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("", result);
    }

    @Test
    @DisplayName("getReplacedText() handles exception in replacement function")
    void testGetReplacedTextException() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello {name}!");
        replacement.registerReplacement("{name}", data -> {
            throw new RuntimeException("Test exception");
        });

        List<String> errors = new ArrayList<>();
        TestData data = new TestData("World", 42);
        String result = replacement.getReplacedText(data, (key, ex) -> {
            errors.add(key);
        });

        assertEquals("Hello [Error]!", result);
        assertEquals(1, errors.size());
        assertEquals("{name}", errors.get(0));
    }

    @Test
    @DisplayName("getReplacedText() calls exception handler for each exception")
    void testGetReplacedTextMultipleExceptions() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("{name} {value}");
        replacement.registerReplacement("{name}", data -> {
            throw new RuntimeException("Error 1");
        });
        replacement.registerReplacement("{value}", data -> {
            throw new RuntimeException("Error 2");
        });

        AtomicInteger errorCount = new AtomicInteger(0);
        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, (key, ex) -> errorCount.incrementAndGet());

        assertEquals("[Error] [Error]", result);
        assertEquals(2, errorCount.get());
    }

    @Test
    @DisplayName("getReplacedText() handles exception with null handler")
    void testGetReplacedTextExceptionNullHandler() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello {name}!");
        replacement.registerReplacement("{name}", data -> {
            throw new RuntimeException("Test exception");
        });

        TestData data = new TestData("World", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("Hello [Error]!", result);
    }

    @Test
    @DisplayName("getReplacedText() preserves text without replacements")
    void testGetReplacedTextNoReplacements() {
        TextReplacement<TestData> replacement = TextReplacement.fromString("Hello World!");

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        assertEquals("Hello World!", result);
    }

    @Test
    @DisplayName("getReplacedText() handles replacements in correct order")
    void testGetReplacedTextOrder() {
        TextReplacement<Integer> replacement = TextReplacement.fromString("{a} {b} {c}");

        // Register in non-sequential order
        replacement.registerReplacement("{c}", i -> "3");
        replacement.registerReplacement("{a}", i -> "1");
        replacement.registerReplacement("{b}", i -> "2");

        String result = replacement.getReplacedText(0, null);

        assertEquals("1 2 3", result);
    }

    @Test
    @DisplayName("getReplacedText() handles multiple occurrences of same key")
    void testGetReplacedTextMultipleOccurrences() {
        // The key "{name}" appears multiple times
        TextReplacement<TestData> replacement = TextReplacement.fromString("{name} and {name}");

        boolean registered = replacement.registerReplacement("{name}", data -> data.name);

        assertTrue(registered);

        TestData data = new TestData("Test", 42);
        String result = replacement.getReplacedText(data, null);

        // All occurrences should be replaced
        assertEquals("Test and Test", result);
    }

    @Test
    @DisplayName("getReplacedText() handles replacement at start of text")
    void testGetReplacedTextStartReplacement() {
        TextReplacement<String> replacement = TextReplacement.fromString("{key} rest of text");
        replacement.registerReplacement("{key}", s -> "START");

        String result = replacement.getReplacedText("", null);

        assertEquals("START rest of text", result);
    }

    @Test
    @DisplayName("getReplacedText() handles replacement at end of text")
    void testGetReplacedTextEndReplacement() {
        TextReplacement<String> replacement = TextReplacement.fromString("start of text {key}");
        replacement.registerReplacement("{key}", s -> "END");

        String result = replacement.getReplacedText("", null);

        assertEquals("start of text END", result);
    }

    @Test
    @DisplayName("registerReplacement() can register same key after unregister")
    void testReregisterAfterUnregister() {
        TextReplacement<String> replacement = TextReplacement.fromString("{key}");
        replacement.registerReplacement("{key}", s -> "first");
        replacement.unregisterReplacement("{key}");
        replacement.registerReplacement("{key}", s -> "second");

        String result = replacement.getReplacedText("", null);

        assertEquals("second", result);
    }

    @Test
    @DisplayName("getReplacedText() handles special characters in replacement")
    void testGetReplacedTextSpecialCharacters() {
        TextReplacement<String> replacement = TextReplacement.fromString("Text: {data}");
        replacement.registerReplacement("{data}", s -> "Special: <>&\"'");

        String result = replacement.getReplacedText("", null);

        assertEquals("Text: Special: <>&\"'", result);
    }

    @Test
    @DisplayName("getReplacedText() handles newlines in replacement")
    void testGetReplacedTextNewlines() {
        TextReplacement<String> replacement = TextReplacement.fromString("Text: {data}");
        replacement.registerReplacement("{data}", s -> "Line1\nLine2\nLine3");

        String result = replacement.getReplacedText("", null);

        assertEquals("Text: Line1\nLine2\nLine3", result);
    }

    @Test
    @DisplayName("Different generic types work correctly")
    void testGenericTypes() {
        TextReplacement<Integer> intReplacement = TextReplacement.fromString("Number: {num}");
        intReplacement.registerReplacement("{num}", i -> String.valueOf(i * 2));

        TextReplacement<String> strReplacement = TextReplacement.fromString("String: {str}");
        strReplacement.registerReplacement("{str}", s -> s.toUpperCase());

        assertEquals("Number: 84", intReplacement.getReplacedText(42, null));
        assertEquals("String: HELLO", strReplacement.getReplacedText("hello", null));
    }
}
