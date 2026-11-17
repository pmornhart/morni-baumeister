package de.moinmorni.baumeister.game;

import de.moinmorni.baumeister.logic.Region;
import java.util.UUID;
import org.bukkit.Location;

/** Eine Person in einer Runde: ihre Parzelle, ihr Bereich für die Werkzeuge, wie viel sie gesetzt hat, ob sie da ist. */
public final class Builder {

    private final UUID uuid;
    private final String name;
    private final int plot;
    private final Region region = new Region();
    private int blocks;
    /** Ist gerade nicht verbunden (Platz wird freigehalten). */
    private boolean away;
    /** Kam nicht rechtzeitig zurück. */
    private boolean late;
    /** Hat die Runde selbst verlassen. */
    private boolean left;
    private Location lastSeen;
    private long toolReadyAt;

    Builder(UUID uuid, String name, int plot) {
        this.uuid = uuid;
        this.name = name;
        this.plot = plot;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int plot() {
        return plot;
    }

    public Region region() {
        return region;
    }

    public int blocks() {
        return blocks;
    }

    public void placed() {
        blocks++;
    }

    public boolean away() {
        return away;
    }

    void away(boolean a) {
        away = a;
    }

    public boolean late() {
        return late;
    }

    void late(boolean l) {
        late = l;
    }

    public boolean left() {
        return left;
    }

    void left(boolean l) {
        left = l;
    }

    /** Raus aus der Runde (selbst gegangen oder zu spät)? */
    public boolean gone() {
        return late || left;
    }

    /** Spielt gerade mit (da und nicht raus)? */
    public boolean active() {
        return !away && !gone();
    }

    Location lastSeen() {
        return lastSeen;
    }

    void lastSeen(Location l) {
        lastSeen = l;
    }

    long toolReadyAt() {
        return toolReadyAt;
    }

    void toolReadyAt(long t) {
        toolReadyAt = t;
    }
}
