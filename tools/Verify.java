import de.mrmatt.game.*;

import java.io.*;
import java.util.*;

/** Testet alle Loesungsreplays gegen die Engine.  Aufruf: java Verify <datadir> [-v] */
public class Verify {
    public static void main(String[] args) throws Exception {
        String dir = args[0];
        boolean verbose = args.length > 1 && args[1].equals("-v");
        GameParser gp = new GameParser(GameParser.readAll(new FileInputStream(dir + "/mdecrypt.txt")));
        Map<String, Game> games = new HashMap<String, Game>();
        File[] files = new File(dir + "/mat").listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().endsWith(".mat")) continue;
            Game g = gp.parse(files[i].getName(), GameParser.readAll(new FileInputStream(files[i])));
            games.put(g.title.toLowerCase().trim(), g);
        }
        List<Replay> reps = Replay.parseAll(GameParser.readAll(new FileInputStream(dir + "/mrmatt.sol")));
        int ok = 0, bad = 0;
        StringBuilder okOut = new StringBuilder();
        Map<String, Integer> kinds = new TreeMap<String, Integer>();
        Engine e = new Engine();
        int shown = 0;
        for (int i = 0; i < reps.size(); i++) {
            Replay r = reps.get(i);
            if (System.getProperty("only") != null && !(r.gameName + " L" + r.level).equalsIgnoreCase(System.getProperty("only").replace('_', ' '))) continue;
            Game g = games.get(r.gameName.toLowerCase().trim());
            Level lv = g.levels.get(r.level - 1);
            e.load(lv);
            String mv = r.moves();
            if (System.getProperty("logPush") != null) System.out.println("== " + g.title + " L" + r.level);
            String fail = null;
            int k;
            for (k = 0; k < mv.length(); k++) {
                int d = "LRUD".indexOf(mv.charAt(k));
                if (e.state != Engine.PLAYING) { fail = "ended early (state " + e.state + ") at " + k; break; }
                if (!e.move(d)) { fail = "blocked"; break; }
                if (e.state == Engine.DEAD) { fail = "dead"; break; }
                // Ereignisliste muss das Feld von "stage" nach "cell" ueberfuehren (Animation)
                byte[] b = e.stage.clone();
                for (int q = 0; q < e.eventCount; q += Engine.EV_SIZE) Engine.applyEvent(b, e.events, q);
                if (!Arrays.equals(b, e.cell)) { fail = "events"; break; }
            }
            if (fail == null && e.state != Engine.WON) fail = "notwon";
            if (fail == null && k != mv.length()) fail = "x";
            if (fail == null && e.steps != r.steps) fail = "steps";
            if (System.getProperty("okfile") != null) {
                okOut.append(fail == null ? "OK " : "FAIL ").append(g.title).append(" L").append(r.level).append("\n");
            }
            if (fail == null) { ok++; }
            else {
                bad++;
                Integer c = kinds.get(fail.replaceAll("[0-9]+", "#"));
                kinds.put(fail.replaceAll("[0-9]+", "#"), c == null ? 1 : c + 1);
                if (verbose && shown < 15) {
                    shown++;
                    System.out.println("FAIL " + g.title + " L" + r.level + " move " + k + "/" + mv.length() + " " + fail);
                }
            }
        }
        if (System.getProperty("okfile") != null) {
            FileWriter fw = new FileWriter(System.getProperty("okfile"));
            fw.write(okOut.toString());
            fw.close();
        }
        System.out.println("ok=" + ok + " bad=" + bad + " " + kinds);
    }
}
