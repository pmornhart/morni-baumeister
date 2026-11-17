package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.logic.Variant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/** Alle laufenden Runden (höchstens eine je Bauplatz). */
public final class Games {

    private final BaumeisterPlugin plugin;
    private final Map<Integer, Game> running = new TreeMap<>();

    public Games(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    /** Freier Bauplatz (−1 = keiner): keine Runde und nicht beim Aufräumen. */
    public int freeArena() {
        for (int a = 0; a < PlotGrid.ARENAS; a++) {
            if (!running.containsKey(a) && !plugin.construction().restoring(a)) {
                return a;
            }
        }
        return -1;
    }

    public Game start(int arena, Variant variant, List<List<Player>> teams, Theme forced) {
        Game g = new Game(plugin, arena, variant, teams, forced);
        running.put(arena, g);
        plugin.stateChanged();
        return g;
    }

    public Collection<Game> all() {
        return running.values();
    }

    public Game on(int arena) {
        return running.get(arena);
    }

    /** Runde, in der jemand mitspielt (auch gerade nicht verbunden) oder zuschaut. */
    public Game of(UUID uuid) {
        for (Game g : running.values()) {
            Builder b = g.builder(uuid);
            if ((b != null && !b.gone()) || g.spectating(uuid)) {
                return g;
            }
        }
        return null;
    }

    /** Kam diese Person in einer laufenden Runde nicht rechtzeitig zurück? */
    public boolean late(UUID uuid) {
        for (Game g : running.values()) {
            Builder b = g.builder(uuid);
            if (b != null && b.late()) {
                return true;
            }
        }
        return false;
    }

    /** Runde, in der jemand gerade mitspielt (da und nicht raus). */
    public Game playing(UUID uuid) {
        for (Game g : running.values()) {
            Builder b = g.builder(uuid);
            if (b != null && b.active()) {
                return g;
            }
        }
        return null;
    }

    /** Runde auf dem Bauplatz an dieser Stelle (null = keine). */
    public Game at(int x, int z) {
        int a = PlotGrid.arenaAt(x, z);
        return a < 0 ? null : running.get(a);
    }

    public void tick() {
        for (Game g : new ArrayList<>(running.values())) {
            boolean alive;
            try {
                alive = g.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler in der Runde auf Bauplatz " + (g.arena() + 1)
                        + " – sie wird beendet", e);
                g.close();
                alive = false;
            }
            if (!alive) {
                running.remove(g.arena());
                plugin.stateChanged();
                plugin.boards().refreshHall();
            }
        }
    }

    public void stop(Game g) {
        g.close();
        running.remove(g.arena());
        plugin.stateChanged();
        plugin.boards().refreshHall();
    }

    public void abortAll() {
        for (Game g : new ArrayList<>(running.values())) {
            g.close();
        }
        running.clear();
    }

    public int players() {
        return running.values().stream().mapToInt(Game::activeCount).sum();
    }
}
