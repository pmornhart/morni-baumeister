package de.moinmorni.baumeister.logic;

/**
 * Wo alles liegt (ohne Server): Bauhütte in der Mitte der Welt, daneben {@link #ARENAS} Bauplätze mit je
 * {@link #PLOTS} Parzellen in einem 4 × 4-Raster. Jede Parzelle ist innen {@code size} × {@code size} Blöcke groß
 * (gerade Zahl) und {@code height} Blöcke hoch; der Boden (y = {@link #FLOOR_Y}) gehört dazu, darunter liegt ein
 * fester Sockel. Ein Rand aus Holz umgibt sie eine Reihe weiter außen, zwischen den Parzellen liegt Wiese.
 */
public final class PlotGrid {

    /** Oberste Bodenschicht: man steht auf {@code FLOOR_Y + 1}. */
    public static final int FLOOR_Y = 100;
    public static final int ARENAS = 2;
    public static final int COLUMNS = 4;
    public static final int PLOTS = COLUMNS * COLUMNS;
    /** Abstand der Mittelpunkte zweier Parzellen. */
    public static final int PITCH = 56;
    public static final int MIN_SIZE = 16;
    public static final int MAX_SIZE = 40;
    public static final int MIN_HEIGHT = 16;
    public static final int MAX_HEIGHT = 64;
    /** Abstand der Bauplätze voneinander (x) und von der Bauhütte. */
    public static final int ARENA_SPACING = 1000;
    /** So weit darf man im Bauplatz über eine Parzelle hinaus fliegen (beim Bewerten), bevor man zurückkommt. */
    public static final int ROAM = 8;
    /** Halbe Breite eines Bauplatzes (Wiese um alle Parzellen). */
    public static final int ARENA_HALF = (COLUMNS - 1) * PITCH / 2 + MAX_SIZE / 2 + 12;
    /** Siegerpodest: so weit nördlich der Mitte des Bauplatzes (z). */
    public static final int PODIUM_Z = -(ARENA_HALF + 14);

    private PlotGrid() {
    }

    /** Ein Quader (alle Grenzen eingeschlossen). */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public static Box of(int x1, int y1, int z1, int x2, int y2, int z2) {
            return new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2),
                    Math.max(z1, z2));
        }

        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        /** Nur waagrecht (für Wesen, die gleich hinunterfallen). */
        public boolean containsXz(double x, double z) {
            return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
        }

        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        /** Schnittmenge (null, wenn es keine gibt). */
        public Box intersect(Box o) {
            int x1 = Math.max(minX, o.minX);
            int y1 = Math.max(minY, o.minY);
            int z1 = Math.max(minZ, o.minZ);
            int x2 = Math.min(maxX, o.maxX);
            int y2 = Math.min(maxY, o.maxY);
            int z2 = Math.min(maxZ, o.maxZ);
            return x1 > x2 || y1 > y2 || z1 > z2 ? null : new Box(x1, y1, z1, x2, y2, z2);
        }

        /** Kleinster Quader, der beide enthält. */
        public Box union(Box o) {
            return o == null ? this : new Box(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ),
                    Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
        }

        public Box grow(int dx, int dy, int dz) {
            return new Box(minX - dx, minY - dy, minZ - dz, maxX + dx, maxY + dy, maxZ + dz);
        }
    }

    /** Gerade Größe in den Grenzen. */
    public static int size(int wanted) {
        int s = Math.max(MIN_SIZE, Math.min(MAX_SIZE, wanted));
        return s - (s & 1);
    }

    public static int height(int wanted) {
        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, wanted));
    }

    /** Mitte des Bauplatzes {@code arena} (x; z ist 0). */
    public static int arenaX(int arena) {
        return ARENA_SPACING * (arena + 1);
    }

    /** Mittelpunkt einer Parzelle (x). */
    public static int centerX(int arena, int plot) {
        return arenaX(arena) + (plot % COLUMNS) * PITCH - (COLUMNS - 1) * PITCH / 2;
    }

    /** Mittelpunkt einer Parzelle (z). */
    public static int centerZ(int plot) {
        return (plot / COLUMNS) * PITCH - (COLUMNS - 1) * PITCH / 2;
    }

    /** Baufläche einer Parzelle: innen, vom Boden bis zur Höchsthöhe. */
    public static Box plot(int arena, int plot, int size, int height) {
        int s = size(size);
        int cx = centerX(arena, plot);
        int cz = centerZ(plot);
        return new Box(cx - s / 2, FLOOR_Y, cz - s / 2, cx + s / 2 - 1, FLOOR_Y + height(height), cz + s / 2 - 1);
    }

    /** Wohin man in seiner Parzelle fliegen darf: ein wenig über den Rand hinaus, darüber hinaus nicht. */
    public static Box roam(Box plot) {
        return new Box(plot.minX() - ROAM, FLOOR_Y - 2, plot.minZ() - ROAM, plot.maxX() + ROAM, plot.maxY() + ROAM,
                plot.maxZ() + ROAM);
    }

    /** Der ganze Bauplatz (Wiese, Parzellen, Podest). */
    public static Box arena(int arena) {
        int ox = arenaX(arena);
        return new Box(ox - ARENA_HALF, FLOOR_Y - 4, PODIUM_Z - 12, ox + ARENA_HALF, FLOOR_Y + MAX_HEIGHT + 12,
                ARENA_HALF);
    }

    /** Zu welchem Bauplatz gehört diese Stelle (−1 = keinem)? */
    public static int arenaAt(int x, int z) {
        for (int a = 0; a < ARENAS; a++) {
            Box b = arena(a);
            if (x >= b.minX() && x <= b.maxX() && z >= b.minZ() && z <= b.maxZ()) {
                return a;
            }
        }
        return -1;
    }

    /** In welcher Parzelle (Baufläche) liegt diese Stelle (−1 = in keiner)? */
    public static int plotAt(int arena, int x, int y, int z, int size, int height) {
        for (int i = 0; i < PLOTS; i++) {
            if (plot(arena, i, size, height).contains(x, y, z)) {
                return i;
            }
        }
        return -1;
    }
}
