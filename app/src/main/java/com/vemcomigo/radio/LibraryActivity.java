package com.vemcomigo.radio;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class LibraryActivity extends Activity {

    private MediaPlayer player;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String folder = getIntent().getStringExtra("folder");
        if (folder == null) folder = "Musicas";
        build(folder);
    }

    private void build(String folderName) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(Color.rgb(5, 11, 22));

        TextView title = new TextView(this);
        title.setText(folderName.equals("Pregacoes") ? "PREGAÇÕES" : "MÚSICAS");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(54)));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button close = new Button(this);
        close.setText("VOLTAR");
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(root);
        load(folderName);
    }

    private void load(String folderName) {
        File dir = new File(Environment.getExternalStorageDirectory(), "VemComigo/" + folderName);
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3")
                || name.toLowerCase().endsWith(".m4a")
                || name.toLowerCase().endsWith(".wav")
                || name.toLowerCase().endsWith(".ogg"));

        if (files == null || files.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("Nenhum áudio encontrado.\n\nColoque seus arquivos de áudio em:\nVemComigo/" + folderName);
            empty.setTextColor(0xFFAAB8CC);
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(-1, dp(180)));
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            Button b = new Button(this);
            b.setText(f.getName());
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setOnClickListener(v -> play(f));
            list.addView(b, new LinearLayout.LayoutParams(-1, dp(58)));
        }
    }

    private void play(File file) {
        try {
            if (player != null) {
                player.release();
                player = null;
            }
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
            Toast.makeText(this, "Reproduzindo: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível reproduzir este áudio.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
