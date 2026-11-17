package de.mrmatt.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Merkt sich geloeste Level (SharedPreferences). */
public final class Progress {
    private static final String PREFS = "progress";

    private Progress() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** level ist 0-basiert. */
    public static boolean isSolved(Context c, String file, int level) {
        String s = prefs(c).getString(file, "");
        return s.indexOf("," + (level + 1) + ",") >= 0;
    }

    public static int solvedCount(Context c, String file) {
        String s = prefs(c).getString(file, "");
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') n++;
        }
        return n > 0 ? n - 1 : 0;
    }

    public static int bestSteps(Context c, String file, int level) {
        return prefs(c).getInt(file + "#" + level, 0);
    }

    /** Traegt ein geloestes Level ein (level 0-basiert) und merkt sich die beste Schrittzahl. */
    public static void markSolved(Context c, String file, int level, int steps) {
        SharedPreferences p = prefs(c);
        String s = p.getString(file, ",");
        if (s.length() == 0) s = ",";
        SharedPreferences.Editor e = p.edit();
        if (s.indexOf("," + (level + 1) + ",") < 0) e.putString(file, s + (level + 1) + ",");
        int best = p.getInt(file + "#" + level, 0);
        if (best == 0 || steps < best) e.putInt(file + "#" + level, steps);
        e.commit();
    }
}
