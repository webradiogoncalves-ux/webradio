package com.vemcomigo.radio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private MediaPlayer hourPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastHourKey = "";
    private boolean pageReady = false;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            checkHour();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF05070B);
        getWindow().setNavigationBarColor(0xFF05070B);
        web = new WebView(this);
        web.setBackgroundColor(0xFF05070B);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                updateClockLabel();
                // Give the Caster widget time to create its HTML5 audio element.
                handler.postDelayed(() -> inspectPlayerAudio(), 2500);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.loadUrl("file:///android_asset/player.html");
        setContentView(web);
        handler.post(clockTick);
    }

    private void inspectPlayerAudio() {
        if (!pageReady || web == null) return;
        web.evaluateJavascript("(function(){var a=[].slice.call(document.querySelectorAll('audio')).map(function(x){return x.src||''}).filter(Boolean);return JSON.stringify(a);})()", value -> {
            // The free Caster widget intentionally protects its direct stream URL.
            // If the widget exposes an HTML5 audio element, the app can still use it.
        });
    }

    private void checkHour() {
        if (hourPlayer != null && hourPlayer.isPlaying()) return;
        if (web == null || !pageReady) return;
        // Only announce when the Caster player is actually playing.
        web.evaluateJavascript("(function(){return [].slice.call(document.querySelectorAll('audio')).some(function(a){return !a.paused && !a.ended;});})()", value -> {
            if (!"true".equals(value)) return;
            triggerHourIfNeeded();
        });
    }

    private void triggerHourIfNeeded() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-HH", Locale.US);
        String key = fmt.format(new Date());
        SimpleDateFormat min = new SimpleDateFormat("mm", Locale.US);
        SimpleDateFormat sec = new SimpleDateFormat("ss", Locale.US);
        int m = Integer.parseInt(min.format(new Date()));
        int s = Integer.parseInt(sec.format(new Date()));
        if (m == 0 && s <= 2 && !key.equals(lastHourKey)) {
            lastHourKey = key;
            int h = Integer.parseInt(new SimpleDateFormat("HH", Locale.US).format(new Date()));
            playHour(h);
        }
        updateClockLabel();
    }

    private void playHour(int h) {
        pauseCaster();
        releaseHour();
        int res = getResources().getIdentifier(String.format(Locale.US, "hrs%02d", h), "raw", getPackageName());
        if (res == 0) { resumeCaster(); return; }
        hourPlayer = MediaPlayer.create(this, res);
        if (hourPlayer == null) { resumeCaster(); return; }
        hourPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        hourPlayer.setOnCompletionListener(mp -> {
            releaseHour();
            resumeCaster();
        });
        hourPlayer.setOnErrorListener((mp, what, extra) -> {
            releaseHour();
            resumeCaster();
            return true;
        });
        hourPlayer.start();
        updateLabel("Hora Certa: " + String.format(Locale.US, "%02d:00", h));
    }

    private void pauseCaster() {
        if (web == null) return;
        web.evaluateJavascript("(function(){var n=0;document.querySelectorAll('audio').forEach(function(a){try{a.pause();n++}catch(e){}});return n;})()", null);
        updateLabel("Hora Certa: falando...");
    }

    private void resumeCaster() {
        if (web == null) return;
        web.evaluateJavascript("(function(){var n=0;document.querySelectorAll('audio').forEach(function(a){try{a.play();n++}catch(e){}});return n;})()", null);
        updateLabel("Rádio ao vivo");
    }

    private void updateClockLabel() {
        String now = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        updateLabel("Hora Certa • " + now + " • automática");
    }

    private void updateLabel(String text) {
        if (web == null) return;
        String safe = text.replace("\\", "\\\\").replace("'", "\\'");
        web.evaluateJavascript("(function(){var e=document.getElementById('hour');if(e)e.textContent='" + safe + "';})()", null);
    }

    private void releaseHour() {
        if (hourPlayer != null) {
            try { hourPlayer.stop(); } catch (Exception ignored) {}
            hourPlayer.release();
            hourPlayer = null;
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseHour();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
