package com.webtvfull.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.AlertDialog;
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

public class MainActivity extends ComponentActivity {
    private LinearLayout root;
    private LinearLayout content;
    private int cyan = Color.rgb(57, 213, 255);
    private int card = Color.rgb(32, 38, 43);
    private String activeTab = "canais";
    private JSONArray index;
    private final Map<String, JSONArray> tabData = new HashMap<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildHome();
        loadIndex();
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
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
        top.setPadding(22, 18, 16, 10);
        TextView title = text("WEBTV FULL", 28, Color.WHITE, true);
        top.addView(title, new LinearLayout.LayoutParams(0, 70, 1));

        TextView search = text("⌕", 36, Color.WHITE, false);
        search.setGravity(Gravity.CENTER);
        top.addView(search, new LinearLayout.LayoutParams(58, 70));
        search.setOnClickListener(v -> showSearch());

        TextView more = text("⋮", 30, Color.WHITE, false);
        more.setGravity(Gravity.CENTER);
        top.addView(more, new LinearLayout.LayoutParams(42, 70));
        more.setOnClickListener(v -> Toast.makeText(this, "WEBTV FULL", Toast.LENGTH_SHORT).show());
        root.addView(top);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(10, 0, 10, 0);
        String[][] ts = {{"canais","CANAIS"}, {"novelas","NOVELAS"}, {"jogos","JOGOS"}};
        for (String[] x : ts) {
            TextView tv = text(x[1], 18, Color.LTGRAY, true);
            tv.setGravity(Gravity.CENTER);
            tabs.addView(tv, new LinearLayout.LayoutParams(0, 58, 1));
            tv.setOnClickListener(v -> { activeTab = x[0]; showGroups(); });
        }
        root.addView(tabs);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 16, 16, 30);
        ScrollView sv = new ScrollView(this);
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
            content.addView(err);
        }
    }

    private void showGroups() {
        content.removeAllViews();
        JSONArray a = tabData.get(activeTab);
        if (a == null) return;

        for (int i=0; i<a.length(); i++) {
            try {
                JSONObject g = a.getJSONObject(i);
                String name = g.optString("name","Categoria");
                int count = g.optInt("count",0);
                LinearLayout cardView = new LinearLayout(this);
                cardView.setGravity(Gravity.CENTER_VERTICAL);
                cardView.setPadding(16, 8, 12, 8);
                cardView.setBackground(bg(card, 22));

                TextView icon = text(activeTab.equals("jogos") ? "⚽" : activeTab.equals("novelas") ? "🎬" : "📺", 32, cyan, false);
                icon.setGravity(Gravity.CENTER);
                cardView.addView(icon, new LinearLayout.LayoutParams(62, 76));

                LinearLayout tx = new LinearLayout(this);
                tx.setOrientation(LinearLayout.VERTICAL);
                TextView n = text(name, 19, Color.WHITE, true);
                TextView c = text(count + (count == 1 ? " canal" : " canais"), 14, Color.LTGRAY, false);
                tx.addView(n, new LinearLayout.LayoutParams(-1, 38));
                tx.addView(c, new LinearLayout.LayoutParams(-1, 32));
                cardView.addView(tx, new LinearLayout.LayoutParams(0, 76, 1));

                String file = g.optString("file");
                cardView.setOnClickListener(v -> openGroup(name, file, count));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 92);
                lp.setMargins(0,0,0,12);
                content.addView(cardView, lp);
            } catch (Exception ignored) {}
        }
    }

    private void openGroup(String name, String assetFile, int count) {
        try {
            InputStream raw = getAssets().open(assetFile);
            GZIPInputStream gz = new GZIPInputStream(raw);
            BufferedReader br = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8));
            ArrayList<Channel> items = new ArrayList<>();
            String ext = null, line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#EXTINF:")) ext = line;
                else if (ext != null && !line.startsWith("#") && !line.isEmpty()) {
                    String title = ext.contains(",") ? ext.substring(ext.indexOf(",")+1).trim() : "Canal";
                    String logo = "";
                    java.util.regex.Matcher lm = java.util.regex.Pattern.compile("tvg-logo=\"([^\"]*)\"", 2).matcher(ext);
                    if (lm.find()) logo = lm.group(1);
                    items.add(new Channel(title, line.trim(), logo));
                    ext = null;
                }
            }
            br.close();

            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setBackgroundColor(Color.rgb(5,7,11));
            list.setPadding(16, 12, 16, 24);

            TextView back = text("‹  " + name, 22, Color.WHITE, true);
            back.setPadding(4, 8, 4, 14);
            back.setOnClickListener(v -> setContentView(root));
            list.addView(back);

            for (Channel ch : items) {
                TextView row = text("▶  " + ch.name, 17, Color.WHITE, false);
                row.setPadding(16, 8, 12, 8);
                row.setBackground(bg(card, 16));
                row.setOnClickListener(v -> {
                    Intent in = new Intent(this, PlayerActivity.class);
                    in.putExtra("name", ch.name);
                    in.putExtra("url", ch.url);
                    startActivity(in);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 58);
                lp.setMargins(0,0,0,8);
                list.addView(row, lp);
            }
            setContentView(list);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir esta categoria.", Toast.LENGTH_LONG).show();
        }
    }

    private void showSearch() {
        final EditText input = new EditText(this);
        input.setHint("Digite o nome do canal");
        new AlertDialog.Builder(this)
                .setTitle("Pesquisar")
                .setView(input)
                .setPositiveButton("Buscar", (d,w) -> searchAll(input.getText().toString()))
                .setNegativeButton("Cancelar", null).show();
    }

    private void searchAll(String q) {
        if (q == null || q.trim().isEmpty()) return;
        q = q.toLowerCase(Locale.ROOT);
        ArrayList<String[]> found = new ArrayList<>();
        JSONArray a = tabData.get(activeTab);
        if (a == null) return;
        for (int i=0;i<a.length();i++) {
            try {
                JSONObject g=a.getJSONObject(i);
                String name=g.optString("name","");
                if (name.toLowerCase(Locale.ROOT).contains(q)) found.add(new String[]{name,g.optString("file")});
            } catch(Exception ignored){}
        }
        Toast.makeText(this, found.size()+" categorias encontradas", Toast.LENGTH_SHORT).show();
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
