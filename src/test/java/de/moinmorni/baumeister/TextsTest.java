package de.moinmorni.baumeister;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.baumeister.game.PlotState;
import de.moinmorni.baumeister.logic.Rating;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.logic.Variant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "sky", "au", "em", "dark_gray", "b", "bold",
            "red", "gold", "yellow", "green", "blue", "black", "dark_green", "dark_blue");
    /** Namen von Standard-Tags: ein Platzhalter darf nie so heißen. */
    private static final Set<String> STANDARD = Set.of("red", "blue", "green", "gold", "yellow", "aqua", "black", "b",
            "i", "u", "st", "bold", "italic", "white", "gray", "grey", "dark_gray", "br", "reset", "obf", "newline",
            "score", "lang", "key", "font", "click", "hover", "insert", "rainbow", "gradient", "transition", "nbt");
    // Tags wie <name> oder </gray>.
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    /** Text-Schlüssel als feste Zeichenkette im Quelltext, z. B. "game.rate.plot". */
    // https://regex101.com/?flavor=java&regex=%22((%3F%3Ahall%7Cgame%29%5C.%5Ba-z0-9.%5C-%5D*%5Ba-z0-9%5D)%22&testString=Texts.msg(lang%2C%20%22game.rate.plot%22)%20%22variant.%22
    private static final Pattern KEY = Pattern.compile("\"((?:hall|item|menu|game|theme|rate|award|letter|tools|guard"
            + "|bar|side|spectate|leave|stats|top|board|lobby|only|unknown|howto|variant|banner|category)[.\\-]"
            + "[a-z0-9.\\-]*[a-z0-9])\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
            for (String p : placeholders(de)) {
                assertFalse(STANDARD.contains(p), "Platzhalter heißt wie ein Standard-Tag: " + key + " <" + p + ">");
            }
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("sky", Tag.styling(TextColor.color(Texts.SKY))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "award.won.title"), Texts.raw("en", "award.won.title"));
        assertEquals(Texts.raw("de", "variant.paare"), Texts.raw("fr", "variant.paare"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    /** Jede Spielart, Wertung, Kategorie, Uhrzeit, jedes Kapitel und Banner hat seine Texte. */
    @Test
    void everyGameThingHasAName() {
        Set<String> keys = Texts.keys();
        for (Variant v : Variant.values()) {
            assertTrue(keys.contains("variant." + v.id()) && keys.contains("variant." + v.id() + ".desc"), v.id());
        }
        for (Rating r : Rating.values()) {
            assertTrue(keys.contains("rate." + r.id()) && keys.contains("rate." + r.id() + ".item"), r.id());
        }
        for (Theme.Category c : Theme.Category.values()) {
            assertTrue(keys.contains("category." + c.name().toLowerCase(Locale.ROOT)), c.name());
        }
        for (String t : PlotState.TIME_IDS) {
            assertTrue(keys.contains("tools.time." + t), t);
        }
        for (String k : new String[] {"theme", "build", "helps", "rate", "win", "rules"}) {
            for (String s : new String[] {"", ".text", ".chat"}) {
                assertTrue(keys.contains("howto." + k + s), k + s);
            }
        }
        for (String b : new String[] {"weissblau", "smaragd", "gold", "rotweiss", "gipfel", "tanne", "sonne", "ziegel",
            "nacht"}) {
            assertTrue(keys.contains("banner." + b), b);
        }
        for (String b : new String[] {"floor", "time", "weather.sun", "weather.rain", "heads", "banners", "fill",
            "replace", "clear", "undo", "wand", "forget", "home"}) {
            assertTrue(keys.contains("tools." + b) && keys.contains("tools." + b + ".lore"), b);
        }
        for (String p : new String[] {"theme", "build", "rate", "award", "over"}) {
            assertTrue(keys.contains("side.phase." + p), p);
        }
    }

    /** Jeder Schlüssel, der im Quelltext steht, hat einen Text. */
    @Test
    void everyKeyUsedInTheCodeExists() throws IOException {
        Set<String> missing = new TreeSet<>();
        int found = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    found++;
                    if (!Texts.keys().contains(m.group(1))) {
                        missing.add(m.group(1) + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(found > 150, "nur " + found + " Schlüssel gefunden – stimmt das Muster?");
        assertEquals(Set.of(), missing);
    }

    @Test
    void numbersAndClock() {
        assertEquals("1:05", Ui.clock(65));
        assertEquals("5 Minuten", Ui.minutes(300, "de"));
        assertEquals("1 minute", Ui.minutes(60, "en"));
        assertEquals("2:30 Minuten", Ui.minutes(150, "de"));
        assertEquals("45 seconds", Ui.minutes(45, "en"));
    }
}
