package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

public class DailyWordReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "palavra_do_dia_v2";
    public static final int NOTIFICATION_ID = 7001;

    @Override public void onReceive(Context context, Intent intent) {
        createChannel(context);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_word", true);
        PendingIntent pi = PendingIntent.getActivity(context, 7001, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(com.vemcomigo.radio.R.drawable.radio_logo)
                .setContentTitle("Web Rádio Vem Comigo no Glória")
                .setContentText("🙏 A Palavra do Dia está disponível. Toque para ler.")
                .setStyle(new Notification.BigTextStyle().bigText("🙏 Palavra do Dia\nUma nova mensagem bíblica espera por você."))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            nm.notify(NOTIFICATION_ID, b.build());
        }
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notificacao_gloria);
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Palavra do Dia", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Notificações diárias da Palavra do Dia");
        ch.setSound(sound, aa);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }
}
