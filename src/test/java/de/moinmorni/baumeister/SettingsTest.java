package de.moinmorni.baumeister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.baumeister.logic.Limits;
import de.moinmorni.baumeister.logic.Variant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Die mitgelieferte config.yml passt zu den Vorgaben im Code; unsinnige Werte werden gestutzt. */
class SettingsTest {

    private static YamlConfiguration shipped() throws IOException, InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(Files.readString(Path.of("src/main/resources/config.yml"), StandardCharsets.UTF_8));
        return y;
    }

    @Test
    void shippedConfigEqualsTheDefaults() throws Exception {
        Settings s = Settings.load(shipped());
        Settings empty = Settings.load(new YamlConfiguration());
        assertEquals(90, s.comebackSeconds());
        assertEquals(300, s.buildSeconds());
        assertEquals(120, s.quickBuildSeconds());
        assertEquals(28, s.plotSize());
        assertEquals(40, s.plotHeight());
        assertEquals(Limits.defaults(), s.limits());
        assertEquals(Set.of(Variant.values()), s.variants());
        assertTrue(s.themeVote());
        assertFalse(s.filter().ok("du arsch"), "Wortliste aus der config.yml");
        assertTrue(s.filter().ok("Frischer Barsch"));
        // Ohne Wortliste ist alles bis auf die Liste gleich.
        assertEquals(empty.limits(), s.limits());
        assertEquals(empty.buildSeconds(), s.buildSeconds());
        assertEquals(empty.comebackSeconds(), s.comebackSeconds());
        assertEquals(empty.rateSeconds(), s.rateSeconds());
        assertEquals(empty.variants(), s.variants());
        assertEquals(empty.maxPlayers(), s.maxPlayers());
        assertEquals(empty.hallMinPlayers(), s.hallMinPlayers());
        assertEquals(empty.themeSeconds(), s.themeSeconds());
        assertEquals(empty.awardSeconds(), s.awardSeconds());
        assertEquals(empty.toolCooldown(), s.toolCooldown());
        assertEquals(120, s.buildSeconds(Variant.SCHNELL));
        assertEquals(300, s.buildSeconds(Variant.PAARE));
    }

    @Test
    void everyKeyInTheShippedConfigIsRead() throws Exception {
        YamlConfiguration y = shipped();
        String code = Files.readString(Path.of("src/main/java/de/moinmorni/baumeister/Settings.java"),
                StandardCharsets.UTF_8);
        for (String key : y.getKeys(true)) {
            if (!y.isConfigurationSection(key)) {
                assertTrue(code.contains("\"" + key + "\""), "config.yml-Schlüssel wird nicht gelesen: " + key);
            }
        }
    }

    @Test
    void sillyValuesAreClamped() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("rueckkehr-sekunden", 99999);
        y.set("runde.bauzeit-sekunden", 1);
        y.set("runde.hoechstens-spieler", 99);
        y.set("parzelle.groesse", 31);
        y.set("parzelle.hoehe", 999);
        y.set("schutz.redstone-pro-sekunde", 3);
        y.set("werkzeug.hoechstens-bloecke", 5);
        y.set("spielarten.schnell", false);
        y.set("spielarten.paare", false);
        Settings s = Settings.load(y);
        assertEquals(600, s.comebackSeconds());
        assertEquals(60, s.buildSeconds());
        assertEquals(16, s.maxPlayers());
        assertEquals(30, s.plotSize(), "gerade Zahl");
        assertEquals(64, s.plotHeight());
        assertEquals(10, s.limits().redstonePerSecond());
        assertEquals(16, s.limits().toolBlocks());
        assertEquals(Set.of(Variant.KLASSISCH), s.variants(), "klassisch geht immer");
        y.set("schutz.redstone-pro-sekunde", 0);
        assertEquals(0, Settings.load(y).limits().redstonePerSecond(), "0 = keine Grenze");
    }
}
