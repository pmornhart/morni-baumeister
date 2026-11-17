package de.moinmorni.baumeister.logic;

import java.util.Locale;

/**
 * Spielart, über die in der Bauhütte abgestimmt wird: klassisch (normale Bauzeit), schnell (kurze Bauzeit) oder zu
 * zweit (Paare bauen auf einer Parzelle; Partys zuerst zusammen). Zu zweit braucht mindestens zwei Paare.
 */
public enum Variant {
    KLASSISCH("klassisch", 2), SCHNELL("schnell", 2), PAARE("paare", 4);

    private final String id;
    private final int minPlayers;

    Variant(String id, int minPlayers) {
        this.id = id;
        this.minPlayers = minPlayers;
    }

    public String id() {
        return id;
    }

    public int minPlayers() {
        return minPlayers;
    }

    /** Geht diese Spielart mit so vielen Leuten? */
    public boolean possible(int players) {
        return players >= minPlayers;
    }

    public static Variant byId(String s) {
        if (s == null) {
            return null;
        }
        String k = s.toLowerCase(Locale.ROOT);
        for (Variant v : values()) {
            if (v.id.equals(k) || v.name().toLowerCase(Locale.ROOT).equals(k)) {
                return v;
            }
        }
        return switch (k) {
            case "classic", "normal" -> KLASSISCH;
            case "quick", "fast" -> SCHNELL;
            case "pairs", "duo", "zuzweit", "zu-zweit" -> PAARE;
            default -> null;
        };
    }
}
