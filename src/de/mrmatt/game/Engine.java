package de.mrmatt.game;

import java.util.Arrays;

/**
 * Spiellogik von Mr. Matt (rundenbasiert) - aus den Loesungsreplays rekonstruiert.
 *
 * <h3>Regeln</h3>
 * <ul>
 * <li><b>Mr. Matt</b> geht in leere Felder, graebt Erde, frisst Aepfel. Er schiebt genau
 *     <i>einen</i> Stein, eine Bombe oder eine Kiste waagerecht, wenn das Feld dahinter leer ist.
 *     Senkrecht schieben geht nicht. Ein nicht moeglicher Zug zaehlt nicht als Schritt.
 *     Sind alle Aepfel gefressen, ist das Level sofort gewonnen.</li>
 * <li><b>Physik</b> laeuft nach jedem Zug komplett durch (Matt kann sich waehrenddessen nicht
 *     bewegen). Betrachtet werden nur Objekte, die "geweckt" wurden: das Objekt direkt ueber
 *     Matts altem Feld, das geschobene Objekt und alles, was ueber einem frei gewordenen Feld
 *     liegt. Die Objekte werden Zeile fuer Zeile von unten nach oben (je Zeile von links nach
 *     rechts) abgearbeitet; jedes faellt dabei vollstaendig, bevor das naechste drankommt. Das
 *     wird wiederholt, bis sich nichts mehr bewegt.</li>
 * <li><b>Fallen</b>: Stein, Bombe und Kiste fallen, solange das Feld darunter leer ist.
 *     Faellt ein Objekt auf Matt, ist er tot (liegende Objekte ueber ihm sind harmlos).</li>
 * <li><b>Rollen</b>: Ein <i>gefallenes</i> Objekt (Stein oder Kiste), das auf einem Stein landet,
 *     rollt zur Seite, wenn dort das Feld frei ist und darunter ein freies Feld oder eine Kiste
 *     liegt. Die Richtung: hat das Objekt in diesem Durchgang schon gerollt, rollt es in dieselbe
 *     Richtung weiter; sonst in die Richtung von Matts letztem Zug (bei senkrechtem Zug: links
 *     zuerst). Ist diese Seite nicht moeglich, wird die andere versucht.</li>
 * <li><b>Kiste</b>: Jedes Objekt (Stein, Bombe, Kiste), das in sie hineinfaellt oder hineingeschoben
 *     wird, verschwindet. Nach drei Treffern verschwindet die Kiste selbst.</li>
 * <li><b>Bombe</b>: Faellt sie auf einen Stein oder eine Bombe, verschwinden beide. Auf einer Wand
 *     verschwindet sie allein. Auf Erde, Apfel oder dem Boden passiert nichts. Eine geschobene
 *     Bombe explodiert nicht.</li>
 * </ul>
 */
public final class Engine {
    public static final int COLS = 31;
    public static final int ROWS = 18;
    public static final int N = COLS * ROWS;

    // Feldtypen
    public static final byte EMPTY = 0;
    public static final byte EARTH = 1;
    public static final byte WALL = 2;
    public static final byte STONE = 3;
    public static final byte APPLE = 4;
    public static final byte BOX = 5;    // Kiste, noch 3 Treffer frei
    public static final byte BOMB = 6;
    public static final byte BOX2 = 8;   // Kiste, noch 2 Treffer frei
    public static final byte BOX1 = 9;   // Kiste, noch 1 Treffer frei

    // Richtungen
    public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;

    // Zustand
    public static final int PLAYING = 0, WON = 1, DEAD = 2;

    // Ereignisse (fuer die Animation): je Ereignis EV_SIZE Ints {art, von, nach, typ, neuerWert}
    public static final int EV_FALL = 0;     // Objekt typ faellt von -> nach
    public static final int EV_ROLL = 1;     // Objekt typ rollt von -> nach
    public static final int EV_ABSORB = 2;   // Objekt bei von verschwindet in der Kiste bei nach; Kiste wird neuerWert
    public static final int EV_BOOM = 3;     // Bombe bei von und Objekt bei nach verschwinden
    public static final int EV_VANISH = 4;   // Bombe bei von verschwindet (Wand)
    public static final int EV_KILL = 5;     // Objekt bei von trifft Matt (Feld nach)
    public static final int EV_SIZE = 5;

    // ---- Spielzustand ----
    public final byte[] cell = new byte[N];
    public int mx, my;
    public int apples;
    public int steps;
    public int state;

