package com.vemcomigo.painel;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.content.ClipData;
import android.provider.Settings;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    TextView status, listeners, bitrate, now, next, clock, log;
    final Handler h = new Handler(Looper.getMainLooper());
    final String CASTER_HOST = "sapircast.caster.fm";
    final int CASTER_PORT = 19513;
    final String CASTER_MOUNT = "/QKnuH";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        bind();
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 90);
        tick();
        refresh();
        h.postDelayed(new Runnable(){ public void run(){ refresh(); h.postDelayed(this,10000); } }, 10000);
    }

    void bind() {
        status=f(R.id.status); listeners=f(R.id.listeners); bitrate=f(R.id.bitrate); now=f(R.id.now);
        next=f(R.id.next); clock=f(R.id.clock); log=f(R.id.log);
        f(R.id.start).setOnClickListener(v->startLocalAutomation());
        f(R.id.stop).setOnClickListener(v->stopLocalAutomation());
        f(R.id.automation).setOnClickListener(v->toggleLocalAutomation());
        f(R.id.reconnect).setOnClickListener(v->refresh());
        f(R.id.hour).setOnClickListener(v->showHourCerta());
        f(R.id.calls).setOnClickListener(v->showCalls());
        f(R.id.grade).setOnClickListener(v->showGrade());
        f(R.id.library).setOnClickListener(v->pickAudio());
        f(R.id.sermon).setOnClickListener(v->pickSermon());
        f(R.id.live).setOnClickListener(v->toggleLive());
        f(R.id.word).setOnClickListener(v->editBox("Palavra do Dia"));
        f(R.id.devotional).setOnClickListener(v->editBox("Devocional"));
    }

    TextView f(int id){return findViewById(id);}

    void tick(){
        clock.setText(new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));
        h.postDelayed(this::tick,1000);
    }

    void refresh(){
        final android.content.SharedPreferences p=getSharedPreferences("radio",0);
        boolean auto=p.getBoolean("automation_running",false);
        String local=p.getString("now_playing","--");
        String detail=p.getString("state_detail","");
        String err=p.getString("last_error","");
        String nextProgram=nextProgramText();
        now.setText("TOCANDO AGORA\n"+local);
        next.setText("PRÓXIMO NA GRADE\n"+nextProgram);
        Button a=findViewById(R.id.automation);
        a.setText(auto?"⚙ MOTOR DE AUTOMAÇÃO: LIGADO":"⚙ MOTOR DE AUTOMAÇÃO: DESLIGADO");
        Button lv=findViewById(R.id.live);
        lv.setText(p.getBoolean("live_running",false)?"■ SAIR DO AO VIVO":"🎙️ ENTRADA AO VIVO");
        String sn=p.getString("sermon_name","");
        findViewById(R.id.sermon).setContentDescription(sn.isEmpty()?"Escolher pregação":"Pregação: "+sn);
        if(!err.isEmpty()) log.setText("Erro: "+err);
        else if(!detail.isEmpty()) log.setText(detail);

        status.setText("● VERIFICANDO CASTER...");
        status.setTextColor(Color.parseColor("#FBBF24"));
        new Thread(()->{
            try {
                String s=getUrl("https://"+CASTER_HOST+":"+CASTER_PORT+"/admin/publicstats.json",7000);
                CasterStats cs=parseCaster(s);
                runOnUiThread(()->applyCaster(cs,auto));
            } catch(Exception e) {
                try {
                    String s=getUrl("http://"+CASTER_HOST+":"+CASTER_PORT+"/admin/publicstats.json",7000);
                    CasterStats cs=parseCaster(s);
                    runOnUiThread(()->applyCaster(cs,auto));
                } catch(Exception e2) {
                    runOnUiThread(()->{
                        status.setText(auto?"● AUTOMAÇÃO LIGADA • CASTER NÃO RESPONDE":"● CASTER SEM CONEXÃO");
                        status.setTextColor(Color.rgb(239,68,68));
                        listeners.setText("OUVINTES\n--");
                        bitrate.setText("BITRATE\n--");
                        if(err.isEmpty()) log.setText("Não consegui ler o status público do Caster. A automação local pode continuar tentando transmitir.");
                    });
                }
            }
        }).start();
    }

    void applyCaster(CasterStats cs, boolean auto){
        if(cs.onAir){
            status.setText(auto?"● TRANSMISSÃO NO AR • AUTOMAÇÃO LIGADA":"● TRANSMISSÃO NO AR");
            status.setTextColor(Color.rgb(34,197,94));
            listeners.setText("OUVINTES\n"+cs.listeners);
            bitrate.setText("BITRATE\n"+(cs.bitrate>0?cs.bitrate+" Kbps":"96 Kbps"));
            if(!cs.title.isEmpty() && cs.title.length()>1 && "--".equals(now.getText().toString().split("\\n",2).length>1?now.getText().toString().split("\\n",2)[1]:"--"))
                now.setText("TOCANDO AGORA\n"+cs.title);
            log.setText("Caster conectado • mount "+CASTER_MOUNT+" • atualização "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));
        } else {
            status.setText(auto?"● AUTOMAÇÃO LIGADA • TRANSMISSÃO FORA DO AR":"● CASTER ONLINE • FORA DO AR");
            status.setTextColor(Color.rgb(245,158,11));
            listeners.setText("OUVINTES\n0");
            bitrate.setText("BITRATE\n--");
        }
    }

    String getUrl(String url,int timeout)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(timeout); c.setReadTimeout(timeout); c.setUseCaches(false); c.setRequestProperty("Cache-Control","no-cache");
        int code=c.getResponseCode();
        InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        String body=read(in); c.disconnect();
        if(code<200||code>=300) throw new IOException("HTTP "+code);
        return body;
    }

    CasterStats parseCaster(String s)throws Exception{
        CasterStats out=new CasterStats();
        JSONArray root=new JSONArray(s);
        for(int i=0;i<root.length();i++){
            JSONObject o=root.optJSONObject(i); if(o==null) continue;
            JSONObject sources=o.optJSONObject("source"); if(sources==null) continue;
            JSONObject src=sources.optJSONObject(CASTER_MOUNT); if(src==null) continue;
            out.onAir=true;
            out.listeners=src.optInt("listeners",0);
            out.bitrate=src.optInt("bitrate",src.optInt("total_bitrate",0)/1000);
            out.title=src.optString("display-title","");
            if(out.title.isEmpty()){
                JSONObject meta=src.optJSONObject("metadata"); if(meta!=null) out.title=meta.optString("x_icy_title","");
            }
            break;
        }
        return out;
    }

    static class CasterStats { boolean onAir=false; int listeners=0; int bitrate=0; String title=""; }

    void startLocalAutomation(){
        final EditText e=new EditText(this);
        e.setHint("Senha do transmissor Caster"); e.setInputType(129);
        String old=getSharedPreferences("radio",0).getString("source_password",""); if(!old.isEmpty()) e.setText(old);
        new AlertDialog.Builder(this).setTitle("INICIAR AUTOMAÇÃO")
                .setMessage("O celular reserva vai tocar a grade, hora certa e chamadas e enviar MP3 ao Caster.")
                .setView(e).setNegativeButton("CANCELAR",null)
                .setPositiveButton("INICIAR",(d,w)->{
                    String pass=e.getText().toString().trim();
                    if(pass.isEmpty()){Toast.makeText(this,"Digite a senha do transmissor.",Toast.LENGTH_LONG).show();return;}
                    Intent i=new Intent(this,AutomationService.class); i.putExtra("password",pass);
                    if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
                    getSharedPreferences("radio",0).edit().putBoolean("auto_start",true).apply();
                    log.setText("Iniciando automação e conexão com o Caster..."); refresh();
                }).show();
    }

    void toggleLocalAutomation(){
        if(getSharedPreferences("radio",0).getBoolean("automation_running",false)) stopLocalAutomation();
        else startLocalAutomation();
    }

    void stopLocalAutomation(){
        getSharedPreferences("radio",0).edit().putBoolean("auto_start",false).apply();
        stopService(new Intent(this,AutomationService.class));
        log.setText("Automação do celular parada.");
        refresh();
    }

    void showHourCerta(){
        new AlertDialog.Builder(this).setTitle("HORA CERTA")
                .setMessage("A automação usa as 24 chamadas de hora certa (00h a 23h) extraídas do app original.\n\nEla entra automaticamente no início de cada hora enquanto a transmissão estiver ligada.")
                .setPositiveButton("OK",null).show();
    }

    void showCalls(){
        String[] calls={
                "Almoço com Deus — abertura",
                "Reflexão Tarde — abertura",
                "Vem Comigo Tarde — abertura",
                "Café e Glória — abertura",
                "Louvor e Reflexão — abertura",
                "Tardizinha com Deus — abertura",
                "Fé — edição noite — abertura",
                "Noite com Deus — abertura",
                "Madrugada com Deus — abertura",
                "Vinhetas da rádio"
        };
        new AlertDialog.Builder(this).setTitle("CHAMADAS DE PROGRAMAS")
                .setItems(calls,null).setPositiveButton("OK",null)
                .setMessage("As chamadas marcadas na grade entram automaticamente na troca de programa.")
                .show();
    }

    void showGrade(){
        String[] grade={
                "00:00 — 09:00  |  MADRUGADA COM DEUS",
                "09:00 — 09:30  |  PROGRAMA REFLEXÃO",
                "09:30 — 10:00  |  PROGRAMA FÉ MEIA HORA",
                "10:00 — 12:00  |  VEM COMIGO MANHÃ",
                "12:00 — 12:30  |  PROGRAMA MEIA DIA — EDIÇÃO MEIO-DIA",
                "12:30 — 13:30  |  ALMOÇO COM DEUS",
                "13:30 — 14:00  |  REFLEXÃO TARDE",
                "14:00 — 15:00  |  PROGRAMA VEM COMIGO TARDE",
                "15:00 — 16:00  |  VEM COMIGO CAFÉ E GLÓRIA",
                "16:00 — 19:00  |  LOUVOR E REFLEXÃO",
                "19:00 — 21:00  |  TARDIZINHA COM DEUS",
                "21:00 — 21:55  |  PROGRAMA FÉ — EDIÇÃO NOITE",
                "21:55 — 23:55  |  PROGRAMA NOITE COM DEUS",
                "23:55 — 00:00  |  INTERVALO"
        };
        new AlertDialog.Builder(this).setTitle("GRADE DE PROGRAMAÇÃO").setItems(grade,null).setPositiveButton("FECHAR",null).show();
    }

    String nextProgramText(){
        try{
            JSONObject root=new JSONObject(read(getAssets().open("programacao.json")));
            JSONArray a=root.getJSONArray("programas");
            Calendar c=Calendar.getInstance(); int m=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
            for(int i=0;i<a.length();i++){
                JSONObject x=a.getJSONObject(i); int st=mins(x.getString("inicio")); int en=mins(x.getString("fim"));
                if(m<en){
                    if(m<st) return x.getString("inicio")+" • "+x.getString("nome");
                    if(i+1<a.length()) { JSONObject n=a.getJSONObject(i+1); return n.getString("inicio")+" • "+n.getString("nome"); }
                    return "00:00 • MADRUGADA COM DEUS";
                }
            }
        }catch(Exception ignored){}
        return "--";
    }
    int mins(String s){String[] p=s.split(":");return Integer.parseInt(p[0])*60+Integer.parseInt(p[1]);}

    void editBox(String title){
        EditText e=new EditText(this); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setHint("Digite o conteúdo..."); e.setMinLines(5);
        new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->log.setText(title+" atualizado neste painel.")).show();
    }

    void pickAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,77);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==88 && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            Uri u=data.getData(); long br=audioBitrate(u);
            if(br<=0 || br>96000){ Toast.makeText(this,"Pregação recusada: use MP3 de até 96 Kbps.",Toast.LENGTH_LONG).show(); return; }
            try{ getContentResolver().takePersistableUriPermission(u, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){}
            String name=u.getLastPathSegment(); if(name==null||name.isEmpty()) name=u.toString();
            getSharedPreferences("radio",0).edit().putString("sermon_uri",u.toString()).putString("sermon_name",name).putBoolean("sermon_enabled",true).apply();
            log.setText("Pregação escolhida: "+name+" — "+(br/1000)+" Kbps\nEla será inserida no meio de cada programa.");
            Toast.makeText(this,"Pregação escolhida e ativada.",Toast.LENGTH_SHORT).show();
            return;
        }
        if(requestCode!=77 || resultCode!=RESULT_OK || data==null) return;
        ArrayList<Uri> picked=new ArrayList<>();
        ClipData cd=data.getClipData();
        if(cd!=null){
            for(int i=0;i<cd.getItemCount();i++) picked.add(cd.getItemAt(i).getUri());
        } else if(data.getData()!=null) picked.add(data.getData());
        if(picked.isEmpty()) return;
        StringBuilder all=new StringBuilder();
        StringBuilder names=new StringBuilder();
        int accepted=0, rejected=0;
        for(Uri u:picked){
            long br=audioBitrate(u);
            if(br<=0 || br>96000){
                rejected++;
                continue;
            }
            try{
                int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0) getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }catch(Exception ignored){}
            if(all.length()>0) all.append("\n");
            all.append(u.toString());
            String name=u.getLastPathSegment();
            if(name==null || name.isEmpty()) name=u.toString();
            if(names.length()>0) names.append("\n");
            names.append(name).append(" — ").append(br/1000).append(" Kbps");
            accepted++;
        }
        getSharedPreferences("radio",0).edit().putString("user_audio_uris",all.toString()).putInt("user_audio_count",accepted).apply();
        if(accepted>0){
            log.setText("Músicas carregadas: "+accepted+"\n"+names+(rejected>0?"\n\n"+rejected+" arquivo(s) recusado(s): acima de 96 Kbps ou formato sem bitrate identificado.":""));
            Toast.makeText(this,accepted+" música(s) adicionada(s). Limite do Caster: 96 Kbps.",Toast.LENGTH_LONG).show();
        } else {
            log.setText("Nenhuma música adicionada. O Caster aceita no máximo 96 Kbps; escolha MP3 de até 96 Kbps." );
            Toast.makeText(this,"Nenhum áudio aceito: use MP3 de até 96 Kbps.",Toast.LENGTH_LONG).show();
        }
    }


    long audioBitrate(Uri uri){
        MediaExtractor ex=new MediaExtractor();
        try{
            ex.setDataSource(this,uri,null);
            for(int i=0;i<ex.getTrackCount();i++){
                MediaFormat f=ex.getTrackFormat(i);
                String mime=f.getString(MediaFormat.KEY_MIME);
                if(MediaFormat.MIMETYPE_AUDIO_MPEG.equals(mime)){
                    return f.containsKey(MediaFormat.KEY_BIT_RATE)?f.getInteger(MediaFormat.KEY_BIT_RATE):0;
                }
            }
        }catch(Exception ignored){}
        finally{ try{ex.release();}catch(Exception ignored){} }
        return 0;
    }

    String read(InputStream in)throws Exception{
        if(in==null)return ""; BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8")); StringBuilder b=new StringBuilder(); String x;
        while((x=r.readLine())!=null)b.append(x); r.close(); return b.toString();
    }
}
