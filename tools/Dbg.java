import de.mrmatt.game.*;

import java.io.*;
import java.util.*;

/** Zeigt die Spielfeld-Situation vor Zug k.  Aufruf: java Dbg <datadir> "<spiel>" <level> [von] [bis] */
public class Dbg {
    static final String CH = " -#*+=!?H21";
    static int r0 = 0, r1 = Engine.ROWS - 1, c0 = 0, c1 = Engine.COLS - 1;
    static void dump(Engine e, String title) {
        System.out.println(title + " steps=" + e.steps + " apples=" + e.apples + " state=" + e.state);
        if (c0 != 0 || c1 != Engine.COLS - 1) {
            StringBuilder h = new StringBuilder("   ");
            for (int c = c0; c <= c1; c++) h.append(c % 10);
            System.out.println(h);
        }
        for (int r = r0; r <= r1; r++) {
            StringBuilder sb = new StringBuilder();
            if (c0 != 0 || c1 != Engine.COLS - 1) sb.append(String.format("%2d ", r));
            for (int c = c0; c <= c1; c++) {
                if (r == e.my && c == e.mx) { sb.append('M'); continue; }
                byte t = e.cell[r * Engine.COLS + c];
                sb.append(t >= 8 ? (char)('0' + (t - 6)) : " -#*+=!".charAt(t));
            }
            System.out.println(sb);
        }
    }
    public static void main(String[] args) throws Exception {
        String dir = args[0];
        GameParser gp = new GameParser(GameParser.readAll(new FileInputStream(dir + "/mdecrypt.txt")));
        Game g = null;
        File[] files = new File(dir + "/mat").listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().endsWith(".mat")) continue;
            Game x = gp.parse(files[i].getName(), GameParser.readAll(new FileInputStream(files[i])));
            if (x.title.equalsIgnoreCase(args[1])) g = x;
        }
        String win = System.getProperty("win");
        if (win != null) { String[] w = win.split(":"); r0 = Integer.parseInt(w[0]); r1 = Integer.parseInt(w[1]); c0 = Integer.parseInt(w[2]); c1 = Integer.parseInt(w[3]); }
        int lv = Integer.parseInt(args[2]);
        int from = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : 1 << 30;
        List<Replay> reps = Replay.parseAll(GameParser.readAll(new FileInputStream(dir + "/mrmatt.sol")));
        Replay rr = null;
        for (Replay r : reps) if (r.gameName.equalsIgnoreCase(args[1]) && r.level == lv) rr = r;
        Engine e = new Engine();
        e.load(g.levels.get(lv - 1));
        String mv = rr.moves();
        System.out.println("moves: " + mv);
        for (int k = 0; k < mv.length() && k < to; k++) {
            if (k >= from) dump(e, "before move " + k + " (" + mv.charAt(k) + ")");
            boolean ok = e.move("LRUD".indexOf(mv.charAt(k)));
            if (!ok || e.state != Engine.PLAYING) {
                System.out.println("move " + k + " " + mv.charAt(k) + " -> ok=" + ok + " state=" + e.state);
                dump(e, "after");
                break;
            }
        }
    }
}
