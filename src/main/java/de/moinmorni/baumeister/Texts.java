package de.moinmorni.baumeister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <sky>} Himmelblau,
 * {@code <au>} Gold, {@code <em>} Smaragd. Namen von Spielern und Themen kommen immer als {@code Placeholder.unparsed}
 * hinein.
 */
public final class Texts {

    public static final int SKY = 0x7EC8F2;
    public static final int GOLD = 0xF2B632;
    public static final int EMERALD = 0x2BB673;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("sky", Tag.styling(TextColor.color(SKY))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<em>⚒</em> <au>Baumeister</au> <dark_gray>»</dark_gray> ",
                "<em>⚒</em> <au>Master Builder</au> <dark_gray>»</dark_gray> ");
        t("mode", "Baumeister", "Master Builder");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");

        // ---------- Spielarten ----------
        t("variant.klassisch", "Klassisch", "Classic");
        t("variant.klassisch.desc", "<gray>Jeder baut allein auf seiner Parzelle.</gray>\n<gray>Bauzeit: <white><time></white></gray>",
                "<gray>Everyone builds alone on their own plot.</gray>\n<gray>Build time: <white><time></white></gray>");
        t("variant.schnell", "Schnell", "Quick");
        t("variant.schnell.desc", "<gray>Wie klassisch, aber mit wenig Zeit –</gray>\n<gray>schnelle Ideen gewinnen. Bauzeit: <white><time></white></gray>",
                "<gray>Like classic, but with little time –</gray>\n<gray>quick ideas win. Build time: <white><time></white></gray>");
        t("variant.paare", "Zu zweit", "In pairs");
        t("variant.paare.desc", "<gray>Zwei bauen zusammen auf einer Parzelle</gray>\n<gray>(Partys zuerst). Ab 4 Leuten. Bauzeit: <white><time></white></gray>",
                "<gray>Two build together on one plot</gray>\n<gray>(parties first). From 4 players. Build time: <white><time></white></gray>");

        // ---------- Kategorien ----------
        t("category.alpen", "Berge und Almen", "Mountains and pastures");
        t("category.dorf", "Im Dorf", "In the village");
        t("category.alltag", "Alltag", "Everyday life");
        t("category.fantasie", "Fantasie", "Fantasy");
        t("category.natur", "Natur", "Nature");
        t("category.essen", "Essen und Trinken", "Food and drink");
        t("category.fahrzeug", "Fahrzeuge", "Vehicles");
        t("category.tier", "Tiere", "Animals");
        t("category.fest", "Feste", "Celebrations");

