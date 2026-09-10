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
import androidx.activity.ComponentActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Simple, single-screen WebTV app. Categories replace the content area instead of stacking screens. */
public class MainActivity extends ComponentActivity {
    private LinearLayout root, content;
    private TextView titleView;
    private int cyan = Color.rgb(57, 213, 255);
    private int bgColor = Color.rgb(5, 7, 11);
    private int card = Color.rgb(27, 33, 39);
    private int card2 = Color.rgb(36, 43, 49);
    private String activeTab = "canais";
    private JSONArray index;
    private final Map<String, JSONArray> tabData = new HashMap<>();
    private static final int PAGE_SIZE = 50;

    private boolean inGroup = false;
    private String currentAsset = "";
    private String currentGroup = "";
    private int currentCount = 0;
    private int loaded = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bgColor);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildHome();
        loadIndex();
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable bg(int c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(r);
        return g;
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(14, 4, 8, 2);

        titleView = text("WEBTV FULL", 25, Color.WHITE, true);
        top.addView(titleView, new LinearLayout.LayoutParams(0, 58, 1));

        TextView search = text("⌕", 31, Color.WHITE, false);
        search.setGravity(Gravity.CENTER);
        top.addView(search, new LinearLayout.LayoutParams(48, 58));
        search.setOnClickListener(v -> showSearch());

        root.addView(top);
        root.addView(makeTabs());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(10, 10, 10, 18);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private View makeTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(8, 0, 8, 0);
        String[][] ts = {{"canais", "CANAIS"}, {"novelas", "NOVELAS"}, {"jogos", "JOGOS"}};
        for (String[] x : ts) {
            TextView tv = text(x[1], 16, Color.LTGRAY, true);
            tv.setGravity(Gravity.CENTER);
            tabs.addView(tv, new LinearLayout.LayoutParams(0, 50, 1));
            tv.setOnClickListener(v -> {
                activeTab = x[0];
                inGroup = false;
                updateTabColors(tabs);
                showGroups();
            });
        }
        updateTabColors(tabs);
        return tabs;
    }

    private void updateTabColors(View v) {
        if (!(v instanceof LinearLayout)) return;
        LinearLayout tabs = (LinearLayout) v;
        for (int i = 0; i < tabs.getChildCount(); i++) {
            TextView tv = (TextView) tabs.getChildAt(i);
            String tab = i == 0 ? "canais" : i == 1 ? "novelas" : "jogos";
            tv.setTextColor(tab.equals(activeTab) ? cyan : Color.LTGRAY);
        }
    }

    private void loadIndex() {
        try (InputStream in = getAssets().open("catalog/index.json")) {
            JSONObject o = new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
            for (String k : new String[]{"canais", "novelas", "jogos"}) {
                JSONArray a = o.optJSONArray(k);
                if (a != null) tabData.put(k, a);
            }
            showGroups();
        } catch (Exception e) {
            showError("Não foi possível carregar as categorias.");
        }
    }

    private void showGroups() {
        inGroup = false;
        content.removeAllViews();
        titleView.setText("WEBTV FULL");

        JSONArray a = tabData.get(activeTab);
        if (a == null || a.length() == 0) {
            showError("Nenhuma categoria encontrada.");
            return;
        }

        for (int i = 0; i < a.length(); i++) {
            try {
                JSONObject g = a.getJSONObject(i);
                addGroupButton(g.optString("name", "Categoria"), g.optInt("count", 0), g.optString("file", ""));
            } catch (Exception ignored) { }
        }
    }

    private void addGroupButton(String name, int count, String file) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(12, 5, 12, 5);
        box.setBackground(bg(card, 14));
        box.setClickable(true);
        box.setFocusable(true);

        String iconText = activeTab.equals("jogos") ? "⚽" : activeTab.equals("novelas") ? "🎬" : "📺";
        TextView icon = text(iconText, 26, cyan, false);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon, new LinearLayout.LayoutParams(48, 66));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = text(name, 17, Color.WHITE, true);
        n.setMaxLines(2);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView c = text(count + (count == 1 ? " canal" : " canais"), 12, Color.LTGRAY, false);
        labels.addView(n, new LinearLayout.LayoutParams(-1, 38));
        labels.addView(c, new LinearLayout.LayoutParams(-1, 25));
        box.addView(labels, new LinearLayout.LayoutParams(0, 66, 1));

        TextView arrow = text("›", 30, Color.LTGRAY, false);
        arrow.setGravity(Gravity.CENTER);
        box.addView(arrow, new LinearLayout.LayoutParams(34, 66));

        box.setOnClickListener(v -> openGroup(name, file, count));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 74);
        lp.setMargins(0, 0, 0, 8);
        content.addView(box, lp);
    }

    private void openGroup(String name, String assetFile, int count) {
        if (assetFile == null || assetFile.isEmpty()) {
            showError("Categoria sem lista de canais.");
            return;
        }
        inGroup = true;
        currentAsset = assetFile;
        currentGroup = name;
        currentCount = count;
        loaded = 0;

        content.removeAllViews();
        titleView.setText("‹  " + name);
        titleView.setOnClickListener(v -> returnHome());

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        content.addView(progress, new LinearLayout.LayoutParams(-1, 48));

        new Thread(() -> {
            ArrayList<Channel> page = readPage(assetFile, 0, PAGE_SIZE);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (page.isEmpty()) {
                    showError("Nenhum canal encontrado nesta categoria.");
                } else {
                    renderPage(page);
                }
            });
        }).start();
    }

    private void renderPage(ArrayList<Channel> page) {
        for (Channel ch : page) {
            TextView row = text("▶  " + ch.name, 15, Color.WHITE, false);
            row.setPadding(14, 7, 10, 7);
            row.setMaxLines(2);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setBackground(bg(card2, 11));
            row.setOnClickListener(v -> playChannel(ch));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 56);
            lp.setMargins(0, 0, 0, 6);
            content.addView(row, lp);
        }
        loaded += page.size();
        if (loaded < currentCount && page.size() > 0) {
            Button more = new Button(this);
            more.setText("CARREGAR MAIS");
            more.setTextColor(Color.WHITE);
            more.setAllCaps(false);
            more.setBackground(bg(Color.rgb(22, 70, 86), 12));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, 50);
            mlp.setMargins(0, 8, 0, 12);
            content.addView(more, mlp);
            more.setOnClickListener(v -> {
                more.setEnabled(false);
                more.setText("Carregando...");
                int offset = loaded;
                new Thread(() -> {
                    ArrayList<Channel> next = readPage(currentAsset, offset, PAGE_SIZE);
                    runOnUiThread(() -> {
                        content.removeView(more);
                        renderPage(next);
                    });
                }).start();
            });
        }
    }

    private ArrayList<Channel> readPage(String assetFile, int offset, int limit) {
        ArrayList<Channel> items = new ArrayList<>();
        try (InputStream raw = getAssets().open(assetFile);
             GZIPInputStream gz = new GZIPInputStream(raw);
             BufferedReader br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8), 32768)) {
            String ext = null, line;
            int index = 0;
            Pattern logoPattern = Pattern.compile("tvg-logo=\\\"([^\\\"]*)\\\"");
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#EXTINF:")) {
                    ext = line;
                } else if (ext != null && !line.startsWith("#") && !line.trim().isEmpty()) {
                    if (index >= offset && items.size() < limit) {
                        String title = ext.contains(",") ? ext.substring(ext.indexOf(',') + 1).trim() : "Canal";
                        Matcher lm = logoPattern.matcher(ext);
                        String logo = lm.find() ? lm.group(1) : "";
                        items.add(new Channel(title, line.trim(), logo));
                    }
                    index++;
                    ext = null;
                    if (items.size() >= limit && index > offset) break;
                }
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Erro ao abrir a categoria.", Toast.LENGTH_LONG).show());
        }
        return items;
    }

    private void playChannel(Channel ch) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("url", ch.url);
        i.putExtra("name", ch.name);
        startActivity(i);
    }

    private void showSearch() {
        if (tabData.isEmpty()) return;
        final EditText input = new EditText(this);
        input.setHint("Nome da categoria");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Pesquisar")
                .setView(input)
                .setPositiveButton("Abrir", (d, w) -> {
                    String q = input.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (q.isEmpty()) return;
                    JSONArray a = tabData.get(activeTab);
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject g = a.optJSONObject(i);
                        if (g != null && g.optString("name").toLowerCase(Locale.ROOT).contains(q)) {
                            openGroup(g.optString("name"), g.optString("file"), g.optInt("count"));
                            return;
                        }
                    }
                    Toast.makeText(this, "Categoria não encontrada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showError(String msg) {
        content.removeAllViews();
        TextView t = text(msg, 17, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        content.addView(t, new LinearLayout.LayoutParams(-1, 160));
    }

    private void returnHome() {
        titleView.setOnClickListener(null);
        titleView.setText("WEBTV FULL");
        inGroup = false;
        showGroups();
    }

    @Override public void onBackPressed() {
        if (inGroup) {
            returnHome();
        } else {
            super.onBackPressed();
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    static class Channel {
        final String name, url, logo;
        Channel(String name, String url, String logo) {
            this.name = name;
            this.url = url;
            this.logo = logo;
        }
    }
}
