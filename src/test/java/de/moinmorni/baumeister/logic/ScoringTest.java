package de.moinmorni.baumeister.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Wertung: eigene Parzelle nicht, Durchschnitt, Gleichstand, geteilte Plätze, Sieg und Podest. */
class ScoringTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID D = new UUID(0, 4);

    @Test
    void ownPlotAndPartnerPlotCannotBeRated() {
        Scoring s = new Scoring();
        s.plot(0, Set.of(A));
        s.plot(1, Set.of(B, C));
        assertFalse(s.rate(A, 0, 5), "eigene Parzelle");
        assertFalse(s.rate(B, 1, 5), "eigene Parzelle im Paar");
        assertFalse(s.rate(C, 1, 5), "Partner");
        assertTrue(s.rate(B, 0, 4));
        assertFalse(s.rate(B, 0, 0), "0 Sterne gibt es nicht");
        assertFalse(s.rate(B, 0, 6), "6 Sterne gibt es nicht");
        assertFalse(s.rate(B, 7, 3), "unbekannte Parzelle");
        assertEquals(4, s.ratingOf(B, 0));
        assertTrue(s.rate(B, 0, 2), "umentscheiden");
        assertEquals(2, s.ratingOf(B, 0));
        assertEquals(1, s.given(B));
    }

    @Test
    void averageCountsNotTheNumberOfVoters() {
        Scoring s = new Scoring();
        s.plot(0, Set.of(A));
        s.plot(1, Set.of(B));
        s.plot(2, Set.of(C));
        // Parzelle 0: 5, 5, 4 → 4,67; Parzelle 1: nur eine Stimme 5 → 5,00; Parzelle 2: keine Stimme.
        s.rate(B, 0, 5);
        s.rate(C, 0, 5);
        s.rate(D, 0, 4);
        s.rate(A, 1, 5);
        List<Scoring.Result> r = s.results();
        assertEquals(1, r.get(0).plot());
        assertEquals(500, r.get(0).average());
        assertEquals(0, r.get(1).plot());
        assertEquals(467, r.get(1).average());
        assertEquals(14, r.get(1).sum());
        assertEquals(2, r.get(1).fives());
        assertEquals(2, r.get(2).plot());
        assertEquals(0, r.get(2).votes());
        assertEquals(List.of(1, 2, 3), r.stream().map(Scoring.Result::place).toList());
    }

    @Test
    void tieBreaksBySumThenMasterpiecesThenSharedPlace() {
        Scoring s = new Scoring();
        for (int i = 0; i < 4; i++) {
            s.plot(i, Set.of(new UUID(1, i)));
        }
        UUID[] v = {A, B, C, D};
        // 0: 4,4 (Summe 8)  1: 4,4,4 (Summe 12, mehr Sterne)  2: 5,3 (Summe 8, ein Meisterwerk)  3: 4,4 wie 0
        s.rate(v[0], 0, 4);
        s.rate(v[1], 0, 4);
        s.rate(v[0], 1, 4);
        s.rate(v[1], 1, 4);
        s.rate(v[2], 1, 4);
        s.rate(v[0], 2, 5);
        s.rate(v[1], 2, 3);
        s.rate(v[2], 3, 4);
        s.rate(v[3], 3, 4);
        List<Scoring.Result> r = s.results();
        assertEquals(1, r.get(0).plot(), "gleicher Durchschnitt, mehr Sterne");
        assertEquals(2, r.get(1).plot(), "dann mehr Meisterwerke");
        assertEquals(List.of(1, 2, 3, 3), r.stream().map(Scoring.Result::place).toList(), "0 und 3 teilen Platz 3");
        Scoring two = new Scoring();
        two.plot(0, Set.of(A));
        two.plot(1, Set.of(B));
        two.rate(B, 0, 3);
        two.rate(A, 1, 3);
        List<Scoring.Result> t = two.results();
        assertEquals(1, t.get(0).place());
        assertEquals(1, t.get(1).place(), "zwei Erste");
        assertTrue(Placement.won(t.get(0), 2));
        assertTrue(Placement.won(t.get(1), 2));
    }

    @Test
    void winAndPodiumNeedVotesAndAnOpponent() {
        Scoring.Result first = new Scoring.Result(0, 450, 9, 2, 1, 1);
        Scoring.Result third = new Scoring.Result(1, 300, 6, 2, 0, 3);
        Scoring.Result fourth = new Scoring.Result(2, 200, 4, 2, 0, 4);
        Scoring.Result noVotes = new Scoring.Result(3, 0, 0, 0, 0, 1);
        assertTrue(Placement.won(first, 3));
        assertFalse(Placement.won(first, 1), "allein gewinnt man nicht");
        assertFalse(Placement.won(noVotes, 3));
        assertTrue(Placement.podium(third, 4));
        assertFalse(Placement.podium(fourth, 4));
        assertEquals("4,50", Placement.stars(450, "de"));
        assertEquals("4.05", Placement.stars(405, "en"));
        assertEquals("0,00", Placement.stars(0, "de"));
    }

    @Test
    void ratingsOneToFive() {
        assertEquals(5, Rating.values().length);
        assertEquals(Rating.NAJA, Rating.ofStars(1));
        assertEquals(Rating.MEISTERWERK, Rating.ofStars(5));
        assertEquals("geht-so", Rating.GEHT_SO.id());
        assertEquals(null, Rating.ofStars(0));
    }
}
