package de.moinmorni.baumeister.data;

import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Werte einer Person über alle Runden: gespielt, gewonnen, auf dem Podest, Sterne bekommen, Stimmen abgegeben,
 * „Meisterwerk“-Stimmen bekommen, gesetzte Blöcke und die beste Wertung (Durchschnitt in Hundertsteln, 435 = 4,35).
 */
public final class PlayerData {

    /** Was eine Person in einer Runde geschafft hat. */
    public record Round(boolean won, boolean podium, int points, int votesGiven, int masterworks, int blocks,
            int score) {
    }

    private final UUID uuid;
    private String name;
    private int games;
    private int wins;
    private int podiums;
    private int points;
    private int votesGiven;
    private int masterworks;
    private int blocks;
    private int bestScore;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int wins() {
        return wins;
    }

    public int podiums() {
        return podiums;
    }

    /** Alle Sterne, die ihre Bauten bekommen haben. */
    public int points() {
        return points;
    }

    public int votesGiven() {
        return votesGiven;
    }

    public int masterworks() {
        return masterworks;
    }

    public int blocks() {
        return blocks;
    }

    /** Beste Wertung einer Runde in Hundertsteln (0 = noch keine). */
    public int bestScore() {
        return bestScore;
    }

    /** Eine Runde zu Ende: Werte hochzählen; {@code true} bei neuer Bestwertung. */
    public boolean add(Round r) {
        games++;
        if (r.won()) {
            wins++;
        }
        if (r.podium()) {
            podiums++;
        }
        points += Math.max(0, r.points());
        votesGiven += Math.max(0, r.votesGiven());
        masterworks += Math.max(0, r.masterworks());
        blocks += Math.max(0, r.blocks());
        if (r.score() > bestScore) {
            bestScore = r.score();
            return true;
        }
        return false;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("runden", games);
        y.set("siege", wins);
        y.set("podest", podiums);
        y.set("sterne", points);
        y.set("stimmen", votesGiven);
        y.set("meisterwerke", masterworks);
        y.set("bloecke", blocks);
        y.set("beste-wertung", bestScore);
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.games = y.getInt("runden");
        d.wins = y.getInt("siege");
        d.podiums = y.getInt("podest");
        d.points = y.getInt("sterne");
        d.votesGiven = y.getInt("stimmen");
        d.masterworks = y.getInt("meisterwerke");
        d.blocks = y.getInt("bloecke");
        d.bestScore = y.getInt("beste-wertung");
        return d;
    }
}
