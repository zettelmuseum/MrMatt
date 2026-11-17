package de.mrmatt.game;

/**
 * Eine laufende Partie: Engine + Undo-Speicher (bis zu 200 Schritte) + Loesungswiedergabe.
 * Reines Java ohne Android-Abhaengigkeiten (auch auf der JVM testbar).
 */
public final class GameSession {
    public static final int MAX_UNDO = 200;

    public final Engine engine = new Engine();
    /** Wenn gesetzt, werden Zuege abgelehnt, die Matt toeten wuerden ("keine dummen Zuege"). */
    public boolean safeMode;

    private final Engine.State[] ring = new Engine.State[MAX_UNDO];
    private int ringStart;
    private int ringCount;
    private Engine.State pending = new Engine.State();
    private Level level;

    private String replayMoves;
    private int replayPos;
    private Replay replay;

    public void start(Level lv) {
        level = lv;
        engine.load(lv);
        ringStart = 0;
        ringCount = 0;
        stopReplay();
    }

    public void restart() {
        if (level != null) start(level);
    }

    public Level level() {
        return level;
    }

    /**
     * Fuehrt einen eigenen Zug aus; false, wenn er nicht moeglich ist (oder durch safeMode abgelehnt wird).
     * Ein eigener Zug beendet eine laufende Loesungswiedergabe.
     */
    public boolean move(int dir) {
        if (replayMoves != null) stopReplay();
        return doMove(dir, safeMode);
    }

    private boolean doMove(int dir, boolean checkSafe) {
        if (engine.state != Engine.PLAYING) return false;
        if (checkSafe && !engine.moveIsSafe(dir)) return false;
        engine.save(pending);
        if (!engine.move(dir)) return false;
        int idx;
        if (ringCount == MAX_UNDO) {
            idx = ringStart;
            ringStart = (ringStart + 1) % MAX_UNDO;
        } else {
            idx = (ringStart + ringCount) % MAX_UNDO;
            ringCount++;
        }
        Engine.State old = ring[idx];
        ring[idx] = pending;
        pending = old != null ? old : new Engine.State();
        return true;
    }

    public int undoCount() {
        return ringCount;
    }

    /** Nimmt den letzten Schritt zurueck (auch nach dem Tod). */
    public boolean undo() {
        if (ringCount == 0) return false;
        if (replayMoves != null && replayPos > 0) replayPos--;
        int idx = (ringStart + ringCount - 1) % MAX_UNDO;
        engine.restore(ring[idx]);
        ringCount--;
        return true;
    }

    // ---- Loesungswiedergabe ----

    /** Startet das Level neu und bereitet die Wiedergabe der Loesung vor. */
    public void startReplay(Replay r) {
        start(level);
        replay = r;
        replayMoves = r.moves();
        replayPos = 0;
    }

    public boolean hasReplay() {
        return replayMoves != null;
    }

    public Replay replay() {
        return replay;
    }

    public int replayPosition() {
        return replayPos;
    }

    public int replayLength() {
        return replayMoves == null ? 0 : replayMoves.length();
    }

    public boolean replayFinished() {
        return replayMoves == null || replayPos >= replayMoves.length();
    }

    /** Fuehrt den naechsten Zug der Loesung aus. Liefert die Richtung oder -1 (fertig / Fehler). */
    public int replayStep() {
        if (replayMoves == null || replayPos >= replayMoves.length()) return -1;
        int dir = "LRUD".indexOf(replayMoves.charAt(replayPos));
        if (!doMove(dir, false)) return -1;
        replayPos++;
        return dir;
    }

    public void stopReplay() {
        replayMoves = null;
        replay = null;
        replayPos = 0;
    }
}
