package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.menu.Menu;
import de.moinmorni.baumeister.world.Construction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Die Werkzeugkiste (Bau-Hilfen) als Truhen-Fenster – alles mit einfachem Klick, auch auf Bedrock: Boden wechseln,
 * Uhrzeit und Wetter der Parzelle, Köpfe, Banner mit eigenen Mustern, Füllen/Ersetzen/Leeren im Bereich des Zollstocks,
 * einen Schritt zurück und zurück in die Mitte der Parzelle.
 */
public final class ToolMenus {

    /** Böden zur Wahl. */
    public static final List<Material> FLOORS = List.of(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.MOSS_BLOCK, Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.SNOW_BLOCK, Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS, Material.SMOOTH_STONE,
            Material.MOSSY_COBBLESTONE, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS,
            Material.BRICKS, Material.QUARTZ_BLOCK, Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
            Material.BLUE_ICE, Material.PACKED_ICE, Material.TERRACOTTA, Material.MUD, Material.SANDSTONE,
            Material.END_STONE);
    static final Material[] MOB_HEADS = {Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL, Material.ZOMBIE_HEAD,
        Material.CREEPER_HEAD, Material.PIGLIN_HEAD, Material.DRAGON_HEAD};
    /** Eigene Banner: Kennung, Grundfarbe, Muster („farbe:muster“). */
    static final String[][] BANNERS = {
        {"weissblau", "white", "light_blue:rhombus,light_blue:triangles_top,light_blue:triangles_bottom"},
        {"smaragd", "green", "lime:rhombus,green:circle,yellow:border"},
        {"gold", "yellow", "orange:gradient_up,yellow:stripe_middle,orange:curly_border"},
        {"rotweiss", "red", "white:stripe_center,red:stripe_top,red:stripe_bottom,white:border"},
        {"gipfel", "light_blue", "white:triangle_bottom,gray:triangles_bottom,white:stripe_top"},
        {"tanne", "white", "green:triangle_bottom,green:stripe_middle,brown:stripe_bottom,white:border"},
        {"sonne", "light_blue", "yellow:circle,orange:flower,light_blue:border"},
        {"ziegel", "red", "brown:bricks,red:border"},
        {"nacht", "blue", "black:gradient,yellow:circle,blue:border"},
    };
    static final int CLOSE = 49;

    private ToolMenus() {
    }

    private static Material material(String s) {
        Material m = Material.matchMaterial(s.toUpperCase(Locale.ROOT));
        return m == null ? Material.PAPER : m;
    }

    private static ItemStack button(String lang, Material m, String key) {
        return Items.named(m, 1, Texts.text(lang, "tools." + key), Texts.lines(lang, "tools." + key + ".lore"));
    }

    private static void back(BaumeisterPlugin plugin, Menu menu, String lang, int slot) {
        menu.set(slot, Items.named(Material.ARROW, 1, Texts.text(lang, "menu.back"), null),
                q -> main(plugin, q));
    }

