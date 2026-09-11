package com.vemcomigo.painel;
import android.content.*;
import android.os.Build;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c, Intent i){
  if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && c.getSharedPreferences("radio",0).getBoolean("auto_start",false)){
   Intent s=new Intent(c,AutomationService.class);
   if(Build.VERSION.SDK_INT>=26) c.startForegroundService(s); else c.startService(s);
  }
 }
}
