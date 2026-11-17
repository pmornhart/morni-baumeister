package de.moinmorni.baumeister.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.baumeister.logic.PlotGrid.Box;
import org.junit.jupiter.api.Test;

/** Parzellen-Grenzen, Abstände, Bauplätze und der Bereich der Werkzeuge. */
class PlotGridTest {

    @Test
    void plotsAreSquareEvenAndApart() {
        for (int size : new int[] {16, 28, 40}) {
            for (int a = 0; a < PlotGrid.ARENAS; a++) {
                for (int i = 0; i < PlotGrid.PLOTS; i++) {
                    Box b = PlotGrid.plot(a, i, size, 40);
                    assertEquals(size, b.maxX() - b.minX() + 1);
                    assertEquals(size, b.maxZ() - b.minZ() + 1);
                    assertEquals(PlotGrid.FLOOR_Y, b.minY(), "Boden gehört dazu");
                    assertEquals(PlotGrid.FLOOR_Y + 40, b.maxY());
                    assertEquals(a, PlotGrid.arenaAt(b.minX(), b.minZ()));
                    assertEquals(i, PlotGrid.plotAt(a, b.minX(), b.minY(), b.minZ(), size, 40));
                    assertEquals(i, PlotGrid.plotAt(a, b.maxX(), b.maxY(), b.maxZ(), size, 40));
                    assertEquals(-1, PlotGrid.plotAt(a, b.maxX() + 1, b.minY(), b.minZ(), size, 40), "Rand gehört nicht dazu");
                    assertEquals(-1, PlotGrid.plotAt(a, b.minX(), PlotGrid.FLOOR_Y - 1, b.minZ(), size, 40), "Sockel nicht");
                    assertEquals(-1, PlotGrid.plotAt(a, b.minX(), b.maxY() + 1, b.minZ(), size, 40), "über der Höhe nicht");
                    // Zwei Parzellen (samt Rand und Spielraum zum Fliegen) überschneiden sich nie.
                    for (int j = i + 1; j < PlotGrid.PLOTS; j++) {
                        Box o = PlotGrid.plot(a, j, size, 40);
                        assertNull(PlotGrid.roam(b).intersect(PlotGrid.roam(o)), i + "/" + j);
                    }
                    assertTrue(PlotGrid.arena(a).contains(b.minX(), b.minY(), b.minZ()));
                    assertTrue(PlotGrid.arena(a).contains(b.maxX(), b.maxY(), b.maxZ()));
                }
            }
        }
        assertNull(PlotGrid.arena(0).intersect(PlotGrid.arena(1)), "Bauplätze getrennt");
        assertEquals(-1, PlotGrid.arenaAt(0, 0), "Bauhütte gehört zu keinem Bauplatz");
    }

    @Test
    void sizesAreClampedAndEven() {
        assertEquals(16, PlotGrid.size(3));
        assertEquals(40, PlotGrid.size(200));
        assertEquals(28, PlotGrid.size(29));
        assertEquals(16, PlotGrid.height(1));
        assertEquals(64, PlotGrid.height(500));
    }

    @Test
    void toolAreaStaysInsideThePlotAndHasALimit() {
        Box plot = PlotGrid.plot(0, 5, 28, 40);
        Region r = new Region();
        assertEquals(Region.Check.MISSING, r.check(plot, 4096));
        r.first(plot.minX() - 10, PlotGrid.FLOOR_Y, plot.minZ() - 10);
        r.second(plot.minX() + 3, PlotGrid.FLOOR_Y + 3, plot.minZ() + 3);
        Box b = r.box(plot);
        assertEquals(plot.minX(), b.minX(), "abgeschnitten am Rand");
        assertEquals(4 * 4 * 4, b.volume());
        assertEquals(Region.Check.OK, r.check(plot, 4096));
        assertEquals(Region.Check.TOO_BIG, r.check(plot, 63));
        r.first(plot.maxX() + 5, PlotGrid.FLOOR_Y, plot.maxZ() + 5);
        r.second(plot.maxX() + 9, PlotGrid.FLOOR_Y, plot.maxZ() + 9);
        assertEquals(Region.Check.OUTSIDE, r.check(plot, 4096), "ganz draußen");
        r.second(plot.minX(), PlotGrid.FLOOR_Y + 80, plot.minZ());
        assertEquals(plot.maxY(), r.box(plot).maxY(), "nicht über die Höhe");
        r.clear();
        assertFalse(r.hasFirst());
    }

    @Test
    void boxes() {
        Box a = Box.of(5, 5, 5, 0, 0, 0);
        assertEquals(216, a.volume());
        Box b = new Box(3, 3, 3, 9, 9, 9);
        assertEquals(new Box(3, 3, 3, 5, 5, 5), a.intersect(b));
        assertEquals(new Box(0, 0, 0, 9, 9, 9), a.union(b));
        assertEquals(a, a.union(null));
        assertTrue(a.containsXz(5.99, 0));
        assertFalse(a.containsXz(6.0, 0));
    }
}
