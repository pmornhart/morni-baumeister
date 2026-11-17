package de.moinmorni.baumeister.logic;

/**
 * Wer hat wie abgeschnitten – Werte für die Statistik aus dem Ergebnis einer Parzelle.
 */
public final class Placement {

    private Placement() {
    }

    /** Sieg = Platz 1 (auch geteilt), aber nur, wenn es Stimmen gab und mindestens zwei Bauten im Rennen waren. */
    public static boolean won(Scoring.Result r, int plots) {
        return r.place() == 1 && r.votes() > 0 && plots >= 2;
    }

    /** Auf dem Podest = Platz 1–3 mit Stimmen. */
    public static boolean podium(Scoring.Result r, int plots) {
        return r.place() <= 3 && r.votes() > 0 && plots >= 2;
    }

    /** Durchschnitt als Text mit zwei Nachkommastellen, deutsch mit Komma: 435 → „4,35“. */
    public static String stars(int hundredths, String lang) {
        String s = (hundredths / 100) + "." + String.format(java.util.Locale.ROOT, "%02d", Math.abs(hundredths % 100));
        return "en".equals(lang) ? s : s.replace('.', ',');
    }
}
