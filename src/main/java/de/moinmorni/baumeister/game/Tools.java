package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.Ui;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.PlotGrid.Box;
import de.moinmorni.baumeister.logic.Region;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Bau-Hilfen, die Blöcke setzen: Boden der Parzelle wechseln, Bereich füllen, ersetzen, leeren und einen Schritt
 * zurück. Alles nur innerhalb der eigenen Parzelle, mit einer Höchstzahl an Blöcken und einer kurzen Pause.
 */
public final class Tools {

    /** Was nie über Werkzeuge gesetzt wird (und auch sonst nicht in Parzellen). */
    public static boolean banned(Material m) {
        return switch (m) {
            case TNT, COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK, STRUCTURE_BLOCK, STRUCTURE_VOID,
                    JIGSAW, SPAWNER, TRIAL_SPAWNER, RESPAWN_ANCHOR, END_PORTAL_FRAME, END_PORTAL, NETHER_PORTAL,
                    END_GATEWAY, BARRIER, LIGHT, BEDROCK, DRAGON_EGG -> true;
            default -> false;
        };
    }

    /** Für Werkzeuge taugliche Blöcke (fest, nicht verboten, keine Flüssigkeit). */
    public static boolean usable(Material m) {
        return m != null && m.isBlock() && !m.isAir() && !banned(m) && m != Material.WATER && m != Material.LAVA
                && !m.name().endsWith("_BED") && !m.name().endsWith("_DOOR");
    }

    private record Saved(int x, int y, int z, BlockData data) {
    }

    private final BaumeisterPlugin plugin;
    private final Map<UUID, List<Saved>> undo = new HashMap<>();

