package de.mrmatt.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest .mat-Dateien (Spiele) und die Schluesseltabelle mdecrypt.txt.
 *
 * Dateiformat (zeilenweise, CRLF):
 * <pre>
 * [Version 3.xx #n]        optional (nur in den neueren Dateien)
 * ** Spielname             Anzahl Sterne = Schwierigkeit
 * Autor
 * 18400 5ORE               Kennung (hier unbenutzt)
 * Levelname
 * 18 Zeilen a 31 Zeichen   Karte, verschluesselt oder im Klartext
 * OK | Zahl                Abschluss des Levels
 * ...
 * END                      (nur in den Klartext-Dateien ohne Versionszeile)
 * </pre>
 *
 * Verschluesselung: Pro Feldposition (Zeile, Spalte) gibt es in mdecrypt.txt
 * fuer jeden der sieben Feldtypen (EMPTY, GRASS, WALL, STONE, APPLE, BOX, BOMB)
 * ein eigenes Zeichen. Passt das Zeichen der .mat-Datei zu keinem der sieben,
 * steht dort Mr. Matt. Das Leerzeichen der .mat-Datei entspricht dem 'X'
 * in der Schluesseltabelle (dort wurde das Leerzeichen durch 'X' ersetzt).
 */
public final class GameParser {

    public static final String PLAIN_CHARS = " -#*+=!H12";

    /** keys[typ][zeile] = String mit 31 Zeichen. */
    private final String[][] keys = new String[7][Engine.ROWS];

    public GameParser(String mdecryptText) {
        List<String> rows = new ArrayList<String>();
        String[] lines = splitLines(mdecryptText);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() == Engine.COLS) rows.add(lines[i]);
        }
        if (rows.size() < 7 * Engine.ROWS) {
            throw new IllegalArgumentException("mdecrypt: zu wenige Zeilen: " + rows.size());
        }
        for (int t = 0; t < 7; t++) {
            for (int r = 0; r < Engine.ROWS; r++) {
                keys[t][r] = rows.get(t * Engine.ROWS + r);
            }
        }
    }

    public static String[] splitLines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    public static String readAll(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
            return sb.toString();
        } finally {
            br.close();
        }
    }

    private static boolean isPlain(String[] rows) {
        for (int r = 0; r < rows.length; r++) {
            String s = rows[r];
            for (int c = 0; c < s.length(); c++) {
                if (PLAIN_CHARS.indexOf(s.charAt(c)) < 0) return false;
            }
        }
        return true;
    }

    /** Entschluesselt eine Zeile (Zeilennummer i) zu Klartextzeichen " -#*+=!H". */
    private String decryptRow(String row, int i) {
        StringBuilder out = new StringBuilder(Engine.COLS);
        for (int j = 0; j < Engine.COLS; j++) {
            char ch = row.charAt(j);
            if (ch == ' ') ch = 'X';
            int mn = 7;
            for (int k = 0; k < 7; k++) {
                if (ch == keys[k][i].charAt(j)) mn = k;
            }
            out.append(" -#*+=!H".charAt(mn));
        }
        return out.toString();
    }

    private static byte tileOf(char ch) {
        switch (ch) {
            case '-': return Engine.EARTH;
            case '#': return Engine.WALL;
            case '*': return Engine.STONE;
            case '+': return Engine.APPLE;
            case '=': return Engine.BOX;
            case '2': return Engine.BOX2;
            case '1': return Engine.BOX1;
            case '!': return Engine.BOMB;
            default:  return Engine.EMPTY;   // ' ' und 'H'
        }
    }

    public Game parse(String fileName, String text) {
        String[] L = splitLines(text);
        Game g = new Game();
        g.fileName = fileName;
        int i = 0;
        if (L[0].startsWith("Version")) i = 1;
        String title = L[i];
        g.author = L[i + 1];
        g.code = L[i + 2];
        i += 3;
        int stars = 0;
        while (stars < title.length() && title.charAt(stars) == '*') stars++;
        g.stars = Math.max(1, stars);
        g.title = title.substring(stars).trim();

        while (i < L.length) {
            if (L[i].equals("END")) break;
            if (L[i].length() == 0 && i >= L.length - 1) break;
            if (i + 18 >= L.length) break;
            String lname = L[i];
            String[] rows = new String[Engine.ROWS];
            for (int r = 0; r < Engine.ROWS; r++) rows[r] = L[i + 1 + r];
            for (int r = 0; r < Engine.ROWS; r++) {
                if (rows[r].length() != Engine.COLS) {
                    throw new IllegalArgumentException(fileName + ": Level '" + lname
                            + "' Zeile " + r + " hat " + rows[r].length() + " Zeichen");
                }
            }
            if (!isPlain(rows)) {
                for (int r = 0; r < Engine.ROWS; r++) rows[r] = decryptRow(rows[r], r);
            }
            byte[] cells = new byte[Engine.ROWS * Engine.COLS];
            int sx = -1, sy = -1;
            for (int r = 0; r < Engine.ROWS; r++) {
                for (int c = 0; c < Engine.COLS; c++) {
                    char ch = rows[r].charAt(c);
                    if (ch == 'H') { sx = c; sy = r; }
                    cells[r * Engine.COLS + c] = tileOf(ch);
                }
            }
            g.levels.add(new Level(lname, cells, sx, sy));
            i += 1 + Engine.ROWS + 1;   // Name + 18 Zeilen + Abschlusszeile
        }
        return g;
    }
}
