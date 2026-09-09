package com.vemcomigo.radio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private AudioManager audioManager;
    private TextView status;
    private TextView clock;
    private Button playButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF050B16);
        getWindow().setNavigationBarColor(0xFF050B16);
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        buildScreen();
        updateClock();
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(8,4,8,4); return t;
    }
    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(0xFFFFFFFF); b.setTextSize(11);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(R.drawable.button_bg); return b;
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.setBackgroundResource(R.drawable.bg_main);

        // Área principal do ouvinte: o microfone fica como fundo atrás do nome da rádio.
        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundResource(R.drawable.panel_bg);

        ImageView microphone = new ImageView(this);
        microphone.setImageResource(R.drawable.microphone_listener_bg);
        microphone.setScaleType(ImageView.ScaleType.CENTER_CROP);
        microphone.setAlpha(0.58f);
        hero.addView(microphone, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout titleOverlay = new LinearLayout(this);
        titleOverlay.setOrientation(LinearLayout.VERTICAL);
        titleOverlay.setGravity(Gravity.CENTER);
        titleOverlay.setPadding(dp(18), dp(12), dp(18), dp(12));

        TextView title = text("WEB RÁDIO", 24, 0xFFFFFFFF);
        title.setTypeface(null, 1);
        TextView subtitle = text("Vem Comigo no Glória", 20, 0xFFFFC928);
        subtitle.setTypeface(null, 1);
        TextView listener = text("RÁDIO DO OUVINTE", 12, 0xFFFFFFFF);
        listener.setTypeface(null, 1);

        titleOverlay.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));
        titleOverlay.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));
        titleOverlay.addView(listener, new LinearLayout.LayoutParams(-1, dp(28)));
        hero.addView(titleOverlay, new FrameLayout.LayoutParams(-1, -1));

        clock = text("00:00:00", 14, 0xFFFFC928);
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(dp(92), dp(40), Gravity.TOP | Gravity.END);
        clockLp.setMargins(0, dp(6), dp(6), 0);
        hero.addView(clock, clockLp);

        hero.setOnClickListener(v -> toggleRadio());
        root.addView(hero, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        TextView liveTitle = text("●  TRANSMISSÃO AO VIVO", 14, 0xFFFFC928);
        liveTitle.setTypeface(null, 1);
        root.addView(liveTitle, new LinearLayout.LayoutParams(-1, dp(34)));

        status = text("Rádio pronta para ouvir", 12, 0xFFFFFFFF);
        status.setSingleLine(true);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);

        Button word = button("PALAVRA DO DIA");
        word.setOnClickListener(v -> showWordOfDay());
        Button volDown = button("VOLUME -");
        volDown.setOnClickListener(v -> changeVolume(-1));
        Button play = button("OUVIR RÁDIO");
        playButton = play;
        play.setOnClickListener(v -> toggleRadio());
        Button volUp = button("VOLUME +");
        volUp.setOnClickListener(v -> changeVolume(1));
        Button share = button("COMPARTILHAR");
        share.setOnClickListener(v -> shareApp());

        Button[] bs = {word, volDown, play, volUp, share};
        for (Button b : bs) {
            controls.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
        root.addView(controls, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
    }

    private void showWordOfDay(){
        new AlertDialog.Builder(this).setTitle("PALAVRA DO DIA")
            .setMessage("\"O Senhor é a minha força e o meu escudo.\"\n\nSalmos 28:7")
            .setPositiveButton("FECHAR",null).show();
    }
    private void toggleRadio(){
        if(RadioPlaybackService.isPlaying()){
            sendRadioAction(RadioPlaybackService.ACTION_PAUSE); status.setText("Rádio pausada"); playButton.setText("OUVIR RÁDIO");
        } else {
            sendRadioAction(RadioPlaybackService.ACTION_PLAY); status.setText("Conectando à transmissão..."); playButton.setText("PAUSAR");
        }
    }
    private void sendRadioAction(String action){
        Intent i=new Intent(this,RadioPlaybackService.class); i.setAction(action);
        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
    }
    private void changeVolume(int direction){
        if(audioManager==null)return;
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            direction>0?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);
        Intent i=new Intent(this,RadioPlaybackService.class); i.setAction(RadioPlaybackService.ACTION_APPLY_VOLUME);
        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        int max=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int cur=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        status.setText("Volume: "+Math.round(cur*100f/Math.max(1,max))+"%");
    }
    private void shareApp(){
        Intent send=new Intent(Intent.ACTION_SEND); send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,"Web Rádio Vem Comigo no Glória");
        startActivity(Intent.createChooser(send,"Compartilhar"));
    }
    private void updateClock(){
        if(clock!=null) clock.setText(new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date()));
        if(clock!=null) clock.postDelayed(this::updateClock,1000);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
