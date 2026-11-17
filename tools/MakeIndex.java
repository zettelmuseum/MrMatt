import de.mrmatt.game.*;

import java.io.*;
import java.util.*;

/** Erzeugt assets/index.txt: Datei TAB Titel TAB Sterne TAB Autor TAB Levelzahl.  Aufruf: java MakeIndex <datadir> <out> */
public class MakeIndex {
    public static void main(String[] args) throws Exception {
        String dir = args[0];
        GameParser gp = new GameParser(GameParser.readAll(new FileInputStream(dir + "/mdecrypt.txt")));
        File[] files = new File(dir + "/mat").listFiles();
        Arrays.sort(files);
        Writer w = new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8");
        int n = 0;
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().endsWith(".mat")) continue;
            Game g = gp.parse(files[i].getName(), GameParser.readAll(new FileInputStream(files[i])));
            w.write(files[i].getName() + "\t" + g.title.replace('\t', ' ') + "\t" + g.stars + "\t"
                    + g.author.replace('\t', ' ') + "\t" + g.levels.size() + "\n");
            n++;
        }
        w.close();
        System.out.println(n + " Spiele");
    }
}
