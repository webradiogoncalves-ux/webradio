package com.vemcomigo.radio;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;




public class MainActivity extends Activity {
    private static final String STREAM_URL = "http://sapircast.caster.fm:19513/QKnuH";
    private MediaPlayer player;
    private Button playButton;
    private TextView clock;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable border(int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setStroke(dp(stroke), Color.rgb(22,135,255));
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView tv(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(border(1, 22));
        b.setPadding(dp(5), 0, dp(5), 0);
        return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();
        startClock();
        ensureLibraryFolders();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));
        root.setBackgroundColor(Color.BLACK);

        // Top row: menu + Hora Certa
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = button("☰");
        menu.setTextColor(Color.rgb(255,194,26));
        menu.setTextSize(38);
        menu.setBackgroundColor(Color.TRANSPARENT);
        top.addView(menu, new LinearLayout.LayoutParams(0, dp(70), 1));
        LinearLayout horaBox = new LinearLayout(this);
        horaBox.setGravity(Gravity.CENTER);
        horaBox.setBackground(border(1, 28));
        clock = tv("HORA CERTA\n--:--:--", 17, Color.WHITE);
        clock.setTypeface(null, android.graphics.Typeface.BOLD);
        horaBox.addView(clock);
        top.addView(horaBox, new LinearLayout.LayoutParams(dp(190), dp(70)));
        root.addView(top);

        // Main logo area
        FrameLayout logoBox = new FrameLayout(this);
        logoBox.setBackground(border(1, 28));
        ImageView logo = new ImageView(this);
        logo.setImageResource(com.vemcomigo.radio.R.drawable.microfone_radio);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logoBox.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        root.addView(logoBox, new LinearLayout.LayoutParams(-1, 0, 3.4f));

        // Now playing
        LinearLayout now = new LinearLayout(this);
        now.setGravity(Gravity.CENTER_VERTICAL);
        now.setPadding(dp(8), dp(4), dp(8), dp(4));
        now.setBackground(border(1, 24));
        ImageView art = new ImageView(this);
        art.setImageResource(com.vemcomigo.radio.R.drawable.tocando_agora);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        now.addView(art, new LinearLayout.LayoutParams(dp(150), dp(105)));
        LinearLayout nowText = new LinearLayout(this);
        nowText.setOrientation(LinearLayout.VERTICAL);
        TextView a = tv("TOCANDO AGORA", 18, Color.rgb(255,194,26));
        a.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        a.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView b = tv("Web Rádio Vem Comigo no Glória", 18, Color.WHITE);
        b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView c = tv("Aguardando informações da rádio", 13, Color.LTGRAY);
        c.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        nowText.addView(a, new LinearLayout.LayoutParams(-1, 0, 1));
        nowText.addView(b, new LinearLayout.LayoutParams(-1, 0, 1));
        nowText.addView(c, new LinearLayout.LayoutParams(-1, 0, 1));
        now.addView(nowText, new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(now, new LinearLayout.LayoutParams(-1, 0, 1.2f));

        TextView status = tv("Rádio em reprodução", 15, Color.LTGRAY);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(38)));

        // Audio controls
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setBackground(border(1, 26));

        Button menu2 = button("☰\nMENU");
        Button volDown = button("🔊\nVOLUME −");
        playButton = button("▶\nOUVIR");
        playButton.setTextColor(Color.BLACK);
        playButton.setTextSize(16);
        GradientDrawable yellow = new GradientDrawable();
        yellow.setColor(Color.rgb(255,194,26));
        yellow.setShape(GradientDrawable.OVAL);
        playButton.setBackground(yellow);
        Button volUp = button("🔊\nVOLUME +");
        Button share = button("↗\nCOMPARTILHAR");

        controls.addView(menu2, new LinearLayout.LayoutParams(0, dp(95), 1));
        controls.addView(volDown, new LinearLayout.LayoutParams(0, dp(95), 1));
        controls.addView(playButton, new LinearLayout.LayoutParams(dp(105), dp(105)));
        controls.addView(volUp, new LinearLayout.LayoutParams(0, dp(95), 1));
        controls.addView(share, new LinearLayout.LayoutParams(0, dp(95), 1));

        root.addView(controls, new LinearLayout.LayoutParams(-1, dp(112)));

        // Only AUDIO remains. Programming and all 13 cards are intentionally absent.
        LinearLayout audioRow = new LinearLayout(this);
        audioRow.setGravity(Gravity.CENTER);
        Button audio = button("▣  ÁUDIO");
        audio.setTextColor(Color.rgb(22,135,255));
        audio.setTextSize(17);
        audio.setTypeface(null, android.graphics.Typeface.BOLD);
        audio.setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        audioRow.addView(audio, new LinearLayout.LayoutParams(-1, dp(78)));
        root.addView(audioRow);

        // No ScrollView: the screen is intentionally fixed.
        setContentView(root);

        playButton.setOnClickListener(v -> toggleRadio());
        volDown.setOnClickListener(v -> adjustVolume(false));
        volUp.setOnClickListener(v -> adjustVolume(true));
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Web Rádio Vem Comigo no Glória\nhttps://webradiovemcomigonogloria.ismyradio.com/");
            startActivity(Intent.createChooser(i, "Compartilhar"));
        });
        menu.setOnClickListener(v -> showMenu());
        menu2.setOnClickListener(v -> showMenu());
    }

    private void showMenu() {
        new AlertDialog.Builder(this)
                .setTitle("Web Rádio Vem Comigo no Glória")
                .setItems(new String[]{"Biblioteca de Áudio", "Criar pastas VemComigo"}, (d, which) -> {
                    if (which == 0) startActivity(new Intent(this, LibraryActivity.class));
                    else ensureLibraryFolders();
                }).show();
    }

    private void toggleRadio() {
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
                playButton.setText("▶\nOUVIR");
                return;
            }
            if (player == null) {
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setDataSource(STREAM_URL);
                player.setOnPreparedListener(mp -> {
                    mp.start();
                    playButton.setText("Ⅱ\nPAUSAR");
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    Toast.makeText(this, "Não foi possível conectar à rádio.", Toast.LENGTH_LONG).show();
                    return true;
                });
                player.prepareAsync();
            } else {
                player.start();
                playButton.setText("Ⅱ\nPAUSAR");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao iniciar a rádio.", Toast.LENGTH_LONG).show();
        }
    }

    private void adjustVolume(boolean up) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        am.adjustVolume(up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
    }

    private void startClock() {
        handler.post(new Runnable() {
            @Override public void run() {
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                clock.setText("HORA CERTA\n" + f.format(new Date()));
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void ensureLibraryFolders() {
        try {
            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                // The app can still operate without this; the user can grant access if desired.
                return;
            }
            File root = new File(Environment.getExternalStorageDirectory(), "VemComigo");
            new File(root, "Musicas").mkdirs();
            new File(root, "Pregacoes").mkdirs();
        } catch (Exception ignored) {}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
}
