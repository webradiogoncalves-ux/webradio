package com.vemcomigo.radio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {
    private static final String STREAM_URL =
        "https://webradiovemcomigonogloria.ismyradio.com";

    private MediaPlayer player;
    private Button play;
    private TextView status;
    private TextView state;
    private final Handler handler = new Handler();

    private GradientDrawable rounded(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(20, 16, 20, 16);
        root.setBackgroundColor(Color.rgb(3,5,8));

        TextView title = label("WEB RÁDIO", 26, Color.WHITE, true);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        root.addView(title, new LinearLayout.LayoutParams(-1, 52));

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.radio_art);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setBackground(rounded(Color.BLACK, 30));
        art.setClipToOutline(true);
        LinearLayout.LayoutParams imageParams =
            new LinearLayout.LayoutParams(-1, 0, 1f);
        imageParams.setMargins(0, 5, 0, 10);
        root.addView(art, imageParams);

        root.addView(label("RÁDIO EM REPRODUÇÃO", 15, Color.WHITE, true),
            new LinearLayout.LayoutParams(-1, 32));
        root.addView(label("MÚSICA • FÉ • INFORMAÇÃO • AO VIVO",
                14, Color.rgb(255,179,0), true),
            new LinearLayout.LayoutParams(-1, 32));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);

        controls.addView(label("00:00", 13, Color.LTGRAY, false),
            new LinearLayout.LayoutParams(65, 80));

        play = new Button(this);
        play.setText("▶");
        play.setTextSize(28);
        play.setAllCaps(false);
        play.setTextColor(Color.BLACK);
        play.setBackground(rounded(Color.rgb(255,179,0), 100));
        controls.addView(play, new LinearLayout.LayoutParams(84,84));

        state = label("OFF", 13, Color.LTGRAY, false);
        controls.addView(state, new LinearLayout.LayoutParams(65,80));
        root.addView(controls);

        status = label("Aperte PLAY para ouvir a rádio",
            13, Color.rgb(170,179,192), false);
        root.addView(status, new LinearLayout.LayoutParams(-1, 42));

        setContentView(root);
        play.setOnClickListener(v -> toggle());
    }

    private void toggle() {
        if (player != null && player.isPlaying()) stopRadio();
        else startRadio();
    }

    private void startRadio() {
        status.setText("Conectando à rádio...");
        state.setText("LIVE");
        play.setText("❚❚");
        try {
            if (player != null) player.release();
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build());
            player.setDataSource(STREAM_URL);
            player.setOnPreparedListener(mp -> {
                mp.start();
                status.setText("Rádio ao vivo");
            });
            player.setOnErrorListener((mp, what, extra) -> {
                status.setText("Não foi possível conectar à transmissão.");
                state.setText("ERRO");
                play.setText("▶");
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            status.setText("Erro ao iniciar a rádio.");
            state.setText("ERRO");
            play.setText("▶");
        }
    }

    private void stopRadio() {
        try { player.stop(); } catch (Exception ignored) {}
        try { player.release(); } catch (Exception ignored) {}
        player = null;
        play.setText("▶");
        state.setText("OFF");
        status.setText("Aperte PLAY para ouvir a rádio");
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
        }
        super.onDestroy();
    }
}
