package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import java.util.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            MainActivity.scheduleDailyWordNotification(context);
        }
    }
}
