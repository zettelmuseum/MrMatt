import de.mrmatt.game.*;

import java.io.*;
import java.util.*;

/** Prueft GameSession: Replays ueber replayStep(), Undo bis 200 Schritte, Neustart. */
public class SessionTest {
    public static void main(String[] args) throws Exception {
        String dir = args[0];
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
        GameSession s = new GameSession();
        int ok = 0, bad = 0, undoBad = 0;
        for (int i = 0; i < reps.size(); i += 7) {
            Replay r = reps.get(i);
            Game g = games.get(r.gameName.toLowerCase().trim());
            s.start(g.levels.get(r.level - 1));
            s.startReplay(r);
            byte[][] snaps = new byte[400][];
            int n = 0;
            while (!s.replayFinished()) {
                if (n < snaps.length) snaps[n] = s.engine.cell.clone();
                n++;
                if (s.replayStep() < 0) break;
            }
            if (s.engine.state == Engine.WON && s.replayFinished()) ok++; else bad++;
            // Undo: alle (maximal 200) Schritte zurueck und Brettvergleich
            int steps = Math.min(n, GameSession.MAX_UNDO);
            for (int k = 0; k < steps; k++) {
                if (!s.undo()) { undoBad++; break; }
                int idx = n - 1 - k;
                if (idx < snaps.length && !Arrays.equals(snaps[idx], s.engine.cell)) { undoBad++; break; }
            }
            if (s.undoCount() != 0 && n <= GameSession.MAX_UNDO) undoBad++;
            // Weiterspielen nach Undo muss wieder den gleichen Ausgang haben (lastDir wird mitgesichert)
            s.restart();
            if (s.engine.steps != 0 || s.engine.state != Engine.PLAYING) bad++;
        }
        System.out.println("session replays ok=" + ok + " bad=" + bad + " undoBad=" + undoBad);
    }
}
