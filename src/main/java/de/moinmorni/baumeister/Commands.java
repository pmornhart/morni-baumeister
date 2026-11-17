package de.moinmorni.baumeister;

import de.moinmorni.baumeister.data.Leaderboard;
import de.moinmorni.baumeister.data.PlayerData;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.game.ThemeMenu;
import de.moinmorni.baumeister.game.ToolMenus;
import de.moinmorni.baumeister.hall.HallMenus;
import de.moinmorni.baumeister.logic.Theme;
import de.moinmorni.baumeister.logic.Themes;
import de.moinmorni.baumeister.logic.Variant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. Einstellungen gibt es keine als Befehl – die stehen in der config.yml (im Panel des Netzwerks). */
final class Commands implements TabExecutor {

    private static final String ADMIN = "moinmorni.baumeister.admin";
    private final BaumeisterPlugin plugin;

    Commands(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("baumeisteradmin")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        String lang = plugin.lang(p);
        boolean inHall = plugin.hall().inHall(p.getUniqueId());
        Game g = plugin.games().playing(p.getUniqueId());
        switch (name) {
            case "bereit" -> {
                if (inHall) {
                    plugin.hall().toggleReady(p);
                } else {
                    p.sendMessage(Texts.msg(lang, "hall.only"));
                }
            }
            case "spielart" -> {
                Variant v = args.length > 0 ? Variant.byId(args[0]) : null;
                if (!inHall) {
                    p.sendMessage(Texts.msg(lang, "hall.only"));
                } else if (v == null) {
                    HallMenus.vote(plugin, p);
                } else {
                    plugin.hall().vote(p, v);
                }
            }
            case "thema" -> {
                if (g == null || g.phase() != Game.Phase.THEME) {
                    p.sendMessage(Texts.msg(lang, "theme.not-now"));
                } else if (args.length > 0) {
                    g.voteTheme(p, parse(args[0], 0) - 1);
                } else {
                    ThemeMenu.open(plugin, p, g);
                }
            }
            case "werkzeug" -> ToolMenus.main(plugin, p);
            case "bewerten" -> {
                int stars = args.length > 0 ? parse(args[0], 0) : 0;
                if (g == null) {
                    p.sendMessage(Texts.msg(lang, "rate.not-now"));
                } else if (stars < 1 || stars > 5) {
                    p.sendMessage(Texts.msg(lang, "rate.usage"));
                } else {
                    g.rate(p, stars);
                }
            }
            case "zuschauen" -> spectate(p, args);
            case "verlassen" -> {
                Game any = plugin.games().of(p.getUniqueId());
                if (any == null) {
                    p.sendMessage(Texts.msg(lang, "leave.none"));
                } else {
                    any.leave(p);
                    plugin.hall().enter(p);
                    p.sendMessage(Texts.msg(lang, "leave.done"));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spectate(Player p, String[] args) {
        String lang = plugin.lang(p);
        Game g = null;
        if (args.length > 0) {
            int n = parse(args[0], 0);
            if (n > 0) {
                g = plugin.games().on(n - 1);
            } else {
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) {
                    p.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                    return;
                }
                g = plugin.games().of(t.getUniqueId());
            }
        } else if (!plugin.games().all().isEmpty()) {
            g = plugin.games().all().iterator().next();
        }
        if (g == null) {
            p.sendMessage(Texts.msg(lang, "spectate.none"));
            return;
        }
        Game current = plugin.games().of(p.getUniqueId());
        if (current != null && current != g && current.spectating(p.getUniqueId())) {
            current.leave(p);
        }
        plugin.spectate(p, g);
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = lang(sender);
        UUID uuid;
        String who;
        if (args.length > 0) {
            Player online = Bukkit.getPlayerExact(args[0]);
            OfflinePlayer op = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
            if (op == null) {
                sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", args[0])));
                return;
            }
            uuid = op.getUniqueId();
            who = op.getName() == null ? args[0] : op.getName();
        } else if (sender instanceof Player p) {
            uuid = p.getUniqueId();
            who = p.getName();
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        Optional<PlayerData> d = plugin.store().read(uuid);
        if (d.isEmpty()) {
            sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", who)));
            return;
        }
        sender.sendMessage(Texts.msg(lang, "stats.head", Placeholder.unparsed("name", who)));
        HallMenus.statLines(lang, d.get()).forEach(sender::sendMessage);
    }

    private void top(CommandSender sender) {
        String lang = lang(sender);
        sender.sendMessage(Texts.msg(lang, "top.head"));
        List<Leaderboard.Entry> top = plugin.board().top(10);
        if (top.isEmpty()) {
            sender.sendMessage(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(HallMenus.row(lang, i + 1, top.get(i)));
        }
    }

    /** Verwaltung: info, status, start, thema, vorspulen, stopp, aufraeumen, neubauen, neuladen. */
    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "info";
        Game g = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("#")) {
                g = plugin.games().on(parse(args[i].substring(1), 0) - 1);
            }
        }
        if (g == null && !plugin.games().all().isEmpty()) {
            g = plugin.games().all().iterator().next();
        }
        switch (sub) {
            case "info", "status" -> {
                sender.sendMessage(Component.text("Baumeister: bereit=" + plugin.ready() + " netzwerk="
                        + plugin.net().connected() + " bauhuette=" + plugin.hall().waiting().size() + " countdown="
                        + plugin.hall().countdown() + " runden=" + plugin.games().all().size() + " frei="
                        + plugin.games().freeArena() + " aufgeraeumt=" + plugin.construction().lastRestored()
                        + " themen=" + Themes.all().size()));
                for (Game x : plugin.games().all()) {
                    sender.sendMessage(Component.text(x.status()));
                }
            }
            case "start" -> {
                Variant v = args.length > 1 ? Variant.byId(args[1]) : null;
                Theme t = args.length > 2 ? Themes.byId(args[2]) : null;
                Game started = plugin.hall().start(v != null ? v : plugin.hall().nextVariant(), t);
                sender.sendMessage(Component.text(started == null ? "Zu wenige Leute oder kein Bauplatz frei."
                        : "Gestartet: Bauplatz " + (started.arena() + 1) + " (" + started.variant().id() + ")"));
            }
            case "thema" -> {
                Theme t = args.length > 1 ? Themes.byId(args[1]) : null;
                if (g == null || t == null) {
                    sender.sendMessage(Component.text("/baumeisteradmin thema <kennung> [#bauplatz]"));
                } else {
                    g.forceTheme(t);
                    sender.sendMessage(Component.text("Thema: " + t.id()));
                }
            }
            case "vorspulen" -> {
                if (g == null || args.length < 2) {
                    sender.sendMessage(Component.text("/baumeisteradmin vorspulen <weiter|sekunden> [#bauplatz]"));
                } else {
                    int to = switch (args[1].toLowerCase(Locale.ROOT)) {
                        case "weiter", "next", "ende", "end", "bewerten" -> 0;
                        default -> parse(args[1], g.secondsLeft());
                    };
                    g.skip(to);
                    sender.sendMessage(Component.text("Zeit: " + Math.max(1, to)));
                }
            }
            case "stopp" -> {
                if (g != null) {
                    plugin.games().stop(g);
                    sender.sendMessage(Component.text("Runde abgebrochen."));
                }
            }
            case "neubauen" -> plugin.rebuild(() -> sender.sendMessage(Component.text("Neu gebaut.")));
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Component.text("Einstellungen neu geladen."));
            }
            default -> sender.sendMessage(Component.text(
                    "/baumeisteradmin info|status|start [spielart] [thema]|thema|vorspulen|stopp|neubauen|neuladen"));
        }
        return true;
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            switch (name) {
                case "baumeisteradmin" -> out.addAll(List.of("info", "status", "start", "thema", "vorspulen", "stopp",
                        "neubauen", "neuladen"));
                case "spielart" -> Arrays.stream(Variant.values()).forEach(v -> out.add(v.id()));
                case "thema" -> out.addAll(List.of("1", "2", "3"));
                case "bewerten" -> out.addAll(List.of("1", "2", "3", "4", "5"));
                case "zuschauen", "stats" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 2 && name.equals("baumeisteradmin")) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "start" -> Arrays.stream(Variant.values()).forEach(v -> out.add(v.id()));
                case "thema" -> Themes.all().forEach(t -> out.add(t.id()));
                case "vorspulen" -> out.addAll(List.of("weiter", "10", "30"));
                default -> {
                }
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
