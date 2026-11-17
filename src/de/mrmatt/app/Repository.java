package de.mrmatt.app;

import android.content.Context;

import de.mrmatt.game.Game;
import de.mrmatt.game.GameParser;
import de.mrmatt.game.Replay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zugriff auf die mitgelieferten Daten (assets): Spieleliste, einzelne Spiele (.mat) und
 * die Loesungsreplays (mrmatt.sol).
 */
public final class Repository {
    private static GameParser parser;
    private static List<GameInfo> index;
    private static Map<String, List<Replay>> replays;

    private Repository() {
    }

    private static String readAsset(Context ctx, String name) throws IOException {
        InputStream in = ctx.getAssets().open(name);
        try {
            return GameParser.readAll(in);
        } finally {
            in.close();
        }
    }

    /** Spieleliste aus assets/index.txt (Zeilen: Datei TAB Titel TAB Sterne TAB Autor TAB Levelzahl). */
    public static synchronized List<GameInfo> index(Context ctx) throws IOException {
        if (index != null) return index;
        List<GameInfo> res = new ArrayList<GameInfo>();
        InputStream in = ctx.getAssets().open("index.txt");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\t", -1);
                if (f.length < 5) continue;
                res.add(new GameInfo(f[0], f[1], Integer.parseInt(f[2]), f[3], Integer.parseInt(f[4])));
            }
        } finally {
            in.close();
        }
        index = res;
        return index;
    }

    public static synchronized Game loadGame(Context ctx, String fileName) throws IOException {
        if (parser == null) parser = new GameParser(readAsset(ctx, "mdecrypt.txt"));
        return parser.parse(fileName, readAsset(ctx, "mat/" + fileName));
    }

    private static String key(String title, int level) {
        return title.toLowerCase().trim() + "#" + level;
    }

    /** Laedt die Replays (einmalig; kann im Hintergrund vorgewaermt werden). */
    public static synchronized void warmReplays(Context ctx) throws IOException {
        if (replays != null) return;
        Map<String, List<Replay>> m = new HashMap<String, List<Replay>>();
        List<Replay> all = Replay.parseAll(readAsset(ctx, "mrmatt.sol"));
        for (int i = 0; i < all.size(); i++) {
            Replay r = all.get(i);
            String k = key(r.gameName, r.level);
            List<Replay> l = m.get(k);
            if (l == null) {
                l = new ArrayList<Replay>();
                m.put(k, l);
            }
            l.add(r);
        }
        replays = m;
    }

    /** Kuerzeste gespeicherte Loesung fuer das Level (level 0-basiert) oder null. */
    public static synchronized Replay bestReplay(Context ctx, Game g, int level) {
        try {
            warmReplays(ctx);
        } catch (IOException e) {
            return null;
        }
        List<Replay> l = replays.get(key(g.title, level + 1));
        if (l == null) return null;
        Replay best = null;
        for (int i = 0; i < l.size(); i++) {
            Replay r = l.get(i);
            boolean sameDiff = r.difficulty == g.difficultyLetter();
            boolean bestSame = best != null && best.difficulty == g.difficultyLetter();
            if (best == null || (sameDiff && !bestSame) || (sameDiff == bestSame && r.steps < best.steps)) best = r;
        }
        return best;
    }

    public static synchronized boolean hasReplay(Context ctx, Game g, int level) {
        return bestReplay(ctx, g, level) != null;
    }
}
