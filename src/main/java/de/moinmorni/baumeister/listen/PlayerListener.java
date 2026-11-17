package de.moinmorni.baumeister.listen;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.Ui;
import de.moinmorni.baumeister.game.Builder;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.game.ToolMenus;
import de.moinmorni.baumeister.hall.HallMenus;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.menu.Menu;
import de.moinmorni.baumeister.world.HallPlan;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

/**
 * Betreten, Verlassen, Knöpfe in der Schnellleiste (Bauhütte, Themen-Wahl, Werkzeugkiste, Zollstock, Wertung), dazu:
 * niemand verlässt seine Parzelle und niemand fällt aus der Bauhütte.
 */
public final class PlayerListener implements Listener {

    private final BaumeisterPlugin plugin;

    public PlayerListener(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        plugin.data(p);
        Game g = plugin.games().of(p.getUniqueId());
        Builder b = g == null ? null : g.builder(p.getUniqueId());
        if (g != null && b != null && !b.gone() && g.phase() != Game.Phase.OVER) {
            g.rejoin(p);
            return;
        }
        plugin.hall().enter(p);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.welcome"));
        p.sendMessage(Texts.msg(lang, "hall.how"));
        if (plugin.games().late(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "game.too-late.self"));
        }
        Ui.title(p, Texts.text(lang, "hall.title"), Texts.text(lang, "hall.subtitle"), 2500);
        UUID uuid = p.getUniqueId();
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player q = Bukkit.getPlayer(uuid);
            if (party.isEmpty() || q == null || !plugin.hall().inHall(uuid)) {
                return;
            }
            for (var m : party.get().members()) {
                Game mate = m.uuid().equals(uuid) ? null : plugin.games().of(m.uuid());
                if (mate != null) {
                    q.sendMessage(Texts.msg(plugin.lang(q), "hall.party-playing"));
                    return;
                }
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.hall().leave(p.getUniqueId());
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null) {
            g.quit(p);
        }
        plugin.boards().sidebar().forget(p.getUniqueId());
        plugin.store().unload(p.getUniqueId());
        plugin.forgetLanguage(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player p = e.getPlayer();
        Action a = e.getAction();
        String tag = Items.helpTag(e.getItem());
        if (tag != null) {
            Game g = plugin.games().playing(p.getUniqueId());
            Block block = e.getClickedBlock();
            if (tag.equals("wand")) {
                e.setCancelled(true);
                if (block != null && (a == Action.LEFT_CLICK_BLOCK || a == Action.RIGHT_CLICK_BLOCK)) {
                    plugin.tools().corner(p, g, block, a == Action.LEFT_CLICK_BLOCK);
                } else if (a == Action.RIGHT_CLICK_AIR) {
                    p.sendMessage(Texts.msg(plugin.lang(p), "tools.wand.how"));
                }
                return;
            }
            if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            e.setCancelled(true);
            e.setUseItemInHand(Event.Result.DENY);
            if (g == null) {
                return;
            }
            if (tag.equals("tools")) {
                ToolMenus.main(plugin, p);
            } else if (tag.startsWith("rate:")) {
                g.rate(p, Integer.parseInt(tag.substring(5)));
            } else if (tag.startsWith("theme:")) {
                g.voteTheme(p, Integer.parseInt(tag.substring(6)));
            }
            return;
        }
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        String hall = Items.hallTag(e.getItem());
        if (hall == null) {
            return;
        }
        e.setCancelled(true);
        switch (hall) {
            case "vote" -> HallMenus.vote(plugin, p);
            case "ready" -> plugin.hall().toggleReady(p);
            case "howto" -> HallMenus.howto(plugin, p);
            case "stats" -> HallMenus.stats(plugin, p);
            case "watch" -> HallMenus.watch(plugin, p);
            case "lobby" -> {
                p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                plugin.net().askToLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
            }
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() == plugin.world() && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (Items.hallTag(e.getMainHandItem()) != null || Items.hallTag(e.getOffHandItem()) != null
                || Items.helpTag(e.getMainHandItem()) != null || Items.helpTag(e.getOffHandItem()) != null) {
            e.setCancelled(true);
        }
    }

    /** Außerhalb der Bauzeit (Bauhütte, Wahl, Bewerten) lässt sich im Inventar nichts verschieben. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || p.getWorld() != plugin.world() || plugin.builder(p)) {
            return;
        }
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (holder instanceof Menu) {
            return;
        }
        if (p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && p.getWorld() == plugin.world()) {
            e.setCancelled(true);
            p.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() != plugin.world() || !e.hasChangedBlock()) {
            return;
        }
        Location to = e.getTo();
        if (plugin.hall().inHall(p.getUniqueId())) {
            if (to.getY() < PlotGrid.FLOOR_Y + HallPlan.FALL_Y) {
                p.setFallDistance(0);
                p.teleport(plugin.hall().spawn());
            }
            return;
        }
        Game g = plugin.games().of(p.getUniqueId());
        if (g == null) {
            return;
        }
        Location back = g.contain(p, to);
        if (back != null) {
            p.setFallDistance(0);
            e.setTo(back);
        }
    }
}
