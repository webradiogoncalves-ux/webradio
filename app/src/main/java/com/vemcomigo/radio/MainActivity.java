package com.vemcomigo.radio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;

public class MainActivity extends Activity {

    private AudioManager audioManager;
    private TextView status;
    private TextView clock;
    private Button playButton;
    private int volumeStep = 1;
    private String pendingFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF050B16);
        getWindow().setNavigationBarColor(0xFF050B16);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createFolders();
        buildScreen();
        updateClock();
    }

    private void createFolders() {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "VemComigo");
            new File(root, "Musicas").mkdirs();
            new File(root, "Pregacoes").mkdirs();
        } catch (Exception ignored) {}
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(8, 4, 8, 4);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(com.vemcomigo.radio.R.drawable.button_bg);
        return b;
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackgroundResource(R.drawable.bg_main);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button menu = button("☰");
        header.addView(menu, new LinearLayout.LayoutParams(dp(54), dp(50)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("WEB RÁDIO", 18, 0xFFFFFFFF);
        title.setTypeface(null, 1);
        TextView subtitle = text("Vem Comigo no Glória", 13, 0xFFFFC928);
        titleBox.addView(title, new LinearLayout.LayoutParams(-1, dp(25)));
        titleBox.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(23)));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, dp(50), 1));

        clock = text("00:00:00", 14, 0xFFFFC928);
        clock.setGravity(Gravity.CENTER);
        header.addView(clock, new LinearLayout.LayoutParams(dp(92), dp(50)));

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView tagline = text("Música • Informação • Fé • Você Sempre Conectado", 11, 0xFFB8C7DA);
        root.addView(tagline, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout art = new LinearLayout(this);
        art.setOrientation(LinearLayout.VERTICAL);
        art.setGravity(Gravity.CENTER);
        art.setBackgroundResource(R.drawable.logo_panel);
        TextView mic = text("◉", 54, 0xFFFFC928);
        TextView brand = text("WEB RÁDIO", 28, 0xFFFFFFFF);
        brand.setTypeface(null, 1);
        TextView name = text("Vem Comigo", 25, 0xFFFFC928);
        name.setTypeface(null, 1);
        TextView gl = text("no Glória", 20, 0xFFFFFFFF);
        TextView live = text("●  AO VIVO", 12, 0xFF19D36B);
        art.addView(mic, new LinearLayout.LayoutParams(-1, dp(60)));
        art.addView(brand, new LinearLayout.LayoutParams(-1, dp(38)));
        art.addView(name, new LinearLayout.LayoutParams(-1, dp(34)));
        art.addView(gl, new LinearLayout.LayoutParams(-1, dp(30)));
        art.addView(live, new LinearLayout.LayoutParams(-1, dp(30)));
        root.addView(art, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        TextView liveTitle = text("TRANSMISSÃO AO VIVO", 13, 0xFFFFC928);
        liveTitle.setTypeface(null, 1);
        root.addView(liveTitle, new LinearLayout.LayoutParams(-1, dp(30)));

        status = text("Rádio pronta para ouvir", 12, 0xFFFFFFFF);
        status.setSingleLine(true);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        String[] labels = {"MENU", "VOLUME -", "PAUSAR", "VOLUME +", "COMPARTILHAR"};
        for (String label : labels) {
            Button b = button(label);
            controls.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1));
            if (label.equals("MENU")) b.setOnClickListener(v -> showMenu());
            if (label.equals("VOLUME -")) b.setOnClickListener(v -> changeVolume(-volumeStep));
            if (label.equals("VOLUME +")) b.setOnClickListener(v -> changeVolume(volumeStep));
            if (label.equals("PAUSAR")) {
                playButton = b;
                b.setText("OUVIR RÁDIO");
                b.setOnClickListener(v -> openRadioPlayer());
            }
            if (label.equals("COMPARTILHAR")) b.setOnClickListener(v -> shareApp());
        }
        root.addView(controls, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
    }

    private void showMenu() {
        final String[] items = {"Músicas", "Pregações", "Hora Certa", "Abrir pasta VemComigo"};
        new AlertDialog.Builder(this)
                .setTitle("MENU")
                .setItems(items, (d, which) -> {
                    if (which == 0) openLibrary("Musicas");
                    else if (which == 1) openLibrary("Pregacoes");
                    else if (which == 2) showClock();
                    else openFolderSettings();
                }).show();
    }

    private void openLibrary(String folder) {
        pendingFolder = folder;
        if (hasFolderAccess()) {
            launchLibrary(folder);
        } else {
            requestFolderAccess();
        }
    }

    private void launchLibrary(String folder) {
        pendingFolder = null;
        Intent i = new Intent(this, LibraryActivity.class);
        i.putExtra("folder", folder);
        startActivity(i);
    }

    private boolean hasFolderAccess() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestFolderAccess() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
                Toast.makeText(this, "Permita o acesso aos arquivos e volte para o aplicativo.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Abra as configurações e permita acesso aos arquivos.", Toast.LENGTH_LONG).show();
            }
        } else {
            requestStoragePermission();
        }
    }

    private void showClock() {
        String now = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        new AlertDialog.Builder(this)
                .setTitle("HORA CERTA")
                .setMessage("Horário atual\n\n" + now)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openFolderSettings() {
        requestFolderAccess();
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (android.os.Build.VERSION.SDK_INT <= 29 &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), 10);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && pendingFolder != null && hasFolderAccess()) {
            launchLibrary(pendingFolder);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingFolder != null && hasFolderAccess()) {
            launchLibrary(pendingFolder);
        }
    }

    private void openRadioPlayer() {
        Intent i = new Intent(this, RadioPlayerActivity.class);
        startActivity(i);
    }

    private void changeVolume(int delta) {
        // Altera diretamente o volume da mídia para que os botões do painel
        // tenham efeito real também na transmissão.
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int next = Math.max(0, Math.min(max, current + delta));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0);

        int actual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int percent = Math.round((actual * 100f) / Math.max(1, max));
        status.setText("Volume: " + percent + "%");
        Toast.makeText(this, "Volume " + percent + "%", Toast.LENGTH_SHORT).show();
    }

    private void shareApp() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "Web Rádio Vem Comigo no Glória");
        startActivity(Intent.createChooser(send, "Compartilhar"));
    }

    private void updateClock() {
        if (clock != null) {
            clock.setText(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date()));
        }
        clock.postDelayed(this::updateClock, 1000);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
