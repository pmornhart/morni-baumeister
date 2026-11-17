package de.moinmorni.baumeister;

import de.moinmorni.baumeister.logic.Limits;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.Variant;
import de.moinmorni.baumeister.logic.WordFilter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/** Werte aus der config.yml (Erklärungen stehen dort). Alles wird auf sinnvolle Grenzen gestutzt. */
public record Settings(
        String world,
        int hallCountdown,
        int hallMinPlayers,
        Set<Variant> variants,
        int maxPlayers,
        boolean themeVote,
        int themeSeconds,
        int buildSeconds,
        int quickBuildSeconds,
        int rateSeconds,
        int awardSeconds,
        int comebackSeconds,
        int plotSize,
        int plotHeight,
        int toolCooldown,
        Limits limits,
        WordFilter filter) {

    /** Höchstens so lange wartet eine Runde auf jemanden, der die Verbindung verloren hat. */
    static final int MAX_COMEBACK_SECONDS = 600;

    public static Settings load(ConfigurationSection c) {
        Set<Variant> variants = EnumSet.of(Variant.KLASSISCH);
        if (c.getBoolean("spielarten.schnell", true)) {
            variants.add(Variant.SCHNELL);
        }
        if (c.getBoolean("spielarten.paare", true)) {
            variants.add(Variant.PAARE);
        }
        int redstone = c.getInt("schutz.redstone-pro-sekunde", 60);
        redstone = redstone <= 0 ? 0 : clamp(redstone, 10, 1000);
        Limits limits = new Limits(
                clamp(c.getInt("schutz.wesen", 12), 0, 64),
                clamp(c.getInt("schutz.fluessigkeiten", 24), 0, 256),
                clamp(c.getInt("werkzeug.hoechstens-bloecke", 4096), 16, 32768),
                redstone);
        List<String> blocked = c.getStringList("schutz.gesperrte-woerter");
        List<String> allowed = c.getStringList("schutz.erlaubte-woerter");
        return new Settings(
                c.getString("welt", "baumeister"),
                clamp(c.getInt("bauhuette.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("bauhuette.mindestens-spieler", 3), 2, 16),
                Set.copyOf(variants),
                clamp(c.getInt("runde.hoechstens-spieler", 16), 2, PlotGrid.PLOTS),
                c.getBoolean("runde.themenwahl", true),
                clamp(c.getInt("runde.themenwahl-sekunden", 15), 5, 60),
                clamp(c.getInt("runde.bauzeit-sekunden", 300), 60, 1800),
                clamp(c.getInt("runde.schnell-bauzeit-sekunden", 120), 30, 900),
                clamp(c.getInt("runde.bewertung-sekunden", 15), 5, 60),
                clamp(c.getInt("runde.siegerehrung-sekunden", 15), 5, 60),
                clamp(c.getInt("rueckkehr-sekunden", 90), 0, MAX_COMEBACK_SECONDS),
                PlotGrid.size(c.getInt("parzelle.groesse", 28)),
                PlotGrid.height(c.getInt("parzelle.hoehe", 40)),
                clamp(c.getInt("werkzeug.pause-sekunden", 2), 0, 30),
                limits,
                new WordFilter(blocked, allowed, c.getBoolean("schutz.links-sperren", true)));
    }

    /** Bauzeit für diese Spielart. */
    public int buildSeconds(Variant v) {
        return v == Variant.SCHNELL ? quickBuildSeconds : buildSeconds;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
