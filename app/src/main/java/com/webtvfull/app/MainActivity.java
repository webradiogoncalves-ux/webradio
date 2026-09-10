package com.webtvfull.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

public class MainActivity extends ComponentActivity {
    private static final String EPG_URL = "";
    private static final String LOCAL_PLAYLIST = "playlist.m3u.gz";
    private static final int PAGE_SIZE = 150;

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView status, epg;
    private LinearLayout list;
    private Spinner category;
    private Button moreButton;
    private final List<String> categories = new ArrayList<>();
    private final List<Item> visibleItems = new ArrayList<>();
    private final Map<String, Bitmap> logoCache = new ConcurrentHashMap<>();
    private final Map<String, String> epgMap = new HashMap<>();
    private final Map<String, EpgInfo> epgPrograms = new HashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService logoExecutor = Executors.newFixedThreadPool(4);
    private String currentGroup = "";
    private int visibleLimit = PAGE_SIZE;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        loadCategoriesFromCompressedPlaylist();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(sp);
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,12,18));

        TextView title = text("WEBTV FULL", 22);
        title.setGravity(Gravity.CENTER); title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));

        playerView = new PlayerView(this);
        playerView.setUseController(true); playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView, new LinearLayout.LayoutParams(-1, dp(215)));

        status = text("Carregando lista...", 14);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(44)));

        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(8), 0, dp(8), 0);
        category = new Spinner(this);
        row.addView(category, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(row);

        epg = text("EPG: selecione um canal", 13);
        epg.setTextColor(Color.LTGRAY);
        root.addView(epg, new LinearLayout.LayoutParams(-1, dp(45)));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < categories.size()) {
                    currentGroup = categories.get(pos); visibleLimit = PAGE_SIZE;
                    loadSelectedCategory();
                }
            }
        });
    }

    /**
     * A lista original tem ~84 MB descompactada. Ela fica no APK como GZIP (~7,6 MB),
     * portanto nenhum arquivo individual ultrapassa o limite de upload do GitHub.
     * Primeiro lemos somente os group-title; os itens completos só são carregados
     * quando uma categoria é escolhida.
     */
    private void loadCategoriesFromCompressedPlaylist() {
        worker.execute(() -> {
            LinkedHashSet<String> found = new LinkedHashSet<>();
            try (BufferedReader r = openPlaylistReader()) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.startsWith("#EXTINF:")) {
                        Item x = parseExtinf(line);
                        found.add(x.group);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Erro ao abrir a lista local."));
                return;
            }
            categories.clear(); categories.addAll(found);
            if (categories.isEmpty()) categories.add("Outros");
            runOnUiThread(() -> {
                ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
                    @Override public View getView(int p, View v, android.view.ViewGroup g) {
                        TextView t = (TextView) super.getView(p, v, g); t.setTextColor(Color.WHITE); return t;
                    }
                };
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                category.setAdapter(a);
                status.setText("Lista pronta • " + categories.size() + " categorias");
            });
        });
    }

    private BufferedReader openPlaylistReader() throws IOException {
        InputStream raw = getAssets().open(LOCAL_PLAYLIST);
        GZIPInputStream gz = new GZIPInputStream(new BufferedInputStream(raw, 32768));
        return new BufferedReader(new InputStreamReader(gz, java.nio.charset.StandardCharsets.UTF_8), 32768);
    }

    private void loadSelectedCategory() {
        status.setText("Carregando: " + currentGroup + "...");
        list.removeAllViews();
        worker.execute(() -> {
            ArrayList<Item> result = new ArrayList<>();
            try (BufferedReader r = openPlaylistReader()) {
                String line; Item cur = null;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#EXTINF:")) cur = parseExtinf(line);
                    else if (cur != null && !line.isEmpty() && !line.startsWith("#")) {
                        cur.url = line;
                        if (cur.group.equals(currentGroup)) result.add(cur);
                        cur = null;
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Erro ao carregar categoria."));
                return;
            }
            synchronized (visibleItems) { visibleItems.clear(); visibleItems.addAll(result); }
            runOnUiThread(() -> renderList());
        });
    }

    private Item parseExtinf(String l) {
        Item x = new Item();
        int comma = l.indexOf(',');
        x.name = comma >= 0 ? l.substring(comma + 1).trim() : "Canal";
        x.id = attr(l, "tvg-id"); x.group = attr(l, "group-title"); x.logo = attr(l, "tvg-logo");
        if (x.group.isEmpty()) x.group = "Outros";
        return x;
    }

    private String attr(String s, String key) {
        String p = key + "=\""; int a = s.indexOf(p);
        if (a < 0) return ""; a += p.length(); int b = s.indexOf('"', a);
        return b < 0 ? "" : s.substring(a, b);
    }

    private void renderList() {
        if (list == null) return;
        list.removeAllViews();
        GridLayout grid = new GridLayout(this); grid.setColumnCount(2); grid.setUseDefaultMargins(false);
        int total = visibleItems.size();
        int shown = Math.min(visibleLimit, total);
        for (int i = 0; i < shown; i++) {
            Item x = visibleItems.get(i);
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
            card.setPadding(dp(7), dp(8), dp(7), dp(8));
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(43,47,53)); bg.setCornerRadius(dp(7)); card.setBackground(bg);
            ImageView logo = new ImageView(this); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            logo.setImageResource(android.R.drawable.ic_menu_gallery);
            card.addView(logo, new LinearLayout.LayoutParams(-1, dp(78)));
            TextView name = text(x.name, 14); name.setGravity(Gravity.CENTER); name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(name, new LinearLayout.LayoutParams(-1, dp(48)));
            card.setOnClickListener(v -> play(x));
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams(); gp.width = 0; gp.height = dp(132);
            gp.columnSpec = GridLayout.spec(i % 2, 1f); gp.rowSpec = GridLayout.spec(i / 2);
            gp.setMargins(dp(5), dp(5), dp(5), dp(5)); grid.addView(card, gp);
            loadLogo(x, logo);
        }
        list.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        if (shown < total) {
            moreButton = new Button(this); moreButton.setText("Carregar mais (" + shown + "/" + total + ")");
            moreButton.setOnClickListener(v -> { visibleLimit = Math.min(visibleLimit + PAGE_SIZE, total); renderList(); });
            list.addView(moreButton, new LinearLayout.LayoutParams(-1, dp(55)));
        }
        status.setText(total + " itens • " + currentGroup);
    }

    private void loadLogo(Item x, ImageView target) {
        if (x.logo == null || x.logo.isEmpty()) { target.setImageResource(android.R.drawable.ic_menu_gallery); return; }
        Bitmap cached = logoCache.get(x.logo); if (cached != null) { target.setImageBitmap(cached); return; }
        logoExecutor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(x.logo).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(12000); c.setRequestProperty("User-Agent", "WebTVFull/1.0"); c.connect();
                Bitmap b = BitmapFactory.decodeStream(c.getInputStream()); c.disconnect();
                if (b != null) { logoCache.put(x.logo, b); runOnUiThread(() -> target.setImageBitmap(b)); }
            } catch (Exception ignored) {}
        });
    }

    private void play(Item x) {
        status.setText("Abrindo: " + x.name); epg.setText("EPG: " + formatEpg(x));
        if (player != null) player.release();
        player = new ExoPlayer.Builder(this).build(); playerView.setPlayer(player);
        MediaItem.Builder mb = new MediaItem.Builder().setUri(Uri.parse(x.url));
        String low = x.url.toLowerCase(Locale.ROOT);
        if (low.contains(".m3u8")) mb.setMimeType(MimeTypes.APPLICATION_M3U8);
        else if (low.contains(".mpd")) mb.setMimeType(MimeTypes.APPLICATION_MPD);
        player.setMediaItem(mb.build()); player.prepare(); player.play();
    }

    private String formatEpg(Item x) {
        EpgInfo e = epgPrograms.get(x.id);
        if (e != null) {
            String cur = e.current == null ? "sem programa atual" : e.current;
            String next = e.next == null ? "" : " • Próximo: " + e.next;
            return "EPG: " + cur + next;
        }
        return "EPG: " + epgMap.getOrDefault(x.id, "sem programação encontrada");
    }

    private void loadEpg() {
        if (EPG_URL.isEmpty()) return;
        try {
            URL u = new URL(EPG_URL); HttpURLConnection c = (HttpURLConnection)u.openConnection();
            c.setConnectTimeout(10000); c.setReadTimeout(30000);
            InputStream in = c.getInputStream(); XmlPullParser p = XmlPullParserFactory.newInstance().newPullParser(); p.setInput(in, "UTF-8");
            int e; String channelId = null, progId = null, progTitle = null; Date progStart = null, progStop = null; long now = System.currentTimeMillis();
            while ((e = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (e == XmlPullParser.START_TAG) {
                    String n = p.getName();
                    if (n.equals("channel")) channelId = p.getAttributeValue(null, "id");
                    else if (n.equals("display-name") && channelId != null) epgMap.putIfAbsent(channelId, p.nextText());
                    else if (n.equals("programme")) { progId = p.getAttributeValue(null, "channel"); progStart = parseXmlTvDate(p.getAttributeValue(null, "start")); progStop = parseXmlTvDate(p.getAttributeValue(null, "stop")); progTitle = null; }
                    else if (n.equals("title") && progId != null) progTitle = p.nextText();
                } else if (e == XmlPullParser.END_TAG && p.getName().equals("programme") && progId != null && progTitle != null && progStart != null) {
                    EpgInfo info = epgPrograms.get(progId); if (info == null) info = new EpgInfo();
                    long st = progStart.getTime(), sp = progStop == null ? Long.MAX_VALUE : progStop.getTime();
                    if (st <= now && now < sp) info.current = progTitle;
                    else if (st > now && (info.nextStart == 0 || st < info.nextStart)) { info.next = progTitle; info.nextStart = st; }
                    epgPrograms.put(progId, info);
                }
            }
            in.close();
        } catch (Exception ignored) {}
    }

    private Date parseXmlTvDate(String v) {
        if (v == null || v.isEmpty()) return null;
        String t = v.trim();
        String[] fmts = {"yyyyMMddHHmmss Z", "yyyyMMddHHmmss", "yyyyMMddHHmmssXXX"};
        for (String f : fmts) try {
            java.text.SimpleDateFormat d = new java.text.SimpleDateFormat(f, Locale.US);
            if (!f.contains("Z") && !f.contains("X")) d.setTimeZone(TimeZone.getDefault());
            return d.parse(t);
        } catch (Exception ignored) {}
        return null;
    }

    static class EpgInfo { String current, next; long nextStart; }
    static class Item { String name = "", url = "", group = "", id = "", logo = ""; }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        worker.shutdownNow(); logoExecutor.shutdownNow(); super.onDestroy();
    }
}
