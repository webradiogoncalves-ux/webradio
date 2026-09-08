package com.vemcomigo.radio;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class RadioPlayerActivity extends Activity {

    private MediaPlayer player;
    private Button playButton;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(5, 11, 22));
        getWindow().setNavigationBarColor(Color.rgb(5, 11, 22));
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(5, 11, 22));

        TextView title = text("WEB RÁDIO VEM COMIGO NO GLÓRIA", 18, Color.WHITE);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView live = text("●  TRANSMISSÃO DIRETA", 16, Color.rgb(25, 211, 107));
        live.setTypeface(null, 1);
        root.addView(live, new LinearLayout.LayoutParams(-1, dp(45)));

        TextView info = text("Ouça a rádio diretamente pela transmissão ao vivo.", 13, Color.LTGRAY);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(55)));

        status = text("Rádio pronta para ouvir", 13, Color.WHITE);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(45)));

        playButton = button("OUVIR RÁDIO");
        playButton.setOnClickListener(v -> toggleRadio());
        root.addView(playButton, new LinearLayout.LayoutParams(-1, dp(64)));

        Button stop = button("PARAR");
        stop.setOnClickListener(v -> stopRadio());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(58));
        stopParams.topMargin = dp(12);
        root.addView(stop, stopParams);

        setContentView(root);
    }

    private void toggleRadio() {
        if (player != null && player.isPlaying()) {
            player.pause();
            playButton.setText("OUVIR RÁDIO");
            status.setText("Rádio pausada");
            return;
        }

        if (player != null) {
            player.start();
            playButton.setText("PAUSAR");
            status.setText("Transmissão ao vivo");
            return;
        }

        status.setText("Conectando à transmissão...");
        playButton.setEnabled(false);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(mp -> {
            mp.start();
            playButton.setEnabled(true);
            playButton.setText("PAUSAR");
            status.setText("Transmissão ao vivo");
        });
        player.setOnCompletionListener(mp -> {
            playButton.setText("OUVIR RÁDIO");
            status.setText("Transmissão encerrada");
        });
        player.setOnErrorListener((mp, what, extra) -> {
            playButton.setEnabled(true);
            playButton.setText("OUVIR RÁDIO");
            status.setText("Não foi possível conectar à transmissão");
            releasePlayer();
            return true;
        });

        try {
            player.setDataSource(RadioConfig.RADIO_STREAM_URL);
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException e) {
            playButton.setEnabled(true);
            playButton.setText("OUVIR RÁDIO");
            status.setText("Erro ao abrir a transmissão");
            releasePlayer();
        }
    }

    private void stopRadio() {
        releasePlayer();
        playButton.setEnabled(true);
        playButton.setText("OUVIR RÁDIO");
        status.setText("Rádio parada");
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.reset(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(8, 4, 8, 4);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(R.drawable.button_bg);
        return b;
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