    /** Spielfeld direkt nach Matts Schritt, vor der Physik (Ausgangspunkt der Animation). */
    public final byte[] stage = new byte[N];
    /** Ereignisse der letzten Physik-Runde. */
    public int[] events = new int[EV_SIZE * 64];
    public int eventCount;   // Anzahl Ints (nicht Ereignisse)

    private final boolean[] active = new boolean[N];
    private int lastDir = LEFT;
    private int pushedCell = -1;
    private int rollDirInRun;     // Richtung des letzten Rollens des gerade fallenden Objekts
    private Engine scratch;

    /** Gesicherter Spielzustand (fuer Undo). */
    public static final class State {
        final byte[] cell = new byte[N];
        int mx, my, apples, steps, state, lastDir;
    }

    public Engine() {
    }

    public void load(Level lv) {
        System.arraycopy(lv.cells, 0, cell, 0, N);
        mx = lv.startX;
        my = lv.startY;
        apples = lv.countApples();
        steps = 0;
        state = PLAYING;
        lastDir = LEFT;
        pushedCell = -1;
        eventCount = 0;
        Arrays.fill(active, false);
        System.arraycopy(cell, 0, stage, 0, N);
    }

    public void save(State s) {
        System.arraycopy(cell, 0, s.cell, 0, N);
        s.mx = mx;
        s.my = my;
        s.apples = apples;
        s.steps = steps;
        s.state = state;
        s.lastDir = lastDir;
    }

    public void restore(State s) {
        System.arraycopy(s.cell, 0, cell, 0, N);
        mx = s.mx;
        my = s.my;
        apples = s.apples;
        steps = s.steps;
        state = s.state;
        lastDir = s.lastDir;
        eventCount = 0;
        System.arraycopy(cell, 0, stage, 0, N);
    }

    public void copyFrom(Engine o) {
        System.arraycopy(o.cell, 0, cell, 0, N);
        mx = o.mx;
        my = o.my;
        apples = o.apples;
        steps = o.steps;
        state = o.state;
        lastDir = o.lastDir;
        eventCount = 0;
    }

    // ------------------------------------------------------------------
    public static boolean isBox(byte t) {
        return t == BOX || t == BOX2 || t == BOX1;
    }

    public static boolean isObject(byte t) {
        return t == STONE || t == BOMB || isBox(t);
    }

    /** Zustand einer Kiste nach einem Treffer (EMPTY, wenn aufgebraucht). */
    private static byte damaged(byte b) {
        if (b == BOX) return BOX2;
        if (b == BOX2) return BOX1;
        return EMPTY;
    }

    /** Wendet das Ereignis an Position off in ev auf ein Spielfeld an. */
    public static void applyEvent(byte[] board, int[] ev, int off) {
        int kind = ev[off], from = ev[off + 1], to = ev[off + 2];
        switch (kind) {
            case EV_FALL:
            case EV_ROLL:
                board[from] = EMPTY;
                board[to] = (byte) ev[off + 3];
                break;
            case EV_ABSORB:
                board[from] = EMPTY;
                board[to] = (byte) ev[off + 4];
                break;
            case EV_BOOM:
                board[from] = EMPTY;
                board[to] = EMPTY;
                break;
            case EV_VANISH:
                board[from] = EMPTY;
                break;
            default:
                break;
        }
    }

    private void event(int kind, int from, int to, int type, int value) {
        if (eventCount + EV_SIZE > events.length) events = Arrays.copyOf(events, events.length * 2);
        events[eventCount] = kind;
        events[eventCount + 1] = from;
        events[eventCount + 2] = to;
        events[eventCount + 3] = type;
        events[eventCount + 4] = value;
        eventCount += EV_SIZE;
    }

    /** Wuerde dieser Zug Matt toeten? (fuer die Option "keine dummen Zuege") */
    public boolean moveIsSafe(int dir) {
        if (scratch == null) scratch = new Engine();
        scratch.copyFrom(this);
        return !scratch.move(dir) || scratch.state != DEAD;
    }

