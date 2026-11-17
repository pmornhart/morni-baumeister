package de.moinmorni.baumeister.logic;

/** Wertung eines Baus: ein bis fünf Sterne, von „Naja“ bis „Meisterwerk“. */
public enum Rating {
    NAJA(1), GEHT_SO(2), GUT(3), SUPER(4), MEISTERWERK(5);

    private final int stars;

    Rating(int stars) {
        this.stars = stars;
    }

    public int stars() {
        return stars;
    }

    /** Schlüssel für Texte und Befehle: „naja“, „geht-so“ … */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    /** Wertung zu 1–5 (null sonst). */
    public static Rating ofStars(int stars) {
        for (Rating r : values()) {
            if (r.stars == stars) {
                return r;
            }
        }
        return null;
    }
}
