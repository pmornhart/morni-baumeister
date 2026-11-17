package de.moinmorni.baumeister.world;

import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.PlotGrid.Box;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Baut Bauhütte und Bauplätze aus den Bauplänen – nur wenn sich Bau-Version oder Parzellen-Größe geändert haben
 * (gemerkt in {@code bau.yml}) – und räumt nach jeder Runde die benutzten Parzellen auf: alles über dem Boden weg,
 * Boden wieder Gras, herumstehende Wesen weg. Gesetzt wird Stück für Stück (höchstens {@link #BUDGET_NANOS} pro Tick).
 * Ein Bauplatz, auf dem gerade gespielt wird, steht in {@code aufraeumen.yml}: stürzt der Server ab, wird er beim
 * nächsten Start ganz geräumt.
 */
public final class Construction {

    /** Bau-Version: erhöhen, wenn sich Bauhütte oder Bauplätze ändern – dann wird beim nächsten Start neu gebaut. */
    public static final int VERSION = 1;
    public static final String FLOOR = "grass_block[snowy=false]";
    private static final long BUDGET_NANOS = 25_000_000L;
    private static final int CHECK_EVERY = 256;

    private interface Job {
        boolean step(long deadline);
    }

    private final Plugin plugin;
    private final Logger log;
    private final World world;
    private final Path stateFile;
    private final Path dirtyFile;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private final Map<String, BlockData> cache = new HashMap<>();
    private final Set<String> broken = new HashSet<>();
    private final Set<Integer> restoring = new HashSet<>();
    private final Set<Integer> dirty = new TreeSet<>();
    private BukkitTask task;
    private int planning;
    private int placed;
    private boolean building;
    private int lastRestored;

    public Construction(Plugin plugin, World world) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.world = world;
        this.stateFile = plugin.getDataFolder().toPath().resolve("bau.yml");
        this.dirtyFile = plugin.getDataFolder().toPath().resolve("aufraeumen.yml");
    }

    public boolean building() {
        return building;
    }

    /** Wird dieser Bauplatz gerade gebaut oder aufgeräumt? */
    public boolean restoring(int arena) {
        return restoring.contains(arena);
    }

    /** Blöcke, die beim letzten Aufräumen gesetzt wurden (für Tests). */
    public int lastRestored() {
        return lastRestored;
    }

    /** Blockzustand für einen Bau-Text (Boden-Werkzeug, Tests); null bei unbekanntem Namen. */
    public BlockData data(String spec) {
        BlockData d = cache.get(spec);
        if (d != null || broken.contains(spec)) {
            return d;
        }
        try {
            d = Bukkit.createBlockData(spec.indexOf(':') >= 0 ? spec : "minecraft:" + spec);
            cache.put(spec, d);
        } catch (IllegalArgumentException e) {
            broken.add(spec);
            log.severe("Unbekannter Block im Bauplan: " + spec + " – wird ausgelassen.");
        }
        return d;
    }

    /**
     * Baut, falls nötig; {@code done} läuft auf dem Hauptthread, sobald alles steht. Bauplätze, die beim letzten Mal
     * mitten in einer Runde waren, werden ganz geräumt.
     */
    public void ensure(boolean force, int size, int height, Runnable done) {
        YamlConfiguration state = readState();
        readDirty();
        for (int a = 0; a < PlotGrid.ARENAS; a++) {
            restoring.add(a);
        }
        if (!force && state.getInt("version") == VERSION && state.getInt("groesse") == size) {
            for (int a = 0; a < PlotGrid.ARENAS; a++) {
                int arena = a;
                if (dirty.contains(a)) {
                    log.info("Bauplatz " + (a + 1) + " war beim Abschalten belegt – wird geräumt.");
                    jobs.add(resetJob(a, allPlots(a, size, height), null));
                } else {
                    jobs.add(deadline -> {
                        restoring.remove(arena);
                        return true;
                    });
                }
            }
            jobs.add(deadline -> {
                done.run();
                return true;
            });
            startTask();
            return;
        }
        building = true;
        log.info("Bauhütte und Bauplätze werden gebaut (Bau-Version " + VERSION + ", Parzellen " + size + " × " + size
                + ") …");
        long start = System.currentTimeMillis();
        for (String box : state.getStringList("bereiche")) {
            int[] b = parseBox(box);
            if (b != null) {
                jobs.add(clearJob(b));
            }
        }
        List<String> boxes = new ArrayList<>();
        Plan hall = HallPlan.build();
        boxes.add(boxOf(hall, 0, PlotGrid.FLOOR_Y, 0));
        jobs.add(placeJob(hall, 0, PlotGrid.FLOOR_Y, 0));
        planning++;
        startTask();
        var scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskAsynchronously(plugin, () -> {
            Plan arena = null;
            try {
                arena = ArenaPlan.build(size);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Bauplan der Bauplätze ließ sich nicht erstellen", e);
            }
            Plan plan = arena;
            scheduler.runTask(plugin, () -> {
                planning--;
                if (plan != null) {
                    for (int a = 0; a < PlotGrid.ARENAS; a++) {
                        int ox = PlotGrid.arenaX(a);
                        int arenaIndex = a;
                        boxes.add(boxOf(plan, ox, PlotGrid.FLOOR_Y, 0));
                        jobs.add(placeJob(plan, ox, PlotGrid.FLOOR_Y, 0));
                        jobs.add(resetJob(arenaIndex, allPlots(arenaIndex, size, height), null));
                    }
                }
                jobs.add(deadline -> {
                    YamlConfiguration fresh = new YamlConfiguration();
                    fresh.set("version", VERSION);
                    fresh.set("groesse", size);
                    fresh.set("bereiche", boxes);
                    writeState(fresh);
                    dirty.clear();
                    writeDirty();
                    building = false;
                    log.info("Fertig gebaut: " + placed + " Blöcke in " + (System.currentTimeMillis() - start) / 1000
                            + " s.");
                    done.run();
                    return true;
                });
                startTask();
            });
        });
    }

    private static List<Box> allPlots(int arena, int size, int height) {
        List<Box> all = new ArrayList<>();
        for (int i = 0; i < PlotGrid.PLOTS; i++) {
            all.add(PlotGrid.plot(arena, i, size, height));
        }
        return all;
    }

    /** Auf diesem Bauplatz beginnt eine Runde: merken (falls der Server abstürzt). */
    public void inUse(int arena) {
        if (dirty.add(arena)) {
            writeDirty();
        }
    }

    /** Benutzte Parzellen aufräumen; {@code done} (darf null sein) läuft danach. */
    public void restore(int arena, List<Box> boxes, Runnable done) {
        restoring.add(arena);
        jobs.add(resetJob(arena, boxes, done));
        startTask();
    }

    private void startTask() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.step(deadline);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
            }
        }
        if (jobs.isEmpty() && task != null && planning == 0) {
            task.cancel();
            task = null;
        }
    }

    private Job placeJob(Plan plan, int ox, int oy, int oz) {
        List<Long> keys = new ArrayList<>(plan.size());
        List<String> specs = new ArrayList<>(plan.size());
        plan.forEach((k, s) -> {
            keys.add(k);
            specs.add(s);
        });
        List<Plan.Banner> banners = plan.banners();
        int[] index = {0};
        return deadline -> {
            int i = index[0];
            int n = 0;
            while (i < keys.size()) {
                long k = keys.get(i);
                BlockData d = data(specs.get(i));
                if (d != null) {
                    world.getBlockAt(ox + Plan.keyX(k), oy + Plan.keyY(k), oz + Plan.keyZ(k)).setBlockData(d, false);
                    placed++;
                }
                i++;
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    index[0] = i;
                    return false;
                }
            }
            index[0] = i;
            for (Plan.Banner b : banners) {
                patterns(world.getBlockAt(ox + b.x(), oy + b.y(), oz + b.z()), b.patterns());
            }
            return true;
        };
    }

    private void patterns(Block block, List<String> specs) {
        BlockState st = block.getState();
        if (st instanceof org.bukkit.block.Banner banner) {
            banner.setPatterns(patterns(specs, log));
            banner.update(true, false);
        }
    }

    /** {@code "lime:rhombus"} → Muster (auch für Banner-Gegenstände der Bau-Hilfen). */
    public static List<Pattern> patterns(List<String> specs, Logger log) {
        var registry = io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.BANNER_PATTERN);
        List<Pattern> out = new ArrayList<>();
        for (String s : specs) {
            String[] parts = s.split(":");
            try {
                PatternType type = registry.get(NamespacedKey.minecraft(parts[1]));
                if (type != null) {
                    out.add(new Pattern(DyeColor.valueOf(parts[0].toUpperCase(Locale.ROOT)), type));
                }
            } catch (RuntimeException e) {
                log.warning("Bannermuster unbekannt: " + s);
            }
        }
        return out;
    }

    /**
     * Parzellen räumen: Chunk für Chunk eine Momentaufnahme lesen; über dem Boden alles weg, der Boden wird wieder
     * Gras. Nur innerhalb der Boxen (das sind immer Bauflächen – Rand und Wiese bleiben unberührt). Danach alle Wesen
     * auf dem Bauplatz weg (außer Spielern).
     */
    private Job resetJob(int arena, List<Box> boxes, Runnable done) {
        BlockData floor = data(FLOOR);
        List<int[]> work = new ArrayList<>();
        for (int bi = 0; bi < boxes.size(); bi++) {
            Box b = boxes.get(bi);
            for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
                for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                    work.add(new int[] {bi, cx, cz});
                }
            }
        }
        int minH = world.getMinHeight();
        int[] index = {0};
        int[] changed = {0};
        return deadline -> {
            while (index[0] < work.size()) {
                int[] w = work.get(index[0]++);
                Box b = boxes.get(w[0]);
                Chunk chunk = world.getChunkAt(w[1], w[2]);
                ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
                int y1 = Math.max(b.minY(), PlotGrid.FLOOR_Y);
                int y2 = Math.min(b.maxY(), world.getMaxHeight() - 1);
                for (int sy = (y1 - minH) >> 4; sy <= (y2 - minH) >> 4; sy++) {
                    if (snap.isSectionEmpty(sy)) {
                        continue;
                    }
                    int from = Math.max(y1, minH + sy * 16);
                    int to = Math.min(y2, minH + sy * 16 + 15);
                    for (int lx = 0; lx < 16; lx++) {
                        int x = (w[1] << 4) + lx;
                        if (x < b.minX() || x > b.maxX()) {
                            continue;
                        }
                        for (int lz = 0; lz < 16; lz++) {
                            int z = (w[2] << 4) + lz;
                            if (z < b.minZ() || z > b.maxZ()) {
                                continue;
                            }
                            for (int y = from; y <= to; y++) {
                                BlockData cur = snap.getBlockData(lx, y, lz);
                                if (y == PlotGrid.FLOOR_Y) {
                                    if (!cur.equals(floor)) {
                                        world.getBlockAt(x, y, z).setBlockData(floor, false);
                                        changed[0]++;
                                    }
                                } else if (!cur.getMaterial().isAir()) {
                                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                                    changed[0]++;
                                }
                            }
                        }
                    }
                }
                if (System.nanoTime() > deadline) {
                    return false;
                }
            }
            Box all = PlotGrid.arena(arena);
            for (Entity e : world.getNearbyEntities(new org.bukkit.util.BoundingBox(all.minX(), all.minY() - 64,
                    all.minZ(), all.maxX() + 1, all.maxY() + 64, all.maxZ() + 1))) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
            restoring.remove(arena);
            lastRestored = changed[0];
            if (dirty.remove(arena)) {
                writeDirty();
            }
            if (changed[0] > 0) {
                log.info("Bauplatz " + (arena + 1) + " aufgeräumt: " + changed[0] + " Blöcke.");
            }
            if (done != null) {
                done.run();
            }
            return true;
        };
    }

    /** Alten Bau abräumen (vor einem Neubau): Chunk für Chunk, leere Abschnitte werden übersprungen. */
    private Job clearJob(int[] b) {
        int minH = world.getMinHeight();
        int y1 = Math.max(minH, b[1]);
        int y2 = Math.min(world.getMaxHeight() - 1, b[4]);
        List<int[]> chunks = new ArrayList<>();
        for (int cx = b[0] >> 4; cx <= b[3] >> 4; cx++) {
            for (int cz = b[2] >> 4; cz <= b[5] >> 4; cz++) {
                chunks.add(new int[] {cx, cz});
            }
        }
        int[] index = {0};
        return deadline -> {
            while (index[0] < chunks.size()) {
                int[] c = chunks.get(index[0]++);
                Chunk chunk = world.getChunkAt(c[0], c[1]);
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) {
                        e.remove();
                    }
                }
                ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
                for (int sy = (y1 - minH) >> 4; sy <= (y2 - minH) >> 4; sy++) {
                    if (snap.isSectionEmpty(sy)) {
                        continue;
                    }
                    int from = Math.max(y1, minH + sy * 16);
                    int to = Math.min(y2, minH + sy * 16 + 15);
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int y = from; y <= to; y++) {
                                if (!snap.getBlockType(lx, y, lz).isAir()) {
                                    world.getBlockAt((c[0] << 4) + lx, y, (c[1] << 4) + lz).setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                }
                if (System.nanoTime() > deadline) {
                    return index[0] >= chunks.size();
                }
            }
            return true;
        };
    }

    private static String boxOf(Plan plan, int ox, int oy, int oz) {
        int[] b = plan.bounds();
        return (ox + b[0]) + " " + (oy + b[1]) + " " + (oz + b[2]) + " " + (ox + b[3]) + " " + (oy + b[4]) + " "
                + (oz + b[5]);
    }

    private static int[] parseBox(String s) {
        String[] parts = s.trim().split("\\s+");
        if (parts.length != 6) {
            return null;
        }
        int[] b = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                b[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return b;
    }

    private YamlConfiguration read(Path file) {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(file)) {
            try {
                y.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                log.warning(file.getFileName() + " ließ sich nicht lesen: " + e.getMessage());
            }
        }
        return y;
    }

    private YamlConfiguration readState() {
        return read(stateFile);
    }

    private void readDirty() {
        dirty.addAll(read(dirtyFile).getIntegerList("bauplaetze"));
    }

    private void writeDirty() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("bauplaetze", new ArrayList<>(dirty));
        write(dirtyFile, y);
    }

    private void writeState(YamlConfiguration y) {
        write(stateFile, y);
    }

    private void write(Path file, YamlConfiguration y) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, y.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.log(Level.WARNING, file.getFileName() + " ließ sich nicht schreiben", e);
        }
    }
}
