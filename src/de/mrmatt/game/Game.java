package de.mrmatt.game;

import java.util.ArrayList;
import java.util.List;

/** Ein Spiel (= eine .mat-Datei): Titel, Schwierigkeit (1-4 Sterne), Autor, Levels. */
public final class Game {
    public String title = "";
    public int stars = 1;
    public String author = "";
    public String code = "";
    public String fileName = "";
    public final List<Level> levels = new ArrayList<Level>();

    /** Schwierigkeitsbuchstabe wie in der .sol-Datei: A=1 Stern ... D=4 Sterne. */
    public char difficultyLetter() {
        return (char) ('A' + Math.max(0, Math.min(3, stars - 1)));
    }

    public String toString() {
        return title;
    }
}
