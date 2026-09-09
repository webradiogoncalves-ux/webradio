package com.webtv.full;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebTV Full: dois motores de reprodução, mas apenas UM player visível.
 * O app tenta o motor principal e, se falhar, pode trocar automaticamente
 * para o segundo motor. Isso evita deixar dois players aparecendo na tela.
 */
public class MainActivity extends AppCompatActivity {
    private ExoPlayer exoPlayer;
    private MediaPlayer mediaPlayer;
    private PlayerView playerView;
    private SurfaceView secondaryPlayerView;
    private TextView status, section;
    private EditText search;
    private ContentAdapter adapter;
    private final List<Content> all = new ArrayList<>();
    private String selected = "INÍCIO";
    private Content currentContent;
    private boolean triedFallback;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.playerStatus);
        section = findViewById(R.id.section);
        search = findViewById(R.id.search);
        playerView = findViewById(R.id.playerView);
        secondaryPlayerView = findViewById(R.id.secondaryPlayerView);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContentAdapter(this::playContent);
        list.setAdapter(adapter);

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        exoPlayer.addListener(new androidx.media3.common.Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if (currentContent != null && !triedFallback && currentContent.playerMode == Content.PlayerMode.AUTO) {
                    triedFallback = true;
                    playWithMediaPlayer(currentContent);
                } else {
                    showStatus("Não foi possível reproduzir este conteúdo.");
                }
            }
        });

        secondaryPlayerView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                if (mediaPlayer != null) mediaPlayer.setDisplay(holder);
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                if (mediaPlayer != null) mediaPlayer.setDisplay(null);
            }
        });

        findViewById(R.id.testButton).setOnClickListener(v -> startActivity(new android.content.Intent(this, TestSiteActivity.class)));

        setupCategories();
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) { filter(s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        loadCatalog();
    }

    private void setupCategories() {
        LinearLayout box = findViewById(R.id.categories);
        String[] cats = {"🏠 INÍCIO", "⚽ ESPORTES", "🎭 NOVELAS", "📺 CANAIS"};
        for (String label : cats) {
            TextView tab = new TextView(this);
            tab.setText(label); tab.setTextColor(Color.WHITE); tab.setTextSize(13); tab.setGravity(Gravity.CENTER);
            tab.setPadding(20,0,20,0);
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(17,30,45)); bg.setCornerRadius(28);
            tab.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2,-1); lp.setMargins(4,0,4,0);
            box.addView(tab, lp);
            tab.setOnClickListener(v -> {
                selected = label.substring(label.indexOf(' ') + 1);
                section.setText(selected);
                filter(search.getText().toString());
            });
        }
    }

    private void loadCatalog() {
        all.clear();
        all.addAll(StreamCatalog.items());
        filter("");
    }

    private void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Content> filtered = new ArrayList<>();
        for (Content c : all) {
            boolean categoryOk = selected.equals("INÍCIO") || c.category.equals(selected);
            boolean queryOk = q.isEmpty() || c.name.toLowerCase().contains(q);
            if (categoryOk && queryOk) filtered.add(c);
        }
        adapter.setData(filtered);
    }

    private void playContent(Content content) {
        currentContent = content;
        triedFallback = false;
        releaseMediaPlayer();
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
        secondaryPlayerView.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        if (content.url == null || content.url.trim().isEmpty()) {
            showStatus("Link de transmissão ainda não configurado para este conteúdo.");
            return;
        }

        if (content.playerMode == Content.PlayerMode.MEDIAPLAYER) {
            playWithMediaPlayer(content);
        } else {
            playWithExo(content);
        }
    }

    private void playWithExo(Content content) {
        showStatus("Conectando: " + content.name);
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(new HashMap<String,String>() {{
                    put("User-Agent", "Mozilla/5.0 (Android)");
                }});
        DefaultMediaSourceFactory factory = new DefaultMediaSourceFactory(http);
        exoPlayer.setMediaSource(factory.createMediaSource(MediaItem.fromUri(Uri.parse(content.url))));
        exoPlayer.prepare();
        exoPlayer.play();
    }

    /** Segundo motor nativo. Fica exatamente no mesmo espaço do player principal. */
    private void playWithMediaPlayer(Content content) {
        releaseMediaPlayer();
        exoPlayer.pause();
        playerView.setVisibility(View.GONE);
        secondaryPlayerView.setVisibility(View.VISIBLE);
        showStatus("Tentando outro modo de reprodução...");

        try {
            mediaPlayer = new MediaPlayer();
            Map<String,String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Android)");
            mediaPlayer.setDataSource(this, Uri.parse(content.url), headers);
            mediaPlayer.setOnPreparedListener(mp -> {
                if (secondaryPlayerView.getHolder().getSurface().isValid()) mp.setDisplay(secondaryPlayerView.getHolder());
                hideStatus();
                mp.start();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                showStatus("Este conteúdo não é compatível com os dois modos de reprodução.");
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            showStatus("Não foi possível iniciar o segundo modo de reprodução.");
        }
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        status.setVisibility(View.GONE);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override protected void onDestroy() {
        releaseMediaPlayer();
        if (exoPlayer != null) exoPlayer.release();
        super.onDestroy();
    }
}
