package de.moinmorni.baumeister.logic;

/**
 * Ein Bau-Thema: Kennung (klein, ohne Leerzeichen), Kategorie und der Name auf Deutsch und Englisch.
 */
public record Theme(String id, Category category, String de, String en) {

    /** Grobe Sorten – drei Vorschläge kommen möglichst aus drei verschiedenen. */
    public enum Category {
        ALPEN, DORF, ALLTAG, FANTASIE, NATUR, ESSEN, FAHRZEUG, TIER, FEST
    }

    /** Name in der Sprache ({@code en} = Englisch, sonst Deutsch). */
    public String name(String lang) {
        return "en".equals(lang) ? en : de;
    }
}
