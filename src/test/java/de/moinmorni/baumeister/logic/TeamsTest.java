package de.moinmorni.baumeister.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Wer mit wem baut, wer in die Runde kommt, Abstimmung über die Spielart. */
class TeamsTest {

    private static UUID u(int i) {
        return new UUID(0, i);
    }

    @Test
    void aloneEveryoneHasAPlot() {
        List<List<UUID>> t = Teams.form(List.of(List.of(u(1), u(2)), List.of(u(3))), false);
        assertEquals(List.of(List.of(u(1)), List.of(u(2)), List.of(u(3))), t);
    }

    @Test
    void pairsKeepPartiesTogether() {
        // Party Anna+Carl, Bert allein, Party aus drei (Dora, Emil, Fritz), Gustl allein.
        List<List<UUID>> groups = List.of(List.of(u(1), u(3)), List.of(u(2)), List.of(u(4), u(5), u(6)), List.of(u(7)));
        List<List<UUID>> t = Teams.form(groups, true);
        assertTrue(t.contains(List.of(u(1), u(3))), "Party zusammen");
        assertTrue(t.contains(List.of(u(4), u(5))), "große Party: die ersten zwei");
        assertTrue(t.contains(List.of(u(2), u(6))), "Übrige finden zusammen");
        assertTrue(t.contains(List.of(u(7))), "einer übrig: allein");
        assertEquals(4, t.size());
    }

    @Test
    void groupsFitTheRound() {
        List<UUID> picked = Groups.pick(List.of(List.of(u(1), u(2)), List.of(u(3), u(4), u(5)), List.of(u(6))), 4);
        assertEquals(List.of(u(1), u(2), u(6)), picked, "die Dreier-Party wartet, Kleine rücken nach");
    }

    @Test
    void variantVote() {
        Votes<Variant> v = new Votes<>(Variant.class);
        v.vote(u(1), Variant.PAARE);
        v.vote(u(2), Variant.PAARE);
        v.vote(u(3), Variant.SCHNELL);
        assertEquals(Variant.PAARE, v.winner(EnumSet.allOf(Variant.class)));
        assertEquals(Variant.SCHNELL, v.winner(EnumSet.of(Variant.KLASSISCH, Variant.SCHNELL)), "zu zweit nicht möglich");
        assertFalse(Variant.PAARE.possible(3));
        assertTrue(Variant.PAARE.possible(4));
        assertTrue(Variant.KLASSISCH.possible(2));
        assertEquals(Variant.PAARE, Variant.byId("duo"));
        assertEquals(Variant.SCHNELL, Variant.byId("Schnell"));
        assertEquals(null, Variant.byId("x"));
    }
}
