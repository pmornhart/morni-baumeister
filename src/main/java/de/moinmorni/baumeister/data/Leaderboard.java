package de.moinmorni.baumeister.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Bestenliste der Baumeister: meiste Siege, dann öfter auf dem Podest, dann beste Wertung, dann mehr Sterne, dann
 * weniger Runden. Gespeichert werden höchstens {@link #KEEP} Einträge.
 */
public final class Leaderboard {

    public static final int KEEP = 50;
    private static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::wins).reversed()
            .thenComparing(Comparator.comparingInt(Entry::podiums).reversed())
            .thenComparing(Comparator.comparingInt(Entry::best).reversed())
            .thenComparing(Comparator.comparingInt(Entry::points).reversed())
            .thenComparingInt(Entry::games)
            .thenComparing(Entry::name);

    public record Entry(UUID uuid, String name, int wins, int podiums, int best, int points, int games) {
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public void update(PlayerData d) {
        entries.put(d.uuid(), new Entry(d.uuid(), d.name(), d.wins(), d.podiums(), d.bestScore(), d.points(),
                d.games()));
        if (entries.size() > KEEP) {
            List<Entry> sorted = top(entries.size());
            entries.remove(sorted.get(sorted.size() - 1).uuid());
        }
    }

    public List<Entry> top(int n) {
        List<Entry> all = new ArrayList<>(entries.values());
        all.sort(ORDER);
        return all.subList(0, Math.min(n, all.size()));
    }

    public int rank(UUID uuid) {
        List<Entry> all = top(entries.size());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        for (Entry e : top(entries.size())) {
            String k = "spieler." + e.uuid();
            y.set(k + ".name", e.name());
            y.set(k + ".siege", e.wins());
            y.set(k + ".podest", e.podiums());
            y.set(k + ".beste-wertung", e.best());
            y.set(k + ".sterne", e.points());
            y.set(k + ".runden", e.games());
        }
        return y.saveToString();
    }

    public static Leaderboard fromYaml(String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        Leaderboard b = new Leaderboard();
        ConfigurationSection s = y.getConfigurationSection("spieler");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    b.entries.put(u, new Entry(u, s.getString(k + ".name", "?"), s.getInt(k + ".siege"),
                            s.getInt(k + ".podest"), s.getInt(k + ".beste-wertung"), s.getInt(k + ".sterne"),
                            s.getInt(k + ".runden")));
                } catch (IllegalArgumentException ignored) {
                    // kaputter Eintrag
                }
            }
        }
        return b;
    }
}