        // ---------- Bauhütte ----------
        t("hall.title", "<em>⚒ Baumeister</em>", "<em>⚒ Master Builder</em>");
        t("hall.subtitle", "<gray>Ein Thema, eine Parzelle – die Zeit läuft!</gray>",
                "<gray>One theme, one plot – the clock is ticking!</gray>");
        t("hall.welcome", "<gray>Servus in der <em>Bauhütte</em>! Mit dem <white>Kompass</white> stimmst du über die Spielart ab, mit dem <white>grünen Farbstoff</white> sagst du „bereit“, im <white>Buch</white> steht, wie es geht.</gray>",
                "<gray>Welcome to the <em>Builders' Lodge</em>! Use the <white>compass</white> to vote for a game type, the <white>green dye</white> to say you're ready, and the <white>book</white> explains how to play.</gray>");
        t("hall.how", "<gray>So geht's: Alle bekommen ein <au>Thema</au> und bauen auf ihrer eigenen Parzelle. Wenn die Zeit um ist, schaut ihr euch jeden Bau an und gebt <white>1 bis 5 Sterne</white>. Der beste Bau bekommt den <au>Meisterbrief</au>!</gray>",
                "<gray>How it works: everyone gets a <au>theme</au> and builds on their own plot. When time is up, you visit every build and give <white>1 to 5 stars</white>. The best build wins the <au>master's certificate</au>!</gray>");
        t("hall.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da, ab <white><min></white> geht's von selbst los)</gray>",
                "<gray>Waiting for players … (<white><n></white> here, starts by itself from <white><min></white>)</gray>");
        t("hall.countdown", "<gray>Nächste Runde in <au><secs> s</au> · <sky><variant></sky></gray>",
                "<gray>Next round in <au><secs> s</au> · <sky><variant></sky></gray>");
        t("hall.no-arena", "<gray>Gerade sind alle Bauplätze belegt – gleich wird einer frei.</gray>",
                "<gray>All building sites are busy – one will be free soon.</gray>");
        t("hall.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("hall.ready.alone", "<em>Du bist bereit.</em> <gray>Sobald noch jemand da ist, geht's los.</gray>",
                "<em>You're ready.</em> <gray>As soon as someone else joins, we start.</gray>");
        t("hall.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("hall.voted", "<gray>Deine Stimme: <sky><choice></sky>.</gray>", "<gray>Your vote: <sky><choice></sky>.</gray>");
        t("hall.variant-off", "<gray>Diese Spielart ist gerade abgeschaltet.</gray>",
                "<gray>This game type is switched off right now.</gray>");
        t("hall.starting", "<em>Auf geht's!</em> <gray>Spielart: <sky><variant></sky> · <white><n></white> Leute</gray>",
                "<em>Let's go!</em> <gray>Game type: <sky><variant></sky> · <white><n></white> players</gray>");
        t("hall.full", "<gray>Diese Runde ist voll – du bist in der nächsten dabei. Zuschauen geht mit dem Fernrohr.</gray>",
                "<gray>This round is full – you'll be in the next one. You can watch with the spyglass.</gray>");
        t("hall.only", "<gray>Das geht nur in der Bauhütte.</gray>", "<gray>That only works in the lodge.</gray>");
        t("hall.party-playing", "<gray>Deine Party baut gerade – mit <white>/zuschauen</white> kannst du zuschauen.</gray>",
                "<gray>Your party is building right now – use <white>/spectate</white> to watch.</gray>");

        // ---------- Gegenstände ----------
        t("item.vote", "<sky>Spielart wählen</sky> <gray>(Rechtsklick)</gray>", "<sky>Choose game type</sky> <gray>(right-click)</gray>");
        t("item.vote.lore", "<gray>Klassisch, schnell oder zu zweit?</gray>", "<gray>Classic, quick or in pairs?</gray>");
        t("item.ready", "<em>Bereit</em> <gray>(Rechtsklick)</gray>", "<em>Ready</em> <gray>(right-click)</gray>");
        t("item.ready.lore", "<gray>Sind alle bereit, geht's nach 5 Sekunden los.</gray>",
                "<gray>Once everyone is ready, we start in 5 seconds.</gray>");
        t("item.howto", "<au>So geht's</au> <gray>(Rechtsklick)</gray>", "<au>How to play</au> <gray>(right-click)</gray>");
        t("item.howto.lore", "<gray>Thema, bauen, bewerten, gewinnen.</gray>", "<gray>Theme, build, rate, win.</gray>");
        t("item.stats", "<au>Statistik</au> <gray>(Rechtsklick)</gray>", "<au>Statistics</au> <gray>(right-click)</gray>");
        t("item.stats.lore", "<gray>Deine Werte und die besten Baumeister.</gray>",
                "<gray>Your stats and the best builders.</gray>");
        t("item.watch", "<sky>Zuschauen</sky> <gray>(Rechtsklick)</gray>", "<sky>Watch</sky> <gray>(right-click)</gray>");
        t("item.watch.lore", "<gray>Einer laufenden Runde zuschauen.</gray>", "<gray>Watch a round in progress.</gray>");
        t("item.lobby", "<au>Zur Lobby</au> <gray>(Rechtsklick)</gray>", "<au>To the lobby</au> <gray>(right-click)</gray>");
        t("item.lobby.lore", "<gray>Zurück ins Dorf von MoinMorni.</gray>", "<gray>Back to the MoinMorni village.</gray>");
        t("item.theme", "<au><theme></au> <gray>(Rechtsklick: wählen)</gray>", "<au><theme></au> <gray>(right-click: vote)</gray>");
        t("item.theme.lore", "<gray>Die Mehrheit entscheidet über das Thema.</gray>",
                "<gray>The majority decides the theme.</gray>");
        t("item.tools", "<em>Werkzeugkiste</em> <gray>(Rechtsklick)</gray>", "<em>Toolbox</em> <gray>(right-click)</gray>");
        t("item.tools.lore", "<gray>Boden, Zeit, Wetter, Köpfe, Banner,</gray>\n<gray>Füllen, Ersetzen, Rückgängig.</gray>",
                "<gray>Floor, time, weather, heads, banners,</gray>\n<gray>fill, replace, undo.</gray>");
        t("item.wand", "<au>Zollstock</au> <gray>(Ecken setzen)</gray>", "<au>Folding rule</au> <gray>(set corners)</gray>");
        t("item.wand.lore", "<gray>Linksklick auf einen Block: erste Ecke</gray>\n<gray>Rechtsklick auf einen Block: zweite Ecke</gray>\n<gray>Dann in der Werkzeugkiste füllen oder ersetzen.</gray>",
                "<gray>Left-click a block: first corner</gray>\n<gray>Right-click a block: second corner</gray>\n<gray>Then fill or replace in the toolbox.</gray>");
        t("item.own", "<au>Dein Bau</au>", "<au>Your build</au>");
        t("item.own.lore", "<gray>Den eigenen Bau bewerten die anderen.</gray>", "<gray>The others rate your own build.</gray>");

        // ---------- Themen-Wahl ----------
        t("menu.theme.title", "Welches Thema bauen wir?", "Which theme shall we build?");
        t("menu.theme.option", "<au><theme></au>", "<au><theme></au>");
        t("menu.theme.category", "<gray>Bereich: <white><cat></white></gray>", "<gray>Category: <white><cat></white></gray>");
        t("menu.theme.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.theme.click", "<em>Klick: dafür stimmen</em>", "<em>Click: vote for it</em>");
        t("menu.theme.mine", "<em>✔ Deine Stimme</em>", "<em>✔ Your vote</em>");
        t("theme.voted", "<gray>Deine Stimme: <au><theme></au>.</gray>", "<gray>Your vote: <au><theme></au>.</gray>");
        t("theme.not-now", "<gray>Gerade wird kein Thema gewählt.</gray>", "<gray>No theme is being chosen right now.</gray>");
        t("theme.chosen", "<gray>Das Thema ist <au><theme></au> (Stimmen: <white><votes></white>).</gray>",
                "<gray>The theme is <au><theme></au> (votes: <white><votes></white>).</gray>");

        // ---------- Bauen ----------
        t("game.build.title", "<au><theme></au>", "<au><theme></au>");
        t("game.build.sub", "<gray><time> – los geht's!</gray>", "<gray><time> – off you go!</gray>");
        t("game.build.start", "<gray>Das Thema: <au><theme></au>. Ihr habt <white><time></white>. Viel Spaß beim Bauen!</gray>",
                "<gray>The theme: <au><theme></au>. You have <white><time></white>. Have fun building!</gray>");
        t("game.build.help", "<gray>Bau nur auf deiner Parzelle (Kreativmodus, fliegen geht). Hilfen: <white>Werkzeugkiste</white> (Stern) und <white>Zollstock</white> in der Schnellleiste.</gray>",
                "<gray>Build only on your plot (creative mode, you can fly). Helpers: <white>toolbox</white> (star) and <white>folding rule</white> in your hotbar.</gray>");
        t("game.build.partner", "<gray>Du baust zusammen mit <white><name></white>.</gray>",
                "<gray>You're building together with <white><name></white>.</gray>");
        t("game.build.warn", "<gray>Noch <au><secs> Sekunden</au> Bauzeit!</gray>", "<gray><au><secs> seconds</au> of building left!</gray>");
        t("game.build.count", "<au><secs></au>", "<au><secs></au>");

        // ---------- Bewerten ----------
        t("game.rate.title", "<au>Bewertung</au>", "<au>Rating</au>");
        t("game.rate.sub", "<gray>Gebt jedem Bau 1 bis 5 Sterne (<n> insgesamt)</gray>", "<gray>Give each build 1 to 5 stars (<n> in total)</gray>");
        t("game.rate.start", "<gray>Die Zeit ist um! Jetzt geht's der Reihe nach zu jedem Bau (<white><n></white> insgesamt). Wertung mit der Wolle in der Schnellleiste (Rechtsklick) oder <white>/bewerten 1–5</white>.</gray>",
                "<gray>Time's up! Now we visit every build in turn (<white><n></white> in total). Rate with the wool in your hotbar (right-click) or <white>/rate 1–5</white>.</gray>");
        t("game.rate.nothing", "<gray>Niemand hat etwas gebaut – diesmal gibt es nichts zu bewerten.</gray>",
                "<gray>Nobody built anything – there's nothing to rate this time.</gray>");
        t("game.rate.plot", "<au>Bau <pos> von <of></au>", "<au>Build <pos> of <of></au>");
        t("game.rate.ask", "<gray>Wie gut passt das zu „<theme>“?</gray>", "<gray>How well does it fit “<theme>”?</gray>");
        t("game.rate.own", "<gray>Das ist dein Bau – die anderen bewerten.</gray>", "<gray>This is your build – the others rate it.</gray>");
        t("rate.naja", "Naja", "Meh");
        t("rate.geht-so", "Geht so", "Okay");
        t("rate.gut", "Gut", "Good");
        t("rate.super", "Super", "Great");
        t("rate.meisterwerk", "Meisterwerk", "Masterpiece");
        t("rate.naja.item", "<red>★ Naja</red> <gray>(1)</gray>", "<red>★ Meh</red> <gray>(1)</gray>");
        t("rate.geht-so.item", "<gold>★★ Geht so</gold> <gray>(2)</gray>", "<gold>★★ Okay</gold> <gray>(2)</gray>");
        t("rate.gut.item", "<yellow>★★★ Gut</yellow> <gray>(3)</gray>", "<yellow>★★★ Good</yellow> <gray>(3)</gray>");
        t("rate.super.item", "<green>★★★★ Super</green> <gray>(4)</gray>", "<green>★★★★ Great</green> <gray>(4)</gray>");
        t("rate.meisterwerk.item", "<sky>★★★★★ Meisterwerk</sky> <gray>(5)</gray>", "<sky>★★★★★ Masterpiece</sky> <gray>(5)</gray>");
        t("rate.item.lore", "<gray>Rechtsklick: so bewerten (umentscheiden geht).</gray>",
                "<gray>Right-click: rate like this (you can change it).</gray>");
        t("rate.given", "<gray>Deine Wertung: <au><stars></au> <white><name></white></gray>",
                "<gray>Your rating: <au><stars></au> <white><name></white></gray>");
        t("rate.not-now", "<gray>Gerade gibt es nichts zu bewerten.</gray>", "<gray>There's nothing to rate right now.</gray>");
        t("rate.own", "<gray>Den eigenen Bau bewerten die anderen.</gray>", "<gray>The others rate your own build.</gray>");
        t("rate.usage", "<gray>So geht's: <white>/bewerten 1</white> (Naja) bis <white>/bewerten 5</white> (Meisterwerk).</gray>",
                "<gray>Usage: <white>/rate 1</white> (meh) to <white>/rate 5</white> (masterpiece).</gray>");

        // ---------- Siegerehrung ----------
        t("award.head", "<au>Siegerehrung</au> <gray>– Thema „<theme>“:</gray>", "<au>Award ceremony</au> <gray>– theme “<theme>”:</gray>");
        t("award.row", "<au><pos>.</au> <white><name></white> <gray>– <au><stars> ★</au> (Stimmen: <votes>)</gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <au><stars> ★</au> (votes: <votes>)</gray>");
        t("award.mine", "<gray>Dein Bau: Platz <au><pos></au> von <white><of></white> mit <au><stars> ★</au> (<white><fives></white>× Meisterwerk).</gray>",
                "<gray>Your build: place <au><pos></au> of <white><of></white> with <au><stars> ★</au> (<white><fives></white>× masterpiece).</gray>");
        t("award.none", "<gray>Diesmal gibt es keinen Sieger – es wurde nichts bewertet.</gray>",
                "<gray>No winner this time – nothing was rated.</gray>");
        t("award.none.title", "<gray>Kein Sieger</gray>", "<gray>No winner</gray>");
        t("award.won.title", "<au>Meisterwerk!</au>", "<au>Masterpiece!</au>");
        t("award.won.sub", "<gray>Du bist Baumeister – <au><stars> ★</au></gray>", "<gray>You're the master builder – <au><stars> ★</au></gray>");
        t("award.title", "<au><name></au>", "<au><name></au>");
        t("award.sub", "<gray>baut das schönste „<theme>“</gray>", "<gray>built the best “<theme>”</gray>");
        t("letter.title", "Meisterbrief", "Master's certificate");
        t("letter.author", "Bauhütte MoinMorni", "MoinMorni Builders' Lodge");
        t("letter.page", "<dark_green><b>Meisterbrief</b></dark_green>\n\n<black>Hiermit wird</black>\n<dark_blue><b><name></b></dark_blue>\n<black>zum Baumeister ernannt.</black>\n\n<gray>Thema:</gray> <black><theme></black>\n<gray>Wertung:</gray> <black><stars> ★ (Stimmen: <votes>)</black>\n\n<gray><date> · Bauhütte MoinMorni</gray>",
                "<dark_green><b>Master's Certificate</b></dark_green>\n\n<black>This certifies that</black>\n<dark_blue><b><name></b></dark_blue>\n<black>is a master builder.</black>\n\n<gray>Theme:</gray> <black><theme></black>\n<gray>Rating:</gray> <black><stars> ★ (votes: <votes>)</black>\n\n<gray><date> · MoinMorni Builders' Lodge</gray>");
        t("letter.chat", "<au>Dein Meisterbrief</au> <gray>liegt in der Schnellleiste – Rechtsklick zum Lesen.</gray>",
                "<au>Your master's certificate</au> <gray>is in your hotbar – right-click to read it.</gray>");

        // ---------- Kommen und Gehen ----------
        t("game.away", "<gray><white><name></white> hat die Verbindung verloren – die Parzelle bleibt <white><secs> s</white> frei.</gray>",
                "<gray><white><name></white> lost connection – their plot is kept for <white><secs> s</white>.</gray>");
        t("game.back", "<gray><white><name></white> ist wieder da!</gray>", "<gray><white><name></white> is back!</gray>");
        t("game.welcome-back", "<em>Willkommen zurück!</em> <gray>Thema: <au><theme></au> – weiter geht's.</gray>",
                "<em>Welcome back!</em> <gray>Theme: <au><theme></au> – carry on.</gray>");
        t("game.too-late", "<gray><white><name></white> ist nicht rechtzeitig zurückgekommen.</gray>",
                "<gray><white><name></white> didn't come back in time.</gray>");
        t("game.too-late.self", "<gray>Leider zu spät – die Runde ging ohne dich weiter. In der nächsten bist du wieder dabei!</gray>",
                "<gray>Sorry, too late – the round went on without you. You'll be in the next one!</gray>");
        t("game.left", "<gray><white><name></white> hat die Runde verlassen.</gray>", "<gray><white><name></white> left the round.</gray>");
        t("game.ended", "<gray>Die Runde wurde beendet.</gray>", "<gray>The round has ended.</gray>");
        t("leave.none", "<gray>Du bist in keiner Runde.</gray>", "<gray>You're not in a round.</gray>");
        t("leave.done", "<gray>Zurück in der Bauhütte.</gray>", "<gray>Back in the lodge.</gray>");
        t("spectate.none", "<gray>Gerade läuft keine Runde.</gray>", "<gray>No round is running right now.</gray>");
        t("spectate.playing", "<gray>Du baust gerade selbst mit.</gray>", "<gray>You're building yourself right now.</gray>");
        t("spectate.on", "<gray>Du schaust zu (Thema: <au><theme></au>). Zurück mit <white>/verlassen</white>.</gray>",
                "<gray>You're watching (theme: <au><theme></au>). Back with <white>/leave</white>.</gray>");

        // ---------- Werkzeugkiste ----------
        t("menu.tools.title", "Werkzeugkiste", "Toolbox");
        t("menu.back", "<gray>« Zurück</gray>", "<gray>« Back</gray>");
        t("menu.close", "<gray>Schließen</gray>", "<gray>Close</gray>");
        t("tools.floor", "<em>Boden wechseln</em>", "<em>Change floor</em>");
        t("tools.floor.lore", "<gray>Der ganze Boden deiner Parzelle</gray>\n<gray>in einem anderen Block.</gray>",
                "<gray>Your whole plot floor</gray>\n<gray>in a different block.</gray>");
        t("tools.time", "<em>Uhrzeit</em>", "<em>Time of day</em>");
        t("tools.time.lore", "<gray>Morgen, Mittag, Abend oder Nacht –</gray>\n<gray>so sehen es auch die anderen beim Bewerten.</gray>",
                "<gray>Morning, noon, evening or night –</gray>\n<gray>the others see it like this when rating.</gray>");
        t("tools.weather.sun", "<em>Wetter: Sonne</em>", "<em>Weather: sunny</em>");
        t("tools.weather.sun.lore", "<gray>Klick: Regen über deiner Parzelle.</gray>", "<gray>Click: rain over your plot.</gray>");
        t("tools.weather.rain", "<em>Wetter: Regen</em>", "<em>Weather: rain</em>");
        t("tools.weather.rain.lore", "<gray>Klick: wieder Sonne.</gray>", "<gray>Click: sunshine again.</gray>");
        t("tools.heads", "<em>Köpfe</em>", "<em>Heads</em>");
        t("tools.heads.lore", "<gray>Köpfe von Wesen und von allen,</gray>\n<gray>die in dieser Runde bauen.</gray>",
                "<gray>Mob heads and heads of everyone</gray>\n<gray>building in this round.</gray>");
        t("tools.banners", "<em>Banner</em>", "<em>Banners</em>");
        t("tools.banners.lore", "<gray>Fertige Banner mit eigenen Mustern.</gray>", "<gray>Ready-made banners with our patterns.</gray>");
        t("tools.fill", "<au>Füllen</au>", "<au>Fill</au>");
        t("tools.fill.lore", "<gray>Den Bereich des Zollstocks mit einem</gray>\n<gray>Block aus deiner Schnellleiste füllen.</gray>",
                "<gray>Fill the folding-rule area with a</gray>\n<gray>block from your hotbar.</gray>");
        t("tools.replace", "<au>Ersetzen</au>", "<au>Replace</au>");
        t("tools.replace.lore", "<gray>Im Bereich einen Block durch einen</gray>\n<gray>anderen ersetzen (oder durch Luft).</gray>",
                "<gray>Replace one block with another</gray>\n<gray>in the area (or with air).</gray>");
        t("tools.clear", "<au>Leeren</au>", "<au>Clear</au>");
        t("tools.clear.lore", "<gray>Alles im Bereich wegnehmen.</gray>", "<gray>Remove everything in the area.</gray>");
        t("tools.undo", "<au>Rückgängig</au>", "<au>Undo</au>");
        t("tools.undo.lore", "<gray>Den letzten Werkzeug-Schritt zurücknehmen.</gray>", "<gray>Undo the last tool step.</gray>");
        t("tools.wand", "<au>Zollstock</au>", "<au>Folding rule</au>");
        t("tools.wand.lore", "<gray>Zollstock in die Schnellleiste</gray>\n<gray>(falls er fehlt) und kurz erklärt.</gray>",
                "<gray>Put the folding rule in your hotbar</gray>\n<gray>(if missing) with a short explanation.</gray>");
        t("tools.forget", "<gray>Bereich vergessen</gray>", "<gray>Forget area</gray>");
        t("tools.forget.lore", "<gray>Beide Ecken löschen.</gray>", "<gray>Remove both corners.</gray>");
        t("tools.home", "<sky>Zur Mitte</sky>", "<sky>To the middle</sky>");
        t("tools.home.lore", "<gray>Zurück in die Mitte deiner Parzelle.</gray>", "<gray>Back to the middle of your plot.</gray>");
        t("tools.time.morgen", "Morgen", "Morning");
        t("tools.time.mittag", "Mittag", "Noon");
        t("tools.time.abend", "Abend", "Evening");
        t("tools.time.nacht", "Nacht", "Night");
        t("tools.time.set", "<gray>Uhrzeit deiner Parzelle: <white><time></white>.</gray>",
                "<gray>Time of day on your plot: <white><time></white>.</gray>");
        t("tools.weather.sun.set", "<gray>Über deiner Parzelle scheint die Sonne.</gray>", "<gray>The sun shines over your plot.</gray>");
        t("tools.weather.rain.set", "<gray>Über deiner Parzelle regnet es.</gray>", "<gray>It's raining over your plot.</gray>");
        t("tools.not-now", "<gray>Die Werkzeugkiste gibt es nur während der Bauzeit.</gray>",
                "<gray>The toolbox is only available while building.</gray>");
        t("tools.wait", "<gray>Kurz verschnaufen – noch <white><secs> s</white>.</gray>",
                "<gray>Take a breather – <white><secs> s</white> left.</gray>");
        t("tools.floor.done", "<gray>Neuer Boden liegt.</gray>", "<gray>New floor laid.</gray>");
        t("tools.fill.done", "<gray>Gefüllt: <white><n></white> Blöcke.</gray>", "<gray>Filled: <white><n></white> blocks.</gray>");
        t("tools.replace.done", "<gray>Ersetzt: <white><n></white> Blöcke.</gray>", "<gray>Replaced: <white><n></white> blocks.</gray>");
        t("tools.clear.done", "<gray>Geleert: <white><n></white> Blöcke.</gray>", "<gray>Cleared: <white><n></white> blocks.</gray>");
        t("tools.undo.done", "<gray>Zurückgenommen: <white><n></white> Blöcke.</gray>", "<gray>Undone: <white><n></white> blocks.</gray>");
        t("tools.undo.none", "<gray>Es gibt nichts zurückzunehmen.</gray>", "<gray>There's nothing to undo.</gray>");
        t("tools.forget.done", "<gray>Bereich vergessen.</gray>", "<gray>Area forgotten.</gray>");
        t("tools.region.missing", "<gray>Setz zuerst zwei Ecken mit dem <au>Zollstock</au> (Links- und Rechtsklick auf Blöcke).</gray>",
                "<gray>First set two corners with the <au>folding rule</au> (left- and right-click on blocks).</gray>");
        t("tools.region.outside", "<gray>Das liegt nicht in deiner Parzelle.</gray>", "<gray>That's not on your plot.</gray>");
        t("tools.region.too-big", "<gray>Der Bereich ist zu groß (<white><n></white> Blöcke, höchstens <white><max></white>) – mach ihn kleiner.</gray>",
                "<gray>The area is too big (<white><n></white> blocks, at most <white><max></white>) – make it smaller.</gray>");
        t("tools.corner.first", "<au>Erste Ecke</au> <gray><pos> · Bereich: <white><n></white> Blöcke</gray>",
                "<au>First corner</au> <gray><pos> · area: <white><n></white> blocks</gray>");
        t("tools.corner.second", "<au>Zweite Ecke</au> <gray><pos> · Bereich: <white><n></white> Blöcke</gray>",
                "<au>Second corner</au> <gray><pos> · area: <white><n></white> blocks</gray>");
        t("tools.wand.how", "<gray>Zollstock: <white>Linksklick</white> auf einen Block = erste Ecke, <white>Rechtsklick</white> = zweite Ecke. Dann in der Werkzeugkiste füllen, ersetzen oder leeren.</gray>",
                "<gray>Folding rule: <white>left-click</white> a block = first corner, <white>right-click</white> = second corner. Then fill, replace or clear in the toolbox.</gray>");
        t("menu.floor.title", "Boden wählen", "Choose floor");
        t("menu.floor.lore", "<gray>Klick: ganzer Boden in diesem Block</gray>", "<gray>Click: whole floor in this block</gray>");
        t("menu.time.title", "Uhrzeit wählen", "Choose time of day");
        t("menu.heads.title", "Köpfe", "Heads");
        t("menu.banners.title", "Banner", "Banners");
        t("menu.pick.fill", "Füllen mit …", "Fill with …");
        t("menu.pick.from", "Was ersetzen?", "Replace what?");
        t("menu.pick.to", "Womit ersetzen?", "Replace with?");
        t("menu.pick.lore", "<gray>Klick: diesen Block nehmen</gray>", "<gray>Click: use this block</gray>");
        t("menu.pick.air", "<gray>Luft (wegnehmen)</gray>", "<gray>Air (remove)</gray>");
        t("menu.pick.none", "<gray>Keine Blöcke in der Schnellleiste</gray>", "<gray>No blocks in your hotbar</gray>");
        t("menu.pick.none.lore", "<gray>Leg zuerst Blöcke in die Schnellleiste.</gray>", "<gray>Put some blocks in your hotbar first.</gray>");
        t("banner.weissblau", "<sky>Weiß-blaue Rauten</sky>", "<sky>White-blue lozenges</sky>");
        t("banner.smaragd", "<em>Smaragd</em>", "<em>Emerald</em>");
        t("banner.gold", "<au>Gold</au>", "<au>Gold</au>");
        t("banner.rotweiss", "<red>Rot-Weiß</red>", "<red>Red and white</red>");
        t("banner.gipfel", "<white>Gipfel</white>", "<white>Summit</white>");
        t("banner.tanne", "<green>Tanne</green>", "<green>Fir tree</green>");
        t("banner.sonne", "<yellow>Sonne</yellow>", "<yellow>Sun</yellow>");
        t("banner.ziegel", "<red>Ziegel</red>", "<red>Bricks</red>");
        t("banner.nacht", "<blue>Sternennacht</blue>", "<blue>Starry night</blue>");

        // ---------- Schutz ----------
        t("guard.outside", "<gray>Gebaut wird nur auf deiner eigenen Parzelle.</gray>", "<gray>You can only build on your own plot.</gray>");
        t("guard.stay", "<gray>Bleib bei deiner Parzelle.</gray>", "<gray>Stay at your plot.</gray>");
        t("guard.banned", "<gray>Diesen Block gibt es beim Baumeister nicht.</gray>", "<gray>This block isn't allowed in Master Builder.</gray>");
        t("guard.liquids", "<gray>Mehr als <white><max></white> Wasser- oder Lavaquellen gehen auf einer Parzelle nicht.</gray>",
                "<gray>No more than <white><max></white> water or lava sources per plot.</gray>");
        t("guard.entities", "<gray>Mehr als <white><max></white> Tiere und Dinge gehen auf einer Parzelle nicht.</gray>",
                "<gray>No more than <white><max></white> mobs and things per plot.</gray>");
        t("guard.redstone", "<gray>Eine Redstone-Schaltung lief ohne Pause und bremste den Server – das Bauteil bei <white><pos></white> ist weg.</gray>",
                "<gray>A redstone circuit ran non-stop and slowed the server down – the part at <white><pos></white> was removed.</gray>");
        t("guard.word", "<gray>Dieser Text geht so nicht – bitte freundlich bleiben.</gray>",
                "<gray>That text isn't okay – please keep it friendly.</gray>");
        t("guard.link", "<gray>Links und Adressen gehen hier nicht.</gray>", "<gray>Links and addresses aren't allowed here.</gray>");

        // ---------- Leisten ----------
        t("bar.theme", "<au>Wählt ein Thema</au> <gray>– noch <white><secs> s</white></gray>",
                "<au>Choose a theme</au> <gray>– <white><secs> s</white> left</gray>");
        t("bar.build", "<au><theme></au> <gray>· noch <white><time></white></gray>", "<au><theme></au> <gray>· <white><time></white> left</gray>");
        t("bar.rate", "<au>Bewertung</au> <gray>· Bau <white><pos></white> von <white><of></white> · <white><secs> s</white></gray>",
                "<au>Rating</au> <gray>· build <white><pos></white> of <white><of></white> · <white><secs> s</white></gray>");
        t("bar.award", "<au>Siegerehrung</au> <gray>· <theme></gray>", "<au>Award ceremony</au> <gray>· <theme></gray>");
        t("side.title", "<em><b>⚒ Baumeister</b></em>", "<em><b>⚒ Master Builder</b></em>");
        t("side.theme", "<gray>Thema: <au><theme></au></gray>", "<gray>Theme: <au><theme></au></gray>");
        t("side.theme.open", "wird gewählt", "being chosen");
        t("side.phase.theme", "<sky>Themen-Wahl</sky>", "<sky>Choosing a theme</sky>");
        t("side.phase.build", "<em>Bauzeit</em>", "<em>Building</em>");
        t("side.phase.rate", "<au>Bewertung</au>", "<au>Rating</au>");
        t("side.phase.award", "<au>Siegerehrung</au>", "<au>Award ceremony</au>");
        t("side.phase.over", "<gray>Vorbei</gray>", "<gray>Over</gray>");
        t("side.time", "<gray>Zeit: <white><time></white></gray>", "<gray>Time: <white><time></white></gray>");
        t("side.plot", "<gray>Parzelle: <white><n></white></gray>", "<gray>Plot: <white><n></white></gray>");
        t("side.blocks", "<gray>Gesetzt: <white><n></white></gray>", "<gray>Placed: <white><n></white></gray>");
        t("side.rated", "<gray>Bewertet: <white><n></white></gray>", "<gray>Rated: <white><n></white></gray>");
        t("side.players", "<gray>Bauleute: <white><n></white></gray>", "<gray>Builders: <white><n></white></gray>");
        t("side.footer", "<sky>moinmorni.net</sky>", "<sky>moinmorni.net</sky>");
        t("side.hall.players", "<gray>In der Bauhütte: <white><n></white></gray>", "<gray>In the lodge: <white><n></white></gray>");
        t("side.hall.start", "<gray>Start in <au><secs> s</au></gray>", "<gray>Start in <au><secs> s</au></gray>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler</gray>", "<gray>Waiting for players</gray>");
        t("side.hall.variant", "<gray>Spielart: <sky><variant></sky></gray>", "<gray>Game type: <sky><variant></sky></gray>");
        t("side.hall.rounds", "<gray>Laufende Runden: <white><n></white></gray>", "<gray>Rounds running: <white><n></white></gray>");
        t("side.hall.wins", "<gray>Meisterbriefe: <au><n></au></gray>", "<gray>Certificates: <au><n></au></gray>");
        t("side.hall.podiums", "<gray>Auf dem Podest: <white><n></white></gray>", "<gray>On the podium: <white><n></white></gray>");

        // ---------- Menüs der Bauhütte ----------
        t("menu.vote.title", "Welche Spielart?", "Which game type?");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.click", "<em>Klick: dafür stimmen</em>", "<em>Click: vote for it</em>");
        t("menu.vote.mine", "<em>✔ Deine Stimme</em>", "<em>✔ Your vote</em>");
        t("menu.vote.off", "<gray>Gerade abgeschaltet</gray>", "<gray>Switched off right now</gray>");
        t("menu.howto.title", "So geht Baumeister", "How Master Builder works");
        t("menu.stats.title", "Statistik", "Statistics");
        t("menu.stats.me", "<au>Deine Werte</au>", "<au>Your stats</au>");
        t("menu.stats.top", "<au>Die besten Baumeister</au>", "<au>The best builders</au>");
        t("menu.watch.title", "Zuschauen", "Watch");
        t("menu.watch.arena", "<sky>Bauplatz <n></sky>", "<sky>Building site <n></sky>");
        t("menu.watch.theme", "<gray>Thema: <au><theme></au></gray>", "<gray>Theme: <au><theme></au></gray>");
        t("menu.watch.players", "<gray>Bauleute: <white><n></white></gray>", "<gray>Builders: <white><n></white></gray>");
        t("menu.watch.click", "<em>Klick: zuschauen</em>", "<em>Click: watch</em>");
        t("howto.theme", "Das Thema", "The theme");
        t("howto.theme.text", "<gray>Zu Beginn gibt es drei Vorschläge –</gray>\n<gray>die Mehrheit entscheidet.</gray>",
                "<gray>At the start there are three suggestions –</gray>\n<gray>the majority decides.</gray>");
        t("howto.theme.chat", "<gray>Thema: Drei Vorschläge kommen in die Schnellleiste und in ein Fenster. Ein Klick ist eine Stimme, die Mehrheit gewinnt, bei Gleichstand entscheidet das Los.</gray>",
                "<gray>Theme: three suggestions appear in your hotbar and in a window. One click is one vote, the majority wins, a tie is decided by lot.</gray>");
        t("howto.build", "Bauen", "Building");
        t("howto.build.text", "<gray>Jeder baut im Kreativmodus auf seiner</gray>\n<gray>eigenen Parzelle – bis die Zeit um ist.</gray>",
                "<gray>Everyone builds in creative mode on their</gray>\n<gray>own plot – until time is up.</gray>");
        t("howto.build.chat", "<gray>Bauen: Du hast eine eigene Parzelle mit Rand. Dort darfst du alles bauen und fliegen – hinaus kommt nichts. Die Zeit siehst du oben in der Leiste.</gray>",
                "<gray>Building: you get your own plot with a border. Build anything and fly there – nothing gets out. The time is shown in the bar at the top.</gray>");
        t("howto.helps", "Bau-Hilfen", "Building helpers");
        t("howto.helps.text", "<gray>Werkzeugkiste: Boden, Zeit, Wetter,</gray>\n<gray>Köpfe, Banner. Zollstock: füllen, ersetzen.</gray>",
                "<gray>Toolbox: floor, time, weather,</gray>\n<gray>heads, banners. Folding rule: fill, replace.</gray>");
        t("howto.helps.chat", "<gray>Bau-Hilfen: Mit dem Stern öffnest du die Werkzeugkiste. Mit dem Zollstock setzt du zwei Ecken und kannst den Bereich füllen, ersetzen oder leeren (mit Rückgängig).</gray>",
                "<gray>Building helpers: the star opens the toolbox. With the folding rule you set two corners and can fill, replace or clear the area (with undo).</gray>");
        t("howto.rate", "Bewerten", "Rating");
        t("howto.rate.text", "<gray>Alle besuchen jeden Bau und geben</gray>\n<gray>1 (Naja) bis 5 Sterne (Meisterwerk).</gray>",
                "<gray>Everyone visits every build and gives</gray>\n<gray>1 (meh) to 5 stars (masterpiece).</gray>");
        t("howto.rate.chat", "<gray>Bewerten: Ihr werdet zu jedem Bau gebracht und wählt mit der Wolle in der Schnellleiste 1 bis 5 Sterne. Den eigenen Bau bewerten nur die anderen. Wer gebaut hat, bleibt geheim, bis die Sieger feststehen.</gray>",
                "<gray>Rating: you're taken to each build and choose 1 to 5 stars with the wool in your hotbar. Only the others rate your own build. Who built what stays secret until the winners are announced.</gray>");
        t("howto.win", "Gewinnen", "Winning");
        t("howto.win.text", "<gray>Der beste Durchschnitt gewinnt</gray>\n<gray>den Meisterbrief.</gray>",
                "<gray>The best average wins</gray>\n<gray>the master's certificate.</gray>");
        t("howto.win.chat", "<gray>Gewinnen: Es zählt der Durchschnitt der Sterne, bei Gleichstand mehr Sterne insgesamt, dann mehr „Meisterwerke“. Die ersten drei stehen auf dem Podest, der Sieger bekommt den Meisterbrief.</gray>",
                "<gray>Winning: the average of the stars counts, on a tie more stars in total, then more “masterpieces”. The top three stand on the podium, the winner gets the master's certificate.</gray>");
        t("howto.rules", "Fair bauen", "Build fair");
        t("howto.rules.text", "<gray>Keine Beleidigungen auf Schildern,</gray>\n<gray>keine Bauten, die den Server bremsen.</gray>",
                "<gray>No insults on signs,</gray>\n<gray>no builds that slow down the server.</gray>");
        t("howto.rules.chat", "<gray>Fair bauen: Schilder, Namen und Bücher werden geprüft. Tiere, Rüstungsständer und Flüssigkeiten sind je Parzelle begrenzt, Redstone-Uhren werden gestoppt. TNT, Portale und Geschosse gibt es nicht.</gray>",
                "<gray>Build fair: signs, names and books are checked. Mobs, armour stands and liquids are limited per plot, redstone clocks get stopped. No TNT, portals or projectiles.</gray>");

        // ---------- Statistik ----------
        t("stats.head", "<au>Statistik von <name></au>", "<au>Statistics for <name></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>", "<gray>No stats for <white><name></white> yet.</gray>");
        t("stats.lines", "<gray>Runden: <white><games></white> · Meisterbriefe: <au><wins></au> · Podest: <white><podiums></white></gray>\n<gray>Sterne bekommen: <au><points></au> · Meisterwerke: <white><masterworks></white></gray>\n<gray>Bewertet: <white><votes></white> · Blöcke gesetzt: <white><blocks></white></gray>\n<gray>Beste Wertung: <au><best> ★</au></gray>",
                "<gray>Rounds: <white><games></white> · Certificates: <au><wins></au> · Podium: <white><podiums></white></gray>\n<gray>Stars received: <au><points></au> · Masterpieces: <white><masterworks></white></gray>\n<gray>Rated: <white><votes></white> · Blocks placed: <white><blocks></white></gray>\n<gray>Best rating: <au><best> ★</au></gray>");
        t("top.head", "<au>Die besten Baumeister</au>", "<au>The best builders</au>");
        t("top.empty", "<gray>Noch niemand – sei der Erste!</gray>", "<gray>Nobody yet – be the first!</gray>");
        t("top.row", "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> Meisterbriefe, <white><podiums></white>× Podest, beste <au><best> ★</au></gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <au><wins></au> certificates, <white><podiums></white>× podium, best <au><best> ★</au></gray>");

        // ---------- Tafeln ----------
        t("board.title", "<au><b>Die besten Baumeister</b></au>", "<au><b>The best builders</b></au>");
        t("board.welcome", "<em><b>⚒ Baumeister ⚒</b></em>\n<gray>Ein Thema, eine Parzelle – die Zeit läuft!</gray>",
                "<em><b>⚒ Master Builder ⚒</b></em>\n<gray>One theme, one plot – the clock is ticking!</gray>");
        t("board.howto", "<au><b>So geht's</b></au>\n<gray>1. Thema wählen</gray>\n<gray>2. Auf Zeit bauen</gray>\n<gray>3. Alle Bauten bewerten</gray>\n<gray>4. Meisterbrief gewinnen</gray>",
                "<au><b>How to play</b></au>\n<gray>1. Choose a theme</gray>\n<gray>2. Build against the clock</gray>\n<gray>3. Rate every build</gray>\n<gray>4. Win the certificate</gray>");
        t("board.variants", "<sky><b>Spielarten</b></sky>", "<sky><b>Game types</b></sky>");
        t("board.variant", "<white><variant></white> <gray>– <white><n></white> Stimmen</gray>",
                "<white><variant></white> <gray>– <white><n></white> votes</gray>");
        t("board.running", "<au>Bauplatz <n>:</au> <gray><phase></gray>", "<au>Site <n>:</au> <gray><phase></gray>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „⚒ Baumeister »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung). */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }
}