    /** Hauptfenster der Werkzeugkiste. */
    public static void main(BaumeisterPlugin plugin, Player p) {
        Game g = plugin.games().playing(p.getUniqueId());
        String lang = plugin.lang(p);
        if (g == null || g.buildPlot(p.getUniqueId()) == null) {
            p.sendMessage(Texts.msg(lang, "tools.not-now"));
            return;
        }
        PlotState s = g.buildPlot(p.getUniqueId());
        Menu menu = new Menu(6, Texts.text(lang, "menu.tools.title"));
        menu.set(10, button(lang, Material.GRASS_BLOCK, "floor"), q -> floors(plugin, q));
        menu.set(12, button(lang, Material.CLOCK, "time"), q -> times(plugin, q));
        menu.set(14, Items.glow(button(lang, s.rain() ? Material.WATER_BUCKET : Material.SUNFLOWER,
                s.rain() ? "weather.rain" : "weather.sun"), true), q -> {
                    g.setRain(q, !s.rain());
                    main(plugin, q);
                });
        menu.set(16, button(lang, Material.PLAYER_HEAD, "heads"), q -> heads(plugin, q));
        menu.set(28, button(lang, Material.WHITE_BANNER, "banners"), q -> banners(plugin, q));
        menu.set(30, button(lang, Material.BUCKET, "fill"), q -> pick(plugin, q, null, false));
        menu.set(31, button(lang, Material.SHEARS, "replace"), q -> pick(plugin, q, null, true));
        menu.set(32, button(lang, Material.FEATHER, "clear"), q -> {
            q.closeInventory();
            plugin.tools().fill(q, g, Material.AIR, null);
        });
        menu.set(34, button(lang, Material.RECOVERY_COMPASS, "undo"), q -> {
            q.closeInventory();
            plugin.tools().undo(q, g);
        });
        menu.set(46, button(lang, Material.BLAZE_ROD, "wand"), q -> {
            q.closeInventory();
            g.giveTools(q);
            q.sendMessage(Texts.msg(lang, "tools.wand.how"));
        });
        menu.set(47, button(lang, Material.STRUCTURE_VOID, "forget"), q -> {
            g.builder(q.getUniqueId()).region().clear();
            q.sendMessage(Texts.msg(lang, "tools.forget.done"));
            q.closeInventory();
        });
        menu.set(51, button(lang, Material.ENDER_PEARL, "home"), q -> {
            q.closeInventory();
            var b = g.buildPlot(q.getUniqueId());
            if (b != null) {
                q.teleport(new org.bukkit.Location(q.getWorld(), (b.box().minX() + b.box().maxX() + 1) / 2.0,
                        b.box().minY() + 1, (b.box().minZ() + b.box().maxZ() + 1) / 2.0, q.getYaw(), q.getPitch()));
            }
        });
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    static void floors(BaumeisterPlugin plugin, Player p) {
        Game g = plugin.games().playing(p.getUniqueId());
        String lang = plugin.lang(p);
        Menu menu = new Menu(6, Texts.text(lang, "menu.floor.title"));
        for (int i = 0; i < FLOORS.size(); i++) {
            Material m = FLOORS.get(i);
            menu.set(i, Items.named(m, 1, null, Texts.lines(lang, "menu.floor.lore")), q -> {
                q.closeInventory();
                plugin.tools().floor(q, g, m);
            });
        }
        back(plugin, menu, lang, 45);
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    static void times(BaumeisterPlugin plugin, Player p) {
        Game g = plugin.games().playing(p.getUniqueId());
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.time.title"));
        Material[] icons = {Material.ORANGE_DYE, Material.YELLOW_DYE, Material.MAGENTA_DYE, Material.BLUE_DYE};
        for (int i = 0; i < PlotState.TIME_IDS.length; i++) {
            int t = i;
            menu.set(10 + i * 2, Items.named(icons[i], 1, Texts.text(lang, "tools.time." + PlotState.TIME_IDS[i]), null),
                    q -> {
                        g.setTime(q, t);
                        q.closeInventory();
                    });
        }
        back(plugin, menu, lang, 18);
        menu.set(26, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    static void heads(BaumeisterPlugin plugin, Player p) {
        Game g = plugin.games().playing(p.getUniqueId());
        String lang = plugin.lang(p);
        Menu menu = new Menu(4, Texts.text(lang, "menu.heads.title"));
        int slot = 0;
        for (Material m : MOB_HEADS) {
            ItemStack head = new ItemStack(m);
            menu.set(slot++, head, q -> q.getInventory().addItem(new ItemStack(m)));
        }
        slot = 9;
        for (Builder b : g == null ? List.<Builder>of() : g.builders()) {
            Player owner = Bukkit.getPlayer(b.uuid());
            if (owner == null || slot >= 27) {
                continue;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
            menu.set(slot++, head, q -> q.getInventory().addItem(head.clone()));
        }
        back(plugin, menu, lang, 27);
        menu.set(35, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** Banner-Gegenstand eines eigenen Musters. */
    public static ItemStack banner(BaumeisterPlugin plugin, String lang, String[] design) {
        ItemStack item = new ItemStack(material(design[1] + "_banner"));
        item.editMeta(BannerMeta.class, meta -> {
            meta.setPatterns(Construction.patterns(List.of(design[2].split(",")), plugin.getLogger()));
            meta.displayName(Texts.text(lang, "banner." + design[0]));
        });
        return item;
    }

    static void banners(BaumeisterPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.banners.title"));
        for (int i = 0; i < BANNERS.length; i++) {
            String[] d = BANNERS[i];
            ItemStack item = banner(plugin, lang, d);
            menu.set(i, item, q -> q.getInventory().addItem(banner(plugin, plugin.lang(q), d)));
        }
        back(plugin, menu, lang, 18);
        menu.set(26, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** Blöcke aus der Schnellleiste zur Wahl (für Füllen und Ersetzen). */
    static List<Material> hotbarBlocks(Player p) {
        Set<Material> out = new LinkedHashSet<>();
        for (int i = 0; i < 9; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it != null && Tools.usable(it.getType())) {
                out.add(it.getType());
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * Block wählen: Füllen (womit?), Ersetzen Schritt 1 (was?) bzw. Schritt 2 (womit? – auch Luft).
     *
     * @param from beim Ersetzen Schritt 2: was ersetzt wird
     */
    static void pick(BaumeisterPlugin plugin, Player p, Material from, boolean replace) {
        Game g = plugin.games().playing(p.getUniqueId());
        String lang = plugin.lang(p);
        String title = !replace ? "menu.pick.fill" : from == null ? "menu.pick.from" : "menu.pick.to";
        Menu menu = new Menu(3, Texts.text(lang, title));
        List<Material> blocks = hotbarBlocks(p);
        if (blocks.isEmpty()) {
            menu.set(13, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.pick.none"),
                    Texts.lines(lang, "menu.pick.none.lore")), Player::closeInventory);
        }
        for (int i = 0; i < blocks.size(); i++) {
            Material m = blocks.get(i);
            menu.set(9 + i, Items.named(m, 1, null, Texts.lines(lang, "menu.pick.lore")), q -> {
                if (replace && from == null) {
                    pick(plugin, q, m, true);
                    return;
                }
                q.closeInventory();
                plugin.tools().fill(q, g, m, from);
            });
        }
        if (replace && from != null) {
            menu.set(17, Items.named(Material.GLASS, 1, Texts.text(lang, "menu.pick.air"), null), q -> {
                q.closeInventory();
                plugin.tools().fill(q, g, Material.AIR, from);
            });
        }
        back(plugin, menu, lang, 18);
        menu.set(26, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }
}
