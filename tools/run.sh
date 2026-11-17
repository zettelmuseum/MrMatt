#!/bin/bash
# Kompiliert Engine + Hilfsprogramme (ohne Android) und startet eine Klasse.
#   tools/run.sh Verify assets                 alle Loesungsreplays gegen die Engine pruefen
#   tools/run.sh SessionTest assets            Undo/Replay-Sitzung pruefen
#   tools/run.sh Dbg assets "Annelie" 1 0 20   Brettzustaende eines Replays ausgeben
#   tools/run.sh MakeIndex assets assets/index.txt
# Optionen fuer Verify: -Donly=Spielname_L<n> (Leerzeichen als _), -Dokfile=datei
HERE="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/mrmatt-build"
mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -d "$OUT" "$HERE"/src/de/mrmatt/game/*.java "$HERE"/tools/*.java || exit 1
java $JOPTS -cp "$OUT" "$@"
