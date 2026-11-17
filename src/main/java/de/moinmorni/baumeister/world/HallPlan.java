package de.moinmorni.baumeister.world;

import java.util.Random;

/**
 * Die Bauhütte: eine Alm-Insel in den Wolken mit einer großen Zimmerer-Werkstatt im Norden (Bestenliste davor),
 * Werkbänken, Holzstapeln, einem Baugerüst mit Kran und einem kleinen Musterhaus. Hier wartet man auf die nächste
 * Runde, stimmt über die Spielart ab und liest, wie es geht. Eigener Entwurf, nur Minecraft-Blöcke.
 */
public final class HallPlan {

    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = 1;
    public static final double SPAWN_Z = 10.5;
    public static final float SPAWN_YAW = 180f;
    /** Alles innerhalb dieses Abstands zur Mitte gehört zur Bauhütte (bleibt geladen). */
    public static final int REACH = 32;
    public static final int RADIUS = 19;
    /** Bestenliste vor der Werkstatt. */
    public static final int[] BOARD = {0, -9};
    /** Tafel „So geht's“ (Westen). */
    public static final int[] HOWTO = {-12, 4};
    /** Tafel „Spielarten“ (Osten). */
    public static final int[] VARIANTS = {12, 4};
    /** Wer tiefer fällt, kommt zurück zum Ankunftsplatz. */
    public static final int FALL_Y = -12;

    private HallPlan() {
    }

