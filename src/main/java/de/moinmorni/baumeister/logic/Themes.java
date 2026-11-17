package de.moinmorni.baumeister.logic;

import de.moinmorni.baumeister.logic.Theme.Category;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Alle Bau-Themen (eigene Liste, Deutsch und Englisch). Vorschläge kommen zufällig, möglichst aus verschiedenen
 * Kategorien und nicht aus den zuletzt gespielten.
 */
public final class Themes {

    private static final Map<String, Theme> ALL = new LinkedHashMap<>();

    private Themes() {
    }

    private static void t(String id, Category c, String de, String en) {
        if (ALL.put(id, new Theme(id, c, de, en)) != null) {
            throw new IllegalStateException("Thema doppelt: " + id);
        }
    }

    static {
        // Alpen
        t("almhuette", Category.ALPEN, "Almhütte", "Alpine hut");
        t("gipfelkreuz", Category.ALPEN, "Gipfelkreuz", "Summit cross");
        t("seilbahn", Category.ALPEN, "Seilbahn", "Cable car");
        t("berghof", Category.ALPEN, "Berghof", "Mountain farm");
        t("wasserfall", Category.ALPEN, "Wasserfall", "Waterfall");
        t("bergsee", Category.ALPEN, "Bergsee", "Mountain lake");
        t("skihuette", Category.ALPEN, "Skihütte", "Ski lodge");
        t("sprungschanze", Category.ALPEN, "Sprungschanze", "Ski jump");
        t("klamm", Category.ALPEN, "Klamm mit Steg", "Gorge with a walkway");
        t("almabtrieb", Category.ALPEN, "Almabtrieb", "Cattle drive");
        t("berghuette_nacht", Category.ALPEN, "Berghütte bei Nacht", "Mountain hut at night");
        t("lawinenschutz", Category.ALPEN, "Lawinenschutz", "Avalanche barrier");
        t("sennerei", Category.ALPEN, "Sennerei", "Alpine dairy");
        t("hochsitz", Category.ALPEN, "Hochsitz", "Hunting stand");
        t("murmeltierbau", Category.ALPEN, "Murmeltierbau", "Marmot burrow");
        // Dorf
        t("maibaum", Category.DORF, "Maibaum", "Maypole");
        t("dorfplatz", Category.DORF, "Dorfplatz", "Village square");
        t("wirtshaus", Category.DORF, "Wirtshaus", "Village inn");
        t("biergarten", Category.DORF, "Biergarten", "Beer garden");
        t("zwiebelturm", Category.DORF, "Kirche mit Zwiebelturm", "Onion-domed church");
        t("dorfbrunnen", Category.DORF, "Dorfbrunnen", "Village fountain");
        t("bauernhof", Category.DORF, "Bauernhof", "Farm");
        t("muehle", Category.DORF, "Mühle", "Mill");
        t("schmiede", Category.DORF, "Schmiede", "Smithy");
        t("baeckerei", Category.DORF, "Bäckerei", "Bakery");
        t("kegelbahn", Category.DORF, "Kegelbahn", "Bowling alley");
        t("feuerwehr", Category.DORF, "Feuerwehrhaus", "Fire station");
        t("rathaus", Category.DORF, "Rathaus", "Town hall");
        t("marktstand", Category.DORF, "Marktstand", "Market stall");
        t("bushaltestelle", Category.DORF, "Bushaltestelle", "Bus stop");
        t("dorfschule", Category.DORF, "Dorfschule", "Village school");
        // Alltag
        t("kinderzimmer", Category.ALLTAG, "Kinderzimmer", "Children's room");
        t("kueche", Category.ALLTAG, "Küche", "Kitchen");
        t("badezimmer", Category.ALLTAG, "Badezimmer", "Bathroom");
        t("spielplatz", Category.ALLTAG, "Spielplatz", "Playground");
        t("bahnhof", Category.ALLTAG, "Bahnhof", "Train station");
        t("freibad", Category.ALLTAG, "Freibad", "Outdoor pool");
        t("bibliothek", Category.ALLTAG, "Bibliothek", "Library");
        t("supermarkt", Category.ALLTAG, "Supermarkt", "Supermarket");
        t("zeltlager", Category.ALLTAG, "Zeltlager", "Campsite");
        t("baumhaus", Category.ALLTAG, "Baumhaus", "Treehouse");
        t("leuchtturm", Category.ALLTAG, "Leuchtturm", "Lighthouse");
        t("garten", Category.ALLTAG, "Schrebergarten", "Allotment garden");
        t("werkstatt", Category.ALLTAG, "Werkstatt", "Workshop");
        t("schreibtisch", Category.ALLTAG, "Riesen-Schreibtisch", "Giant desk");
        t("fussballplatz", Category.ALLTAG, "Fußballplatz", "Football pitch");
        t("kino", Category.ALLTAG, "Kino", "Cinema");
        t("hochhaus", Category.ALLTAG, "Hochhaus", "Skyscraper");
        t("bruecke", Category.ALLTAG, "Brücke", "Bridge");
        t("uhr", Category.ALLTAG, "Riesige Uhr", "Giant clock");
        t("musikinstrument", Category.ALLTAG, "Musikinstrument", "Musical instrument");
        // Fantasie
        t("drache", Category.FANTASIE, "Drache", "Dragon");
        t("burg", Category.FANTASIE, "Ritterburg", "Knight's castle");
        t("zauberturm", Category.FANTASIE, "Zauberturm", "Wizard's tower");
        t("schwebende_insel", Category.FANTASIE, "Schwebende Insel", "Floating island");
        t("piratenschiff", Category.FANTASIE, "Piratenschiff", "Pirate ship");
        t("raumschiff", Category.FANTASIE, "Raumschiff", "Spaceship");
        t("roboter", Category.FANTASIE, "Roboter", "Robot");
        t("hexenhaus", Category.FANTASIE, "Hexenhaus", "Witch's cottage");
        t("unterwasserstadt", Category.FANTASIE, "Unterwasserstadt", "Underwater city");
        t("riese", Category.FANTASIE, "Freundlicher Riese", "Friendly giant");
        t("zwergenmine", Category.FANTASIE, "Zwergenmine", "Dwarf mine");
        t("feenwald", Category.FANTASIE, "Feenwald", "Fairy forest");
        t("schatzinsel", Category.FANTASIE, "Schatzinsel", "Treasure island");
        t("zeitmaschine", Category.FANTASIE, "Zeitmaschine", "Time machine");
        t("vulkan", Category.FANTASIE, "Vulkan", "Volcano");
        t("geisterhaus", Category.FANTASIE, "Geisterhaus", "Haunted house");
        t("wolkenschloss", Category.FANTASIE, "Wolkenschloss", "Castle in the clouds");
        t("mondbasis", Category.FANTASIE, "Mondbasis", "Moon base");
        t("tatzelwurm", Category.FANTASIE, "Tatzelwurm", "Alpine dragon-worm");
        t("wolpertinger", Category.FANTASIE, "Wolpertinger", "Wolpertinger");
        // Natur
        t("riesenbaum", Category.NATUR, "Riesenbaum", "Giant tree");
        t("blumenwiese", Category.NATUR, "Blumenwiese", "Flower meadow");
        t("pilz", Category.NATUR, "Riesenpilz", "Giant mushroom");
        t("regenbogen", Category.NATUR, "Regenbogen", "Rainbow");
        t("hoehle", Category.NATUR, "Tropfsteinhöhle", "Dripstone cave");
        t("oase", Category.NATUR, "Oase", "Oasis");
        t("koralle", Category.NATUR, "Korallenriff", "Coral reef");
        t("gewitter", Category.NATUR, "Gewitterwolke", "Thundercloud");
        t("jahreszeiten", Category.NATUR, "Vier Jahreszeiten", "Four seasons");
        t("insel", Category.NATUR, "Einsame Insel", "Desert island");
        t("sonnenblume", Category.NATUR, "Sonnenblume", "Sunflower");
        t("kaktus", Category.NATUR, "Kaktus", "Cactus");
        // Essen
        t("brotzeit", Category.ESSEN, "Brotzeit", "Bavarian snack platter");
        t("brezn", Category.ESSEN, "Brezn", "Pretzel");
        t("weisswurst", Category.ESSEN, "Weißwurst-Frühstück", "Sausage breakfast");
        t("torte", Category.ESSEN, "Geburtstagstorte", "Birthday cake");
        t("eisbecher", Category.ESSEN, "Eisbecher", "Ice cream sundae");
        t("obstkorb", Category.ESSEN, "Obstkorb", "Fruit basket");
        t("pizza", Category.ESSEN, "Pizza", "Pizza");
        t("kaesebrett", Category.ESSEN, "Käsebrett", "Cheese board");
        t("lebkuchenhaus", Category.ESSEN, "Lebkuchenhaus", "Gingerbread house");
        t("burger", Category.ESSEN, "Burger", "Burger");
        t("kaiserschmarrn", Category.ESSEN, "Kaiserschmarrn", "Shredded pancake");
        t("teekanne", Category.ESSEN, "Teekanne", "Teapot");
        // Fahrzeuge
        t("traktor", Category.FAHRZEUG, "Traktor", "Tractor");
        t("dampflok", Category.FAHRZEUG, "Dampflok", "Steam locomotive");
        t("heissluftballon", Category.FAHRZEUG, "Heißluftballon", "Hot-air balloon");
        t("segelschiff", Category.FAHRZEUG, "Segelschiff", "Sailing ship");
        t("flugzeug", Category.FAHRZEUG, "Flugzeug", "Airplane");
        t("feuerwehrauto", Category.FAHRZEUG, "Feuerwehrauto", "Fire engine");
        t("pferdekutsche", Category.FAHRZEUG, "Pferdekutsche", "Horse carriage");
        t("uboot", Category.FAHRZEUG, "U-Boot", "Submarine");
        t("rakete", Category.FAHRZEUG, "Rakete", "Rocket");
        t("fahrrad", Category.FAHRZEUG, "Fahrrad", "Bicycle");
        t("pistenbully", Category.FAHRZEUG, "Pistenraupe", "Snow groomer");
        t("hubschrauber", Category.FAHRZEUG, "Hubschrauber", "Helicopter");
        // Tiere
        t("kuh", Category.TIER, "Kuh mit Glocke", "Cow with a bell");
        t("adler", Category.TIER, "Adler", "Eagle");
        t("murmeltier", Category.TIER, "Murmeltier", "Marmot");
        t("steinbock", Category.TIER, "Steinbock", "Ibex");
        t("dackel", Category.TIER, "Dackel", "Dachshund");
        t("schnecke", Category.TIER, "Schnecke", "Snail");
        t("aquarium", Category.TIER, "Aquarium", "Aquarium");
        t("bienenstock", Category.TIER, "Bienenstock", "Beehive");
        t("pinguin", Category.TIER, "Pinguin", "Penguin");
        t("schildkroete", Category.TIER, "Schildkröte", "Turtle");
        t("eule", Category.TIER, "Eule", "Owl");
        t("zoo", Category.TIER, "Zoo", "Zoo");
        // Feste
        t("wiesn", Category.FEST, "Volksfest", "Funfair");
        t("riesenrad", Category.FEST, "Riesenrad", "Ferris wheel");
        t("christkindlmarkt", Category.FEST, "Christkindlmarkt", "Christmas market");
        t("schneemann", Category.FEST, "Schneemann", "Snowman");
        t("osternest", Category.FEST, "Osternest", "Easter nest");
        t("kuerbis", Category.FEST, "Kürbisgesicht", "Jack-o'-lantern");
        t("feuerwerk", Category.FEST, "Feuerwerk", "Fireworks");
        t("hochzeit", Category.FEST, "Hochzeitstorte", "Wedding cake");
        t("zirkus", Category.FEST, "Zirkuszelt", "Circus tent");
        t("sonnwendfeuer", Category.FEST, "Sonnwendfeuer", "Midsummer bonfire");
        t("karussell", Category.FEST, "Karussell", "Carousel");
        t("geschenk", Category.FEST, "Geschenk", "Gift");
    }

    /** Alle Themen in fester Reihenfolge. */
    public static List<Theme> all() {
        return List.copyOf(ALL.values());
    }

    /** Thema nach Kennung (null, wenn unbekannt). */
    public static Theme byId(String id) {
        return id == null ? null : ALL.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * {@code n} verschiedene Vorschläge: zuerst je Kategorie höchstens einer (solange es reicht), keine aus
     * {@code recent} (zuletzt gespielt). Sind zu wenige übrig, kommen auch zuletzt gespielte wieder in Frage.
     */
    public static List<Theme> suggest(Random random, int n, Collection<String> recent) {
        Set<String> avoid = new HashSet<>(recent);
        List<Theme> pool = new ArrayList<>();
        for (Theme t : ALL.values()) {
            if (!avoid.contains(t.id())) {
                pool.add(t);
            }
        }
        if (pool.size() < n) {
            pool = new ArrayList<>(ALL.values());
        }
        Collections.shuffle(pool, random);
        List<Theme> out = new ArrayList<>();
        Set<Category> used = EnumSet.noneOf(Category.class);
        for (Theme t : pool) {
            if (out.size() < n && used.add(t.category())) {
                out.add(t);
            }
        }
        for (Theme t : pool) {
            if (out.size() < n && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }
}
