package com.webtvfull.app;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends ComponentActivity {
    private ExoPlayer player;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String url = getIntent().getStringExtra("url");
        String name = getIntent().getStringExtra("name");

        PlayerView view = new PlayerView(this);
        view.setBackgroundColor(Color.BLACK);
        view.setUseController(true);
        view.setControllerAutoShow(true);
        view.setControllerHideOnTouch(true);
        setContentView(view);

        hideBars();
        player = new ExoPlayer.Builder(this).build();
        view.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Não foi possível reproduzir este canal. Tente outro.",
                        Toast.LENGTH_LONG).show();
            }
        });

        if (url != null && !url.isEmpty()) {
            MediaItem item = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.VIDEO_MP2T)
                    .build();
            player.setMediaItem(item);
            player.prepare();
            player.setPlayWhenReady(true);
        } else {
            Toast.makeText(this, "Endereço da transmissão inválido.", Toast.LENGTH_LONG).show();
        }

        view.setOnClickListener(v -> hideBars());
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override public void onBackPressed() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onBackPressed();
    }
}