    public Tools(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(Player p) {
        return plugin.lang(p);
    }

    /** Darf die Person gerade ein Werkzeug benutzen (Bauzeit, Pause vorbei)? Liefert die Parzelle oder null. */
    private PlotState ready(Player p, Game g) {
        PlotState s = g == null ? null : g.buildPlot(p.getUniqueId());
        if (s == null) {
            p.sendMessage(Texts.msg(lang(p), "tools.not-now"));
            return null;
        }
        Builder b = g.builder(p.getUniqueId());
        long now = Bukkit.getCurrentTick();
        if (now < b.toolReadyAt()) {
            p.sendMessage(Texts.msg(lang(p), "tools.wait", Placeholder.unparsed("secs",
                    String.valueOf((b.toolReadyAt() - now + 19) / 20))));
            return null;
        }
        return s;
    }

    private void used(Game g, Player p) {
        g.builder(p.getUniqueId()).toolReadyAt(Bukkit.getCurrentTick() + 20L * plugin.settings().toolCooldown());
    }

    /** Boden der ganzen Parzelle wechseln. */
    public void floor(Player p, Game g, Material m) {
        PlotState s = ready(p, g);
        if (s == null || !usable(m)) {
            return;
        }
        Box b = s.box();
        Box floor = new Box(b.minX(), PlotGrid.FLOOR_Y, b.minZ(), b.maxX(), PlotGrid.FLOOR_Y, b.maxZ());
        apply(p, g, s, floor, m.createBlockData(), null);
        p.sendMessage(Texts.msg(lang(p), "tools.floor.done"));
    }

    /** Bereich füllen ({@code from} == null) oder nur {@code from} durch {@code to} ersetzen. */
    public void fill(Player p, Game g, Material to, Material from) {
        PlotState s = ready(p, g);
        if (s == null) {
            return;
        }
        Region r = g.builder(p.getUniqueId()).region();
        String lang = lang(p);
        int max = plugin.settings().limits().toolBlocks();
        switch (r.check(s.box(), max)) {
            case MISSING -> {
                p.sendMessage(Texts.msg(lang, "tools.region.missing"));
                return;
            }
            case OUTSIDE -> {
                p.sendMessage(Texts.msg(lang, "tools.region.outside"));
                return;
            }
            case TOO_BIG -> {
                p.sendMessage(Texts.msg(lang, "tools.region.too-big", Placeholder.unparsed("n",
                        String.valueOf(r.box(s.box()).volume())), Placeholder.unparsed("max", String.valueOf(max))));
                return;
            }
            default -> {
            }
        }
        if (to != Material.AIR && !usable(to)) {
            return;
        }
        int n = apply(p, g, s, r.box(s.box()), to.createBlockData(), from);
        p.sendMessage(Texts.msg(lang, to == Material.AIR && from == null ? "tools.clear.done"
                : from == null ? "tools.fill.done" : "tools.replace.done", Placeholder.unparsed("n", String.valueOf(n))));
    }

    private int apply(Player p, Game g, PlotState s, Box box, BlockData data, Material only) {
        World w = plugin.world();
        List<Saved> saved = new ArrayList<>();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (only != null && b.getType() != only) {
                        continue;
                    }
                    if (b.getBlockData().equals(data)) {
                        continue;
                    }
                    saved.add(new Saved(x, y, z, b.getBlockData()));
                    b.setBlockData(data, false);
                }
            }
        }
        undo.put(p.getUniqueId(), saved);
        s.touch(box);
        used(g, p);
        Ui.sound(p, "minecraft:block.wood.place", 0.8f);
        return saved.size();
    }

    /** Letzten Werkzeug-Schritt zurücknehmen. */
    public void undo(Player p, Game g) {
        PlotState s = ready(p, g);
        if (s == null) {
            return;
        }
        List<Saved> saved = undo.remove(p.getUniqueId());
        if (saved == null || saved.isEmpty()) {
            p.sendMessage(Texts.msg(lang(p), "tools.undo.none"));
            return;
        }
        World w = plugin.world();
        for (Saved b : saved) {
            if (s.box().contains(b.x(), b.y(), b.z())) {
                w.getBlockAt(b.x(), b.y(), b.z()).setBlockData(b.data(), false);
            }
        }
        used(g, p);
        p.sendMessage(Texts.msg(lang(p), "tools.undo.done", Placeholder.unparsed("n", String.valueOf(saved.size()))));
    }

    public void forget(UUID uuid) {
        undo.remove(uuid);
    }

    /** Zollstock: erste bzw. zweite Ecke setzen. */
    public void corner(Player p, Game g, Block b, boolean first) {
        PlotState s = g == null ? null : g.buildPlot(p.getUniqueId());
        if (s == null) {
            return;
        }
        Region r = g.builder(p.getUniqueId()).region();
        String lang = lang(p);
        if (!s.box().contains(b.getX(), b.getY(), b.getZ())) {
            p.sendActionBar(Texts.text(lang, "tools.region.outside"));
            return;
        }
        if (first) {
            r.first(b.getX(), b.getY(), b.getZ());
        } else {
            r.second(b.getX(), b.getY(), b.getZ());
        }
        Box box = r.box(s.box());
        p.sendActionBar(Texts.text(lang, first ? "tools.corner.first" : "tools.corner.second",
                Placeholder.unparsed("pos", b.getX() + " " + b.getY() + " " + b.getZ()),
                Placeholder.unparsed("n", box == null ? "–" : String.valueOf(box.volume()))));
        Ui.sound(p, "minecraft:block.note_block.pling", first ? 1.2f : 1.6f);
        outline(p, g.builder(p.getUniqueId()), s);
    }

    /** Kanten des Bereichs als kleine Partikel – nur für die Person selbst. */
    static void outline(Player p, Builder b, PlotState s) {
        if (b == null || s == null) {
            return;
        }
        Box box = b.region().box(s.box());
        if (box == null) {
            return;
        }
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0xF2B632), 0.8f);
        double x1 = box.minX();
        double y1 = box.minY();
        double z1 = box.minZ();
        double x2 = box.maxX() + 1;
        double y2 = box.maxY() + 1;
        double z2 = box.maxZ() + 1;
        double[][] edges = {
            {x1, y1, z1, x2, y1, z1}, {x1, y1, z2, x2, y1, z2}, {x1, y2, z1, x2, y2, z1}, {x1, y2, z2, x2, y2, z2},
            {x1, y1, z1, x1, y2, z1}, {x2, y1, z1, x2, y2, z1}, {x1, y1, z2, x1, y2, z2}, {x2, y1, z2, x2, y2, z2},
            {x1, y1, z1, x1, y1, z2}, {x2, y1, z1, x2, y1, z2}, {x1, y2, z1, x1, y2, z2}, {x2, y2, z1, x2, y2, z2}};
        for (double[] e : edges) {
            double len = Math.max(Math.abs(e[3] - e[0]), Math.max(Math.abs(e[4] - e[1]), Math.abs(e[5] - e[2])));
            int steps = (int) Math.min(40, Math.max(1, len));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                p.spawnParticle(Particle.DUST, e[0] + (e[3] - e[0]) * t, e[1] + (e[4] - e[1]) * t,
                        e[2] + (e[5] - e[2]) * t, 1, 0, 0, 0, 0, dust);
            }
        }
    }
}
