package de.moinmorni.baumeister;

import de.moinmorni.baumeister.data.Leaderboard;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.hall.HallMenus;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.Variant;
import de.moinmorni.baumeister.world.HallPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Leiste rechts in der Bauhütte und die schwebenden Tafeln dort (Begrüßung, So geht's, Spielarten, Bestenliste). */
public final class Boards {

    private static final int TOP_ROWS = 8;

    private final BaumeisterPlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> hallHolos = new ArrayList<>();

    Boards(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    /** Leiste in der Bauhütte. */
    public void hall(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.players", Placeholder.unparsed("n", String.valueOf(plugin.hall().waiting().size()))));
        int cd = plugin.hall().countdown();
        lines.add(cd >= 0 ? Texts.text(lang, "side.hall.start", Placeholder.unparsed("secs", String.valueOf(cd)))
                : Texts.text(lang, "side.hall.wait"));
        lines.add(Component.text(" "));
        Variant next = plugin.hall().nextVariant();
        lines.add(Texts.text(lang, "side.hall.variant", Placeholder.unparsed("variant", Texts.plain(lang, "variant." + next.id()))));
        lines.add(Texts.text(lang, "side.hall.rounds", Placeholder.unparsed("n", String.valueOf(plugin.games().all().size()))));
        lines.add(Component.text("  "));
        var d = plugin.store().loaded(p.getUniqueId());
        lines.add(Texts.text(lang, "side.hall.wins", Placeholder.unparsed("n", String.valueOf(d.map(x -> x.wins()).orElse(0)))));
        lines.add(Texts.text(lang, "side.hall.podiums",
                Placeholder.unparsed("n", String.valueOf(d.map(x -> x.podiums()).orElse(0)))));
        lines.add(Component.text("   "));
        lines.add(Texts.text(lang, "side.footer"));
        sidebar.show(p, Texts.text(lang, "side.title"), lines);
    }

    public void hallAll() {
        for (UUID u : plugin.hall().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                hall(p);
            }
        }
    }

    // ---------- Tafeln in der Bauhütte ----------

    public void spawnHall() {
        removeHall();
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.BOARD[0] + 0.5, 2.9, HallPlan.BOARD[1] - 0.6), this::top, 0.9f));
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.VARIANTS[0] + 0.6, 2.4, HallPlan.VARIANTS[1] + 0.5), this::variants,
                0.8f));
        hallHolos.add(Holo.spawn(plugin, at(HallPlan.HOWTO[0] + 0.4, 2.4, HallPlan.HOWTO[1] + 0.5),
                lang -> Texts.text(lang, "board.howto"), 0.8f));
        hallHolos.add(Holo.spawn(plugin, at(0.5, 4.2, 4.5), lang -> Texts.text(lang, "board.welcome"), 1.1f));
    }

    private Location at(double x, double y, double z) {
        return new Location(plugin.world(), x, PlotGrid.FLOOR_Y + y, z);
    }

    private Component variants(String lang) {
        Component c = Texts.text(lang, "board.variants");
        for (Variant v : Variant.values()) {
            if (!plugin.hall().allowed().contains(v)) {
                continue;
            }
            c = c.append(Component.newline()).append(Texts.text(lang, "board.variant",
                    Placeholder.unparsed("variant", Texts.plain(lang, "variant." + v.id())),
                    Placeholder.unparsed("n", String.valueOf(plugin.hall().votes().count(v)))));
        }
        for (Game g : plugin.games().all()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "board.running",
                    Placeholder.unparsed("n", String.valueOf(g.arena() + 1)),
                    Placeholder.unparsed("phase", Texts.plain(lang, "side.phase." + g.phase().name().toLowerCase(Locale.ROOT)))));
        }
        return c;
    }

    private Component top(String lang) {
        Component c = Texts.text(lang, "board.title");
        List<Leaderboard.Entry> top = plugin.board().top(TOP_ROWS);
        if (top.isEmpty()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            c = c.append(Component.newline()).append(HallMenus.row(lang, i + 1, top.get(i)));
        }
        return c;
    }

    public void refreshHall() {
        if (hallHolos.isEmpty()) {
            return;
        }
        if (!hallHolos.stream().allMatch(Holo::valid)) {
            spawnHall();
            return;
        }
        hallHolos.get(0).update(this::top);
        hallHolos.get(1).update(this::variants);
    }

    public void removeHall() {
        hallHolos.forEach(Holo::remove);
        hallHolos.clear();
    }
}
