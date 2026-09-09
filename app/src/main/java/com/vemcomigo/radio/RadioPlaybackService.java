package com.vemcomigo.radio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import java.io.IOException;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.vemcomigo.radio.PLAY";
    public static final String ACTION_PAUSE = "com.vemcomigo.radio.PAUSE";
    public static final String ACTION_STOP = "com.vemcomigo.radio.STOP";
    public static final String ACTION_VOLUME_UP = "com.vemcomigo.radio.VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "com.vemcomigo.radio.VOLUME_DOWN";
    public static final String ACTION_APPLY_VOLUME = "com.vemcomigo.radio.APPLY_VOLUME";
    public static final String ACTION_STATE = "com.vemcomigo.radio.STATE";
    public static final String EXTRA_STATE = "state";

    private static final String CHANNEL_ID = "radio_playback";
    private static final String STATE_PLAYING = "playing";
    private static final String STATE_PAUSED = "paused";
    private static final String STATE_CONNECTING = "connecting";
    private static final String STATE_ERROR = "error";

    private static MediaPlayer player;
    private static boolean playing = false;
    private static boolean connecting = false;
    private static boolean pausedByUser = false;
    private final Handler handler = new Handler();

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
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
            pausedByUser = false;
            playPlayback();
        }
        return START_STICKY;
    }

    private void playPlayback() {
        if (player != null && player.isPlaying()) {
            playing = true;
            connecting = false;
            sendState(STATE_PLAYING);
            return;
        }

        if (player != null && !connecting) {
            try {
                player.start();
                playing = true;
                pausedByUser = false;
                applyCurrentVolume();
                updateNotification();
                sendState(STATE_PLAYING);
                return;
            } catch (Exception ignored) {
                releasePlayer();
            }
        }

        releasePlayer();
        connecting = true;
        playing = false;
        sendState(STATE_CONNECTING);
        updateNotification();

        player = new MediaPlayer();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            player.setOnPreparedListener(mp -> {
                connecting = false;
                if (pausedByUser) {
                    playing = false;
                    applySystemVolume(mp);
                    updateNotification();
                    sendState(STATE_PAUSED);
                    return;
                }
                applySystemVolume(mp);
                mp.start();
                playing = true;
                updateNotification();
                sendState(STATE_PLAYING);
            });
            player.setOnCompletionListener(mp -> {
                playing = false;
                connecting = false;
                releasePlayer();
                updateNotification();
                sendState(STATE_PAUSED);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                playing = false;
                connecting = false;
                releasePlayer();
                updateNotification();
                sendState(STATE_ERROR);
                return true;
            });
            player.setDataSource(RadioConfig.RADIO_STREAM_URL);
            player.prepareAsync();
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            playing = false;
            connecting = false;
            releasePlayer();
            updateNotification();
            sendState(STATE_ERROR);
        }
    }

    private void pausePlayback() {
        pausedByUser = true;
        if (player != null && player.isPlaying()) {
            try { player.pause(); } catch (Exception ignored) {}
        }
        playing = false;
        connecting = false;
        updateNotification();
        sendState(STATE_PAUSED);
    }

    private void stopPlayback() {
        playing = false;
        connecting = false;
        pausedByUser = true;
        releasePlayer();
        sendState(STATE_PAUSED);
    }

    private void changeVolume(boolean up) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int next = Math.max(0, Math.min(max, current + (up ? 1 : -1)));
        am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);
        applyCurrentVolume();
        sendVolumeState(am);
    }

    private void applyCurrentVolume() {
        if (player == null) return;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float level = current / (float) Math.max(1, max);
        try { player.setVolume(level, level); } catch (Exception ignored) {}
    }

    private void applySystemVolume(MediaPlayer mp) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float level = current / (float) Math.max(1, max);
        try { mp.setVolume(level, level); } catch (Exception ignored) {}
    }

    private void sendVolumeState(AudioManager am) {
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        Intent i = new Intent(ACTION_STATE);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_STATE, "volume");
        i.putExtra("volume_percent", Math.round(current * 100f / Math.max(1, max)));
        sendBroadcast(i);
    }

    private void sendState(String state) {
        Intent i = new Intent(ACTION_STATE);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_STATE, state);
        sendBroadcast(i);
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
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1001, buildNotification());
    }

    public static boolean isPlaying() { return playing; }
    public static boolean isConnecting() { return connecting; }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopPlayback();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
