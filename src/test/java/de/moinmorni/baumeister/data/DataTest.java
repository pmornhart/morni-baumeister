package de.moinmorni.baumeister.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spielerdaten und Bestenliste: zählen, speichern, wieder lesen, Reihenfolge. */
class DataTest {

    private static final UUID ANNA = new UUID(0, 1);
    private static final UUID BERT = new UUID(0, 2);
    private static final UUID CARL = new UUID(0, 3);

    @Test
    void roundsAddUpAndSurviveSaving() {
        PlayerData d = new PlayerData(ANNA, "Anna");
        assertTrue(d.add(new PlayerData.Round(true, true, 18, 3, 2, 140, 450)), "erste Wertung ist Bestwert");
        assertFalse(d.add(new PlayerData.Round(false, false, 6, 4, 0, 30, 200)));
        PlayerData back = PlayerData.fromYaml(ANNA, d.toYaml());
        assertEquals(2, back.games());
        assertEquals(1, back.wins());
        assertEquals(1, back.podiums());
        assertEquals(24, back.points());
        assertEquals(7, back.votesGiven());
        assertEquals(2, back.masterworks());
        assertEquals(170, back.blocks());
        assertEquals(450, back.bestScore());
        assertEquals("Anna", back.name());
    }

    @Test
    void leaderboardOrder() {
        Leaderboard b = new Leaderboard();
        PlayerData a = new PlayerData(ANNA, "Anna");
        a.add(new PlayerData.Round(true, true, 10, 1, 1, 1, 400));
        PlayerData bert = new PlayerData(BERT, "Bert");
        bert.add(new PlayerData.Round(true, true, 10, 1, 1, 1, 480));
        PlayerData c = new PlayerData(CARL, "Carl");
        c.add(new PlayerData.Round(false, true, 30, 1, 1, 1, 300));
        b.update(a);
        b.update(bert);
        b.update(c);
        assertEquals("Bert", b.top(3).get(0).name(), "gleich viele Siege: beste Wertung");
        assertEquals(3, b.rank(CARL));
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(b.top(3), back.top(3));
    }
}
