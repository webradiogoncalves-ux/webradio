package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;

public class RadioService extends Service {
    public static final String ACTION_PLAY="PLAY", ACTION_STOP="STOP", ACTION_TOGGLE="TOGGLE", ACTION_HOUR="HOUR";
    public static final String STREAM_URL="http://sapircast.caster.fm:19513/QKnuH";
    private MediaPlayer player, hourPlayer;
    private Handler handler;
    private boolean hourEnabled=true, manualPaused=false;
    private int lastHour=-1;
    private AudioManager audio;
    private final Runnable tick = new Runnable(){ public void run(){ checkHour(); handler.postDelayed(this,1000); }};

    @Override public void onCreate(){ super.onCreate(); audio=(AudioManager)getSystemService(AUDIO_SERVICE); handler=new Handler(Looper.getMainLooper()); createChannel(); startForeground(77, notification("Rádio pronta")); handler.post(tick); }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel("radio","Web Rádio",NotificationManager.IMPORTANCE_LOW); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c); }}
    private Notification notification(String text){ Intent i=new Intent(this,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT); return new Notification.Builder(this,"radio").setContentTitle("Web Rádio Vem Comigo no Glória").setContentText(text).setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).setOngoing(true).build(); }
    @Override public int onStartCommand(Intent intent,int flags,int startId){ if(intent!=null){ String a=intent.getAction(); if(ACTION_PLAY.equals(a)) play(); else if(ACTION_STOP.equals(a)) stopRadio(); else if(ACTION_TOGGLE.equals(a)) { if(isPlaying()) stopRadio(); else play(); } else if(ACTION_HOUR.equals(a)) hourEnabled=intent.getBooleanExtra("enabled",true); } return START_STICKY; }
    private void play(){ try{ if(player!=null){ player.start(); return; } player=new MediaPlayer(); player.setAudioStreamType(AudioManager.STREAM_MUSIC); player.setDataSource(this, Uri.parse(STREAM_URL)); player.setOnPreparedListener(mp->{ mp.start(); update("Ao vivo"); }); player.setOnErrorListener((mp,what,extra)->{ update("Reconectando…"); releasePlayer(); return true; }); player.setOnCompletionListener(mp->releasePlayer()); player.prepareAsync(); update("Conectando…"); }catch(Exception e){ update("Erro na transmissão"); releasePlayer(); } }
    private void stopRadio(){ manualPaused=true; releasePlayer(); update("Pausada"); }
    private void releasePlayer(){ if(player!=null){ try{player.stop();}catch(Exception ignored){} try{player.release();}catch(Exception ignored){} player=null; } }
    private boolean isPlaying(){ return player!=null && player.isPlaying(); }
    public void setVolume(int dir){ if(audio!=null) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,dir>0?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI); }
    private void checkHour(){ if(!hourEnabled) return; java.util.Calendar c=java.util.Calendar.getInstance(); int m=c.get(java.util.Calendar.MINUTE), s=c.get(java.util.Calendar.SECOND), h=c.get(java.util.Calendar.HOUR_OF_DAY); if(m==0 && s<2 && h!=lastHour){ lastHour=h; playHour(h); } }
    private void playHour(int h){ if(hourPlayer!=null) try{hourPlayer.release();}catch(Exception ignored){} int id=getResources().getIdentifier(String.format("hrs%02d_o",h),"raw",getPackageName()); if(id==0) return; boolean resume=isPlaying(); if(resume) player.pause(); hourPlayer=MediaPlayer.create(this,id); if(hourPlayer==null){ if(resume) player.start(); return; } update("Hora certa"); hourPlayer.setOnCompletionListener(mp->{ try{mp.release();}catch(Exception ignored){} hourPlayer=null; if(resume && player!=null){ try{player.start(); update("Ao vivo");}catch(Exception ignored){} } }); hourPlayer.start(); }
    private void update(String text){ try{ getSystemService(NotificationManager.class).notify(77,notification(text)); }catch(Exception ignored){} }
    @Override public void onDestroy(){ handler.removeCallbacks(tick); releasePlayer(); if(hourPlayer!=null) hourPlayer.release(); super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent){ return null; }
}
