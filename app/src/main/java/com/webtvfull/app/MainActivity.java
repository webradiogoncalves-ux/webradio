package com.webtvfull.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/** WEBTV FULL LITE: one content view, three tabs, one list and one native player. */
public class MainActivity extends ComponentActivity {
    private final int bg = Color.rgb(7, 9, 13);
    private final int card = Color.rgb(25, 30, 36);
    private final int accent = Color.rgb(58, 210, 255);

    private LinearLayout root;
    private TextView title;
    private LinearLayout tabs;
    private ListView list;
    private ProgressBar progress;
    private JSONArray canais, novelas, jogos;
    private String activeTab = "canais";
    private boolean showingChannels = false;
    private String currentAsset = "";
    private String currentGroup = "";
    private int currentCount = 0;
    private int loaded = 0;
    private static final int PAGE = 60;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(Color.BLACK);
        build();
        loadIndex();
    }

    private TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(14, 4, 8, 2);

        title = tv("WEBTV FULL", 24, Color.WHITE, true);
        title.setPadding(0, 0, 4, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        TextView search = tv("⌕", 29, Color.WHITE, false);
        search.setGravity(Gravity.CENTER);
        search.setOnClickListener(v -> searchGroups());
        header.addView(search, new LinearLayout.LayoutParams(48, 56));
        root.addView(header);

        tabs = new LinearLayout(this);
        tabs.setPadding(8, 0, 8, 4);
        String[][] names = {{"canais", "CANAIS"}, {"novelas", "NOVELAS"}, {"jogos", "JOGOS"}};
        for (String[] n : names) {
            TextView b = tv(n[1], 14, Color.LTGRAY, true);
            b.setGravity(Gravity.CENTER);
            b.setOnClickListener(v -> {
                activeTab = n[0];
                showGroups();
            });
            tabs.addView(b, new LinearLayout.LayoutParams(0, 46, 1));
        }
        root.addView(tabs);

        FrameLayout frame = new FrameLayout(this);
        list = new ListView(this);
        list.setDivider(null);
        list.setPadding(8, 8, 8, 12);
        frame.addView(list, new FrameLayout.LayoutParams(-1, -1));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(60, 60, Gravity.CENTER);
        frame.addView(progress, pp);
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadIndex() {
        new Thread(() -> {
            try (InputStream in = getAssets().open("catalog/index.json")) {
                JSONObject o = new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
                canais = o.optJSONArray("canais");
                novelas = o.optJSONArray("novelas");
                jogos = o.optJSONArray("jogos");
                runOnUiThread(this::showGroups);
            } catch (Exception e) {
                runOnUiThread(() -> message("Não foi possível carregar as categorias."));
            }
        }).start();
    }

    private JSONArray currentGroups() {
        if ("novelas".equals(activeTab)) return novelas;
        if ("jogos".equals(activeTab)) return jogos;
        return canais;
    }

    private void colorTabs() {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            TextView b = (TextView) tabs.getChildAt(i);
            String k = i == 0 ? "canais" : i == 1 ? "novelas" : "jogos";
            b.setTextColor(k.equals(activeTab) ? accent : Color.LTGRAY);
        }
    }

    private void showGroups() {
        showingChannels = false;
        title.setText("WEBTV FULL");
        title.setOnClickListener(null);
        colorTabs();
        JSONArray a = currentGroups();
        if (a == null) return;

        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject g = a.optJSONObject(i);
            if (g != null) {
                names.add(g.optString("name", "Categoria") + "  •  " + g.optInt("count", 0));
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override public View getView(int pos, View convert, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(pos, convert, parent);
                v.setTextColor(Color.WHITE);
                v.setTextSize(16);
                v.setGravity(Gravity.CENTER_VERTICAL);
                v.setPadding(18, 0, 14, 0);
                v.setBackground(rounded(card, 12));
                AbsListView.LayoutParams lp = new AbsListView.LayoutParams(-1, 58);
                v.setLayoutParams(lp);
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            JSONObject g = currentGroups().optJSONObject(pos);
            if (g != null) openGroup(g.optString("name"), g.optString("file"), g.optInt("count"));
        });
    }

    private void openGroup(String name, String asset, int count) {
        if (asset == null || asset.isEmpty()) {
            message("Esta categoria não possui lista.");
            return;
        }
        showingChannels = true;
        currentAsset = asset;
        currentGroup = name;
        currentCount = count;
        loaded = 0;
        title.setText("‹  " + name);
        title.setOnClickListener(v -> showGroups());
        list.setAdapter(null);
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            ArrayList<Channel> page = readPage(0, PAGE);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (page.isEmpty()) message("A lista não pôde ser lida.");
                else render(page);
            });
        }).start();
    }

    private void render(ArrayList<Channel> page) {
        loaded += page.size();
        ArrayList<Channel> data = new ArrayList<>(page);
        if (loaded < currentCount) data.add(new Channel("__MORE__", "", ""));
        ArrayAdapter<Channel> adapter = new ArrayAdapter<Channel>(this, android.R.layout.simple_list_item_1, data) {
            @Override public View getView(int pos, View convert, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(pos, convert, parent);
                Channel c = getItem(pos);
                v.setText("__MORE__".equals(c.name) ? "CARREGAR MAIS" : "▶  " + c.name);
                v.setTextColor("__MORE__".equals(c.name) ? accent : Color.WHITE);
                v.setTextSize(15);
                v.setGravity(Gravity.CENTER_VERTICAL);
                v.setPadding(18, 0, 12, 0);
                v.setBackground(rounded(card, 12));
                v.setLayoutParams(new AbsListView.LayoutParams(-1, 56));
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Channel c = (Channel) p.getItemAtPosition(pos);
            if ("__MORE__".equals(c.name)) loadMore(); else play(c);
        });
    }

    private void loadMore() {
        int offset = loaded;
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            ArrayList<Channel> next = readPage(offset, PAGE);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (!next.isEmpty()) {
                    // Read the already visible items again so the single ListView remains simple and reliable.
                    ArrayList<Channel> all = readPage(0, loaded + next.size());
                    render(all);
                }
            });
        }).start();
    }

    private ArrayList<Channel> readPage(int offset, int limit) {
        ArrayList<Channel> out = new ArrayList<>();
        try (InputStream raw = getAssets().open(currentAsset);
             GZIPInputStream gz = new GZIPInputStream(raw);
             BufferedReader br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8), 32768)) {
            String ext = null, line;
            int index = 0;
            Pattern logo = Pattern.compile("tvg-logo=\\\"([^\\\"]*)\\\"");
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#EXTINF:")) ext = line;
                else if (ext != null && !line.startsWith("#") && !line.trim().isEmpty()) {
                    if (index >= offset && out.size() < limit) {
                        String name = ext.contains(",") ? ext.substring(ext.indexOf(',') + 1).trim() : "Canal";
                        Matcher m = logo.matcher(ext);
                        out.add(new Channel(name, line.trim(), m.find() ? m.group(1) : ""));
                    }
                    index++;
                    ext = null;
                    if (out.size() >= limit) break;
                }
            }
        } catch (Exception e) {
            // Keep the UI responsive; the caller shows the empty-list message.
        }
        return out;
    }

    private void play(Channel c) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("url", c.url);
        i.putExtra("name", c.name);
        startActivity(i);
    }

    private void searchGroups() {
        JSONArray a = currentGroups();
        if (a == null) return;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nome da categoria");
        new AlertDialog.Builder(this).setTitle("Pesquisar categoria").setView(input)
                .setPositiveButton("Abrir", (d, w) -> {
                    String q = input.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (q.isEmpty()) return;
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject g = a.optJSONObject(i);
                        if (g != null && g.optString("name").toLowerCase(Locale.ROOT).contains(q)) {
                            openGroup(g.optString("name"), g.optString("file"), g.optInt("count"));
                            return;
                        }
                    }
                    Toast.makeText(this, "Categoria não encontrada.", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private void message(String s) {
        list.setAdapter(null);
        TextView t = tv(s, 16, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        list.addHeaderView(t, null, false);
    }

    @Override public void onBackPressed() {
        if (showingChannels) showGroups(); else super.onBackPressed();
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return out.toByteArray();
    }

    static class Channel {
        final String name, url, logo;
        Channel(String n, String u, String l) { name = n; url = u; logo = l; }
    }
}
