package de.mrmatt.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.mrmatt.game.Engine;
import de.mrmatt.game.Game;
import de.mrmatt.game.GameSession;
import de.mrmatt.game.Replay;

/**
 * Der Spielbildschirm. Steuerung: Wischen/Ziehen auf dem Feld, Tippen in Matts Zeile oder
 * Spalte (Matt rennt dorthin), Steuerkreuz, Pfeiltasten/WASD, Backspace = Zurueck.
 */
public class PlayActivity extends Activity implements BoardView.Listener, DpadView.Listener {

    private static final int[] SPEED_MS = {500, 250, 120, 60, 0};
    private static final String[] SPEED_NAMES = {"1x", "2x", "4x", "8x", "max"};

    private Game game;
    private String file;
    private int levelIndex;
    private final GameSession session = new GameSession();
    private final Handler handler = new Handler();

    private BoardView board;
    private DpadView dpad;
    private TextView hud;

    private boolean animating;
    private int queuedDir = -1;
    private int runDir = -1, runCol = -1, runRow = -1;
    private boolean replayPlaying;
    private boolean usedReplay;
    private boolean finished;

    // Einstellungen
    private boolean animationOn = true;
    private boolean showDpad = true;
    private int speedIdx = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        file = getIntent().getStringExtra("file");
        levelIndex = getIntent().getIntExtra("level", 0);
        try {
            game = Repository.loadGame(this, file);
        } catch (Exception e) {
            Toast.makeText(this, "Spiel nicht lesbar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
        session.safeMode = sp.getBoolean("safe", false);
        animationOn = sp.getBoolean("anim", true);
        showDpad = sp.getBoolean("dpad", true);
        speedIdx = sp.getInt("speed", 1);
        if (levelIndex < 0 || levelIndex >= game.levels.size()) levelIndex = 0;

        buildUi();
        startLevel(levelIndex);
    }

    private void saveSettings() {
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putBoolean("safe", session.safeMode)
                .putBoolean("anim", animationOn)
                .putBoolean("dpad", showDpad)
                .putInt("speed", speedIdx)
                .commit();
    }

    // ------------------------------------------------------------------ UI

    private Button makeButton(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        b.setFocusable(false);
        b.setOnClickListener(l);
        return b;
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Ui.BAR);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 4), 0);

        hud = new TextView(this);
        hud.setTextSize(13);
        hud.setTextColor(Ui.TEXT);
        hud.setSingleLine(true);
        hud.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(hud, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(makeButton("Zur\u00fcck", new View.OnClickListener() {
            public void onClick(View v) {
                undo();
            }
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(makeButton("Neustart", new View.OnClickListener() {
            public void onClick(View v) {
                startLevel(levelIndex);
            }
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(makeButton("L\u00f6sung", new View.OnClickListener() {
            public void onClick(View v) {
                toggleReplay();
            }
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(makeButton("Men\u00fc", new View.OnClickListener() {
            public void onClick(View v) {
                showMenu();
            }
        }), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        board = new BoardView(this);
        board.setListener(this);

        col.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 40)));
        col.addView(board, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        dpad = new DpadView(this);
        dpad.setListener(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(this, 150), Ui.dp(this, 150),
                Gravity.BOTTOM | Gravity.RIGHT);
        lp.setMargins(0, 0, Ui.dp(this, 10), Ui.dp(this, 10));
        root.addView(dpad, lp);
        dpad.setVisibility(showDpad ? View.VISIBLE : View.GONE);

        setContentView(root);
    }

    // ------------------------------------------------------------------ Ablauf

    private void startLevel(int idx) {
        handler.removeCallbacks(replayRun);
        board.finishNow();
        levelIndex = idx;
        session.start(game.levels.get(idx));
        animating = false;
        queuedDir = -1;
        runDir = -1;
        replayPlaying = false;
        usedReplay = false;
        finished = false;
        showBoard();
        updateHud();
    }

    private void showBoard() {
        Engine e = session.engine;
        board.show(e.cell, e.mx, e.my, e.state == Engine.DEAD);
    }

    private void updateHud() {
        Engine e = session.engine;
        StringBuilder sb = new StringBuilder();
        sb.append("Level ").append(levelIndex + 1).append('/').append(game.levels.size());
        sb.append("  ").append(session.level().name);
        sb.append("   \u00c4pfel ").append(e.apples);
        sb.append("   Schritte ").append(e.steps);
        if (session.hasReplay()) {
            sb.append("   L\u00f6sung ").append(session.replayPosition()).append('/').append(session.replayLength());
            if (!replayPlaying && !session.replayFinished()) sb.append(" (Pause)");
        }
        if (e.state == Engine.DEAD) sb.append("   \u2013 Matt wurde getroffen!");
        hud.setText(sb.toString());
        hud.setTextColor(e.state == Engine.DEAD ? 0xFFFF8A80 : (e.state == Engine.WON ? 0xFFA5D6A7 : Ui.TEXT));
    }

    /** Eingabe des Spielers (Richtung). */
    private void input(int dir) {
        if (replayPlaying) {
            pauseReplay();
            return;
        }
        runDir = -1;
        userMove(dir);
    }

    private void userMove(int dir) {
        if (finished) return;
        if (animating) {
            queuedDir = dir;
            return;
        }
        doMove(dir);
    }

    private boolean doMove(int dir) {
        if (!session.move(dir)) {
            runDir = -1;
            return false;
        }
        afterMove();
        return true;
    }

    private void afterMove() {
        Engine e = session.engine;
        updateHud();
        if (animationOn && e.eventCount > 0) {
            animating = true;
            board.play(e.stage, e.events, e.eventCount, e.cell, e.mx, e.my, e.state == Engine.DEAD, animDone);
        } else {
            board.show(e.cell, e.mx, e.my, e.state == Engine.DEAD);
            onSettled();
        }
    }

    private final Runnable animDone = new Runnable() {
        public void run() {
            animating = false;
            onSettled();
        }
    };

    private void onSettled() {
        Engine e = session.engine;
        updateHud();
        if (e.state == Engine.WON) {
            handleWin();
            return;
        }
        if (e.state == Engine.DEAD) {
            runDir = -1;
            queuedDir = -1;
            replayPlaying = false;
            return;
        }
        if (replayPlaying) {
            scheduleReplayStep();
            return;
        }
        if (queuedDir >= 0) {
            int d = queuedDir;
            queuedDir = -1;
            doMove(d);
            return;
        }
        if (runDir >= 0) continueRun();
    }

    // ---- Laufen (Tippen / Home / End ...)

    private void startRun(int dir, int col, int row) {
        if (replayPlaying) {
            pauseReplay();
            return;
        }
        runDir = dir;
        runCol = col;
        runRow = row;
        queuedDir = -1;
        if (!animating) continueRun();
    }

    private void continueRun() {
        Engine e = session.engine;
        if (runDir < 0 || finished) return;
        if (runCol >= 0 && e.mx == runCol && e.my == runRow) {
            runDir = -1;
            return;
        }
        if (!doMove(runDir)) runDir = -1;
    }

    // ---- Zurueck / Neustart

    private void undo() {
        if (replayPlaying) pauseReplay();
        board.finishNow();
        animating = false;
        queuedDir = -1;
        runDir = -1;
        if (session.undo()) {
            finished = false;
            showBoard();
            updateHud();
        }
    }

    // ---- Loesungswiedergabe

    private void toggleReplay() {
        if (replayPlaying) {
            pauseReplay();
        } else if (session.hasReplay() && !session.replayFinished()) {
            resumeReplay();
        } else {
            startReplay();
        }
    }

    private void startReplay() {
        Replay r = Repository.bestReplay(this, game, levelIndex);
        if (r == null) {
            Toast.makeText(this, "F\u00fcr dieses Level ist keine L\u00f6sung gespeichert.", Toast.LENGTH_SHORT).show();
            return;
        }
        startLevel(levelIndex);
        session.startReplay(r);
        usedReplay = true;
        Toast.makeText(this, "L\u00f6sung von " + r.player + " (" + r.steps + " Schritte)", Toast.LENGTH_SHORT).show();
        resumeReplay();
    }

    private void resumeReplay() {
        replayPlaying = true;
        runDir = -1;
        queuedDir = -1;
        updateHud();
        if (!animating) scheduleReplayStep();
    }

    private void pauseReplay() {
        replayPlaying = false;
        handler.removeCallbacks(replayRun);
        updateHud();
    }

    private void scheduleReplayStep() {
        handler.removeCallbacks(replayRun);
        handler.postDelayed(replayRun, SPEED_MS[speedIdx]);
    }

    private final Runnable replayRun = new Runnable() {
        public void run() {
            if (!replayPlaying) return;
            if (animating) {
                handler.postDelayed(this, 30);
                return;
            }
            int d = session.replayStep();
            if (d < 0) {
                replayPlaying = false;
                updateHud();
                return;
            }
            afterMove();
        }
    };

    // ---- Gewonnen

    private void handleWin() {
        finished = true;
        runDir = -1;
        queuedDir = -1;
        replayPlaying = false;
        Engine e = session.engine;
        if (!usedReplay) Progress.markSolved(this, file, levelIndex, e.steps);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(usedReplay ? "L\u00f6sung abgespielt" : "Level geschafft!");
        b.setMessage(usedReplay ? "Die gespeicherte L\u00f6sung wurde komplett abgespielt (" + e.steps + " Schritte)."
                : "Du hast alle \u00c4pfel in " + e.steps + " Schritten gegessen.");
        if (levelIndex + 1 < game.levels.size()) {
            b.setPositiveButton("N\u00e4chstes Level", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    startLevel(levelIndex + 1);
                }
            });
        }
        b.setNegativeButton("Nochmal", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                startLevel(levelIndex);
            }
        });
        b.setNeutralButton("Level-Auswahl", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                finish();
            }
        });
        b.show();
    }

    // ---- Menue

    private void showMenu() {
        final String replayItem = replayPlaying ? "L\u00f6sung pausieren"
                : (session.hasReplay() && !session.replayFinished() ? "L\u00f6sung fortsetzen" : "L\u00f6sung abspielen");
        final String[] items = {
                "N\u00e4chstes Level",
                "Vorheriges Level",
                "Level-Auswahl",
                replayItem,
                "Tempo der L\u00f6sung: " + SPEED_NAMES[speedIdx],
                (session.safeMode ? "\u2713 " : "") + "Keine dummen Z\u00fcge",
                (animationOn ? "\u2713 " : "") + "Animation",
                (showDpad ? "\u2713 " : "") + "Steuerkreuz",
                "Spielregeln"
        };
        new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                switch (which) {
                    case 0:
                        if (levelIndex + 1 < game.levels.size()) startLevel(levelIndex + 1);
                        break;
                    case 1:
                        if (levelIndex > 0) startLevel(levelIndex - 1);
                        break;
                    case 2:
                        finish();
                        break;
                    case 3:
                        toggleReplay();
                        break;
                    case 4:
                        speedIdx = (speedIdx + 1) % SPEED_MS.length;
                        saveSettings();
                        Toast.makeText(PlayActivity.this, "Tempo " + SPEED_NAMES[speedIdx], Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        session.safeMode = !session.safeMode;
                        saveSettings();
                        break;
                    case 6:
                        animationOn = !animationOn;
                        saveSettings();
                        break;
                    case 7:
                        showDpad = !showDpad;
                        dpad.setVisibility(showDpad ? View.VISIBLE : View.GONE);
                        saveSettings();
                        break;
                    default:
                        showHelp();
                        break;
                }
            }
        }).show();
    }

    private void showHelp() {
        String text = "Ziel: alle \u00c4pfel essen.\n\n"
                + "Matt l\u00e4uft in leere Felder, gr\u00e4bt Erde und frisst \u00c4pfel. Er schiebt einen "
                + "einzelnen Stein, eine Bombe oder eine Kiste seitlich, wenn dahinter Platz ist - nie nach oben oder unten.\n\n"
                + "Steine, Bomben und Kisten fallen, sobald darunter Platz ist, und rollen von Steinen seitlich ab. "
                + "Ein fallendes Objekt t\u00f6tet Matt. W\u00e4hrend etwas f\u00e4llt, kann er sich nicht bewegen.\n\n"
                + "Eine Kiste verschluckt drei fallende oder hineingeschobene Objekte und verschwindet dann. "
                + "Eine Bombe zerst\u00f6rt beim Aufprall den Stein (oder die Bombe) unter sich und sich selbst; "
                + "auf einer Mauer explodiert sie allein.\n\n"
                + "Steuerung: Wischen auf dem Feld, in Matts Zeile/Spalte tippen (er rennt dorthin), Steuerkreuz "
                + "oder Pfeiltasten/WASD. Zur\u00fcck nimmt bis zu 200 Schritte zur\u00fcck (Backspace). "
                + "Home/Ende/Bild auf/Bild ab laufen bis zum Hindernis.";
        new AlertDialog.Builder(this).setTitle("Spielregeln").setMessage(text)
                .setPositiveButton("OK", null).show();
    }

    // ------------------------------------------------------------------ Eingabe

    public void onSwipe(int dir) {
        input(dir);
    }

    public void onTap(int col, int row) {
        Engine e = session.engine;
        int dx = col - e.mx, dy = row - e.my;
        if (dx == 0 && dy == 0) return;
        if (dx == 0 || dy == 0) {
            int dir = dx != 0 ? (dx < 0 ? Engine.LEFT : Engine.RIGHT) : (dy < 0 ? Engine.UP : Engine.DOWN);
            startRun(dir, col, row);
        } else if (Math.abs(dx) >= Math.abs(dy)) {
            input(dx < 0 ? Engine.LEFT : Engine.RIGHT);
        } else {
            input(dy < 0 ? Engine.UP : Engine.DOWN);
        }
    }

    public void onDir(int dir) {
        input(dir);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                input(Engine.LEFT);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                input(Engine.RIGHT);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                input(Engine.UP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                input(Engine.DOWN);
                return true;
            case KeyEvent.KEYCODE_MOVE_HOME:
                startRun(Engine.LEFT, -1, -1);
                return true;
            case KeyEvent.KEYCODE_MOVE_END:
                startRun(Engine.RIGHT, -1, -1);
                return true;
            case KeyEvent.KEYCODE_PAGE_UP:
                startRun(Engine.UP, -1, -1);
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                startRun(Engine.DOWN, -1, -1);
                return true;
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_U:
                undo();
                return true;
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_ESCAPE:
                startLevel(levelIndex);
                return true;
            case KeyEvent.KEYCODE_MENU:
                showMenu();
                return true;
            default:
                return super.onKeyDown(keyCode, ev);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(replayRun);
        replayPlaying = false;
        queuedDir = -1;
        runDir = -1;
        if (board != null) board.finishNow();
        animating = false;
    }
}
