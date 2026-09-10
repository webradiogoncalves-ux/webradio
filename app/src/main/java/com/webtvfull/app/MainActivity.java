package com.webtvfull.app;

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

public class MainActivity extends ComponentActivity {
    private LinearLayout root, content;
    private int cyan = Color.rgb(57, 213, 255);
    private int card = Color.rgb(31, 38, 44);
    private int card2 = Color.rgb(38, 46, 52);
    private String activeTab = "canais";
    private JSONArray index;
    private final Map<String, JSONArray> tabData = new HashMap<>();
    private static final int PAGE_SIZE = 60;
    private String currentAsset = "";
    private String currentGroup = "";
    private int currentCount = 0;
    private int loaded = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(5,7,11));
        getWindow().setNavigationBarColor(Color.BLACK);
        buildHome();
        loadIndex();
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable bg(int c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c); g.setCornerRadius(r);
        return g;
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5,7,11));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(18, 8, 10, 2);
        TextView title = text("WEBTV FULL", 26, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, 62, 1));

        TextView search = text("⌕", 34, Color.WHITE, false);
        search.setGravity(Gravity.CENTER);
        top.addView(search, new LinearLayout.LayoutParams(52, 62));
        search.setOnClickListener(v -> showSearch());

        TextView more = text("⋮", 29, Color.WHITE, false);
        more.setGravity(Gravity.CENTER);
        top.addView(more, new LinearLayout.LayoutParams(38, 62));
        more.setOnClickListener(v -> Toast.makeText(this, "WEBTV FULL", Toast.LENGTH_SHORT).show());
        root.addView(top);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(8, 0, 8, 0);
        String[][] ts = {{"canais","CANAIS"}, {"novelas","NOVELAS"}, {"jogos","JOGOS"}};
        for (String[] x : ts) {
            TextView tv = text(x[1], 18, x[0].equals(activeTab) ? cyan : Color.LTGRAY, true);
            tv.setGravity(Gravity.CENTER);
            tabs.addView(tv, new LinearLayout.LayoutParams(0, 54, 1));
            tv.setOnClickListener(v -> { activeTab = x[0]; showGroups(); });
        }
        root.addView(tabs);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(28,45,54));
        root.addView(divider, new LinearLayout.LayoutParams(-1, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(10, 12, 10, 26);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadIndex() {
        try (InputStream in = getAssets().open("catalog/index.json")) {
            String s = new String(readAll(in), StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(s);
            for (String k : new String[]{"canais","novelas","jogos"}) tabData.put(k, o.optJSONArray(k));
            showGroups();
        } catch (Exception e) {
            content.removeAllViews();
            TextView err = text("Não foi possível carregar o catálogo.", 18, Color.WHITE, false);
            err.setPadding(20,20,20,20);
            content.addView(err);
        }
    }

    private void showGroups() {
        content.removeAllViews();
        JSONArray a = tabData.get(activeTab);
        if (a == null) return;

        // 2-column layout, matching the requested WebTV-style interface.
        LinearLayout row = null;
        for (int i=0; i<a.length(); i++) {
            try {
                JSONObject g = a.getJSONObject(i);
                String name = g.optString("name","Categoria");
                int count = g.optInt("count",0);
                if (i % 2 == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    content.addView(row, new LinearLayout.LayoutParams(-1, 104));
                }
                View cardView = makeGroupCard(name, count, g.optString("file"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 96, 1);
                lp.setMargins(i % 2 == 0 ? 0 : 5, 0, i % 2 == 0 ? 5 : 0, 8);
                row.addView(cardView, lp);
            } catch (Exception ignored) {}
        }
    }

    private View makeGroupCard(String name, int count, String file) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(8, 8, 7, 8);
        box.setBackground(bg(card, 16));
        box.setElevation(2f);

        TextView icon = text(activeTab.equals("jogos") ? "⚽" : activeTab.equals("novelas") ? "🎬" : "📺", 28, cyan, false);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon, new LinearLayout.LayoutParams(50, 78));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = text(name, 17, Color.WHITE, true);
        n.setMaxLines(2);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView c = text(count + (count == 1 ? " canal" : " canais"), 13, Color.LTGRAY, false);
        tx.addView(n, new LinearLayout.LayoutParams(-1, 44));
        tx.addView(c, new LinearLayout.LayoutParams(-1, 27));
        box.addView(tx, new LinearLayout.LayoutParams(0, 78, 1));
        box.setOnClickListener(v -> openGroup(name, file, count));
        return box;
    }

    private void openGroup(String name, String assetFile, int count) {
        currentAsset = assetFile;
        currentGroup = name;
        currentCount = count;
        loaded = 0;

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackgroundColor(Color.rgb(5,7,11));
        list.setPadding(12, 10, 12, 24);
        TextView back = text("‹  " + name, 21, Color.WHITE, true);
        back.setPadding(4, 8, 4, 12);
        back.setOnClickListener(v -> setContentView(root));
        list.addView(back);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        list.addView(progress, new LinearLayout.LayoutParams(-1, 42));
        setContentView(list);

        new Thread(() -> {
            ArrayList<Channel> page = readPage(assetFile, 0, PAGE_SIZE);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                renderPage(list, page, true);
            });
        }).start();
    }

    private void renderPage(LinearLayout list, ArrayList<Channel> page, boolean first) {
        if (first) loaded = 0;
        for (Channel ch : page) {
            TextView row = text("▶  " + ch.name, 16, Color.WHITE, false);
            row.setPadding(14, 8, 10, 8);
            row.setMaxLines(2);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setBackground(bg(card2, 13));
            row.setOnClickListener(v -> playChannel(ch));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 58);
            lp.setMargins(0,0,0,7);
            list.addView(row, lp);
        }
        loaded += page.size();

        if (loaded < currentCount) {
            Button more = new Button(this);
            more.setText("CARREGAR MAIS");
            more.setTextColor(Color.WHITE);
            more.setTextSize(13);
            more.setAllCaps(false);
            more.setBackground(bg(Color.rgb(22,70,86), 14));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, 52);
            mlp.setMargins(0, 8, 0, 8);
            list.addView(more, mlp);
            more.setOnClickListener(v -> {
                more.setEnabled(false);
                more.setText("Carregando...");
                int offset = loaded;
                new Thread(() -> {
                    ArrayList<Channel> next = readPage(currentAsset, offset, PAGE_SIZE);
                    runOnUiThread(() -> {
                        int idx = list.indexOfChild(more);
                        list.removeView(more);
                        renderPage(list, next, false);
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
                if (line.startsWith("#EXTINF:")) ext = line;
                else if (ext != null && !line.startsWith("#") && !line.isEmpty()) {
                    if (index >= offset && items.size() < limit) {
                        String title = ext.contains(",") ? ext.substring(ext.indexOf(",")+1).trim() : "Canal";
                        Matcher lm = logoPattern.matcher(ext);
                        String logo = lm.find() ? lm.group(1) : "";
                        items.add(new Channel(title, line.trim(), logo));
                    }
                    index++;
                    ext = null;
                    if (items.size() >= limit) break;
                }
            }
        } catch (Exception ignored) {}
        return items;
    }

    private void playChannel(Channel ch) {
        Intent in = new Intent(this, PlayerActivity.class);
        in.putExtra("name", ch.name);
        in.putExtra("url", ch.url);
        startActivity(in);
    }

    private void showSearch() {
        final EditText input = new EditText(this);
        input.setHint("Nome do canal ou categoria");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Pesquisar")
                .setView(input)
                .setPositiveButton("Buscar", (d,w) -> searchAll(input.getText().toString()))
                .setNegativeButton("Cancelar", null).show();
    }

    private void searchAll(String q) {
        if (q == null || q.trim().isEmpty()) return;
        q = q.toLowerCase(Locale.ROOT).trim();
        JSONArray a = tabData.get(activeTab);
        if (a == null) return;
        for (int i=0;i<a.length();i++) {
            try {
                JSONObject g=a.getJSONObject(i);
                String name=g.optString("name","");
                if (name.toLowerCase(Locale.ROOT).contains(q)) {
                    openGroup(name, g.optString("file"), g.optInt("count"));
                    return;
                }
            } catch(Exception ignored){}
        }
        Toast.makeText(this, "Nada encontrado", Toast.LENGTH_SHORT).show();
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] b=new byte[8192]; int n;
        while((n=in.read(b))!=-1) out.write(b,0,n);
        return out.toByteArray();
    }

    static class Channel {
        String name,url,logo;
        Channel(String n,String u,String l){name=n;url=u;logo=l;}
    }
}
