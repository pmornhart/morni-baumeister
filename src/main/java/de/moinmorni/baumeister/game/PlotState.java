package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.logic.PlotGrid.Box;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Eine Parzelle in einer Runde: wer darauf baut, ihre Baufläche, was bisher verändert wurde (zum Aufräumen), Uhrzeit
 * und Wetter, die die Bauleute gewählt haben, und wie viele Quellen schon gesetzt sind.
 */
public final class PlotState {

    /** Uhrzeiten zur Wahl (Minecraft-Zeit). */
    public static final long[] TIMES = {23500L, 6000L, 12600L, 18000L};
    public static final String[] TIME_IDS = {"morgen", "mittag", "abend", "nacht"};

    private final int index;
    private final List<UUID> members;
    private final Box box;
    private Box dirty;
    private int time = 1;
    private boolean rain;
    private int liquids;

    PlotState(int index, List<UUID> members, Box box) {
        this.index = index;
        this.members = new ArrayList<>(members);
        this.box = box;
    }

    public int index() {
        return index;
    }

    public List<UUID> members() {
        return members;
    }

    public Box box() {
        return box;
    }

    /** Was bisher verändert wurde (null = nichts). */
    public Box dirty() {
        return dirty;
    }

    /** Diese Stelle hat sich geändert (wird auf die Parzelle begrenzt). */
    public void touch(Box b) {
        Box in = b.intersect(box);
        if (in != null) {
            dirty = in.union(dirty);
        }
    }

    public void touch(int x, int y, int z) {
        touch(new Box(x, y, z, x, y, z));
    }

    /** Wurde überhaupt etwas gebaut? */
    public boolean built() {
        return dirty != null;
    }

    public int time() {
        return time;
    }

    public void time(int t) {
        time = Math.max(0, Math.min(TIMES.length - 1, t));
    }

    public long ticks() {
        return TIMES[time];
    }

    public boolean rain() {
        return rain;
    }

    public void rain(boolean r) {
        rain = r;
    }

    public int liquids() {
        return liquids;
    }

    public void addLiquid() {
        liquids++;
    }
}
