package de.moinmorni.baumeister.hall;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Holo;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.Ui;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.logic.Groups;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.Teams;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.logic.Variant;
import de.moinmorni.baumeister.logic.Votes;
import de.moinmorni.baumeister.world.HallPlan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Die Bauhütte: hier wartet man, stimmt über die Spielart ab und sagt „bereit“. Ein Countdown startet die nächste
 * Runde auf einem freien Bauplatz; zu zweit bauen Partys zusammen.
 */
public final class Hall {

    public static final int SLOT_VOTE = 0;
    public static final int SLOT_READY = 1;
    public static final int SLOT_HOWTO = 2;
    public static final int SLOT_STATS = 4;
    public static final int SLOT_WATCH = 6;
    public static final int SLOT_LOBBY = 8;
    private static final int ALL_READY_SECONDS = 5;
    private static final int MIN_PLAYERS = 2;
    /** So viele zuletzt gespielte Themen kommen nicht gleich wieder dran. */
    private static final int RECENT = 25;

    private final BaumeisterPlugin plugin;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Votes<Variant> votes = new Votes<>(Variant.class);
    private final Deque<String> recent = new ArrayDeque<>();
    private int countdown = -1;

    public Hall(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return new Location(plugin.world(), HallPlan.SPAWN_X, PlotGrid.FLOOR_Y + HallPlan.SPAWN_Y, HallPlan.SPAWN_Z,
                HallPlan.SPAWN_YAW, 0f);
    }

    public boolean inHall(UUID uuid) {
        return waiting.contains(uuid);
    }

    public Set<UUID> waiting() {
        return waiting;
    }

    public Votes<Variant> votes() {
        return votes;
    }

    public int countdown() {
        return countdown;
    }

    public List<String> recentThemes() {
        return List.copyOf(recent);
    }

    /** Dieses Thema wurde gespielt (kommt eine Weile nicht wieder). */
    public void played(Theme t) {
        recent.remove(t.id());
        recent.addLast(t.id());
        while (recent.size() > RECENT) {
            recent.removeFirst();
        }
    }

    /** Spielarten, die die config.yml erlaubt. */
    public Set<Variant> allowed() {
        return plugin.settings().variants();
    }

    /** Spielart, die als Nächstes drankäme (Mehrheit unter den erlaubten, die mit so vielen Leuten gehen). */
    public Variant nextVariant() {
        Set<Variant> possible = EnumSet.noneOf(Variant.class);
        int n = Math.min(waiting.size(), plugin.settings().maxPlayers());
        for (Variant v : allowed()) {
            if (v.possible(n)) {
                possible.add(v);
            }
        }
        if (possible.isEmpty()) {
            possible.add(Variant.KLASSISCH);
        }
        Variant w = votes.winner(possible);
        return w == null ? Variant.KLASSISCH : w;
    }

