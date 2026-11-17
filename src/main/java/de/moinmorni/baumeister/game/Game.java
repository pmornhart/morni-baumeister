package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Items;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.Ui;
import de.moinmorni.baumeister.data.PlayerData;
import de.moinmorni.baumeister.logic.Comeback;
import de.moinmorni.baumeister.logic.Placement;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.PlotGrid.Box;
import de.moinmorni.baumeister.logic.Rating;
import de.moinmorni.baumeister.logic.Scoring;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.logic.ThemeVote;
import de.moinmorni.baumeister.logic.Themes;
import de.moinmorni.baumeister.logic.Throttle;
import de.moinmorni.baumeister.logic.Variant;
import de.moinmorni.baumeister.world.ArenaPlan;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Eine Runde Baumeister auf einem Bauplatz: Themen-Wahl (oder Los), Bauzeit im Kreativmodus auf der eigenen Parzelle,
 * Bewertung aller Bauten der Reihe nach (eigener Bau nicht), Siegerehrung am Podest mit Meisterbrief – dann zurück in
 * die Bauhütte, und die Parzellen werden geräumt.
 */
public final class Game {

    /** Ablauf einer Runde. */
    public enum Phase {
        THEME, BUILD, RATE, AWARD, OVER
    }

    public static final int SLOT_WAND = 7;
    public static final int SLOT_TOOLS = 8;
    public static final int[] THEME_SLOTS = {2, 4, 6};
    private static final int SECOND = 20;
    private static final int[] WARN = {60, 30, 10, 5, 4, 3, 2, 1};
    /** Haben alle bewertet, geht es nach so vielen Sekunden weiter. */
    private static final int ALL_VOTED_SECONDS = 3;
    private static final int TOOLS_CHECK = 40;
    private static final Material[] RATE_ITEMS = {Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
        Material.LIME_WOOL, Material.LIGHT_BLUE_WOOL};
    /** Wird gerade ein eigenes Feuerwerk gezündet? (Der Schutz lässt nur das durch.) */
    public static boolean spawningOwn;

    private final BaumeisterPlugin plugin;
    private final int arena;
    private final Variant variant;
    private final Random random;
    private final Map<UUID, Builder> builders = new LinkedHashMap<>();
    private final Map<Integer, PlotState> plots = new LinkedHashMap<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Comeback comeback = new Comeback();
    private final Throttle redstone;
    private final Scoring scoring = new Scoring();
    private final LangBar bar = new LangBar(BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final int buildSeconds;
    private Phase phase;
    private ThemeVote themeVote;
    private Theme theme;
    private int secondsLeft;
    private int phaseSeconds;
    private long ticks;
    private final List<Integer> rateOrder = new ArrayList<>();
    private int rateIndex = -1;
    private List<Scoring.Result> results = List.of();
    private final Set<UUID> statsSent = new HashSet<>();

    public Game(BaumeisterPlugin plugin, int arena, Variant variant, List<List<Player>> teams, Theme forced) {
        this.plugin = plugin;
        this.arena = arena;
        this.variant = variant;
        this.random = new Random(System.nanoTime());
        this.redstone = new Throttle(plugin.settings().limits().redstonePerSecond());
        this.buildSeconds = plugin.settings().buildSeconds(variant);
        int size = plugin.settings().plotSize();
        int height = plugin.settings().plotHeight();
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < PlotGrid.PLOTS; i++) {
            free.add(i);
        }
        // Parzellen über den Platz verteilt: erst jede zweite, dann der Rest (weniger Gedränge beim Zuschauen).
        List<Integer> order = new ArrayList<>();
        for (int i : free) {
            if (((i % PlotGrid.COLUMNS) + (i / PlotGrid.COLUMNS)) % 2 == 0) {
                order.add(i);
            }
        }
        for (int i : free) {
            if (!order.contains(i)) {
                order.add(i);
            }
        }
        for (int t = 0; t < teams.size() && t < order.size(); t++) {
            int plot = order.get(t);
            List<UUID> members = teams.get(t).stream().map(Player::getUniqueId).toList();
            plots.put(plot, new PlotState(plot, members, PlotGrid.plot(arena, plot, size, height)));
            for (Player p : teams.get(t)) {
                builders.put(p.getUniqueId(), new Builder(p.getUniqueId(), p.getName(), plot));
                plugin.store().pin(p.getUniqueId());
            }
        }
        plugin.construction().inUse(arena);
        for (Builder b : builders.values()) {
            Player p = Bukkit.getPlayer(b.uuid());
            if (p != null) {
                prepare(p, b);
                plugin.net().playing(p, true);
            }
        }
        if (forced != null) {
            theme = forced;
            startBuild();
        } else if (plugin.settings().themeVote()) {
            themeVote = new ThemeVote(Themes.suggest(random, 3, plugin.hall().recentThemes()));
            phase = Phase.THEME;
            phaseSeconds = plugin.settings().themeSeconds();
            secondsLeft = phaseSeconds;
            for (Player p : players()) {
                themeItems(p);
                ThemeMenu.open(plugin, p, this);
            }
        } else {
            theme = Themes.suggest(random, 1, plugin.hall().recentThemes()).get(0);
            startBuild();
        }
        refreshBar();
    }

    // ---------------------------------------------------------------- Zugriff

    public int arena() {
        return arena;
    }

    public Variant variant() {
        return variant;
    }

    public Phase phase() {
        return phase;
    }

