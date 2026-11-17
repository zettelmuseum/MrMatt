package de.mrmatt.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Loesungsreplay (.sol-Datei).
 *
 * Zeilenformat:
 * <pre>
 * 1B 115 Annelie #1290|2b Or Not 2b|12R2ULRU3R...14D 51311
 * |  |   |             |            |                 +- Pruefzahl
 * |  |   |             |            +- Zuege, lauflaengencodiert (3R = RRR)
 * |  |   |             +- Spielname
 * |  |   +- Spieler
 * |  +- Anzahl Schritte
 * +- Levelnummer (1-basiert) + Schwierigkeit A..D (A = 1 Stern ... D = 4 Sterne)
 * </pre>
 * Zugzeichen: L, R, U, D.
 */
public final class Replay {
    public int level;        // 1-basiert
    public char difficulty;  // 'A'..'D'
    public int steps;
    public String player;
    public String gameName;
    public String rle;
    public long check;

    /** Dekodiert die Lauflaengencodierung: "2UL3R" -> "UULRRR". */
    public static String decode(String rle) {
        StringBuilder out = new StringBuilder();
        int n = 0;
        boolean have = false;
        for (int i = 0; i < rle.length(); i++) {
            char c = rle.charAt(i);
            if (c >= '0' && c <= '9') {
                n = n * 10 + (c - '0');
                have = true;
            } else {
                int cnt = have ? n : 1;
                for (int k = 0; k < cnt; k++) out.append(c);
                n = 0;
                have = false;
            }
        }
        return out.toString();
    }

    /** Codiert eine Zugfolge wieder lauflaengenweise. */
    public static String encode(String moves) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < moves.length()) {
            char c = moves.charAt(i);
            int j = i;
            while (j < moves.length() && moves.charAt(j) == c) j++;
            int cnt = j - i;
            if (cnt > 1) out.append(cnt);
            out.append(c);
            i = j;
        }
        return out.toString();
    }

    public String moves() {
        return decode(rle);
    }

    /** Parst den ganzen Inhalt einer .sol-Datei. */
    public static List<Replay> parseAll(String text) {
        String[] lines = GameParser.splitLines(text);
        List<Replay> res = new ArrayList<Replay>();
        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i];
            if (ln.length() == 0 || ln.startsWith("Version")) continue;
            Replay r = parseLine(ln);
            if (r != null) res.add(r);
        }
        return res;
    }

    public static Replay parseLine(String ln) {
        int sp1 = ln.indexOf(' ');
        if (sp1 < 2) return null;
        String head = ln.substring(0, sp1);
        char letter = head.charAt(head.length() - 1);
        int level;
        try {
            level = Integer.parseInt(head.substring(0, head.length() - 1));
        } catch (NumberFormatException e) {
            return null;
        }
        int sp2 = ln.indexOf(' ', sp1 + 1);
        if (sp2 < 0) return null;
        int steps;
        try {
            steps = Integer.parseInt(ln.substring(sp1 + 1, sp2));
        } catch (NumberFormatException e) {
            return null;
        }
        int p1 = ln.indexOf('|', sp2);
        int p2 = p1 < 0 ? -1 : ln.indexOf('|', p1 + 1);
        if (p1 < 0 || p2 < 0) return null;
        int lastSp = ln.lastIndexOf(' ');
        if (lastSp < p2) return null;
        Replay r = new Replay();
        r.level = level;
        r.difficulty = letter;
        r.steps = steps;
        r.player = ln.substring(sp2 + 1, p1);
        r.gameName = ln.substring(p1 + 1, p2);
        r.rle = ln.substring(p2 + 1, lastSp);
        try {
            r.check = Long.parseLong(ln.substring(lastSp + 1).trim());
        } catch (NumberFormatException e) {
            r.check = -1;
        }
        return r;
    }
}
