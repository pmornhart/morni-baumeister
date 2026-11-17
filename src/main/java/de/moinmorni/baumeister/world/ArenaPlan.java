package de.moinmorni.baumeister.world;

import de.moinmorni.baumeister.logic.PlotGrid;
import java.util.Random;

/**
 * Ein Bauplatz: eine weite Bergwiese mit 16 Parzellen im Raster (Rand aus geschälten Fichtenstämmen, an den Ecken
 * Laternen), Kieswege dazwischen, am Nordrand das Siegerpodest mit Fahnen. Koordinaten relativ zur Mitte des
 * Bauplatzes, y = 0 ist {@link PlotGrid#FLOOR_Y}. Unter jeder Parzelle liegt ein fester Sockel (Erde) – die Baufläche
 * selbst fängt beim Gras an.
 */
public final class ArenaPlan {

    /** Siegerpodest (relativ): Mitte = Platz 1, links Platz 2, rechts Platz 3; Höhe der Stufen. */
    public static final int[][] PODIUM = {{0, 3}, {-3, 2}, {3, 1}};
    /** Zuschauer stehen so weit südlich des Podests. */
    public static final int AUDIENCE = 9;

    private ArenaPlan() {
    }

    /** Bauplan für Parzellen der Größe {@code size} (gerade). */
    public static Plan build(int size) {
        Plan p = new Plan();
        Random r = new Random(4711);
        int half = PlotGrid.ARENA_HALF;
        int zMin = PlotGrid.PODIUM_Z - 10;
        for (int x = -half; x <= half; x++) {
            for (int z = zMin; z <= half; z++) {
                p.set(x, 0, z, "grass_block[snowy=false]");
                p.set(x, -1, z, "dirt");
                p.set(x, -2, z, x == -half || x == half || z == zMin || z == half ? "stone" : "dirt");
                p.set(x, -3, z, "stone");
            }
        }
        int s = PlotGrid.size(size);
        int ox = PlotGrid.arenaX(0);
        // Wege in der Mitte zwischen den Parzellen.
        for (int i = 0; i < PlotGrid.COLUMNS - 1; i++) {
            int line = PlotGrid.centerZ(i * PlotGrid.COLUMNS) + PlotGrid.PITCH / 2;
            int col = PlotGrid.centerX(0, i) - ox + PlotGrid.PITCH / 2;
            for (int k = -half + 4; k <= half - 4; k++) {
                for (int w = -1; w <= 1; w++) {
                    path(p, k, line + w, r);
                    path(p, col + w, k, r);
                }
            }
        }
        for (int i = 0; i < PlotGrid.PLOTS; i++) {
            plot(p, PlotGrid.centerX(0, i) - ox, PlotGrid.centerZ(i), s);
        }
        podium(p);
        // Bäume und Büsche am Rand der Wiese.
        for (int i = 0; i < 40; i++) {
            int x = -half + 3 + r.nextInt(2 * half - 6);
            int z = zMin + 3 + r.nextInt(half - zMin - 6);
            boolean edge = Math.abs(x) > half - 9 || z > half - 9 || z < zMin + 9;
            if (edge) {
                if (r.nextBoolean()) {
                    Decor.spruce(p, x, 1, z, 6 + r.nextInt(4), r);
                } else {
                    Decor.bush(p, x, 1, z, r);
                }
            }
        }
        Decor.flowers(p, -half, zMin, half, half, 9, r, (x, z) -> nearPlot(x, z, s) || nearPodium(x, z));
        Decor.mountainRing(p, half + 70, half + 70, -30, r);
        p.connect();
        return p;
    }

    private static void path(Plan p, int x, int z, Random r) {
        String g = p.get(x, 0, z);
        if (g != null && g.startsWith("grass_block")) {
            p.set(x, 0, z, Decor.pick(r, "gravel", "dirt_path", "dirt_path", "coarse_dirt"));
        }
    }

    /** Rand einer Parzelle (eine Reihe außerhalb der Baufläche) mit Laternen an den Ecken. */
    private static void plot(Plan p, int cx, int cz, int s) {
        int x1 = cx - s / 2 - 1;
        int x2 = cx + s / 2;
        int z1 = cz - s / 2 - 1;
        int z2 = cz + s / 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                    p.set(x, 0, z, corner ? "chiseled_stone_bricks" : (x == x1 || x == x2)
                            ? "stripped_spruce_log[axis=z]" : "stripped_spruce_log[axis=x]");
                    if (corner) {
                        p.set(x, 1, z, "spruce_fence");
                        p.set(x, 2, z, "lantern[hanging=false,waterlogged=false]");
                    }
                } else {
                    p.set(x, 0, z, "grass_block[snowy=false]");
                }
                p.set(x, -1, z, "dirt");
            }
        }
    }

    private static boolean nearPlot(int x, int z, int s) {
        int ox = PlotGrid.arenaX(0);
        for (int i = 0; i < PlotGrid.PLOTS; i++) {
            int cx = PlotGrid.centerX(0, i) - ox;
            int cz = PlotGrid.centerZ(i);
            if (Math.abs(x - cx) <= s / 2 + 2 && Math.abs(z - cz) <= s / 2 + 2) {
                return true;
            }
        }
        return Math.abs(x) <= 2 || Math.abs(z) <= 2;
    }

    private static boolean nearPodium(int x, int z) {
        return Math.abs(x) <= 8 && Math.abs(z - PlotGrid.PODIUM_Z) <= AUDIENCE + 3;
    }

    /** Siegerpodest aus Gold, Eisen und Kupfer mit Treppe davor und Fahnen dahinter. */
    private static void podium(Plan p) {
        int z = PlotGrid.PODIUM_Z;
        for (int x = -6; x <= 6; x++) {
            for (int dz = -3; dz <= AUDIENCE + 2; dz++) {
                p.set(x, 0, z + dz, Math.abs(x) == 6 || dz == -3 ? "stone_bricks" : "polished_andesite");
            }
        }
        String[] block = {"gold_block", "iron_block", "waxed_copper_block"};
        for (int i = 0; i < PODIUM.length; i++) {
            int px = PODIUM[i][0];
            int h = PODIUM[i][1];
            for (int dx = -1; dx <= 1; dx++) {
                for (int y = 1; y < h; y++) {
                    p.set(px + dx, y, z, "smooth_stone");
                }
                p.set(px + dx, h, z, dx == 0 ? block[i] : "smooth_stone_slab[type=bottom]");
            }
        }
        for (int x = -5; x <= 5; x += 2) {
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, z - 3, "spruce_fence");
            }
            p.banner(x, 4, z - 2, (x % 4 == 1 || x % 4 == -3 ? "light_blue" : "lime") + "_wall_banner[facing=south]",
                    x % 4 == 1 || x % 4 == -3 ? Decor.lozenges() : Decor.emerald());
        }
        Decor.lanternPost(p, -7, 1, z + AUDIENCE, "spruce_fence");
        Decor.lanternPost(p, 7, 1, z + AUDIENCE, "spruce_fence");
    }
}
