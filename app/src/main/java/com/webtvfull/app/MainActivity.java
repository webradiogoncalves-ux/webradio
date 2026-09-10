package com.webtvfull.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public class MainActivity extends Activity {
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView status;
    private ListView list;
    private List<ContentItem> current = Collections.emptyList();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        status = findViewById(R.id.status);
        list = findViewById(R.id.list);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        findViewById(R.id.btnCanais).setOnClickListener(v -> show(Providers.canais(), "Canais • PlayTV"));
        findViewById(R.id.btnNovelas).setOnClickListener(v -> show(Providers.novelas(), "Novelas/Filmes • Novecalizando"));
        findViewById(R.id.btnJogos).setOnClickListener(v -> show(Providers.jogos(), "Jogos/Esportes • GetFut"));

        show(Providers.canais(), "Canais • PlayTV");
    }

    private void show(List<ContentItem> items, String label) {
        current = items;
        status.setText(label);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1,
            titles(items));
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> play(current.get(pos)));
    }

    private List<String> titles(List<ContentItem> items) {
        List<String> out = new ArrayList<>();
        for (ContentItem i : items) out.add(i.title);
        return out;
    }

    private void play(ContentItem item) {
        if (item.url == null || item.url.isEmpty()) {
            status.setText(item.provider + " ainda não possui uma fonte de reprodução integrada neste projeto.");
            return;
        }

        MediaItem.Builder mb = new MediaItem.Builder().setUri(item.url);
        if (item.mime != null && !item.mime.isEmpty()) mb.setMimeType(item.mime);
        MediaItem media = mb.build();

        player.setMediaItem(media);
        player.prepare();
        player.play();
        status.setText("Reproduzindo • " + item.title + " • " + item.mime);
    }

    @Override protected void onDestroy() {
        player.release();
        super.onDestroy();
    }
}
