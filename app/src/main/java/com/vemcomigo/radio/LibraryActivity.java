package com.vemcomigo.radio;

import android.app.*;
import android.os.*;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class LibraryActivity extends Activity {
    private LinearLayout root;
    private MediaPlayer player;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        build();
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(20);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        return t;
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(Color.BLACK);

        TextView head = title("BIBLIOTECA — VemComigo");
        root.addView(head, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView path = title("VemComigo/Musicas e VemComigo/Pregacoes");
        path.setTextSize(14);
        path.setTextColor(Color.LTGRAY);
        root.addView(path, new LinearLayout.LayoutParams(-1, dp(50)));

        Button refresh = new Button(this);
        refresh.setText("ATUALIZAR BIBLIOTECA");
        refresh.setOnClickListener(v -> load());
        root.addView(refresh, new LinearLayout.LayoutParams(-1, dp(55)));

        ScrollView listScroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(list);
        root.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        load();
    }

    private void load() {
        // Replace the current content after the first three fixed controls.
        if (root.getChildCount() > 3) root.removeViews(3, root.getChildCount() - 3);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(list);

        File rootDir = new File(Environment.getExternalStorageDirectory(), "VemComigo");
        File mus = new File(rootDir, "Musicas");
        File preg = new File(rootDir, "Pregacoes");
        mus.mkdirs();
        preg.mkdirs();

        addFolder(list, "🎵 MÚSICAS", mus);
        addFolder(list, "🙏 PREGAÇÕES", preg);

        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void addFolder(LinearLayout list, String label, File dir) {
        TextView h = title(label);
        h.setTextColor(Color.rgb(22,135,255));
        list.addView(h);
        File[] files = dir.listFiles((d,n) -> {
            String x = n.toLowerCase(Locale.ROOT);
            return x.endsWith(".mp3") || x.endsWith(".m4a") || x.endsWith(".wav") || x.endsWith(".aac");
        });
        if (files == null || files.length == 0) {
            TextView empty = title("Nenhum áudio encontrado");
            empty.setTextSize(14);
            empty.setTextColor(Color.GRAY);
            list.addView(empty);
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            Button b = new Button(this);
            b.setText("▶  " + f.getName());
            b.setAllCaps(false);
            b.setTextColor(Color.WHITE);
            b.setOnClickListener(v -> play(f));
            list.addView(b, new LinearLayout.LayoutParams(-1, dp(58)));
        }
    }

    private void play(File f) {
        try {
            if (player != null) player.release();
            player = new MediaPlayer();
            player.setDataSource(f.getAbsolutePath());
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
            Toast.makeText(this, "Reproduzindo: " + f.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível reproduzir o arquivo.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        if (player != null) try { player.release(); } catch(Exception ignored) {}
        super.onDestroy();
    }
}
