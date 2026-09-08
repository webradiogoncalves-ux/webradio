package com.vemcomigo.radio;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    // Listener endpoint derived from the Icecast host/port/mount shown in MediaCast.
    private static final String STREAM_URL = "http://sapircast.caster.fm:19513/QKnuH";

    private MediaPlayer streamPlayer;
    private MediaPlayer hourPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastHourKey = "";
    private boolean playing = false;
    private boolean hourPlaying = false;

    private Button playButton;
    private TextView statusText;
    private TextView clockText;
    private SeekBar volumeBar;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            checkHour();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(4, 8, 18));
        getWindow().setNavigationBarColor(Color.rgb(4, 8, 18));
        buildScreen();
        handler.post(clockTick);
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildScreen() {
        int blue = Color.rgb(10, 27, 61);
        int blue2 = Color.rgb(19, 49, 101);
        int gold = Color.rgb(245, 181, 27);
        int white = Color.WHITE;
        int gray = Color.rgb(190, 201, 220);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(20, 18, 20, 24);
        root.setBackgroundColor(Color.rgb(4, 8, 18));

        TextView title = text("Web Rádio Vem Comigo no Glória", 23, white, true);
        root.addView(title, new LinearLayout.LayoutParams(-1, 56));

        TextView subtitle = text("A RÁDIO QUE CONECTA VOCÊ", 13, gold, true);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 32));

        ImageView art = new ImageView(this);
        art.setImageResource(com.vemcomigo.radio.R.drawable.radio_art);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable artBg = bg(blue2, 28);
        art.setBackground(artBg);
        art.setClipToOutline(true);
        LinearLayout.LayoutParams artLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        artLp.setMargins(0, 8, 0, 14);
        root.addView(art, artLp);

        statusText = text("Pronto para tocar", 14, gray, false);
        root.addView(statusText, new LinearLayout.LayoutParams(-1, 34));

        playButton = new Button(this);
        playButton.setText("▶  TOCAR RÁDIO");
        playButton.setTextSize(17);
        playButton.setTextColor(Color.rgb(4, 8, 18));
        playButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        playButton.setAllCaps(false);
        playButton.setBackground(bg(gold, 60));
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(-1, 62);
        playLp.setMargins(0, 6, 0, 10);
        root.addView(playButton, playLp);
        playButton.setOnClickListener(v -> toggleRadio());

        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView volDown = text("−", 28, white, true);
        TextView volUp = text("+", 28, white, true);
        volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(80);
        volumeRow.addView(volDown, new LinearLayout.LayoutParams(45, 55));
        volumeRow.addView(volumeBar, new LinearLayout.LayoutParams(0, 55, 1f));
        volumeRow.addView(volUp, new LinearLayout.LayoutParams(45, 55));
        root.addView(volumeRow, new LinearLayout.LayoutParams(-1, 55));
        volDown.setOnClickListener(v -> volumeBar.setProgress(Math.max(0, volumeBar.getProgress() - 10)));
        volUp.setOnClickListener(v -> volumeBar.setProgress(Math.min(100, volumeBar.getProgress() + 10)));
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int p, boolean fromUser) { applyVolume(); }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        LinearLayout hourCard = new LinearLayout(this);
        hourCard.setOrientation(LinearLayout.VERTICAL);
        hourCard.setGravity(Gravity.CENTER);
        hourCard.setPadding(14, 10, 14, 10);
        hourCard.setBackground(bg(Color.rgb(15, 24, 42), 22));
        TextView hourTitle = text("🕐  HORA CERTA", 15, gold, true);
        hourCard.addView(hourTitle, new LinearLayout.LayoutParams(-1, 28));
        clockText = text("--:--:--  •  automática", 13, gray, false);
        hourCard.addView(clockText, new LinearLayout.LayoutParams(-1, 28));
        LinearLayout.LayoutParams hourLp = new LinearLayout.LayoutParams(-1, 72);
        hourLp.setMargins(0, 8, 0, 0);
        root.addView(hourCard, hourLp);

        setContentView(root);
    }

    private void toggleRadio() {
        if (hourPlaying) return;
        if (playing && streamPlayer != null) {
            pauseStream();
        } else {
            startStream();
        }
    }

    private void startStream() {
        releaseStream();
        statusText.setText("Conectando à rádio...");
        playButton.setText("⏳  CONECTANDO...");
        try {
            streamPlayer = new MediaPlayer();
            streamPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            streamPlayer.setDataSource(STREAM_URL);
            streamPlayer.setOnPreparedListener(mp -> {
                playing = true;
                applyVolume();
                mp.start();
                playButton.setText("⏸  PAUSAR RÁDIO");
                statusText.setText("● RÁDIO AO VIVO");
            });
            streamPlayer.setOnErrorListener((mp, what, extra) -> {
                playing = false;
                playButton.setText("▶  TENTAR NOVAMENTE");
                statusText.setText("Não foi possível conectar ao sinal");
                Toast.makeText(this, "O servidor não aceitou o stream direto.", Toast.LENGTH_LONG).show();
                releaseStream();
                return true;
            });
            streamPlayer.prepareAsync();
        } catch (Exception e) {
            playing = false;
            playButton.setText("▶  TENTAR NOVAMENTE");
            statusText.setText("Erro ao abrir a rádio");
            releaseStream();
        }
    }

    private void pauseStream() {
        if (streamPlayer != null && streamPlayer.isPlaying()) {
            streamPlayer.pause();
            playing = false;
            playButton.setText("▶  CONTINUAR RÁDIO");
            statusText.setText("Rádio pausada");
        }
    }

    private void stopStream() {
        if (streamPlayer != null) {
            try { if (streamPlayer.isPlaying()) streamPlayer.stop(); } catch (Exception ignored) {}
            releaseStream();
        }
        playing = false;
    }

    private void releaseStream() {
        if (streamPlayer != null) {
            try { streamPlayer.reset(); } catch (Exception ignored) {}
            try { streamPlayer.release(); } catch (Exception ignored) {}
            streamPlayer = null;
        }
    }

    private void applyVolume() {
        float v = volumeBar == null ? 0.8f : volumeBar.getProgress() / 100f;
        if (streamPlayer != null) try { streamPlayer.setVolume(v, v); } catch (Exception ignored) {}
        if (hourPlayer != null) try { hourPlayer.setVolume(v, v); } catch (Exception ignored) {}
    }

    private void updateClock() {
        if (clockText != null) {
            clockText.setText(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "  •  automática");
        }
    }

    private void checkHour() {
        if (!playing || hourPlaying) return;
        Date now = new Date();
        int minute = Integer.parseInt(new SimpleDateFormat("mm", Locale.US).format(now));
        int second = Integer.parseInt(new SimpleDateFormat("ss", Locale.US).format(now));
        String key = new SimpleDateFormat("yyyy-MM-dd-HH", Locale.US).format(now);
        if (minute == 0 && second <= 2 && !key.equals(lastHourKey)) {
            lastHourKey = key;
            int h = Integer.parseInt(new SimpleDateFormat("HH", Locale.US).format(now));
            playHour(h);
        }
    }

    private void playHour(int hour) {
        hourPlaying = true;
        stopStream();
        int res = getResources().getIdentifier(String.format(Locale.US, "hrs%02d", hour), "raw", getPackageName());
        if (res == 0) { hourPlaying = false; startStream(); return; }
        try {
            hourPlayer = MediaPlayer.create(this, res);
            if (hourPlayer == null) throw new Exception("audio nulo");
            hourPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            applyVolume();
            playButton.setText("🕐  HORA CERTA");
            statusText.setText("Hora Certa • " + String.format(Locale.US, "%02d:00", hour));
            hourPlayer.setOnCompletionListener(mp -> finishHour());
            hourPlayer.setOnErrorListener((mp, what, extra) -> { finishHour(); return true; });
            hourPlayer.start();
        } catch (Exception e) {
            finishHour();
        }
    }

    private void finishHour() {
        releaseHour();
        hourPlaying = false;
        startStream();
    }

    private void releaseHour() {
        if (hourPlayer != null) {
            try { hourPlayer.stop(); } catch (Exception ignored) {}
            try { hourPlayer.release(); } catch (Exception ignored) {}
            hourPlayer = null;
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseHour();
        releaseStream();
        super.onDestroy();
    }
}
