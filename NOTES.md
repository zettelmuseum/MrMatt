# Mr. Matt – Reconstruction for Android (AIDE, Java 7)

Reimplementation of the old shareware game *Mr. Matt* (J. Andrzej Wrotniak). The game logic was reconstructed from the level files (`.mat`) and solution replays (`mrmatt.sol`).

**Verification status:** All **3,885 solution replays** for the 767 included games are played by the engine without errors, all the way to victory (the move counts also match). This satisfies the criterion “engine is correct = every replay wins”:

```
$ tools/run.sh Verify assets
ok=3885 bad=0 {}
```

## Open in AIDE

1. Extract the ZIP archive and copy the `MrMatt` folder, for example, to `/sdcard/AppProjects/`.
2. In AIDE: *Open Project* → `MrMatt` (contains `AndroidManifest.xml`, `src/`, `res/`, and `assets/`).
3. *Build & Run*. Requirements: Java 7, Android API ≥ 14 (`project.properties`: `target=android-19`), no support libraries, and no Gradle required.

The app interface was developed without the Android SDK: it was type-checked against stubs of the Android APIs used, but **has not been tested on a device**. The engine, undo, and replay playback, on the other hand, have been fully tested on the JVM.

## Controls

* **Home screen:** Game list with search and difficulty filter (1–4 stars), followed by level selection. Solved levels are marked in green (with the best move count).
* **Game board:** Swipe/drag on the board; tap in Matt's row or column (Matt runs there); use the D-pad at the bottom right, arrow keys, or WASD. Home/End/Page Up/Page Down move until an obstacle is reached.
* **Undo** (Backspace): up to 200 moves, including after death. **Restart:** Space/Esc.
* **Solution:** Plays the shortest stored solution (pause and speed from 1x up to maximum in the menu). Any move of your own ends playback; levels played with the solution aid do not count as solved.
* **Menu:** Next/previous level, “No stupid moves” (rejects moves that would kill Matt), animation, D-pad on/off, game rules.

## Reconstructed game rules

| Element | Behavior |
|---|---|
| Matt | Moves into empty cells, digs dirt, eats apples; pushes **one** object horizontally if there is room behind it; cannot push vertically; impossible moves do not count |
| Victory | As soon as the last apple is eaten (immediately, before physics) |
| Physics | After every move, all physics are processed completely; objects are processed from bottom to top, and from left to right within each row, with each object falling all the way; this is repeated until everything is at rest |
| Falling | Rocks, bombs, and crates fall while the cell below is empty; if an object falls onto Matt, he dies (objects resting above him are harmless) |
| Rolling | An object (rock/crate) that has *fallen* and lands on a **rock** rolls sideways if that side is clear and the cell below is empty or contains a crate |
| Rolling direction | If the object has already rolled during this pass, it continues in the same direction; otherwise, it rolls toward the direction of Matt's last move; if that side is blocked, the other side is tried |
| Crate | Swallows any object that falls or is pushed **into it** (including bombs/crates); disappears after 3 hits |
| Bomb | If it falls onto a rock/bomb, both disappear; on a wall, only the bomb disappears; on dirt/floor, nothing happens; a pushed bomb does not explode |
| Wake-up rule | Only objects that have been “woken up” are moved (above a cell that has become empty, or an object that has been pushed). Therefore, rocks that are floating at the beginning only fall once something below them becomes empty |

Rolling direction was the most difficult part: with a fixed “left first” rule, only 3,346 replays won; “direction toward Matt” brought this to 3,514; “direction of Matt's last move” to 3,719; only the additional momentum rule per object achieved 3,885/3,885.

### What the replays do not establish (assumptions)

* After vertical moves, the engine prefers left; the replays do not distinguish this.
* Pushing several objects in a row and pushing objects vertically do not occur in any replay (blocked moves are not recorded). The help file is followed here: exactly one object, horizontally only.
* Rolling of crates or bombs off crates is unreachable (crates swallow objects that land on them).

## Project structure

```
AndroidManifest.xml, project.properties, res/    Android project (classic AIDE/ADT layout)
assets/mat/g001..g767.mat                        Level files (simplified filenames; titles are in index.txt)
assets/index.txt, mdecrypt.txt, mrmatt.sol       Game list, decryption table, solution replays
src/de/mrmatt/game/                              Pure Java logic (no Android dependency)
    Engine.java        Rules + event list for animation + snapshots
    GameSession.java   Engine + undo (200) + solution playback
    GameParser.java    Read and decrypt .mat files (plain-text files are detected)
    Replay.java        Read .sol files, decode RLE
    Level.java, Game.java
src/de/mrmatt/app/                               Android interface
    GameListActivity, LevelListActivity, PlayActivity
    BoardView (board, animation, swiping), DpadView, Tiles (Canvas-based tiles)
    Repository, Progress, GameInfo, Ui
tools/                                           JVM tools (no Android required)
    run.sh      Compiles and runs:  tools/run.sh Verify assets
    Verify      Verify all replays (+ animation events + move counts)
    SessionTest Test undo/replay sessions
    Dbg         Print replay board states:  tools/run.sh Dbg assets "Marks" 1 90 93
    MakeIndex   Regenerate assets/index.txt
```

## File formats (brief)

* **.mat:** Header lines (title with stars, author, code), followed by each level's name + 18 rows × 31 characters. Decryption is performed character by character using `mdecrypt.txt` (7 field types × 18 × 31); if a character does not match any type, it represents Matt. Some files are already plain text.
* **.sol:** `<Level><A–D> <Moves> <Player>|<Game>|<RLE moves> <Checksum>`, moves `L R U D`.
