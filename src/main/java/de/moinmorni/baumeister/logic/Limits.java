package de.moinmorni.baumeister.logic;

/**
 * Grenzen je Parzelle, die den Server schonen (aus der config.yml): Wesen (Tiere, Rüstungsständer, Rahmen, Loren …),
 * Flüssigkeiten (gesetzte Wasser- und Lavaquellen), Blöcke je Werkzeug-Aufruf.
 */
public record Limits(int entities, int liquids, int toolBlocks, int redstonePerSecond) {

    /** Vorgaben, wie in der mitgelieferten config.yml. */
    public static Limits defaults() {
        return new Limits(12, 24, 4096, 60);
    }

    public boolean entityAllowed(int already) {
        return already < entities;
    }

    public boolean liquidAllowed(int already) {
        return already < liquids;
    }
}
