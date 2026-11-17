package de.moinmorni.baumeister.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Themen-Wahl zu Beginn einer Runde: drei Vorschläge, jede Person eine Stimme (umentscheiden geht), die Mehrheit
 * gewinnt. Gleichstand (auch: niemand hat gewählt): das Los unter den Gleichstarken.
 */
public final class ThemeVote {

    private final List<Theme> options;
    private final Map<UUID, Integer> votes = new HashMap<>();

    public ThemeVote(List<Theme> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("keine Vorschläge");
        }
        this.options = List.copyOf(options);
    }

    public List<Theme> options() {
        return options;
    }

    /** Stimme für Vorschlag {@code index} (0 …); false bei falscher Zahl. */
    public boolean vote(UUID player, int index) {
        if (index < 0 || index >= options.size()) {
            return false;
        }
        votes.put(player, index);
        return true;
    }

    /** Gewählter Vorschlag (−1 = noch keiner). */
    public int voteOf(UUID player) {
        return votes.getOrDefault(player, -1);
    }

    public void forget(UUID player) {
        votes.remove(player);
    }

    public int count(int index) {
        return (int) votes.values().stream().filter(v -> v == index).count();
    }

    public int voters() {
        return votes.size();
    }

    /** Ergebnis: meiste Stimmen, bei Gleichstand das Los. */
    public Theme winner(Random random) {
        int best = -1;
        List<Integer> top = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            int c = count(i);
            if (c > best) {
                best = c;
                top.clear();
            }
            if (c == best) {
                top.add(i);
            }
        }
        return options.get(top.get(random.nextInt(top.size())));
    }
}