    /** In die Bauhütte (Betreten, nach der Runde, /verlassen). */
    public void enter(Player p) {
        String lang = plugin.lang(p);
        p.teleport(spawn());
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.clearActivePotionEffects();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);
        p.resetPlayerWeather();
        p.resetPlayerTime();
        p.closeInventory();
        items(p, lang);
        waiting.add(p.getUniqueId());
        ready.remove(p.getUniqueId());
        plugin.tools().forget(p.getUniqueId());
        Holo.refresh(plugin, p, lang);
        plugin.boards().hall(p);
        refreshParty(p.getUniqueId());
    }

    public void items(Player p, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_VOTE, Items.hallTag(Items.named(Material.COMPASS, 1, Texts.text(lang, "item.vote"),
                Texts.lines(lang, "item.vote.lore")), "vote"));
        inv.setItem(SLOT_READY, readyItem(p, lang));
        inv.setItem(SLOT_HOWTO, Items.hallTag(Items.named(Material.WRITABLE_BOOK, 1, Texts.text(lang, "item.howto"),
                Texts.lines(lang, "item.howto.lore")), "howto"));
        inv.setItem(SLOT_STATS, Items.hallTag(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), "stats"));
        inv.setItem(SLOT_WATCH, Items.hallTag(Items.named(Material.SPYGLASS, 1, Texts.text(lang, "item.watch"),
                Texts.lines(lang, "item.watch.lore")), "watch"));
        inv.setItem(SLOT_LOBBY, Items.hallTag(Items.named(Material.RED_BED, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), "lobby"));
    }

    private ItemStack readyItem(Player p, String lang) {
        boolean r = ready.contains(p.getUniqueId());
        return Items.glow(Items.hallTag(Items.named(r ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, "item.ready"), Texts.lines(lang, "item.ready.lore")), "ready"), r);
    }

    public void leave(UUID uuid) {
        waiting.remove(uuid);
        ready.remove(uuid);
        votes.forget(uuid);
        leaders.remove(uuid);
    }

    public void toggleReady(Player p) {
        if (!waiting.contains(p.getUniqueId())) {
            return;
        }
        String lang = plugin.lang(p);
        if (ready.remove(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "hall.not-ready"));
        } else {
            ready.add(p.getUniqueId());
            p.sendMessage(Texts.msg(lang, waiting.size() < MIN_PLAYERS ? "hall.ready.alone" : "hall.ready"));
            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
        }
        p.getInventory().setItem(SLOT_READY, readyItem(p, lang));
    }

    public void vote(Player p, Variant v) {
        String lang = plugin.lang(p);
        if (!allowed().contains(v)) {
            p.sendMessage(Texts.msg(lang, "hall.variant-off"));
            return;
        }
        votes.vote(p.getUniqueId(), v);
        p.sendMessage(Texts.msg(lang, "hall.voted", Placeholder.unparsed("choice", Texts.plain(lang, "variant." + v.id()))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
        plugin.boards().refreshHall();
    }

    /** Party-Leitung nachschlagen (kommt auf einem anderen Thread zurück). */
    public void refreshParty(UUID uuid) {
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (party.isPresent()) {
                leaders.put(uuid, party.get().leader());
            } else {
                leaders.remove(uuid);
            }
        }));
    }

    /** Jede Sekunde: Countdown, Start. */
    public void tick() {
        waiting.removeIf(u -> Bukkit.getPlayer(u) == null);
        ready.retainAll(waiting);
        votes.retain(waiting);
        if (!plugin.ready()) {
            return;
        }
        int arena = plugin.games().freeArena();
        boolean anyReady = !ready.isEmpty();
        boolean allReady = !waiting.isEmpty() && ready.containsAll(waiting);
        if (waiting.size() < MIN_PLAYERS || arena < 0
                || (!anyReady && waiting.size() < plugin.settings().hallMinPlayers())) {
            countdown = -1;
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(arena < 0 ? Texts.text(lang, "hall.no-arena")
                            : Texts.text(lang, "hall.waiting", Placeholder.unparsed("n", String.valueOf(waiting.size())),
                                    Placeholder.unparsed("min", String.valueOf(plugin.settings().hallMinPlayers()))));
                }
            }
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().hallCountdown();
        }
        if (allReady && countdown > ALL_READY_SECONDS) {
            countdown = ALL_READY_SECONDS;
        }
        Variant next = nextVariant();
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "hall.countdown", Placeholder.unparsed("secs", String.valueOf(countdown)),
                        Placeholder.unparsed("variant", Texts.plain(lang, "variant." + next.id()))));
                if (countdown <= 3 && countdown > 0) {
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.2f);
                }
            }
        }
        if (countdown-- <= 0) {
            start(next, null);
            countdown = -1;
        }
    }

    /** Runde starten (Countdown abgelaufen oder Verwaltung). null, wenn es nicht geht. */
    public Game start(Variant wanted, Theme forced) {
        int arena = plugin.games().freeArena();
        if (arena < 0) {
            return null;
        }
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID u : waiting) {
            if (Bukkit.getPlayer(u) == null) {
                continue;
            }
            UUID leader = leaders.getOrDefault(u, u);
            if (!waiting.contains(leader)) {
                leader = u;
            }
            groups.computeIfAbsent(leader, k -> new ArrayList<>()).add(u);
        }
        List<UUID> picked = Groups.pick(new ArrayList<>(groups.values()), plugin.settings().maxPlayers());
        if (picked.size() < MIN_PLAYERS) {
            return null;
        }
        Variant variant = wanted != null && wanted.possible(picked.size()) ? wanted : Variant.KLASSISCH;
        List<List<UUID>> pickedGroups = new ArrayList<>();
        for (List<UUID> g : groups.values()) {
            List<UUID> in = g.stream().filter(picked::contains).toList();
            if (!in.isEmpty()) {
                pickedGroups.add(in);
            }
        }
        List<List<UUID>> teams = Teams.form(pickedGroups, variant == Variant.PAARE);
        List<List<Player>> players = new ArrayList<>();
        for (List<UUID> t : teams) {
            players.add(t.stream().map(Bukkit::getPlayer).toList());
        }
        for (UUID u : picked) {
            leave(u);
        }
        votes.played(variant, System.currentTimeMillis());
        for (UUID u : picked) {
            Player p = Bukkit.getPlayer(u);
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "hall.starting", Placeholder.unparsed("variant", Texts.plain(lang,
                    "variant." + variant.id())), Placeholder.unparsed("n", String.valueOf(picked.size()))));
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "hall.full"));
            }
        }
        return plugin.games().start(arena, variant, players, forced);
    }
}
