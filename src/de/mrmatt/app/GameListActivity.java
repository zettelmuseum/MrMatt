package de.mrmatt.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Startbildschirm: Liste aller Spiele (mit Suche und Filter nach Schwierigkeit). */
public class GameListActivity extends Activity {

    private List<GameInfo> all = new ArrayList<GameInfo>();
    private final List<GameInfo> shown = new ArrayList<GameInfo>();
    private EditText search;
    private Button starButton;
    private TextView countView;
    private int starFilter;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            all = new ArrayList<GameInfo>(Repository.index(this));
        } catch (IOException e) {
            Toast.makeText(this, "Spieleliste nicht lesbar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        Collections.sort(all, new Comparator<GameInfo>() {
            public int compare(GameInfo a, GameInfo b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 12);
        root.setPadding(pad, pad, pad, 0);

        TextView title = new TextView(this);
        title.setText("Mr. Matt");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Ui.ACCENT);
        root.addView(title);

        countView = new TextView(this);
        countView.setTextColor(Ui.TEXT_DIM);
        countView.setTextSize(13);
        root.addView(countView);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        search = new EditText(this);
        search.setHint("Suchen \u2026");
        search.setSingleLine(true);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.TEXT_DIM);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                refilter();
            }
        });
        row.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        starButton = new Button(this);
        starButton.setAllCaps(false);
        starButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                starFilter = (starFilter + 1) % 5;
                refilter();
            }
        });
        row.addView(starButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(row);

        ListView list = new ListView(this);
        adapter = new GameAdapter();
        list.setAdapter(adapter);
        list.setDivider(new android.graphics.drawable.ColorDrawable(0xFF22303F));
        list.setDividerHeight(1);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Intent i = new Intent(GameListActivity.this, LevelListActivity.class);
                i.putExtra("file", shown.get(pos).file);
                startActivity(i);
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        refilter();

        // Replays im Hintergrund vorladen
        new Thread(new Runnable() {
            public void run() {
                try {
                    Repository.warmReplays(getApplicationContext());
                } catch (IOException e) {
                    // wird beim Abspielen erneut gemeldet
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void refilter() {
        String q = search.getText().toString().trim().toLowerCase();
        shown.clear();
        for (int i = 0; i < all.size(); i++) {
            GameInfo g = all.get(i);
            if (starFilter != 0 && g.stars != starFilter) continue;
            if (q.length() > 0 && g.title.toLowerCase().indexOf(q) < 0 && g.author.toLowerCase().indexOf(q) < 0) continue;
            shown.add(g);
        }
        starButton.setText(starFilter == 0 ? "Alle Stufen" : starFilter + "\u2605");
        countView.setText(shown.size() + " von " + all.size() + " Spielen");
        adapter.notifyDataSetChanged();
    }

    private final class GameAdapter extends BaseAdapter {
        public int getCount() {
            return shown.size();
        }

        public Object getItem(int i) {
            return shown.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            TextView a, b;
            if (convert == null) {
                row = new LinearLayout(GameListActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                int p = Ui.dp(GameListActivity.this, 10);
                row.setPadding(p, p, p, p);
                a = new TextView(GameListActivity.this);
                a.setTextSize(17);
                a.setTextColor(Ui.TEXT);
                a.setSingleLine(true);
                a.setEllipsize(TextUtils.TruncateAt.END);
                b = new TextView(GameListActivity.this);
                b.setTextSize(12);
                b.setTextColor(Ui.TEXT_DIM);
                b.setSingleLine(true);
                b.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(a);
                row.addView(b);
                row.setTag(new TextView[] {a, b});
            } else {
                row = (LinearLayout) convert;
                TextView[] t = (TextView[]) row.getTag();
                a = t[0];
                b = t[1];
            }
            GameInfo g = shown.get(pos);
            int solved = Progress.solvedCount(GameListActivity.this, g.file);
            a.setText(g.starText() + "  " + g.title);
            b.setText(g.author + "  \u00b7  " + g.levels + " Level  \u00b7  gel\u00f6st " + solved + "/" + g.levels);
            b.setTextColor(solved >= g.levels ? 0xFF81C784 : Ui.TEXT_DIM);
            return row;
        }
    }
}
