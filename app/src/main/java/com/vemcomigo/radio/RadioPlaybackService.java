package com.vemcomigo.radio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.vemcomigo.radio.PLAY";
    public static final String ACTION_PAUSE = "com.vemcomigo.radio.PAUSE";
    public static final String ACTION_STOP = "com.vemcomigo.radio.STOP";
    public static final String ACTION_VOLUME_UP = "com.vemcomigo.radio.VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "com.vemcomigo.radio.VOLUME_DOWN";
    public static final String ACTION_APPLY_VOLUME = "com.vemcomigo.radio.APPLY_VOLUME";

    private static final String CHANNEL_ID = "radio_playback";
    private static MediaPlayer player;
    private static boolean playing = false;
    private static boolean connecting = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1001, buildNotification());

        String action = intent != null ? intent.getAction() : ACTION_PLAY;
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        } else if (ACTION_PAUSE.equals(action)) {
            pausePlayback();
        } else if (ACTION_VOLUME_UP.equals(action)) {
            changeVolume(true);
        } else if (ACTION_VOLUME_DOWN.equals(action)) {
            changeVolume(false);
        } else if (ACTION_APPLY_VOLUME.equals(action)) {
            applyCurrentVolume();
        } else {
            playPlayback();
        }
        return START_STICKY;
    }

    private void playPlayback() {
        if (player != null && player.isPlaying()) {
            playing = true;
            return;
        }
        if (player != null) {
            try { player.start(); playing = true; updateNotification(); return; } catch (Exception ignored) {}
        }

        connecting = true;
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(mp -> {
            connecting = false;
            applySystemVolume(mp);
            mp.start();
            playing = true;
            updateNotification();
        });
        player.setOnCompletionListener(mp -> {
            playing = false;
            connecting = false;
            releasePlayer();
            updateNotification();
        });
        player.setOnErrorListener((mp, what, extra) -> {
            playing = false;
            connecting = false;
            releasePlayer();
            updateNotification();
            return true;
        });

        try {
            player.setDataSource(RadioConfig.RADIO_STREAM_URL);
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException e) {
            playing = false;
            connecting = false;
            releasePlayer();
            updateNotification();
        }
    }

    private void pausePlayback() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        playing = false;
        updateNotification();
    }

    private void stopPlayback() {
        playing = false;
        connecting = false;
        releasePlayer();
    }


    private void changeVolume(boolean up) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
        applyCurrentVolume();
    }

    private void applyCurrentVolume() {
        if (player == null) return;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float level = current / (float) Math.max(1, max);
        player.setVolume(level, level);
    }
    private void applySystemVolume(MediaPlayer mp) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float level = current / (float) Math.max(1, max);
        mp.setVolume(level, level);
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.reset(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = connecting ? "Conectando à transmissão..." : (playing ? "Transmissão ao vivo" : "Rádio pausada");
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Web Rádio Vem Comigo no Glória")
                .setContentText(text)
                .setContentIntent(pending)
                .setOngoing(playing || connecting)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Rádio", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Reprodução da transmissão da rádio");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1001, buildNotification());
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static boolean isConnecting() {
        return connecting;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
