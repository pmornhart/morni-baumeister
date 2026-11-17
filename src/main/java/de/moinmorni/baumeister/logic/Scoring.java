package de.moinmorni.baumeister.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bewertung aller Bauten einer Runde. Jede Parzelle bekommt von allen anderen 1–5 Sterne (die eigene Parzelle nicht
 * – im Paar auch nicht die des Partners). Gezählt wird der Durchschnitt der abgegebenen Stimmen, damit es keine Rolle
 * spielt, wie viele gerade bewertet haben. Reihenfolge: höherer Durchschnitt, dann mehr Sterne insgesamt, dann mehr
 * „Meisterwerk“-Stimmen. Ist alles gleich, teilen sich die Bauten den Platz (zwei Erste, dann Platz 3).
 */
public final class Scoring {

    /** Ergebnis einer Parzelle; {@code average} in Hundertsteln (435 = 4,35 Sterne), 0 ohne Stimmen. */
    public record Result(int plot, int average, int sum, int votes, int fives, int place) {
    }

    private final Map<Integer, Map<UUID, Integer>> votes = new LinkedHashMap<>();
    private final Map<Integer, Set<UUID>> owners = new LinkedHashMap<>();

    /** Parzelle anmelden (auch ohne Stimmen kommt sie in die Wertung). */
    public void plot(int plot, Set<UUID> builders) {
        owners.put(plot, Set.copyOf(builders));
        votes.computeIfAbsent(plot, k -> new LinkedHashMap<>());
    }

    /** Darf diese Person die Parzelle bewerten? Nicht die eigene (und nicht die des Partners). */
    public boolean mayRate(UUID voter, int plot) {
        Set<UUID> o = owners.get(plot);
        return o != null && !o.contains(voter);
    }

    /** Stimme abgeben oder ändern; false, wenn es nicht geht (eigene Parzelle, falsche Zahl). */
    public boolean rate(UUID voter, int plot, int stars) {
        if (stars < 1 || stars > 5 || !mayRate(voter, plot)) {
            return false;
        }
        votes.get(plot).put(voter, stars);
        return true;
    }

    /** Stimme dieser Person für diese Parzelle (0 = keine). */
    public int ratingOf(UUID voter, int plot) {
        Map<UUID, Integer> v = votes.get(plot);
        return v == null ? 0 : v.getOrDefault(voter, 0);
    }

    /** Wie viele Stimmen hat diese Person insgesamt abgegeben? */
    public int given(UUID voter) {
        return (int) votes.values().stream().filter(v -> v.containsKey(voter)).count();
    }

    public Set<UUID> owners(int plot) {
        return owners.getOrDefault(plot, Set.of());
    }

    /** Ergebnis aller Parzellen, beste zuerst, mit geteilten Plätzen. */
    public List<Result> results() {
        List<Result> raw = new ArrayList<>();
        for (var e : votes.entrySet()) {
            Map<UUID, Integer> v = e.getValue();
            int sum = v.values().stream().mapToInt(Integer::intValue).sum();
            int n = v.size();
            int fives = (int) v.values().stream().filter(s -> s == 5).count();
            int avg = n == 0 ? 0 : (int) Math.round(sum * 100.0 / n);
            raw.add(new Result(e.getKey(), avg, sum, n, fives, 0));
        }
        Comparator<Result> order = Comparator.comparingInt(Result::average).reversed()
                .thenComparing(Comparator.comparingInt(Result::sum).reversed())
                .thenComparing(Comparator.comparingInt(Result::fives).reversed());
        raw.sort(order.thenComparingInt(Result::plot));
        List<Result> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Result r = raw.get(i);
            int place = i + 1;
            if (i > 0 && order.compare(raw.get(i - 1), r) == 0) {
                place = out.get(i - 1).place();
            }
            out.add(new Result(r.plot(), r.average(), r.sum(), r.votes(), r.fives(), place));
        }
        return out;
    }
}
