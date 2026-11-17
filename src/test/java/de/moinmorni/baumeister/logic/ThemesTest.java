package de.moinmorni.baumeister.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Themen-Liste, Vorschläge und Themen-Wahl. */
class ThemesTest {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    @Test
    void bigOwnListInBothLanguages() {
        List<Theme> all = Themes.all();
        assertTrue(all.size() >= 120, "nur " + all.size() + " Themen");
        Set<String> de = new HashSet<>();
        Set<String> en = new HashSet<>();
        for (Theme t : all) {
            assertTrue(ID.matcher(t.id()).matches(), t.id());
            assertFalse(t.de().isBlank(), t.id());
            assertFalse(t.en().isBlank(), t.id());
            assertTrue(de.add(t.de()), "deutscher Name doppelt: " + t.de());
            assertTrue(en.add(t.en()), "englischer Name doppelt: " + t.en());
            assertEquals(t, Themes.byId(t.id().toUpperCase()));
        }
        for (Theme.Category c : Theme.Category.values()) {
            assertTrue(all.stream().filter(t -> t.category() == c).count() >= 10, c + " hat zu wenige Themen");
        }
        assertNull(Themes.byId("gibtsnicht"));
        assertNotNull(Themes.byId("almhuette"));
        assertNotNull(Themes.byId("maibaum"));
        assertNotNull(Themes.byId("brotzeit"));
        assertNotNull(Themes.byId("drache"));
    }

    @Test
    void threeSuggestionsFromDifferentCategoriesNotRecent() {
        for (int seed = 0; seed < 300; seed++) {
            List<String> recent = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                recent.add(Themes.all().get((seed * 7 + i * 3) % Themes.all().size()).id());
            }
            List<Theme> s = Themes.suggest(new Random(seed), 3, recent);
            assertEquals(3, s.size());
            assertEquals(3, new HashSet<>(s).size(), "keine doppelten");
            assertEquals(3, s.stream().map(Theme::category).distinct().count(), "drei Kategorien");
            for (Theme t : s) {
                assertFalse(recent.contains(t.id()), "zuletzt gespielt: " + t.id());
            }
        }
    }

    @Test
    void ifEverythingWasPlayedRecentlyThereAreStillSuggestions() {
        List<String> all = Themes.all().stream().map(Theme::id).toList();
        assertEquals(3, Themes.suggest(new Random(1), 3, all).size());
    }

    @Test
    void majorityWinsTieByLot() {
        UUID a = new UUID(0, 1);
        UUID b = new UUID(0, 2);
        UUID c = new UUID(0, 3);
        List<Theme> options = Themes.suggest(new Random(5), 3, List.of());
        ThemeVote v = new ThemeVote(options);
        assertTrue(v.vote(a, 2));
        assertTrue(v.vote(b, 2));
        assertTrue(v.vote(c, 0));
        assertFalse(v.vote(c, 3), "nur 0–2");
        assertEquals(options.get(2), v.winner(new Random(1)));
        v.vote(b, 0);
        assertEquals(1, v.count(2));
        assertEquals(2, v.count(0));
        assertEquals(options.get(0), v.winner(new Random(1)), "umentschieden");
        v.forget(c);
        Set<Theme> winners = new HashSet<>();
        Random lot = new Random(42);
        for (int i = 0; i < 50; i++) {
            winners.add(v.winner(lot));
        }
        assertEquals(Set.of(options.get(0), options.get(2)), winners, "Gleichstand: nur die Gleichstarken");
        ThemeVote nobody = new ThemeVote(options);
        Set<Theme> any = new HashSet<>();
        for (int i = 0; i < 80; i++) {
            any.add(nobody.winner(lot));
        }
        assertEquals(3, any.size(), "ohne Stimmen: alle drei möglich");
    }
}
