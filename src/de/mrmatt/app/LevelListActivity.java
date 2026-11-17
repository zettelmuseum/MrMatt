package de.mrmatt.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.mrmatt.game.Game;

/** Levelauswahl eines Spiels. */
public class LevelListActivity extends Activity {

    private Game game;
    private String file;
    private BaseAdapter adapter;
    private TextView sub;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        file = getIntent().getStringExtra("file");
        try {
            game = Repository.loadGame(this, file);
        } catch (Exception e) {
            Toast.makeText(this, "Spiel nicht lesbar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 12);
        root.setPadding(pad, pad, pad, 0);

        TextView title = new TextView(this);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < game.stars; i++) stars.append('\u2605');
        title.setText(stars + "  " + game.title);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Ui.ACCENT);
        root.addView(title);

        sub = new TextView(this);
        sub.setTextColor(Ui.TEXT_DIM);
        sub.setTextSize(13);
        sub.setPadding(0, 0, 0, pad);
        root.addView(sub);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(this, 92));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(Ui.dp(this, 6));
        grid.setVerticalSpacing(Ui.dp(this, 6));
        adapter = new LevelAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Intent i = new Intent(LevelListActivity.this, PlayActivity.class);
                i.putExtra("file", file);
                i.putExtra("level", pos);
                startActivity(i);
            }
        });
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        updateSub();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            updateSub();
        }
    }

    private void updateSub() {
        sub.setText(game.author + "  \u00b7  gel\u00f6st " + Progress.solvedCount(this, file) + "/" + game.levels.size());
    }

    private final class LevelAdapter extends BaseAdapter {
        public int getCount() {
            return game.levels.size();
        }

        public Object getItem(int i) {
            return game.levels.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout cell;
            TextView num, name;
            if (convert == null) {
                cell = new LinearLayout(LevelListActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setMinimumHeight(Ui.dp(LevelListActivity.this, 66));
                int p = Ui.dp(LevelListActivity.this, 4);
                cell.setPadding(p, p, p, p);
                num = new TextView(LevelListActivity.this);
                num.setTextSize(22);
                num.setTypeface(Typeface.DEFAULT_BOLD);
                num.setTextColor(Ui.TEXT);
                num.setGravity(Gravity.CENTER);
                name = new TextView(LevelListActivity.this);
                name.setTextSize(10);
                name.setTextColor(Ui.TEXT_DIM);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                name.setGravity(Gravity.CENTER);
                cell.addView(num);
                cell.addView(name);
                cell.setTag(new TextView[] {num, name});
            } else {
                cell = (LinearLayout) convert;
                TextView[] t = (TextView[]) cell.getTag();
                num = t[0];
                name = t[1];
            }
            boolean solved = Progress.isSolved(LevelListActivity.this, file, pos);
            cell.setBackgroundColor(solved ? Ui.GOOD : Ui.NEUTRAL);
            num.setText(String.valueOf(pos + 1));
            int best = Progress.bestSteps(LevelListActivity.this, file, pos);
            String n = game.levels.get(pos).name;
            name.setText(solved && best > 0 ? best + " Schritte" : n);
            return cell;
        }
    }
}
