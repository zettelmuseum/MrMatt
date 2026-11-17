package de.mrmatt.app;

/** Eintrag der Spieleliste (aus assets/index.txt). */
public final class GameInfo {
    public final String file;
    public final String title;
    public final int stars;
    public final String author;
    public final int levels;

    public GameInfo(String file, String title, int stars, String author, int levels) {
        this.file = file;
        this.title = title;
        this.stars = stars;
        this.author = author;
        this.levels = levels;
    }

    public String starText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) sb.append('\u2605');
        return sb.toString();
    }
}
