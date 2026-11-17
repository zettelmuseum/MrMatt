# Mr. Matt – Rekonstruktion für Android (AIDE, Java 7)

Neuimplementierung des alten Shareware-Spiels *Mr. Matt* (J. Andrzej Wrotniak). Die Spiellogik wurde
aus den Level-Dateien (`.mat`) und den Lösungsreplays (`mrmatt.sol`) rekonstruiert.

**Prüfstand:** Alle **3885 Lösungsreplays** der 767 mitgelieferten Spiele werden von der Engine
fehlerfrei bis zum Sieg abgespielt (auch die Schrittzahlen stimmen). Das Kriterium „Engine korrekt =
jedes Replay gewinnt“ ist damit erfüllt:

```
$ tools/run.sh Verify assets
ok=3885 bad=0 {}
```

## In AIDE öffnen

1. ZIP entpacken, den Ordner `MrMatt` z. B. nach `/sdcard/AppProjects/` kopieren.
2. In AIDE: *Open Project* → `MrMatt` (enthält `AndroidManifest.xml`, `src/`, `res/`, `assets/`).
3. *Build & Run*. Voraussetzungen: Java 7, Android-API ≥ 14 (`project.properties`: `target=android-19`),
   keine Support-Bibliotheken, kein Gradle nötig.

Die App-Oberfläche wurde ohne Android-SDK entwickelt: Sie ist gegen Stubs der verwendeten Android-API
typgeprüft, aber **nicht auf einem Gerät ausprobiert**. Engine, Undo und Replay-Wiedergabe laufen dagegen
vollständig getestet auf der JVM.

## Bedienung

* **Startseite:** Spieleliste mit Suche und Filter nach Schwierigkeit (1–4 Sterne), dann Levelauswahl.
  Gelöste Level werden grün markiert (mit bester Schrittzahl).
* **Spielfeld:** auf dem Feld wischen/ziehen, in Matts Zeile oder Spalte tippen (Matt rennt dorthin),
  Steuerkreuz unten rechts, Pfeiltasten oder WASD. Home/Ende/Bild↑/Bild↓ laufen bis zum Hindernis.
* **Zurück** (Backspace): bis zu 200 Schritte, auch nach dem Tod. **Neustart:** Leertaste/Esc.
* **Lösung:** spielt die kürzeste gespeicherte Lösung ab (Pause, Tempo 1x … max im Menü).
  Ein eigener Zug beendet die Wiedergabe; mit Lösungshilfe gespielte Level zählen nicht als gelöst.
* **Menü:** nächstes/voriges Level, „Keine dummen Züge“ (verweigert Züge, die Matt töten), Animation,
  Steuerkreuz ein/aus, Spielregeln.

## Rekonstruierte Spielregeln

| Element | Verhalten |
|---|---|
| Matt | läuft in leere Felder, gräbt Erde, frisst Äpfel; schiebt **ein** Objekt waagerecht, wenn dahinter Platz ist; kein senkrechtes Schieben; unmögliche Züge zählen nicht |
| Sieg | sobald der letzte Apfel gefressen ist (sofort, noch vor der Physik) |
| Physik | nach jedem Zug läuft alles komplett durch; Objekte werden von unten nach oben, je Zeile von links nach rechts abgearbeitet, jedes fällt dabei vollständig; wiederholt bis Ruhe |
| Fallen | Stein, Bombe, Kiste fallen, solange darunter frei ist; fällt etwas auf Matt, stirbt er (liegende Objekte über ihm sind harmlos) |
| Rollen | ein *gefallenes* Objekt (Stein/Kiste), das auf einem **Stein** landet, rollt seitlich, wenn dort frei ist und darunter frei oder eine Kiste ist |
| Rollrichtung | rollte das Objekt in diesem Durchgang schon, rollt es in dieselbe Richtung weiter; sonst in Richtung von Matts letztem Zug; ist die Seite blockiert, wird die andere versucht |
| Kiste | verschluckt jedes hineinfallende **oder hineingeschobene** Objekt (auch Bomben/Kisten); nach 3 Treffern verschwindet sie |
| Bombe | fällt sie auf Stein/Bombe, verschwinden beide; auf Wand verschwindet sie allein; auf Erde/Boden nichts; eine geschobene Bombe explodiert nicht |
| Weckregel | nur Objekte, die „geweckt“ wurden, werden bewegt (über einem frei gewordenen Feld, geschobenes Objekt). Anfangs schwebende Steine fallen daher erst, wenn darunter etwas frei wird |

Die Rollrichtung war der schwierigste Teil: Mit fester „links zuerst“-Regel gewannen nur 3346 Replays,
„Richtung zu Matt“ brachte 3514, „Richtung von Matts letztem Zug“ 3719, und erst die zusätzliche
Schwung-Regel pro Objekt ergab 3885/3885.

### Was die Replays nicht belegen (Annahmen)

* Nach senkrechten Zügen bevorzugt die Engine links; die Replays unterscheiden das nicht.
* Mehrere Objekte hintereinander schieben und Objekte senkrecht schieben kommen in keinem Replay vor
  (blockierte Züge werden nicht aufgezeichnet). Hier gilt die Hilfedatei: genau ein Objekt, nur seitlich.
* Rollen von Kisten oder Bomben ab Kisten ist unerreichbar (Kisten verschlucken landende Objekte).

## Projektaufbau

```
AndroidManifest.xml, project.properties, res/    Android-Projekt (klassisches AIDE/ADT-Layout)
assets/mat/g001..g767.mat                        Level-Dateien (Dateinamen vereinfacht, Titel stehen in index.txt)
assets/index.txt, mdecrypt.txt, mrmatt.sol       Spieleliste, Entschlüsselungstabelle, Lösungsreplays
src/de/mrmatt/game/                              reine Java-Logik (ohne Android)
    Engine.java        Regeln + Ereignisliste für die Animation + Snapshots
    GameSession.java   Engine + Undo (200) + Lösungswiedergabe
    GameParser.java    .mat lesen und entschlüsseln (Klartext-Dateien werden erkannt)
    Replay.java        .sol lesen, RLE dekodieren
    Level.java, Game.java
src/de/mrmatt/app/                               Android-Oberfläche
    GameListActivity, LevelListActivity, PlayActivity
    BoardView (Spielfeld, Animation, Wischen), DpadView, Tiles (Kacheln per Canvas)
    Repository, Progress, GameInfo, Ui
tools/                                           JVM-Werkzeuge (kein Android nötig)
    run.sh      kompiliert und startet:  tools/run.sh Verify assets
    Verify      alle Replays prüfen (+ Animationsereignisse + Schrittzahlen)
    SessionTest Undo/Replay-Sitzung prüfen
    Dbg         Brettzustände eines Replays ausgeben:  tools/run.sh Dbg assets "Marks" 1 90 93
    MakeIndex   assets/index.txt neu erzeugen
```

## Dateiformate (kurz)

* **.mat:** Kopfzeilen (Titel mit Sternen, Autor, Code), dann je Level Name + 18 Zeilen × 31 Zeichen.
  Entschlüsselt wird zeichenweise über `mdecrypt.txt` (7 Feldtypen × 18 × 31); passt ein Zeichen zu
  keinem Typ, steht dort Matt. Einige Dateien liegen im Klartext vor.
* **.sol:** `<Level><A–D> <Schritte> <Spieler>|<Spiel>|<RLE-Züge> <Prüfzahl>`, Züge `L R U D`.
