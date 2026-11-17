package de.moinmorni.baumeister.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Schutz: Wortliste auf Schildern, Links, Redstone-Bremse, Grenzen. */
class GuardTest {

    private static final WordFilter FILTER = new WordFilter(List.of("arsch", "idiot", "Hurensohn"),
            List.of("barsch", "marsch"), true);

    @Test
    void blockedWordsAreFoundEvenWhenDisguised() {
        assertFalse(FILTER.ok("Du Arsch"));
        assertFalse(FILTER.ok("ARSCH!"));
        assertFalse(FILTER.ok("4rsch"), "Zahl statt Buchstabe");
        assertFalse(FILTER.ok("a r s c h"), "mit Leerzeichen");
        assertFalse(FILTER.ok("a.r.s.c.h"), "mit Punkten");
        assertFalse(FILTER.ok("du id1ot"));
        assertFalse(FILTER.ok("Hurens0hn!"), "Zahl im Wort");
        assertFalse(FILTER.ok("Ärsche"), "Umlaut"); 
        assertEquals("arsch", FILTER.reason("blödarsch"));
    }

    @Test
    void harmlessWordsStayAllowed() {
        assertTrue(FILTER.ok("Almhütte am Bergsee"));
        assertTrue(FILTER.ok("Frischer Barsch"), "erlaubtes Wort");
        assertTrue(FILTER.ok("Marschmusik"));
        assertTrue(FILTER.ok(""));
        assertTrue(FILTER.ok(null));
        assertTrue(FILTER.ok("Bar Schild"), "zwei Wörter zählen einzeln");
        assertTrue(FILTER.ok("Gipfel 2026 – 3 Stunden"));
    }

    @Test
    void linksAreBlockedIfWanted() {
        assertEquals("link", FILTER.reason("komm auf meinserver.net"));
        assertEquals("link", FILTER.reason("www.x.de"));
        assertEquals("link", FILTER.reason("1.2.3.4"));
        assertNull(FILTER.reason("z.B. ok."));
        assertTrue(new WordFilter(List.of(), List.of(), false).ok("meinserver.net"));
    }

    @Test
    void redstoneClockIsStoppedThenRemoved() {
        Throttle t = new Throttle(10);
        long tick = 1000;
        for (int i = 0; i < 10; i++) {
            assertEquals(Throttle.Verdict.OK, t.hit(3, tick));
        }
        assertEquals(Throttle.Verdict.STOP, t.hit(3, tick + 1), "11. in dieser Sekunde");
        assertEquals(Throttle.Verdict.OK, t.hit(4, tick + 1), "andere Parzelle zählt für sich");
        // Zwei weitere Sekunden hintereinander drüber → abbauen.
        Throttle.Verdict last = Throttle.Verdict.OK;
        for (int s = 1; s <= 2; s++) {
            for (int i = 0; i < 12; i++) {
                last = t.hit(3, tick + 20L * s + i);
            }
        }
        assertEquals(Throttle.Verdict.REMOVE, last);
        // Eine ruhige Sekunde dazwischen: die Zählung beginnt neu.
        Throttle calm = new Throttle(10);
        for (int s = 0; s < 6; s += 2) {
            Throttle.Verdict v = Throttle.Verdict.OK;
            for (int i = 0; i < 12; i++) {
                v = calm.hit(1, 20L * s + i);
            }
            assertEquals(Throttle.Verdict.STOP, v, "nie hintereinander → nur stoppen");
        }
        assertEquals(Throttle.Verdict.OK, new Throttle(0).hit(1, 5), "0 = keine Grenze");
    }

    @Test
    void limits() {
        Limits l = Limits.defaults();
        assertTrue(l.entityAllowed(11));
        assertFalse(l.entityAllowed(12));
        assertTrue(l.liquidAllowed(23));
        assertFalse(l.liquidAllowed(24));
        assertFalse(new Limits(0, 0, 64, 0).entityAllowed(0));
    }
}
