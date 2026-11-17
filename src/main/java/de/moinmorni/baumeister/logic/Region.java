package de.moinmorni.baumeister.logic;

import de.moinmorni.baumeister.logic.PlotGrid.Box;

/**
 * Bereich für die Bau-Werkzeuge (Füllen, Ersetzen, Leeren): zwei Ecken, die man mit dem Zollstock setzt. Gilt nur
 * innerhalb der eigenen Parzelle (was darüber hinausragt, wird abgeschnitten) und nur bis zu einer Höchstzahl an
 * Blöcken – das schont den Server.
 */
public final class Region {

    /** Was beim Prüfen herauskommt. */
    public enum Check {
        OK, MISSING, OUTSIDE, TOO_BIG
    }

    private int[] first;
    private int[] second;

    public void first(int x, int y, int z) {
        first = new int[] {x, y, z};
    }

    public void second(int x, int y, int z) {
        second = new int[] {x, y, z};
    }

    public boolean hasFirst() {
        return first != null;
    }

    public boolean hasSecond() {
        return second != null;
    }

    public void clear() {
        first = null;
        second = null;
    }

    /** Der Bereich innerhalb der Parzelle (null, wenn eine Ecke fehlt oder nichts davon in der Parzelle liegt). */
    public Box box(Box plot) {
        if (first == null || second == null) {
            return null;
        }
        return Box.of(first[0], first[1], first[2], second[0], second[1], second[2]).intersect(plot);
    }

    public Check check(Box plot, int maxBlocks) {
        if (first == null || second == null) {
            return Check.MISSING;
        }
        Box b = box(plot);
        if (b == null) {
            return Check.OUTSIDE;
        }
        return b.volume() > maxBlocks ? Check.TOO_BIG : Check.OK;
    }
}
