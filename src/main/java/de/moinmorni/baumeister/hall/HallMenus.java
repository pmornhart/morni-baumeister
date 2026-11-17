package de.moinmorni.baumeister.hall;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.data.Leaderboard;
import de.moinmorni.baumeister.data.PlayerData;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.logic.Placement;
import de.moinmorni.baumeister.logic.Variant;
import de.moinmorni.baumeister.menu.Menu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Truhen-Menüs der Bauhütte: Spielart wählen, So geht's, Statistik, Zuschauen. Alles mit einfachem Klick. */
public final class HallMenus {

    public static final int[] VARIANT_SLOTS = {11, 13, 15};
    static final Material[] VARIANT_ICONS = {Material.CLOCK, Material.SUGAR, Material.LEAD};
    public static final int CLOSE = 22;
    /** So geht's: sechs Kapitel in einer Reihe. */
    static final String[] HOWTO = {"theme", "build", "helps", "rate", "win", "rules"};
    static final String[] HOWTO_ICONS = {"paper", "bricks", "nether_star", "lime_wool", "gold_block", "shield"};
    public static final int FIRST_HOWTO = 10;

    private HallMenus() {
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? Material.PAPER : m;
    }

    public static void vote(BaumeisterPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.vote.title"));
        Variant[] all = Variant.values();
        for (int i = 0; i < all.length; i++) {
            Variant v = all[i];
            boolean on = plugin.hall().allowed().contains(v);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "variant." + v.id() + ".desc",
                    Placeholder.unparsed("time", de.moinmorni.baumeister.Ui.minutes(plugin.settings().buildSeconds(v), lang))));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.hall().votes().count(v)))));
            boolean mine = plugin.hall().votes().voteOf(p.getUniqueId()) == v;
            lore.add(Texts.text(lang, !on ? "menu.vote.off" : mine ? "menu.vote.mine" : "menu.vote.click"));
            menu.set(VARIANT_SLOTS[i], Items.glow(Items.named(VARIANT_ICONS[i], 1, Texts.text(lang, "variant." + v.id())
                    .color(TextColor.color(Texts.SKY)), lore), mine), q -> {
                        q.closeInventory();
                        plugin.hall().vote(q, v);
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** So geht's: ein Kapitel je Gegenstand (Maus drüber zeigt den Text – auf Bedrock antippen). */
    public static void howto(BaumeisterPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.howto.title"));
        for (int i = 0; i < HOWTO.length; i++) {
            String k = HOWTO[i];
            menu.set(FIRST_HOWTO + i + (i >= 3 ? 1 : 0), Items.named(material(HOWTO_ICONS[i]), 1,
                    Texts.text(lang, "howto." + k).color(TextColor.color(Texts.GOLD)), Texts.lines(lang, "howto." + k + ".text")),
                    q -> q.sendMessage(Texts.msg(lang, "howto." + k + ".chat")));
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void stats(BaumeisterPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.stats.title"));
        PlayerData d = plugin.data(p);
        menu.set(11, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), statLines(lang, d)), null);
        List<Component> lore = new ArrayList<>();
        List<Leaderboard.Entry> top = plugin.board().top(10);
        if (top.isEmpty()) {
            lore.add(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            lore.add(row(lang, i + 1, top.get(i)));
        }
        menu.set(15, Items.named(Material.GOLDEN_HELMET, 1, Texts.text(lang, "menu.stats.top"), lore), null);
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** Laufende Runden zum Zuschauen. */
    public static void watch(BaumeisterPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.watch.title"));
        int slot = 11;
        for (Game g : plugin.games().all()) {
            List<Component> lore = new ArrayList<>();
            lore.add(Texts.text(lang, "menu.watch.theme", Placeholder.unparsed("theme",
                    g.theme() == null ? Texts.plain(lang, "side.theme.open") : g.theme().name(lang))));
            lore.add(Texts.text(lang, "menu.watch.players", Placeholder.unparsed("n", String.valueOf(g.activeCount()))));
            lore.add(Texts.text(lang, "side.phase." + g.phase().name().toLowerCase(Locale.ROOT)));
            lore.add(Texts.text(lang, "menu.watch.click"));
            menu.set(slot, Items.named(Material.SPYGLASS, 1, Texts.text(lang, "menu.watch.arena",
                    Placeholder.unparsed("n", String.valueOf(g.arena() + 1))), lore), q -> {
                        q.closeInventory();
                        plugin.spectate(q, g);
                    });
            slot += 4;
        }
        if (plugin.games().all().isEmpty()) {
            menu.set(13, Items.named(Material.GRAY_DYE, 1, Texts.text(lang, "spectate.none"), null), Player::closeInventory);
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static Component row(String lang, int pos, Leaderboard.Entry e) {
        return Texts.text(lang, "top.row", Placeholder.unparsed("pos", String.valueOf(pos)),
                Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                Placeholder.unparsed("podiums", String.valueOf(e.podiums())),
                Placeholder.unparsed("best", Placement.stars(e.best(), lang)));
    }

    public static List<Component> statLines(String lang, PlayerData d) {
        return Texts.lines(lang, "stats.lines", Placeholder.unparsed("games", String.valueOf(d.games())),
                Placeholder.unparsed("wins", String.valueOf(d.wins())),
                Placeholder.unparsed("podiums", String.valueOf(d.podiums())),
                Placeholder.unparsed("points", String.valueOf(d.points())),
                Placeholder.unparsed("masterworks", String.valueOf(d.masterworks())),
                Placeholder.unparsed("votes", String.valueOf(d.votesGiven())),
                Placeholder.unparsed("blocks", String.valueOf(d.blocks())),
                Placeholder.unparsed("best", Placement.stars(d.bestScore(), lang)));
    }
}
