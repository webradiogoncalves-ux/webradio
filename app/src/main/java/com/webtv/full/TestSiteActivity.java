package com.webtv.full;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Tela de teste: site em WebView + player nativo separado.
 * Não intercepta nem extrai URLs protegidas. O campo de sinal aceita uma URL
 * de mídia autorizada fornecida pelo responsável pelo serviço.
 */
public class TestSiteActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private EditText mediaUrl;
    private TextView status;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_test_site);

        WebView web = findViewById(R.id.siteWebView);
        playerView = findViewById(R.id.nativePlayer);
        mediaUrl = findViewById(R.id.mediaUrl);
        status = findViewById(R.id.testStatus);
        Button play = findViewById(R.id.playNative);
        Button back = findViewById(R.id.backButton);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        web.loadUrl("https://noveflix.lol/");

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        play.setOnClickListener(v -> playNative());
        back.setOnClickListener(v -> finish());
    }

    private void playNative() {
        String url = mediaUrl.getText().toString().trim();
        if (url.isEmpty()) {
            status.setText("Cole aqui o link direto autorizado do sinal (HLS/M3U8 ou MP4) para testar o player nativo.");
            status.setVisibility(View.VISIBLE);
            return;
        }
        try {
            status.setText("Conectando ao sinal...");
            status.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
            player.prepare();
            player.play();
        } catch (Exception e) {
            status.setText("Não foi possível iniciar este sinal.");
            status.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }
}
