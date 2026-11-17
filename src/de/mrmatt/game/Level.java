package de.mrmatt.game;

/**
 * Ein einzelnes Level: Name und Ausgangskarte (31 x 18 Felder).
 * Die Felder sind mit den Konstanten aus {@link Engine} codiert
 * (EMPTY, EARTH, WALL, STONE, APPLE, BOX, BOMB). Mr. Matts Startposition
 * steht separat in startX/startY (das Feld darunter ist EMPTY).
 */
public final class Level {
    public final String name;
    public final byte[] cells;   // ROWS * COLS, Zeile fuer Zeile
    public final int startX;
    public final int startY;

    public Level(String name, byte[] cells, int startX, int startY) {
        this.name = name;
        this.cells = cells;
        this.startX = startX;
        this.startY = startY;
    }

    /** Anzahl der Aepfel in diesem Level. */
    public int countApples() {
        int n = 0;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == Engine.APPLE) n++;
        }
        return n;
    }
}
