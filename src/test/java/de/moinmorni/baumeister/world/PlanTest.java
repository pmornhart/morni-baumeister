package de.moinmorni.baumeister.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.baumeister.game.ToolMenus;
import de.moinmorni.baumeister.game.Tools;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.PlotGrid.Box;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Baupläne ohne Server: nur Blöcke von 26.x, Parzellen frei und mit Gras-Boden, Rand außerhalb der Baufläche,
 * Sockel darunter, Podest und Ankunft begehbar.
 */
class PlanTest {

    /** Blocknamen, die es in 26.x nicht mehr gibt. */
    private static final Set<String> OLD = Set.of("chain", "grass", "grass_path", "sign", "wall_sign");

    private static void onlyKnownBlocks(Plan p) {
        Set<String> bad = new TreeSet<>();
        p.forEach((k, s) -> {
            String base = Blocks.base(s);
            if (OLD.contains(base) || Material.matchMaterial(base) == null) {
                bad.add(s);
            }
        });
        assertEquals(Set.of(), bad);
    }

    @Test
    void hall() {
        Plan p = HallPlan.build();
        onlyKnownBlocks(p);
        int x = (int) Math.floor(HallPlan.SPAWN_X);
        int z = (int) Math.floor(HallPlan.SPAWN_Z);
        assertTrue(p.standable(x, (int) HallPlan.SPAWN_Y, z), "Ankunft begehbar");
        for (int[] b : new int[][] {HallPlan.BOARD, HallPlan.HOWTO, HallPlan.VARIANTS}) {
            assertTrue(p.get(b[0], 0, b[1]) != null, "Boden unter der Tafel");
        }
    }

    @Test
    void arena() {
        for (int size : new int[] {16, 28, 40}) {
            Plan p = ArenaPlan.build(size);
            onlyKnownBlocks(p);
            int ox = PlotGrid.arenaX(0);
            for (int i = 0; i < PlotGrid.PLOTS; i++) {
                Box b = PlotGrid.plot(0, i, size, 40);
                for (int x = b.minX(); x <= b.maxX(); x++) {
                    for (int z = b.minZ(); z <= b.maxZ(); z++) {
                        assertEquals("grass_block[snowy=false]", p.get(x - ox, 0, z), "Boden " + i);
                        assertTrue(p.get(x - ox, -1, z) != null, "Sockel unter Parzelle " + i);
                        for (int y = 1; y <= 40; y++) {
                            assertNull(p.get(x - ox, y, z), "Parzelle " + i + " leer bei " + x + " " + y + " " + z);
                        }
                    }
                }
                // Rand: eine Reihe außerhalb, aus Holz bzw. Ecksteinen.
                String edge = p.get(b.minX() - 1 - ox, 0, b.minZ() + 3);
                assertTrue(edge != null && edge.startsWith("stripped_spruce_log"), "Rand " + i + ": " + edge);
            }
            for (int[] spot : ArenaPlan.PODIUM) {
                assertTrue(p.standable(spot[0], spot[1] + 1, PlotGrid.PODIUM_Z), "Podest " + spot[0]);
            }
            assertTrue(p.standable(0, 1, PlotGrid.PODIUM_Z + ArenaPlan.AUDIENCE), "Zuschauerplatz");
            int[] bounds = p.bounds();
            assertTrue(ox + bounds[0] < PlotGrid.arenaX(1) + bounds[0], "Bauplätze nebeneinander");
            assertTrue(bounds[3] - bounds[0] < PlotGrid.ARENA_SPACING, "Bauplätze überschneiden sich nicht");
        }
    }

    @Test
    void toolBlocksAreReal() {
        for (Material m : ToolMenus.FLOORS) {
            assertTrue(!Tools.banned(m), m.name());
        }
        assertTrue(Tools.banned(Material.TNT));
        assertTrue(Tools.banned(Material.COMMAND_BLOCK));
    }
}
