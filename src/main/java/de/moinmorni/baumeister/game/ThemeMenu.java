package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.menu.Menu;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Themen-Wahl als Truhen-Fenster: drei Vorschläge, ein Klick ist eine Stimme. */
public final class ThemeMenu extends Menu {

    public static final int[] SLOTS = {11, 13, 15};
    private static final Material[] ICONS = {Material.PAPER, Material.MAP, Material.BOOK};

    private ThemeMenu(Component title) {
        super(3, title);
    }

    public static void open(BaumeisterPlugin plugin, Player p, Game g) {
        if (g.themeVote() == null || g.phase() != Game.Phase.THEME) {
            return;
        }
        String lang = plugin.lang(p);
        ThemeMenu menu = new ThemeMenu(Texts.text(lang, "menu.theme.title"));
        Runnable draw = () -> {
            for (int i = 0; i < g.themeVote().options().size(); i++) {
                Theme t = g.themeVote().options().get(i);
                int index = i;
                boolean mine = g.themeVote().voteOf(p.getUniqueId()) == i;
                List<Component> lore = new ArrayList<>();
                lore.add(Texts.text(lang, "menu.theme.category", Placeholder.unparsed("cat",
                        Texts.plain(lang, "category." + t.category().name().toLowerCase(java.util.Locale.ROOT)))));
                lore.add(Texts.text(lang, "menu.theme.votes", Placeholder.unparsed("n", String.valueOf(g.themeVote().count(i)))));
                lore.add(Texts.text(lang, mine ? "menu.theme.mine" : "menu.theme.click"));
                menu.set(SLOTS[i], Items.glow(Items.named(ICONS[i], i + 1, Texts.text(lang, "menu.theme.option",
                        Placeholder.unparsed("theme", t.name(lang))), lore), mine), q -> {
                            g.voteTheme(q, index);
                            menu.refresh();
                        });
            }
        };
        draw.run();
        menu.refresher(draw);
        menu.open(p);
    }
}
