package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    public static final String COMMAND_ACTION = "com.vemcomigo.radio.COMMAND";
    private static final String RADIO_SITE = "https://webradiovemcomigonogloria.ismyradio.com/";
    TextView clock, status, now;
    Button play, bible, devotional, word, share, down, up;
    Switch hourSwitch;
    Handler h = new Handler(Looper.getMainLooper());
    BroadcastReceiver stateReceiver, commandReceiver;
    WebView radioWeb;
    boolean playing = false;
    boolean webReady = false;
    boolean requestedPlay = false;

    final Runnable clockRun = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            h.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        bind();
        setupHiddenRadioPlayer();
        startService(new Intent(this, RadioService.class));
        clockRun.run();

        stateReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String state = i.getStringExtra("state");
                if ("hour".equals(state)) {
                    status.setText("● HORA CERTA");
                    now.setText("Mensagem da Hora Certa");
                }
            }
        };
        commandReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String command = i.getStringExtra("command");
                if ("play".equals(command)) startRadio();
                else if ("pause".equals(command) || "stop".equals(command)) pauseRadio();
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, new IntentFilter(RadioService.STATE_ACTION), Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(commandReceiver, new IntentFilter(COMMAND_ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, new IntentFilter(RadioService.STATE_ACTION));
            registerReceiver(commandReceiver, new IntentFilter(COMMAND_ACTION));
        }

        play.setOnClickListener(v -> {
            if (playing) {
                cmd(RadioService.ACTION_STOP);
            } else {
                cmd(RadioService.ACTION_PLAY);
            }
        });
        down.setOnClickListener(v -> volume(-1));
        up.setOnClickListener(v -> volume(1));
        hourSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            Intent i = new Intent(this, RadioService.class);
            i.setAction(RadioService.ACTION_HOUR);
            i.putExtra("enabled", checked);
            startService(i);
        });
        bible.setOnClickListener(v -> startActivity(new Intent(this, BibleActivity.class)));
        devotional.setOnClickListener(v -> showDevotional());
        word.setOnClickListener(v -> showWord());
        share.setOnClickListener(v -> shareRadio());
    }

    void bind() {
        clock = findViewById(R.id.clock);
        status = findViewById(R.id.status);
        now = findViewById(R.id.nowPlaying);
        play = findViewById(R.id.play);
        bible = findViewById(R.id.bible);
        devotional = findViewById(R.id.devotional);
        word = findViewById(R.id.word);
        share = findViewById(R.id.share);
        down = findViewById(R.id.volumeDown);
        up = findViewById(R.id.volumeUp);
        hourSwitch = findViewById(R.id.hourSwitch);
    }

    private void setupHiddenRadioPlayer() {
        radioWeb = new WebView(this);
        radioWeb.setBackgroundColor(Color.TRANSPARENT);
        radioWeb.setAlpha(0.01f);
        radioWeb.getSettings().setJavaScriptEnabled(true);
        radioWeb.getSettings().setDomStorageEnabled(true);
        radioWeb.getSettings().setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) radioWeb.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        radioWeb.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                webReady = true;
                if (requestedPlay) h.postDelayed(() -> playWebAudio(), 1200);
            }
        });
        radioWeb.setWebChromeClient(new WebChromeClient());
        addContentView(radioWeb, new ViewGroup.LayoutParams(2, 2));
        radioWeb.loadUrl(RADIO_SITE);
    }

    private void startRadio() {
        requestedPlay = true;
        status.setText("● CONECTANDO...");
        now.setText("Conectando à transmissão ao vivo...");
        play.setText("■");
        if (!webReady) {
            radioWeb.loadUrl(RADIO_SITE);
            h.postDelayed(this::playWebAudio, 2500);
        } else {
            playWebAudio();
        }
        h.postDelayed(this::verifyRadioPlaying, 1800);
    }

    private void playWebAudio() {
        if (radioWeb == null) return;
        radioWeb.evaluateJavascript("(function(){var a=document.querySelector('audio');if(a){a.muted=false;a.volume=1;var p=a.play();return 'audio';}var b=document.querySelector('[aria-label*=Play i],button');if(b){b.click();return 'button';}return 'none';})()", value -> {
            h.postDelayed(this::verifyRadioPlaying, 1200);
        });
    }

    private void verifyRadioPlaying() {
        if (radioWeb == null || !requestedPlay) return;
        radioWeb.evaluateJavascript("(function(){var a=document.querySelector('audio');return a ? (!a.paused && a.readyState>0 ? 'playing' : 'waiting') : 'none';})()", value -> {
            if ("\"playing\"".equals(value)) {
                playing = true;
                play.setText("■");
                status.setText("● AO VIVO");
                now.setText("Transmissão ao vivo");
            } else if (requestedPlay) {
                h.postDelayed(this::playWebAudio, 1500);
            }
        });
    }

    private void pauseRadio() {
        requestedPlay = false;
        playing = false;
        if (radioWeb != null) {
            radioWeb.evaluateJavascript("(function(){var a=document.querySelector('audio');if(a){a.pause();a.currentTime=0;}return 'ok';})()", null);
        }
        play.setText("▶");
        status.setText("● PAUSADA");
        now.setText("Transmissão pausada");
    }

    void cmd(String action) {
        Intent i = new Intent(this, RadioService.class);
        i.setAction(action);
        startService(i);
    }

    void volume(int d) {
        android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
        am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                d > 0 ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER,
                android.media.AudioManager.FLAG_SHOW_UI);
    }

    void shareRadio() {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, "Ouça a Web Rádio Vem Comigo no Glória ao vivo: https://webradiovemcomigonogloria.ismyradio.com");
        startActivity(Intent.createChooser(s, "Compartilhar rádio"));
    }

    void showContent(String title, String body) {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("FECHAR", null)
                .create();
        d.setOnShowListener(x -> {
            TextView msg = d.findViewById(android.R.id.message);
            if (msg != null) {
                msg.setTextColor(Color.WHITE);
                msg.setTextSize(18);
                msg.setLineSpacing(0, 1.15f);
            }
        });
        d.show();
    }

    void showWord() {
        Calendar today = Calendar.getInstance();
        int day = today.get(Calendar.DAY_OF_YEAR);
        int bookIndex = day % BibleActivity.names.length;
        int chapter = (day % BibleActivity.chapters[bookIndex]) + 1;
        loadDailyVerse(bookIndex, chapter, day);
    }

    private void loadDailyVerse(int bookIndex, int chapter, int day) {
        showContent("Palavra do Dia • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), "Carregando a Palavra do Dia...");
        final String slug = BibleActivity.slugs[bookIndex];
        final String name = BibleActivity.names[bookIndex];
        new Thread(() -> {
            try {
                URL u = new URL("https://free.bible/bible/pt/" + slug + "/" + chapter + ".json");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000);
                c.setRequestProperty("User-Agent", "WebRadioVemComigoNoGloria/1.0");
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder s = new StringBuilder(); String line;
                while ((line = r.readLine()) != null) s.append(line);
                JSONArray arr = new JSONObject(s.toString()).getJSONArray("verses");
                if (arr.length() == 0) throw new IOException("sem versículos");
                int index = day % arr.length();
                JSONObject v = arr.getJSONObject(index);
                String body = v.getInt("v") + "  " + v.getString("t") + "\n\n— " + name + " " + chapter + ":" + v.getInt("v");
                runOnUiThread(() -> showContent("Palavra do Dia • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), body));
            } catch (Exception e) {
                String fallback = "Confie no Senhor de todo o seu coração.\n\n— Provérbios 3:5";
                runOnUiThread(() -> showContent("Palavra do Dia • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), fallback));
            }
        }).start();
    }

    void showDevotional() {
        String[] dev = {
                "Hoje, faça uma pausa e agradeça a Deus pelo que Ele já fez. Entregue a Ele também aquilo que ainda preocupa o seu coração. Ore com sinceridade e siga o dia confiando que Deus continua cuidando de você.",
                "Mesmo quando você não enxerga o próximo passo, Deus continua presente. Ore, confie e caminhe com fé. Nem toda resposta chega imediatamente, mas você não precisa caminhar sozinho.",
                "Não deixe o medo decidir por você. Busque a Deus, fortaleça o coração e prossiga com esperança. Coloque diante do Senhor seus planos, sua família e tudo aquilo que precisa de direção.",
                "Comece este dia lembrando que a sua vida não está esquecida por Deus. Agradeça, peça sabedoria e faça o bem que estiver ao seu alcance.",
                "Quando o coração estiver cansado, diminua o ritmo e ore. Deus conhece aquilo que você não consegue explicar e pode renovar suas forças.",
                "Não compare a sua caminhada com a de outra pessoa. Cada etapa tem seu tempo. Continue fiel, faça o que é certo e confie no cuidado de Deus.",
                "Hoje é uma nova oportunidade para perdoar, recomeçar e escolher a paz. Entregue a Deus aquilo que você não consegue controlar.",
                "A fé não significa ausência de dificuldades. Significa continuar confiando mesmo quando o caminho ainda não está claro.",
                "Separe alguns minutos para agradecer por três coisas. A gratidão muda o olhar e ajuda o coração a perceber o cuidado de Deus.",
                "Se alguma porta se fechou, não desista. Peça direção, aprenda com o caminho e continue avançando com esperança.",
                "Cuide também das pessoas que Deus colocou perto de você. Uma palavra de carinho pode transformar o dia de alguém.",
                "Antes de tomar uma decisão importante, ore. Peça sabedoria, tranquilidade e um coração disposto a fazer o que é correto.",
                "Você não precisa resolver tudo hoje. Faça o próximo passo possível e entregue o restante a Deus.",
                "Quando vier a preocupação, transforme a preocupação em oração. Fale com Deus e depois volte a fazer aquilo que está ao seu alcance.",
                "A paz de Deus não depende de tudo estar perfeito. Procure a presença de Deus no meio da realidade que você está vivendo.",
                "Não despreze os pequenos começos. Grandes mudanças muitas vezes começam com uma pequena decisão feita com fidelidade.",
                "Ore pela sua família hoje. Mesmo uma oração simples, feita com fé, pode ser um momento precioso diante de Deus.",
                "Se você errou, reconheça, peça perdão e recomece. Culpa não precisa ser o ponto final da sua história.",
                "Escolha hoje palavras que edifiquem. Evite responder no impulso e deixe a sabedoria conduzir suas atitudes.",
                "Deus vê o esforço que ninguém vê. Continue fazendo o bem, mesmo quando não recebe reconhecimento.",
                "Reserve um momento sem distrações para ler a Palavra e ouvir o que Deus pode ensinar ao seu coração.",
                "Não permita que uma dificuldade de hoje apague todas as bênçãos que você já recebeu.",
                "A esperança cresce quando lembramos do que Deus já fez. Recorde uma vitória do passado e agradeça.",
                "Se o caminho parecer lento, permaneça firme. Constância também é uma forma de fé.",
                "Hoje, escolha a humildade. Ouça antes de responder e procure compreender antes de julgar.",
                "Entregue seus planos a Deus e peça que Ele corrija aquilo que precisar ser corrigido.",
                "Mesmo em um dia difícil, procure uma oportunidade para servir alguém. Fazer o bem também fortalece o coração.",
                "Não carregue sozinho aquilo que pode ser colocado em oração. Deus conhece suas necessidades antes mesmo de você falar.",
                "Termine o dia agradecendo. Pense no que aprendeu, peça perdão pelo que precisa e descanse confiando em Deus.",
                "Amanhã ainda não chegou. Viva o dia de hoje com fé, responsabilidade e gratidão. Deus continuará sendo Deus amanhã também."
        };
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        showContent("Devocional • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), dev[(day - 1) % dev.length]);
    }

    @Override protected void onDestroy() {
        h.removeCallbacks(clockRun);
        if (stateReceiver != null) { try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {} }
        if (commandReceiver != null) { try { unregisterReceiver(commandReceiver); } catch (Exception ignored) {} }
        if (radioWeb != null) { try { radioWeb.stopLoading(); radioWeb.destroy(); } catch (Exception ignored) {} }
        super.onDestroy();
    }
}
