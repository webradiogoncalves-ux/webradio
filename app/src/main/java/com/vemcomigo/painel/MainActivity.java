package com.vemcomigo.painel;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.provider.Settings;import android.view.*;import android.widget.*;import java.io.*;import java.net.*;import java.text.*;import java.util.*;import java.util.regex.*;

public class MainActivity extends Activity {
 TextView status,listeners,bitrate,now,clock,log; final Handler h=new Handler(Looper.getMainLooper());
 // Secure backend URL: change this to your deployed panel backend. Never put the Caster private token here.
 String backend="http://10.0.2.2:8787";
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);bind();tick(); refresh();}
 void bind(){status=f(R.id.status);listeners=f(R.id.listeners);bitrate=f(R.id.bitrate);now=f(R.id.now);clock=f(R.id.clock);log=f(R.id.log);
  f(R.id.start).setOnClickListener(v->control("start"));f(R.id.stop).setOnClickListener(v->control("stop"));f(R.id.automation).setOnClickListener(v->toggleAutomation());f(R.id.reconnect).setOnClickListener(v->refresh());
  f(R.id.hour).setOnClickListener(v->showSchedule("Hora Certa"));f(R.id.calls).setOnClickListener(v->showSchedule("Chamadas de programas"));f(R.id.grade).setOnClickListener(v->showGrade());
  f(R.id.library).setOnClickListener(v->pickAudio());f(R.id.word).setOnClickListener(v->editBox("Palavra do Dia"));f(R.id.devotional).setOnClickListener(v->editBox("Devocional")); }
 TextView f(int id){return findViewById(id);}
 void tick(){clock.setText(new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));h.postDelayed(this::tick,1000);}
 void refresh(){status.setText("● VERIFICANDO SINAL...");new Thread(()->{try{URL u=new URL(backend+"/api/status");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);String s=read(c.getInputStream());runOnUiThread(()->applyStatus(s));}catch(Exception e){runOnUiThread(()->{status.setText("● PAINEL SEM CONEXÃO");status.setTextColor(Color.rgb(239,68,68));log.setText("Backend não conectado. Configure a URL do painel backend.");});}}).start();}
 void applyStatus(String s){boolean online=s.contains("\"online\":true");status.setText(online?"● RÁDIO ONLINE":"● RÁDIO OFFLINE");status.setTextColor(Color.parseColor(online?"#22C55E":"#EF4444"));listeners.setText("OUVINTES\n"+val(s,"listeners"));bitrate.setText("BITRATE\n"+val(s,"bitrate"));now.setText("TOCANDO AGORA\n"+val(s,"title"));log.setText("Última atualização: "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));}
 String val(String s,String k){Matcher m=Pattern.compile("\\\""+k+"\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9]+))").matcher(s);return m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"--";}
 void control(String action){new Thread(()->{try{URL u=new URL(backend+"/api/server/"+action);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setDoOutput(true);String r=read(c.getInputStream());runOnUiThread(()->{log.setText(action.toUpperCase(Locale.getDefault())+" enviado ao servidor.");refresh();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível executar o comando: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();}
 void toggleAutomation(){new Thread(()->{try{URL u=new URL(backend+"/api/automation/status");HttpURLConnection c=(HttpURLConnection)u.openConnection();String s=read(c.getInputStream());boolean running=s.contains("\"running\":true");String action=running?"stop":"start";URL u2=new URL(backend+"/api/automation/"+action);HttpURLConnection c2=(HttpURLConnection)u2.openConnection();c2.setRequestMethod("POST");c2.setDoOutput(true);c2.getInputStream().close();runOnUiThread(()->{Button b=findViewById(R.id.automation);b.setText(action.equals("start")?"⚙ MOTOR DE AUTOMAÇÃO: LIGADO":"⚙ MOTOR DE AUTOMAÇÃO: DESLIGADO");log.setText(action.equals("start")?"Automação iniciada. A grade assume o sinal.":"Automação parada.");refresh();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Automação: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();}
 void showSchedule(String title){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(30,10,30,10);TextView info=new TextView(this);info.setText("Agende horários e arquivos desta função.\n\nO próximo passo do painel é salvar a grade no armazenamento e executar os áudios nos horários definidos.");info.setTextColor(Color.WHITE);info.setTextSize(16);l.addView(info);new AlertDialog.Builder(this).setTitle(title).setView(l).setPositiveButton("OK",null).show();}

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
   "21:55 — 23:55  |  PROGRAMA NOITE COM DEUS"
  };
  new AlertDialog.Builder(this).setTitle("GRADE DE PROGRAMAÇÃO").setItems(grade,null).setPositiveButton("FECHAR",null).show();
 }
 void editBox(String title){EditText e=new EditText(this);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setHint("Digite o conteúdo...");e.setMinLines(5);new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->log.setText(title+" atualizado neste painel.")).show();}
 void pickAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,77);}
 String read(InputStream in)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(in));StringBuilder b=new StringBuilder();String x;while((x=r.readLine())!=null)b.append(x);return b.toString();}
}
