package de.mrmatt.app;

import android.content.Context;

/** Kleine Hilfsfunktionen fuer die programmatisch gebauten Oberflaechen. */
final class Ui {
    static final int BG = 0xFF0D1B2A;
    static final int BAR = 0xFF1B263B;
    static final int TEXT = 0xFFE0E1DD;
    static final int TEXT_DIM = 0xFF9DB4C0;
    static final int ACCENT = 0xFFFFD54F;
    static final int GOOD = 0xFF2E7D32;
    static final int NEUTRAL = 0xFF37474F;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