    /** Fuehrt einen Zug aus. Liefert false, wenn der Zug nicht moeglich ist (dann zaehlt er nicht). */
    public boolean move(int dir) {
        if (state != PLAYING) return false;
        int dx = 0, dy = 0;
        switch (dir) {
            case LEFT: dx = -1; break;
            case RIGHT: dx = 1; break;
            case UP: dy = -1; break;
            case DOWN: dy = 1; break;
            default: return false;
        }
        int nx = mx + dx, ny = my + dy;
        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) return false;
        int ni = ny * COLS + nx;
        byte t = cell[ni];
        int pushed = -1;
        switch (t) {
            case EMPTY:
            case EARTH:
                break;
            case APPLE:
                apples--;
                break;
            case WALL:
                return false;
            default:
                // Stein, Bombe oder Kiste: nur waagerecht und nur ein Objekt
                if (dy != 0) return false;
                int bx = nx + dx;
                if (bx < 0 || bx >= COLS || cell[ny * COLS + bx] != EMPTY) return false;
                cell[ny * COLS + bx] = t;
                pushed = ny * COLS + bx;
                break;
        }
        // Zug ausfuehren
        int oldX = mx, oldY = my;
        cell[ni] = EMPTY;
        mx = nx;
        my = ny;
        steps++;
        lastDir = dir;
        eventCount = 0;
        System.arraycopy(cell, 0, stage, 0, N);
        if (apples == 0) {
            state = WON;
            return true;
        }
        pushedCell = pushed;
        Arrays.fill(active, false);
        wake(oldY - 1, oldX);
        if (pushed >= 0) active[pushed] = true;
        settle();
        pushedCell = -1;
        return true;
    }

    // ------------------------------------------------------------------
    private void wake(int r, int c) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) active[r * COLS + c] = true;
    }

    private boolean free(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS && cell[r * COLS + c] == EMPTY
                && !(r == my && c == mx);
    }

    private void settle() {
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            for (int r = ROWS - 2; r >= 0; r--) {
                for (int c = 0; c < COLS; c++) {
                    int i = r * COLS + c;
                    if (active[i] && isObject(cell[i])) {
                        active[i] = false;
                        if (fall(r, c)) changed = true;
                        if (state == DEAD) return;
                    }
                }
            }
            // Sicherung gegen Endlosschleifen (in den Originalleveln nie noetig)
            if (++guard > 5000) return;
        } while (changed);
    }

    /**
     * Laesst das Objekt bei (r, c) so weit fallen/rollen, wie es geht.
     * Liefert true, wenn sich etwas veraendert hat.
     */
    private boolean fall(int r, int c) {
        byte t = cell[r * COLS + c];
        boolean pushedHere = (r * COLS + c) == pushedCell;
        boolean fell = false, moved = false;
        rollDirInRun = 0;
        while (r + 1 < ROWS) {
            int here = r * COLS + c;
            int below = (r + 1) * COLS + c;
            if (r + 1 == my && c == mx) {
                if (fell) {
                    event(EV_KILL, here, below, t, 0);
                    state = DEAD;
                    return true;
                }
                break;
            }
            byte b = cell[below];
            if (b == EMPTY) {
                cell[here] = EMPTY;
                cell[below] = t;
                event(EV_FALL, here, below, t, 0);
                wake(r - 1, c);
                r++;
                fell = true;
                pushedHere = false;
                moved = true;
                continue;
            }
            // in eine Kiste gefallen oder geschoben: Objekt verschwindet, Kiste nimmt Schaden
            if ((fell || pushedHere) && isBox(b)) {
                cell[here] = EMPTY;
                cell[below] = damaged(b);
                event(EV_ABSORB, here, below, t, cell[below]);
                wake(r - 1, c);
                return true;
            }
            if (fell && t == BOMB) {
                if (b == STONE || b == BOMB) {
                    cell[here] = EMPTY;
                    cell[below] = EMPTY;
                    event(EV_BOOM, here, below, t, 0);
                    wake(r - 1, c);
                    return true;
                }
                if (b == WALL) {
                    cell[here] = EMPTY;
                    event(EV_VANISH, here, below, t, 0);
                    wake(r - 1, c);
                    return true;
                }
            }
            if (fell && b == STONE && t != BOMB) {
                int first = rollDirInRun != 0 ? rollDirInRun : (lastDir == RIGHT ? 1 : -1);
                int dc = 0;
                if (canRoll(r, c + first)) dc = first;
                else if (canRoll(r, c - first)) dc = -first;
                if (dc != 0) {
                    cell[here] = EMPTY;
                    cell[r * COLS + c + dc] = t;
                    event(EV_ROLL, here, r * COLS + c + dc, t, 0);
                    wake(r - 1, c);
                    c += dc;
                    rollDirInRun = dc;
                    moved = true;
                    continue;
                }
            }
            break;
        }
        return moved;
    }

    /** Kann ein Objekt auf Hoehe r nach Spalte cc rollen? Feld frei und darunter frei oder eine Kiste. */
    private boolean canRoll(int r, int cc) {
        if (!free(r, cc)) return false;
        if (free(r + 1, cc)) return true;
        return r + 1 < ROWS && isBox(cell[(r + 1) * COLS + cc]);
    }
}