    public static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2611);
        Decor.island(p, RADIUS, RADIUS, 14, r);
        yard(p, r);
        workshop(p, r);
        board(p, HOWTO[0] - 1, HOWTO[1], true);
        board(p, VARIANTS[0] + 1, VARIANTS[1], false);
        scaffold(p, 9, -4);
        modelHouse(p, -11, -5, r);
        for (int[] t : new int[][] {{-6, 4}, {6, 4}}) {
            workbench(p, t[0], t[1]);
        }
        Kit.woodpile(p, -15, 1, 9, 4, 2, false);
        Kit.woodpile(p, 15, 1, 9, 4, 2, false);
        Kit.longTable(p, -3, 1, 13, 3, true, "spruce", true, r, "flower_pot", "lantern[hanging=false,waterlogged=false]");
        for (int[] t : new int[][] {{-14, -11}, {14, -11}, {-13, 14}, {13, 14}}) {
            Decor.broadTree(p, t[0], 1, t[1], "oak_log", "oak_leaves", 4, 3, r);
        }
        for (int[] l : new int[][] {{-4, 8}, {4, 8}, {-9, -1}, {9, 1}, {-4, -4}, {4, -4}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "spruce_fence");
        }
        Decor.flowers(p, -RADIUS, -RADIUS, RADIUS, RADIUS, 5, r, (x, z) -> Math.hypot(x, z) < 15);
        for (int i = 0; i < 12; i++) {
            double a = 2 * Math.PI * i / 12 + r.nextDouble() * 0.3;
            double d = RADIUS - 3 + r.nextInt(8);
            Decor.cloud(p, (int) Math.round(Math.cos(a) * d), -12 - r.nextInt(4), (int) Math.round(Math.sin(a) * d),
                    3 + r.nextInt(3), r);
        }
        Decor.mountainRing(p, 70, 70, -40, r);
        barrierRing(p);
        p.connect();
        return p;
    }

    /** Werkhof: Pflaster mit Holzspänen (Sand, Kies, Erde) und ein Weg zum Ankunftsplatz. */
    private static void yard(Plan p, Random r) {
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                double d = Math.hypot(x, z);
                boolean path = Math.abs(x) <= 1 && z > 0 && z <= 16;
                if (d <= 13.5 || path) {
                    p.set(x, 0, z, d <= 3.5 ? "polished_andesite" : path && d > 13.5 ? "dirt_path"
                            : Decor.pick(r, "stone_bricks", "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks",
                                    "gravel", "coarse_dirt"));
                }
            }
        }
        // Mitte: Zunftzeichen aus Kupfer und Holz – ein großer Winkel.
        for (int i = -2; i <= 2; i++) {
            p.set(i, 0, -2, "waxed_cut_copper");
            p.set(-2, 0, i, "waxed_cut_copper");
        }
    }

    /** Werkstatt (Zimmerei) im Norden, Fassade zum Hof, Tafel für die Bestenliste davor. */
    private static void workshop(Plan p, Random r) {
        Kit.house(p, new Kit.House(-9, -20, 9, -13, 's', 0, "white_terracotta", "spruce", true, true, "spruce_planks"), r);
        int bx = BOARD[0];
        int bz = BOARD[1] - 2;
        for (int dx = -4; dx <= 4; dx++) {
            for (int y = 1; y <= 4; y++) {
                p.set(bx + dx, y, bz, Math.abs(dx) == 4 ? "stripped_spruce_log[axis=y]" : "dark_oak_planks");
            }
            p.set(bx + dx, 5, bz, "spruce_slab[type=bottom]");
        }
        p.banner(bx - 4, 3, bz + 1, "light_blue_wall_banner[facing=south]", Decor.lozenges());
        p.banner(bx + 4, 3, bz + 1, "lime_wall_banner[facing=south]", Decor.emerald());
    }

    /** Tafel mit Dach, Front zur Hofmitte. */
    private static void board(Plan p, int x, int z, boolean west) {
        for (int dz = -2; dz <= 2; dz++) {
            for (int y = 1; y <= 3; y++) {
                p.set(x, y, z + dz, Math.abs(dz) == 2 ? "stripped_spruce_log[axis=y]" : "spruce_planks");
            }
            p.set(x, 4, z + dz, "spruce_slab[type=bottom]");
        }
        p.set(x + (west ? 1 : -1), 1, z - 2, "barrel[facing=up,open=false]");
    }

    /** Werkbank: Tischler-, Kartentisch, Steinsäge, Amboss. */
    private static void workbench(Plan p, int x, int z) {
        p.set(x - 1, 1, z, "crafting_table");
        p.set(x, 1, z, "stonecutter[facing=south]");
        p.set(x + 1, 1, z, "fletching_table");
        p.set(x, 1, z - 1, "anvil[facing=east]");
        p.set(x, 0, z, "spruce_planks");
    }

    /** Baugerüst mit kleinem Kran (Zaunpfosten, Latten, ein Seil mit „Last“). */
    private static void scaffold(Plan p, int x, int z) {
        for (int y = 1; y <= 9; y++) {
            for (int[] c : new int[][] {{0, 0}, {3, 0}, {0, 3}, {3, 3}}) {
                p.set(x + c[0], y, z + c[1], "scaffolding[bottom=false,distance=0,waterlogged=false]");
            }
        }
        for (int dx = 0; dx <= 3; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                p.set(x + dx, 5, z + dz, "spruce_slab[type=bottom]");
                p.set(x + dx, 10, z + dz, "spruce_slab[type=bottom]");
            }
        }
        // Kranarm nach Westen, Seil, Last aus Holzbalken.
        for (int dx = -5; dx <= 1; dx++) {
            p.set(x + dx, 11, z + 1, "stripped_spruce_log[axis=x]");
        }
        for (int y = 7; y <= 10; y++) {
            p.set(x - 5, y, z + 1, "iron_chain[axis=y]");
        }
        p.set(x - 5, 6, z + 1, "stripped_oak_log[axis=x]");
        p.set(x - 6, 6, z + 1, "stripped_oak_log[axis=x]");
        p.set(x - 4, 6, z + 1, "stripped_oak_log[axis=x]");
    }

    /** Kleines Musterhaus (halb fertig, ohne Dach an einer Seite) – zeigt, was man bauen kann. */
    private static void modelHouse(Plan p, int x, int z, Random r) {
        Kit.house(p, new Kit.House(x - 3, z - 2, x + 3, z + 2, 'e', z, "white_terracotta", "spruce", false, false,
                "oak_planks"), r);
        Kit.roof(p, x - 3, z - 2, x + 3, z + 2, Kit.GROUND_TOP + 1, "spruce", false);
    }

    /** Unsichtbarer Zaun um die Insel: niemand fällt hinunter. */
    private static void barrierRing(Plan p) {
        for (int x = -RADIUS - 4; x <= RADIUS + 4; x++) {
            for (int z = -RADIUS - 4; z <= RADIUS + 4; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS - 2 && p.get(x, 0, z) == null && near(p, x, z)) {
                    for (int y = 0; y <= 3; y++) {
                        p.setIfEmpty(x, y, z, "barrier");
                    }
                }
            }
        }
    }

    private static boolean near(Plan p, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String s = p.get(x + dx, 0, z + dz);
                if (s != null && !"barrier".equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
