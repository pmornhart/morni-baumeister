package de.moinmorni.baumeister.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Schutz vor beleidigenden Texten auf Schildern, Namensschildern und umbenannten Dingen – wie der Chat im Netzwerk:
 * eine Liste gesperrter Wörter (aus der config.yml), dazu keine Links und Adressen. Geprüft wird jedes Wort für sich
 * nach dem Vereinfachen (klein, Umlaute ausgeschrieben und einfach, Zahlen wie Buchstaben: „4rsch“ = „arsch“, Satzzeichen weg).
 * Einzelne Buchstaben hintereinander („a r s c h“) zählen als ein Wort. Erlaubte Wörter („Barsch“, „Marsch“) heben
 * einen Treffer in ihrem Inneren wieder auf.
 */
public final class WordFilter {

    // Links und Adressen wie im Netzwerk-Chat: "https://x.de", "www.x.de", "x.com", "1.2.3.4".
    // https://regex101.com/?flavor=java&regex=(https%3F%3A%2F%2F%7Cwww%5C.)%5CS%2B%7C%5Cb%5Ba-z0-9-%5D%2B%5C.(de%7Ccom%7Cnet%7Corg%7Ceu%7Cio%7Cgg%7Cme%7Ctv%7Cat%7Cch%7Cxyz%7Cinfo)%5Cb&testString=komm%20auf%20meinserver.net
    private static final Pattern LINK = Pattern.compile(
            "(https?://|www\\.)\\S+|\\b[a-z0-9-]+\\.(de|com|net|org|eu|io|gg|me|tv|at|ch|xyz|info)\\b"
                    + "|\\b\\d{1,3}(\\.\\d{1,3}){3}\\b", Pattern.CASE_INSENSITIVE);
    // Alles außer Buchstaben weg (nach dem Ersetzen der Zahlen).
    // https://regex101.com/?flavor=java&regex=%5B%5Ea-z%5D&testString=a.r-s_c*h
    private static final Pattern NOT_LETTER = Pattern.compile("[^a-z]");
    // Wörter trennen: Leerzeichen und Zeilen.
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private final List<String> blocked;
    private final List<String> allowed;
    private final boolean links;

    /**
     * @param blocked gesperrte Wörter
     * @param allowed Wörter, in denen ein gesperrtes Stück nicht zählt
     * @param links   Links und Adressen sperren?
     */
    public WordFilter(Collection<String> blocked, Collection<String> allowed, boolean links) {
        this.blocked = simplified(blocked);
        this.allowed = simplified(allowed);
        this.links = links;
    }

    private static List<String> simplified(Collection<String> words) {
        List<String> out = new ArrayList<>();
        for (String w : words) {
            String s = simplify(w);
            if (s.length() >= 2) {
                out.add(s);
            }
        }
        return out;
    }

    /** Klein, Umlaute ausgeschrieben, Zahlen und Zeichen wie Buchstaben, sonst nur a–z. */
    public static String simplify(String s) {
        String t = s.toLowerCase(Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's')
                .replace('7', 't').replace('@', 'a').replace('$', 's').replace('!', 'i').replace('|', 'l');
        return NOT_LETTER.matcher(t).replaceAll("");
    }

    /** Ist der Text in Ordnung? */
    public boolean ok(String text) {
        return reason(text) == null;
    }

    /** Was stört ({@code "link"} oder das gesperrte Wort), null wenn alles in Ordnung ist. */
    public String reason(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (links && LINK.matcher(text).find()) {
            return "link";
        }
        List<String> all = new ArrayList<>(words(text));
        // Umlaute auch einfach („Ärsche“ → „arsche“), nicht nur ausgeschrieben („aersche“).
        String plain = text.replace('ä', 'a').replace('ö', 'o').replace('ü', 'u').replace('Ä', 'a').replace('Ö', 'o')
                .replace('Ü', 'u');
        if (!plain.equals(text)) {
            all.addAll(words(plain));
        }
        for (String word : all) {
            String w = word;
            for (String a : allowed) {
                w = w.replace(a, "-");
            }
            for (String b : blocked) {
                if (w.contains(b)) {
                    return b;
                }
            }
        }
        return null;
    }

    /** Wörter des Textes, vereinfacht; Läufe einzelner Zeichen („a r s c h“) werden zusammengezogen. */
    static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (String raw : SPACE.split(text.trim())) {
            String s = simplify(raw);
            if (s.length() == 1) {
                run.append(s);
                continue;
            }
            if (run.length() > 0) {
                out.add(run.toString());
                run.setLength(0);
            }
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        if (run.length() > 0) {
            out.add(run.toString());
        }
        return out;
    }
}
