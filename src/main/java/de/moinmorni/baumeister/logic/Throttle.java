package de.moinmorni.baumeister.logic;

import java.util.HashMap;
import java.util.Map;

/**
 * Bremse gegen Bauten, die den Server ausbremsen (Redstone-Uhren, Kolben-Schleifen): zählt Ereignisse je Parzelle in
 * einem Fenster von einer Sekunde (20 Spielticks). Wer darüber liegt, wird für den Rest der Sekunde gestoppt; wer das
 * {@link #STRIKES} Sekunden hintereinander schafft, gilt als Dauerläufer – dann baut das Spiel das Bauteil ab.
 */
public final class Throttle {

    /** So viele volle Sekunden hintereinander über der Grenze: Bauteil weg. */
    public static final int STRIKES = 3;
    private static final int TICKS = 20;

    private final int perSecond;
    private final Map<Integer, long[]> windows = new HashMap<>();

    /** @param perSecond höchstens so viele Ereignisse pro Sekunde und Parzelle (0 = keine Grenze) */
    public Throttle(int perSecond) {
        this.perSecond = perSecond;
    }

    /** Ergebnis eines Ereignisses. */
    public enum Verdict {
        /** in Ordnung */
        OK,
        /** zu viel in dieser Sekunde: Ereignis stoppen */
        STOP,
        /** schon wieder zu viel: Bauteil abbauen */
        REMOVE
    }

    /**
     * Ein Ereignis in Parzelle {@code plot} zum Spieltick {@code tick}.
     */
    public Verdict hit(int plot, long tick) {
        if (perSecond <= 0) {
            return Verdict.OK;
        }
        // [Beginn des Fensters, Anzahl, Sekunden in Folge über der Grenze, war diese Sekunde schon drüber]
        long[] w = windows.computeIfAbsent(plot, k -> new long[] {tick, 0, 0, 0});
        if (tick - w[0] >= TICKS) {
            boolean wasOver = w[3] == 1;
            boolean adjacent = tick - w[0] < 2L * TICKS;
            w[2] = wasOver && adjacent ? w[2] : 0;
            w[0] = tick;
            w[1] = 0;
            w[3] = 0;
        }
        w[1]++;
        if (w[1] <= perSecond) {
            return Verdict.OK;
        }
        if (w[3] == 0) {
            w[3] = 1;
            w[2]++;
        }
        return w[2] >= STRIKES ? Verdict.REMOVE : Verdict.STOP;
    }

    public void reset() {
        windows.clear();
    }
}