    public Theme theme() {
        return theme;
    }

    public ThemeVote themeVote() {
        return themeVote;
    }

    public Builder builder(UUID uuid) {
        return builders.get(uuid);
    }

    public Collection<Builder> builders() {
        return builders.values();
    }

    public PlotState plot(int index) {
        return plots.get(index);
    }

    public Collection<PlotState> plots() {
        return plots.values();
    }

    public boolean spectating(UUID uuid) {
        return spectators.contains(uuid);
    }

    public Throttle redstone() {
        return redstone;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    /** Gerade bewertete Parzelle (null außerhalb der Bewertung). */
    public PlotState shown() {
        return phase == Phase.RATE && rateIndex >= 0 && rateIndex < rateOrder.size() ? plots.get(rateOrder.get(rateIndex))
                : null;
    }

    /** Leute, die mitspielen (da und nicht raus). */
    public int activeCount() {
        return (int) builders.values().stream().filter(Builder::active).count();
    }

    /** Parzelle dieser Person, wenn sie gerade bauen darf (sonst null). */
    public PlotState buildPlot(UUID uuid) {
        Builder b = builders.get(uuid);
        return phase == Phase.BUILD && b != null && b.active() ? plots.get(b.plot()) : null;
    }

    /** Parzelle an dieser Stelle (Baufläche), null wenn keine. */
    public PlotState plotAt(int x, int y, int z) {
        for (PlotState s : plots.values()) {
            if (s.box().contains(x, y, z)) {
                return s;
            }
        }
        return null;
    }

    /** Alle Mitspielenden und Zuschauenden, die gerade da sind. */
    List<Player> everyone() {
        List<Player> out = new ArrayList<>(players());
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Mitspielende, die gerade da sind. */
    List<Player> players() {
        List<Player> out = new ArrayList<>();
        for (Builder b : builders.values()) {
            Player p = b.active() ? Bukkit.getPlayer(b.uuid()) : null;
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private World world() {
        return plugin.world();
    }

    private String lang(Player p) {
        return plugin.lang(p);
    }

    private void broadcast(String key, TagResolver... r) {
        for (Player p : everyone()) {
            p.sendMessage(Texts.msg(lang(p), key, r));
        }
    }

    private Location center(PlotState s) {
        Box b = s.box();
        return new Location(world(), (b.minX() + b.maxX() + 1) / 2.0, PlotGrid.FLOOR_Y + 1, (b.minZ() + b.maxZ() + 1) / 2.0,
                180f, 0f);
    }

    /** Blick auf eine Parzelle beim Bewerten: vom Süden, etwas erhöht. */
    private Location view(PlotState s) {
        Box b = s.box();
        return new Location(world(), (b.minX() + b.maxX() + 1) / 2.0, PlotGrid.FLOOR_Y + 8, b.maxZ() + 7.5, 180f, 25f);
    }

    private Location audience() {
        return audience(0);
    }

    /** Zuschauerplatz vor dem Podest: Reihen zu sieben, nebeneinander (niemand steht im anderen). */
    private Location audience(int i) {
        int col = i % 7;
        int row = i / 7;
        double x = (col % 2 == 0 ? col / 2 : -(col + 1) / 2) * 1.5;
        return new Location(world(), PlotGrid.arenaX(arena) + 0.5 + x, PlotGrid.FLOOR_Y + 1,
                PlotGrid.PODIUM_Z + ArenaPlan.AUDIENCE + 0.5 + row * 1.5, 180f, -10f);
    }

    // ---------------------------------------------------------------- Ausrüstung

    /** Spieler für die Runde vorbereiten (Start, Rückkehr). */
    private void prepare(Player p, Builder b) {
        plugin.hall().leave(p.getUniqueId());
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.clearActivePotionEffects();
        p.getInventory().clear();
        if (phase == null || phase == Phase.THEME || phase == Phase.BUILD) {
            PlotState s = plots.get(b.plot());
            Location back = b.lastSeen();
            p.teleport(back != null && PlotGrid.roam(s.box()).contains(back.getBlockX(), back.getBlockY(), back.getBlockZ())
                    ? back : center(s));
            applySky(p, s);
        }
        if (phase == Phase.BUILD) {
            buildMode(p);
        } else if (phase == Phase.THEME || phase == null) {
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(false);
        }
    }

    private void buildMode(Player p) {
        p.setGameMode(GameMode.CREATIVE);
        p.setAllowFlight(true);
        p.getInventory().clear();
        giveTools(p);
        p.getInventory().setHeldItemSlot(0);
    }

    /** Werkzeugkiste und Zollstock in die Schnellleiste (fehlt einer, kommt er wieder). */
    public void giveTools(Player p) {
        String lang = lang(p);
        PlayerInventory inv = p.getInventory();
        if (!has(p, "tools")) {
            put(inv, SLOT_TOOLS, Items.helpTag(Items.named(Material.NETHER_STAR, 1, Texts.text(lang, "item.tools"),
                    Texts.lines(lang, "item.tools.lore")), "tools"));
        }
        if (!has(p, "wand")) {
            put(inv, SLOT_WAND, Items.helpTag(Items.named(Material.BLAZE_ROD, 1, Texts.text(lang, "item.wand"),
                    Texts.lines(lang, "item.wand.lore")), "wand"));
        }
    }

    private static void put(PlayerInventory inv, int slot, ItemStack item) {
        ItemStack there = inv.getItem(slot);
        if (there == null || there.getType().isAir()) {
            inv.setItem(slot, item);
        } else {
            inv.addItem(item);
        }
    }

    private static boolean has(Player p, String tag) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (tag.equals(Items.helpTag(i))) {
                return true;
            }
        }
        return false;
    }

    private void themeItems(Player p) {
        String lang = lang(p);
        p.getInventory().clear();
        for (int i = 0; i < themeVote.options().size(); i++) {
            Theme t = themeVote.options().get(i);
            boolean mine = themeVote.voteOf(p.getUniqueId()) == i;
            p.getInventory().setItem(THEME_SLOTS[i], Items.glow(Items.helpTag(Items.named(Material.PAPER, i + 1,
                    Texts.text(lang, "item.theme", Placeholder.unparsed("theme", t.name(lang))),
                    Texts.lines(lang, "item.theme.lore")), "theme:" + i), mine));
        }
    }

    private void rateItems(Player p) {
        String lang = lang(p);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        PlotState s = shown();
        if (s == null) {
            return;
        }
        if (!scoring.mayRate(p.getUniqueId(), s.index())) {
            inv.setItem(4, Items.named(Material.OAK_SIGN, 1, Texts.text(lang, "item.own"), Texts.lines(lang, "item.own.lore")));
            return;
        }
        int mine = scoring.ratingOf(p.getUniqueId(), s.index());
        for (Rating r : Rating.values()) {
            inv.setItem(r.stars() - 1, Items.glow(Items.helpTag(Items.named(RATE_ITEMS[r.stars() - 1], r.stars(),
                    Texts.text(lang, "rate." + r.id() + ".item"), Texts.lines(lang, "rate.item.lore")),
                    "rate:" + r.stars()), mine == r.stars()));
        }
        inv.setHeldItemSlot(Math.max(0, Math.min(4, mine == 0 ? 2 : mine - 1)));
    }

    private void applySky(Player p, PlotState s) {
        p.setPlayerTime(s.ticks(), false);
        p.setPlayerWeather(s.rain() ? WeatherType.DOWNFALL : WeatherType.CLEAR);
    }

    // ---------------------------------------------------------------- Ablauf

    /** Ein Spieltick; false, wenn die Runde vorbei ist. */
    public boolean tick() {
        if (phase == Phase.OVER) {
            return false;
        }
        ticks++;
        if (ticks % SECOND == 0) {
            second();
        }
        if (ticks % TOOLS_CHECK == 0 && phase == Phase.BUILD) {
            for (Player p : players()) {
                giveTools(p);
                Tools.outline(p, builders.get(p.getUniqueId()), plots.get(builders.get(p.getUniqueId()).plot()));
            }
            plugin.guard().sweep(this);
        }
        return phase != Phase.OVER;
    }

    private void second() {
        long now = Bukkit.getCurrentTick();
        for (UUID u : comeback.expired(now)) {
            Builder b = builders.get(u);
            if (b != null) {
                b.away(false);
                b.late(true);
                plugin.net().holdSlot(u, 0);
                broadcast("game.too-late", Placeholder.unparsed("name", b.name()));
            }
        }
        if (phase != Phase.AWARD && activeCount() == 0 && comeback.count() == 0) {
            finish(false);
            return;
        }
        secondsLeft--;
        switch (phase) {
            case THEME -> {
                if (secondsLeft <= 0) {
                    chooseTheme();
                }
            }
            case BUILD -> {
                for (int w : WARN) {
                    if (secondsLeft == w) {
                        for (Player p : everyone()) {
                            p.sendMessage(Texts.msg(lang(p), w >= 10 ? "game.build.warn" : "game.build.count",
                                    Placeholder.unparsed("secs", String.valueOf(w))));
                            Ui.sound(p, "minecraft:block.note_block.hat", w > 5 ? 1f : 1.5f);
                        }
                    }
                }
                if (secondsLeft <= 0) {
                    startRating();
                }
            }
            case RATE -> {
                PlotState s = shown();
                if (s != null && secondsLeft > ALL_VOTED_SECONDS && allVoted(s)) {
                    secondsLeft = ALL_VOTED_SECONDS;
                }
                if (secondsLeft <= 0) {
                    nextPlot();
                }
            }
            case AWARD -> {
                if (secondsLeft % 2 == 0 && secondsLeft > 3) {
                    fireworks();
                }
                if (secondsLeft <= 0) {
                    finish(true);
                    return;
                }
            }
            default -> {
            }
        }
        refreshBar();
        sidebars();
    }

    private void chooseTheme() {
        theme = themeVote.winner(random);
        for (Player p : everyone()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof ThemeMenu) {
                p.closeInventory();
            }
        }
        int votes = themeVote.count(themeVote.options().indexOf(theme));
        for (Player p : everyone()) {
            p.sendMessage(Texts.msg(lang(p), "theme.chosen", Placeholder.unparsed("votes", String.valueOf(votes)),
                    Placeholder.unparsed("theme", theme.name(lang(p)))));
        }
        startBuild();
    }

    private void startBuild() {
        phase = Phase.BUILD;
        plugin.hall().played(theme);
        phaseSeconds = buildSeconds;
        secondsLeft = buildSeconds;
        for (Player p : players()) {
            String lang = lang(p);
            buildMode(p);
            Ui.title(p, Texts.text(lang, "game.build.title", Placeholder.unparsed("theme", theme.name(lang))),
                    Texts.text(lang, "game.build.sub", Placeholder.unparsed("time", Ui.minutes(buildSeconds, lang))), 3500);
            p.sendMessage(Texts.msg(lang, "game.build.start", Placeholder.unparsed("theme", theme.name(lang)),
                    Placeholder.unparsed("time", Ui.minutes(buildSeconds, lang))));
            p.sendMessage(Texts.msg(lang, "game.build.help"));
            Builder b = builders.get(p.getUniqueId());
            PlotState s = plots.get(b.plot());
            if (s.members().size() > 1) {
                String partner = s.members().stream().filter(u -> !u.equals(p.getUniqueId()))
                        .map(u -> builders.get(u).name()).findFirst().orElse("?");
                p.sendMessage(Texts.msg(lang, "game.build.partner", Placeholder.unparsed("name", partner)));
            }
            Ui.sound(p, "minecraft:block.anvil.use", 1.2f);
        }
        for (Player p : spectatorPlayers()) {
            String lang = lang(p);
            p.sendMessage(Texts.msg(lang, "game.build.start", Placeholder.unparsed("theme", theme.name(lang)),
                    Placeholder.unparsed("time", Ui.minutes(buildSeconds, lang))));
        }
    }

    private List<Player> spectatorPlayers() {
        List<Player> out = new ArrayList<>();
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private void startRating() {
        phase = Phase.RATE;
        for (PlotState s : plots.values()) {
            scoring.plot(s.index(), Set.copyOf(s.members()));
            boolean someone = s.members().stream().anyMatch(u -> !builders.get(u).gone());
            if (s.built() && someone) {
                rateOrder.add(s.index());
            }
        }
        java.util.Collections.shuffle(rateOrder, random);
        for (Player p : players()) {
            String lang = lang(p);
            p.closeInventory();
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
            Ui.title(p, Texts.text(lang, "game.rate.title"), Texts.text(lang, "game.rate.sub",
                    Placeholder.unparsed("n", String.valueOf(rateOrder.size()))), 3000);
            Ui.sound(p, "minecraft:block.bell.use", 1f);
        }
        broadcast("game.rate.start", Placeholder.unparsed("n", String.valueOf(rateOrder.size())));
        if (rateOrder.isEmpty()) {
            broadcast("game.rate.nothing");
            startAward();
            return;
        }
        nextPlot();
    }

    private void nextPlot() {
        rateIndex++;
        while (rateIndex < rateOrder.size()
                && plots.get(rateOrder.get(rateIndex)).members().stream().allMatch(u -> builders.get(u).gone())) {
            rateIndex++;
        }
        if (rateIndex >= rateOrder.size()) {
            startAward();
            return;
        }
        PlotState s = shown();
        phaseSeconds = plugin.settings().rateSeconds();
        secondsLeft = phaseSeconds;
        for (Player p : everyone()) {
            showPlot(p, s);
        }
    }

    private void showPlot(Player p, PlotState s) {
        String lang = lang(p);
        p.teleport(view(s));
        if (!spectators.contains(p.getUniqueId())) {
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
        }
        applySky(p, s);
        rateItems(p);
        String pos = String.valueOf(rateIndex + 1);
        String of = String.valueOf(rateOrder.size());
        boolean own = !scoring.mayRate(p.getUniqueId(), s.index()) && builders.containsKey(p.getUniqueId());
        Ui.title(p, Texts.text(lang, "game.rate.plot", Placeholder.unparsed("pos", pos), Placeholder.unparsed("of", of)),
                Texts.text(lang, own ? "game.rate.own" : "game.rate.ask", Placeholder.unparsed("theme", theme.name(lang))),
                2500);
        Ui.sound(p, "minecraft:entity.experience_orb.pickup", 1.2f);
    }

    private boolean allVoted(PlotState s) {
        for (Player p : players()) {
            if (scoring.mayRate(p.getUniqueId(), s.index()) && scoring.ratingOf(p.getUniqueId(), s.index()) == 0) {
                return false;
            }
        }
        return true;
    }

    /** Bewerten (Schnellleiste, Menü oder /bewerten). */
    public void rate(Player p, int stars) {
        String lang = lang(p);
        PlotState s = shown();
        Builder b = builders.get(p.getUniqueId());
        if (s == null || b == null || !b.active()) {
            p.sendMessage(Texts.msg(lang, "rate.not-now"));
            return;
        }
        if (!scoring.rate(p.getUniqueId(), s.index(), stars)) {
            p.sendMessage(Texts.msg(lang, "rate.own"));
            return;
        }
        Rating r = Rating.ofStars(stars);
        p.sendActionBar(Texts.text(lang, "rate.given", Placeholder.unparsed("name", Texts.plain(lang, "rate." + r.id())),
                Placeholder.unparsed("stars", "★".repeat(stars))));
        Ui.sound(p, "minecraft:block.note_block.bell", 0.6f + stars * 0.2f);
        rateItems(p);
        p.getInventory().setHeldItemSlot(stars - 1);
    }

    /** Themen-Stimme (Menü, Schnellleiste, /thema). */
    public void voteTheme(Player p, int index) {
        String lang = lang(p);
        Builder b = builders.get(p.getUniqueId());
        if (phase != Phase.THEME || b == null || !b.active() || !themeVote.vote(p.getUniqueId(), index)) {
            p.sendMessage(Texts.msg(lang, "theme.not-now"));
            return;
        }
        p.sendMessage(Texts.msg(lang, "theme.voted",
                Placeholder.unparsed("theme", themeVote.options().get(index).name(lang))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
        themeItems(p);
    }

    private void startAward() {
        phase = Phase.AWARD;
        phaseSeconds = plugin.settings().awardSeconds();
        secondsLeft = phaseSeconds;
        results = scoring.results();
        int count = results.size();
        Map<Integer, Scoring.Result> byPlot = new HashMap<>();
        for (Scoring.Result r : results) {
            byPlot.put(r.plot(), r);
        }
        // Statistik für alle, die bis jetzt dabei waren (auch zu spät Gekommene; selbst Gegangene nicht).
        for (Builder b : builders.values()) {
            if (b.left()) {
                continue;
            }
            Scoring.Result r = byPlot.get(b.plot());
            boolean rated = r != null && rateOrder.contains(b.plot()) && !b.late();
            boolean won = rated && Placement.won(r, rateOrder.size());
            boolean podium = rated && Placement.podium(r, rateOrder.size());
            int points = rated ? r.sum() : 0;
            int fives = rated ? r.fives() : 0;
            int score = rated && r.votes() > 0 ? r.average() : 0;
            int given = scoring.given(b.uuid());
            sendStats(b, new PlayerData.Round(won, podium, points, given, fives, b.blocks(), score));
        }
        List<Scoring.Result> ranked = results.stream().filter(r -> rateOrder.contains(r.plot())).toList();
        int seat = 0;
        for (Player p : everyone()) {
            p.closeInventory();
            p.getInventory().clear();
            if (!spectators.contains(p.getUniqueId())) {
                p.setGameMode(GameMode.ADVENTURE);
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.resetPlayerTime();
            p.setPlayerTime(6000L, false);
            p.setPlayerWeather(WeatherType.CLEAR);
            p.teleport(audience(seat++));
        }
        // Podest: Plätze 1–3 (geteilte Plätze stehen nebeneinander).
        for (Scoring.Result r : ranked) {
            if (r.place() > 3 || r.votes() == 0) {
                continue;
            }
            int[] spot = ArenaPlan.PODIUM[r.place() - 1];
            List<UUID> members = plots.get(r.plot()).members();
            for (int i = 0; i < members.size(); i++) {
                Player p = Bukkit.getPlayer(members.get(i));
                if (p != null && builders.get(p.getUniqueId()).active()) {
                    double off = members.size() > 1 ? (i == 0 ? -0.3 : 0.3) : 0;
                    p.teleport(new Location(world(), PlotGrid.arenaX(arena) + spot[0] + 0.5 + off,
                            PlotGrid.FLOOR_Y + spot[1] + 1, PlotGrid.PODIUM_Z + 0.5, 0f, 0f));
                }
            }
        }
        for (Player p : everyone()) {
            announce(p, ranked, byPlot);
        }
        fireworks();
    }

    private String names(int plot) {
        List<String> n = new ArrayList<>();
        for (UUID u : plots.get(plot).members()) {
            n.add(builders.get(u).name());
        }
        return String.join(" & ", n);
    }

    private void announce(Player p, List<Scoring.Result> ranked, Map<Integer, Scoring.Result> byPlot) {
        String lang = lang(p);
        p.sendMessage(Texts.msg(lang, "award.head", Placeholder.unparsed("theme", theme.name(lang))));
        if (ranked.isEmpty() || ranked.get(0).votes() == 0) {
            p.sendMessage(Texts.text(lang, "award.none"));
            Ui.title(p, Texts.text(lang, "award.none.title"), Component.empty(), 2500);
            return;
        }
        for (int i = 0; i < Math.min(5, ranked.size()); i++) {
            Scoring.Result r = ranked.get(i);
            p.sendMessage(Texts.text(lang, "award.row", Placeholder.unparsed("pos", String.valueOf(r.place())),
                    Placeholder.unparsed("name", names(r.plot())),
                    Placeholder.unparsed("stars", Placement.stars(r.average(), lang)),
                    Placeholder.unparsed("votes", String.valueOf(r.votes()))));
        }
        Builder b = builders.get(p.getUniqueId());
        Scoring.Result mine = b == null ? null : byPlot.get(b.plot());
        String winner = names(ranked.get(0).plot());
        if (mine != null && rateOrder.contains(b.plot()) && !b.gone()) {
            p.sendMessage(Texts.msg(lang, "award.mine", Placeholder.unparsed("pos", String.valueOf(mine.place())),
                    Placeholder.unparsed("of", String.valueOf(ranked.size())),
                    Placeholder.unparsed("stars", Placement.stars(mine.average(), lang)),
                    Placeholder.unparsed("fives", String.valueOf(mine.fives()))));
            if (Placement.won(mine, rateOrder.size())) {
                Ui.title(p, Texts.text(lang, "award.won.title"), Texts.text(lang, "award.won.sub",
                        Placeholder.unparsed("stars", Placement.stars(mine.average(), lang))), 4000);
                Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1f);
                letter(p, mine);
                return;
            }
        }
        Ui.title(p, Texts.text(lang, "award.title", Placeholder.unparsed("name", winner)),
                Texts.text(lang, "award.sub", Placeholder.unparsed("theme", theme.name(lang))), 4000);
        Ui.sound(p, "minecraft:entity.player.levelup", 1f);
    }

    /** Meisterbrief: ein Buch, das sich öffnet und in der Schnellleiste bleibt, bis es zurück in die Bauhütte geht. */
    private void letter(Player p, Scoring.Result r) {
        String lang = lang(p);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(BookMeta.class, m -> {
            m.setTitle(Texts.plain(lang, "letter.title"));
            m.setAuthor(Texts.plain(lang, "letter.author"));
            m.addPages(Texts.text(lang, "letter.page", Placeholder.unparsed("name", names(r.plot())),
                    Placeholder.unparsed("theme", theme.name(lang)),
                    Placeholder.unparsed("stars", Placement.stars(r.average(), lang)),
                    Placeholder.unparsed("votes", String.valueOf(r.votes())),
                    Placeholder.unparsed("date", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter
                            .ofPattern("en".equals(lang) ? "yyyy-MM-dd" : "dd.MM.yyyy")))));
        });
        p.getInventory().setItem(4, book);
        p.getInventory().setHeldItemSlot(4);
        p.openBook(book);
        p.sendMessage(Texts.msg(lang, "letter.chat"));
    }

    private void fireworks() {
        Location base = new Location(world(), PlotGrid.arenaX(arena) + 0.5, PlotGrid.FLOOR_Y + 5, PlotGrid.PODIUM_Z + 0.5);
        Color[] colours = {Color.fromRGB(0x2BB673), Color.fromRGB(0xF2B632), Color.fromRGB(0x7EC8F2), Color.WHITE};
        spawningOwn = true;
        try {
            for (int i = 0; i < 2; i++) {
                Location at = base.clone().add(random.nextInt(9) - 4, 0, random.nextInt(3) - 1);
                Firework fw = world().spawn(at, Firework.class, f -> {
                    var meta = f.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(colours[random.nextInt(colours.length)], colours[random.nextInt(colours.length)])
                            .withFade(Color.WHITE).trail(true).build());
                    meta.setPower(1);
                    f.setFireworkMeta(meta);
                    f.setPersistent(false);
                });
                fw.setTicksToDetonate(20 + random.nextInt(15));
            }
        } finally {
            spawningOwn = false;
        }
    }

    private void sendStats(Builder b, PlayerData.Round round) {
        if (!statsSent.add(b.uuid())) {
            return;
        }
        PlayerData d = plugin.store().loaded(b.uuid()).orElseGet(() -> plugin.store().get(b.uuid(), b.name()));
        d.add(round);
        plugin.store().save(d);
        plugin.board().update(d);
        Map<String, Long> add = new LinkedHashMap<>();
        add.put("games", 1L);
        add.put("wins", round.won() ? 1L : 0L);
        add.put("podiums", round.podium() ? 1L : 0L);
        add.put("points", (long) round.points());
        add.put("votes_given", (long) round.votesGiven());
        add.put("masterworks", (long) round.masterworks());
        add.put("blocks", (long) round.blocks());
        add.put("games_" + variant.id(), 1L);
        Map<String, Long> best = new LinkedHashMap<>();
        if (round.score() > 0) {
            best.put("best_score", (long) round.score());
        }
        best.put("best_points", (long) round.points());
        plugin.net().stats(b.uuid(), add, best);
    }

    /** Runde zu Ende: alle zurück in die Bauhütte, Parzellen räumen. */
    private void finish(boolean normal) {
        phase = Phase.OVER;
        for (UUID u : comeback.clear()) {
            plugin.net().holdSlot(u, 0);
        }
        List<Player> back = everyone();
        for (Builder b : builders.values()) {
            Player p = Bukkit.getPlayer(b.uuid());
            if (p != null) {
                plugin.net().playing(p, false);
                bar.hide(p);
            }
            plugin.store().unpin(b.uuid(), p != null);
        }
        for (Player p : back) {
            bar.hide(p);
            p.resetPlayerTime();
            p.resetPlayerWeather();
            plugin.hall().enter(p);
            if (!normal) {
                p.sendMessage(Texts.msg(lang(p), "game.ended"));
            }
        }
        spectators.clear();
        plugin.saveBoard();
        List<Box> dirty = new ArrayList<>();
        for (PlotState s : plots.values()) {
            if (s.dirty() != null) {
                dirty.add(s.dirty().union(new Box(s.box().minX(), PlotGrid.FLOOR_Y, s.box().minZ(), s.box().maxX(),
                        PlotGrid.FLOOR_Y, s.box().maxZ())));
            }
        }
        plugin.construction().restore(arena, dirty, () -> plugin.boards().refreshHall());
    }

    /** Verwaltung: abbrechen ohne Wertung. */
    public void close() {
        if (phase != Phase.OVER) {
            finish(false);
        }
    }

    /** Verwaltung: vorspulen (Sekunden der aktuellen Phase; ≤ 0 = sofort weiter). */
    public void skip(int seconds) {
        secondsLeft = Math.max(1, seconds);
        if (seconds <= 0) {
            secondsLeft = 1;
        }
    }

    /** Verwaltung: Thema festlegen (in der Themen-Wahl beendet das die Wahl). */
    public void forceTheme(Theme t) {
        if (phase == Phase.THEME) {
            theme = t;
            for (Player p : everyone()) {
                p.closeInventory();
            }
            startBuild();
        } else if (phase == Phase.BUILD) {
            theme = t;
        }
    }

    // ---------------------------------------------------------------- Kommen und Gehen

    /** Verbindung weg: Platz freihalten (oder raus). */
    public void quit(Player p) {
        UUID u = p.getUniqueId();
        bar.hide(p);
        if (spectators.remove(u)) {
            return;
        }
        Builder b = builders.get(u);
        if (b == null || b.gone()) {
            return;
        }
        b.lastSeen(p.getLocation());
        if (Comeback.holds(plugin.settings().comebackSeconds(), phase != Phase.AWARD && phase != Phase.OVER)) {
            b.away(true);
            comeback.away(u, Bukkit.getCurrentTick(), plugin.settings().comebackSeconds());
            plugin.net().holdSlot(u, plugin.settings().comebackSeconds());
            broadcast("game.away", Placeholder.unparsed("name", b.name()),
                    Placeholder.unparsed("secs", String.valueOf(plugin.settings().comebackSeconds())));
        } else if (phase != Phase.AWARD) {
            b.left(true);
            broadcast("game.left", Placeholder.unparsed("name", b.name()));
        }
    }

    /** Wieder da (Verbindung neu). */
    public void rejoin(Player p) {
        UUID u = p.getUniqueId();
        Builder b = builders.get(u);
        String lang = lang(p);
        if (b == null) {
            return;
        }
        boolean inTime = comeback.back(u, Bukkit.getCurrentTick());
        b.away(false);
        if (!inTime) {
            b.late(true);
            plugin.hall().enter(p);
            p.sendMessage(Texts.msg(lang, "game.too-late.self"));
            return;
        }
        plugin.net().holdSlot(u, 0);
        plugin.net().playing(p, true);
        plugin.store().pin(u);
        prepare(p, b);
        if (phase == Phase.THEME) {
            themeItems(p);
        } else if (phase == Phase.RATE && shown() != null) {
            showPlot(p, shown());
        } else if (phase == Phase.AWARD) {
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(audience());
        }
        bar.show(p, lang);
        p.sendMessage(Texts.msg(lang, "game.welcome-back", Placeholder.unparsed("theme", theme == null ? "?" : theme.name(lang))));
        for (Player o : everyone()) {
            if (o != p) {
                o.sendMessage(Texts.msg(lang(o), "game.back", Placeholder.unparsed("name", b.name())));
            }
        }
        refreshBar();
    }

    /** Selbst gehen (/verlassen): raus aus der Runde (Zuschauen: nur aufhören). */
    public void leave(Player p) {
        UUID u = p.getUniqueId();
        bar.hide(p);
        p.resetPlayerTime();
        p.resetPlayerWeather();
        if (spectators.remove(u)) {
            return;
        }
        Builder b = builders.get(u);
        if (b == null || b.gone()) {
            return;
        }
        b.left(true);
        plugin.net().playing(p, false);
        broadcast("game.left", Placeholder.unparsed("name", b.name()));
    }

    /** Aus der Bauhütte zuschauen. */
    public void spectate(Player p) {
        spectators.add(p.getUniqueId());
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        PlotState s = shown();
        if (phase == Phase.AWARD) {
            p.teleport(audience());
        } else if (s != null) {
            p.teleport(view(s));
            applySky(p, s);
        } else {
            p.teleport(new Location(world(), PlotGrid.arenaX(arena) + 0.5, PlotGrid.FLOOR_Y + 30, 0.5, 0f, 60f));
        }
        bar.show(p, lang(p));
        p.sendMessage(Texts.msg(lang(p), "spectate.on", Placeholder.unparsed("theme", theme == null ? "…" : theme.name(lang(p)))));
    }

    /**
     * Wohin jemand zurück muss, der zu weit weg ist (null = alles in Ordnung): Bauleute bleiben bei ihrer Parzelle,
     * beim Bewerten bei der gezeigten, Zuschauer auf dem Bauplatz.
     */
    public Location contain(Player p, Location to) {
        int x = to.getBlockX();
        int y = to.getBlockY();
        int z = to.getBlockZ();
        if (spectators.contains(p.getUniqueId())) {
            Box a = PlotGrid.arena(arena).grow(16, 16, 16);
            return a.contains(x, y, z) ? null : new Location(world(), PlotGrid.arenaX(arena) + 0.5, PlotGrid.FLOOR_Y + 30,
                    0.5, to.getYaw(), to.getPitch());
        }
        Builder b = builders.get(p.getUniqueId());
        if (b == null || !b.active()) {
            return null;
        }
        switch (phase) {
            case THEME, BUILD -> {
                PlotState s = plots.get(b.plot());
                if (!PlotGrid.roam(s.box()).contains(x, y, z)) {
                    Location c = center(s);
                    c.setYaw(to.getYaw());
                    c.setPitch(to.getPitch());
                    p.sendActionBar(Texts.text(lang(p), "guard.stay"));
                    return c;
                }
            }
            case RATE -> {
                PlotState s = shown();
                if (s != null && !PlotGrid.roam(s.box()).grow(4, 4, 4).contains(x, y, z)) {
                    return view(s);
                }
            }
            case AWARD -> {
                if (!PlotGrid.arena(arena).contains(x, y, z)) {
                    return audience();
                }
            }
            default -> {
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Bau-Hilfen

    /** Uhrzeit der eigenen Parzelle. */
    public void setTime(Player p, int time) {
        PlotState s = buildPlot(p.getUniqueId());
        if (s == null) {
            return;
        }
        s.time(time);
        skyForMembers(s);
        p.sendMessage(Texts.msg(lang(p), "tools.time.set", Placeholder.unparsed("time",
                Texts.plain(lang(p), "tools.time." + PlotState.TIME_IDS[s.time()]))));
    }

    public void setRain(Player p, boolean rain) {
        PlotState s = buildPlot(p.getUniqueId());
        if (s == null) {
            return;
        }
        s.rain(rain);
        skyForMembers(s);
        p.sendMessage(Texts.msg(lang(p), rain ? "tools.weather.rain.set" : "tools.weather.sun.set"));
    }

    private void skyForMembers(PlotState s) {
        for (UUID u : s.members()) {
            Player q = Bukkit.getPlayer(u);
            if (q != null) {
                applySky(q, s);
            }
        }
    }

    // ---------------------------------------------------------------- Anzeige

    private void refreshBar() {
        float progress = phaseSeconds <= 0 ? 1f : (float) secondsLeft / phaseSeconds;
        Function<String, Component> name = switch (phase) {
            case THEME -> l -> Texts.text(l, "bar.theme", Placeholder.unparsed("secs", String.valueOf(secondsLeft)));
            case BUILD -> l -> Texts.text(l, "bar.build", Placeholder.unparsed("theme", theme.name(l)),
                    Placeholder.unparsed("time", Ui.clock(secondsLeft)));
            case RATE -> l -> Texts.text(l, "bar.rate", Placeholder.unparsed("pos", String.valueOf(rateIndex + 1)),
                    Placeholder.unparsed("of", String.valueOf(rateOrder.size())),
                    Placeholder.unparsed("secs", String.valueOf(secondsLeft)));
            default -> l -> Texts.text(l, "bar.award", Placeholder.unparsed("theme", theme == null ? "" : theme.name(l)));
        };
        BossBar.Color colour = phase == Phase.BUILD && secondsLeft <= 30 ? BossBar.Color.RED
                : phase == Phase.RATE ? BossBar.Color.YELLOW : BossBar.Color.GREEN;
        bar.update(name, progress, colour);
        for (Player p : everyone()) {
            bar.show(p, lang(p));
        }
    }

    private void sidebars() {
        for (Player p : everyone()) {
            String lang = lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.theme", Placeholder.unparsed("theme",
                    theme == null ? Texts.plain(lang, "side.theme.open") : theme.name(lang))));
            lines.add(Texts.text(lang, "side.phase." + phase.name().toLowerCase(java.util.Locale.ROOT)));
            lines.add(Texts.text(lang, "side.time", Placeholder.unparsed("time", Ui.clock(secondsLeft))));
            Builder b = builders.get(p.getUniqueId());
            if (b != null) {
                lines.add(Texts.text(lang, "side.plot", Placeholder.unparsed("n", String.valueOf(b.plot() + 1))));
                lines.add(Texts.text(lang, "side.blocks", Placeholder.unparsed("n", String.valueOf(b.blocks()))));
            }
            if (phase == Phase.RATE) {
                lines.add(Texts.text(lang, "side.rated", Placeholder.unparsed("n", String.valueOf(scoring.given(p.getUniqueId())))));
            }
            lines.add(Texts.text(lang, "side.players", Placeholder.unparsed("n", String.valueOf(activeCount()))));
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.footer"));
            plugin.boards().sidebar().show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    // ---------------------------------------------------------------- Werte für Tests

    /** Zustand in einer Zeile für /baumeisteradmin status. */
    public String status() {
        StringBuilder s = new StringBuilder("bauplatz=").append(arena + 1).append(" phase=").append(phase)
                .append(" art=").append(variant.id()).append(" zeit=").append(secondsLeft)
                .append(" thema=").append(theme == null ? "-" : theme.id());
        if (themeVote != null) {
            List<String> o = new ArrayList<>();
            for (int i = 0; i < themeVote.options().size(); i++) {
                o.add(themeVote.options().get(i).id() + ":" + themeVote.count(i));
            }
            s.append(" vorschlaege=").append(String.join(",", o));
        }
        PlotState shown = shown();
        s.append(" gezeigt=").append(shown == null ? -1 : shown.index()).append(" reihe=")
                .append(rateOrder.isEmpty() ? "-" : String.join(",", rateOrder.stream().map(String::valueOf).toList()));
        for (PlotState p : plots.values()) {
            Box d = p.dirty();
            s.append(" parzelle").append(p.index()).append('=').append(p.box().minX()).append(':').append(p.box().minZ())
                    .append(':').append(p.box().maxX()).append(':').append(p.box().maxZ()).append(':')
                    .append(p.box().maxY()).append(':').append(d == null ? "leer" : "bebaut").append(':')
                    .append(p.liquids()).append(':').append(p.time()).append(':').append(p.rain() ? 1 : 0);
        }
        for (Scoring.Result r : results) {
            s.append(" ergebnis").append(r.plot()).append('=').append(r.place()).append(':').append(r.average())
                    .append(':').append(r.votes());
        }
        for (Builder b : builders.values()) {
            String state = b.left() ? "gegangen" : b.late() ? "zuspaet" : b.away() ? "weg" : "da";
            s.append(' ').append(b.name()).append('=').append(b.plot()).append(':').append(state).append(':')
                    .append(b.blocks()).append(':').append(scoring.given(b.uuid()));
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                s.append(' ').append(p.getName()).append("=zuschauer");
            }
        }
        return s.toString();
    }

    /** Für ein späteres Aufräumen ohne Wertung (Absturz der Runde). */
    public Collection<UUID> members() {
        return builders.keySet();
    }
}
