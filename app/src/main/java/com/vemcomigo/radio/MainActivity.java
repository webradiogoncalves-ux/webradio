package com.vemcomigo.radio;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    TextView clock, status, now;
    Button play, bible, devotional, word, share, down, up;
    Switch hourSwitch;
    Handler h = new Handler(Looper.getMainLooper());
    BroadcastReceiver stateReceiver;
    boolean playing = false;

    final Runnable clockRun = new Runnable() {
        public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            h.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        bind();
        startService(new Intent(this, RadioService.class));
        clockRun.run();

        stateReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                String state = i.getStringExtra("state");
                if (state == null) return;
                if ("playing".equals(state)) {
                    playing = true;
                    play.setText("■");
                    status.setText("● AO VIVO");
                    now.setText("Transmissão ao vivo");
                } else if ("connecting".equals(state)) {
                    status.setText("● CONECTANDO...");
                    now.setText("Aguarde a conexão com a transmissão");
                } else if ("paused".equals(state)) {
                    playing = false;
                    play.setText("▶");
                    status.setText("● PAUSADA");
                    now.setText("Transmissão pausada");
                } else if ("error".equals(state)) {
                    playing = false;
                    play.setText("▶");
                    status.setText("● ERRO NA TRANSMISSÃO");
                    now.setText("Não foi possível conectar. Tente novamente.");
                } else if ("hour".equals(state)) {
                    status.setText("● HORA CERTA");
                    now.setText("Mensagem da Hora Certa");
                }
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, new IntentFilter(RadioService.STATE_ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, new IntentFilter(RadioService.STATE_ACTION));
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
        String[] words = {
                "Confie no Senhor de todo o seu coração. — Provérbios 3:5",
                "O Senhor é a minha força e o meu escudo. — Salmos 28:7",
                "Tudo posso naquele que me fortalece. — Filipenses 4:13",
                "Alegrai-vos sempre no Senhor. — Filipenses 4:4",
                "O amor de Deus lança fora o medo. — 1 João 4:18",
                "Entrega o teu caminho ao Senhor. — Salmos 37:5",
                "O Senhor está perto dos que têm o coração quebrantado. — Salmos 34:18",
                "O Senhor é o meu pastor; nada me faltará. — Salmos 23:1",
                "Deus é o nosso refúgio e fortaleza. — Salmos 46:1",
                "Buscai primeiro o Reino de Deus. — Mateus 6:33",
                "Não temas, porque eu sou contigo. — Isaías 41:10",
                "A minha graça te basta. — 2 Coríntios 12:9",
                "Sede fortes e corajosos. — Josué 1:9",
                "O Senhor firma os passos do homem. — Salmos 37:23",
                "Em paz me deitarei e dormirei. — Salmos 4:8",
                "O Senhor é bom; a sua misericórdia dura para sempre. — Salmos 100:5",
                "A esperança não decepciona. — Romanos 5:5",
                "Andamos por fé, não por vista. — 2 Coríntios 5:7",
                "Orai sem cessar. — 1 Tessalonicenses 5:17",
                "Sede bondosos e compassivos. — Efésios 4:32",
                "O Senhor te abençoe e te guarde. — Números 6:24",
                "Aquele que habita no esconderijo do Altíssimo descansará. — Salmos 91:1",
                "Perto está o Senhor de todos os que o invocam. — Salmos 145:18",
                "A palavra do Senhor é lâmpada para os meus pés. — Salmos 119:105",
                "Deus pode fazer abundar em vós toda graça. — 2 Coríntios 9:8",
                "O Senhor pelejará por vós. — Êxodo 14:14",
                "O coração alegre aformoseia o rosto. — Provérbios 15:13",
                "Aquele que começou boa obra em vós a completará. — Filipenses 1:6",
                "Lança sobre o Senhor a tua carga, e ele te susterá. — Salmos 55:22",
                "O Senhor é a minha luz e a minha salvação. — Salmos 27:1",
                "Seja forte o teu coração, e espera no Senhor. — Salmos 27:14"
        };
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        showContent("Palavra do Dia • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), words[(day - 1) % words.length]);
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
        if (stateReceiver != null) {
            try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
