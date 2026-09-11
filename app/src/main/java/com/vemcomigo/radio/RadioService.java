package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

public class RadioService extends Service {
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_HOUR = "HOUR";
    public static final String STATE_ACTION = "com.vemcomigo.radio.STATE";
    private MediaPlayer hourPlayer;
    private Handler handler;
    private boolean hourEnabled = true;
    private int lastHour = -1;
    private int retryCount = 0;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            checkHour();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(77, notification("Rádio pronta"));
        handler.post(tick);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel("radio", "Web Rádio", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "radio") : new Notification.Builder(this);
        return b.setContentTitle("Web Rádio Vem Comigo no Glória")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String a = intent.getAction();
            if (ACTION_PLAY.equals(a)) sendToActivity("play");
            else if (ACTION_STOP.equals(a)) sendToActivity("stop");
            else if (ACTION_HOUR.equals(a)) hourEnabled = intent.getBooleanExtra("enabled", true);
        }
        return START_STICKY;
    }

    private void sendToActivity(String command) {
        Intent i = new Intent(MainActivity.COMMAND_ACTION);
        i.setPackage(getPackageName());
        i.putExtra("command", command);
        sendBroadcast(i);
    }

    private void checkHour() {
        if (!hourEnabled) return;
        java.util.Calendar c = java.util.Calendar.getInstance();
        int m = c.get(java.util.Calendar.MINUTE);
        int s = c.get(java.util.Calendar.SECOND);
        int h = c.get(java.util.Calendar.HOUR_OF_DAY);
        if (m == 0 && s < 2 && h != lastHour) {
            lastHour = h;
            playHour(h);
        }
    }

    private void playHour(int h) {
        if (hourPlayer != null) {
            try { hourPlayer.release(); } catch (Exception ignored) {}
            hourPlayer = null;
        }
        int id = getResources().getIdentifier(String.format("hrs%02d_o", h), "raw", getPackageName());
        if (id == 0) return;
        sendToActivity("pause");
        hourPlayer = MediaPlayer.create(this, id);
        if (hourPlayer == null) return;
        sendState("hour");
        update("Hora certa");
        hourPlayer.setOnCompletionListener(mp -> {
            try { mp.release(); } catch (Exception ignored) {}
            hourPlayer = null;
            sendToActivity("play");
        });
        hourPlayer.start();
    }

    private void update(String text) {
        try { getSystemService(NotificationManager.class).notify(77, notification(text)); } catch (Exception ignored) {}
    }

    private void sendState(String state) {
        Intent i = new Intent(STATE_ACTION);
        i.setPackage(getPackageName());
        i.putExtra("state", state);
        sendBroadcast(i);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        if (hourPlayer != null) {
            try { hourPlayer.release(); } catch (Exception ignored) {}
            hourPlayer = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
