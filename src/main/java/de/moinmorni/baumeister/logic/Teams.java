package de.moinmorni.baumeister.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wer baut mit wem? Allein: jede Person bekommt ihre eigene Parzelle. Zu zweit: Partys werden zuerst in Paare geteilt
 * (eine Party aus drei gibt ein Paar und eine Person, die auf einen Partner wartet), danach finden die Übrigen in der
 * Reihenfolge, in der sie da waren, zusammen. Bleibt eine Person übrig, baut sie allein.
 */
public final class Teams {

    private Teams() {
    }

    public static List<List<UUID>> form(List<List<UUID>> groups, boolean pairs) {
        List<List<UUID>> out = new ArrayList<>();
        if (!pairs) {
            for (List<UUID> g : groups) {
                for (UUID u : g) {
                    out.add(List.of(u));
                }
            }
            return out;
        }
        List<UUID> singles = new ArrayList<>();
        for (List<UUID> g : groups) {
            int i = 0;
            for (; i + 1 < g.size(); i += 2) {
                out.add(List.of(g.get(i), g.get(i + 1)));
            }
            if (i < g.size()) {
                singles.add(g.get(i));
            }
        }
        int i = 0;
        for (; i + 1 < singles.size(); i += 2) {
            out.add(List.of(singles.get(i), singles.get(i + 1)));
        }
        if (i < singles.size()) {
            out.add(List.of(singles.get(i)));
        }
        return out;
    }
}
