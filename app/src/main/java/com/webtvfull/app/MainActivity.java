package com.webtvfull.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends ComponentActivity {
    // A URL foi colocada aqui porque o usuário pediu a lista pronta no projeto.
    // ATENÇÃO: ela contém credenciais. Se o repositório for público, troque a senha depois do teste.
    private static final String M3U_URL = "http://195.181.163.138:80/get.php?username=Guilherme0707&password=frp0fy7u8h&type=m3u_plus&output=ts";
    private static final String EPG_URL = "http://195.181.163.138:80/xmltv.php?username=Guilherme0707&password=frp0fy7u8h";

    private PlayerView playerView; private ExoPlayer player; private TextView status; private TextView epg;
    private LinearLayout list; private Spinner category; private final List<Item> items=new ArrayList<>(); private final Map<String,String> epgMap=new HashMap<>();
    private final Map<String,Bitmap> logoCache=new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService logoExecutor=Executors.newFixedThreadPool(4);
    private String currentGroup="Todos";
    private final Map<String,EpgInfo> epgPrograms=new HashMap<>();

    @Override public void onCreate(Bundle b){super.onCreate(b); buildUi(); loadPlaylist();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int sp){ TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(sp); t.setPadding(dp(14),dp(10),dp(14),dp(10)); return t; }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(8,12,18));
        TextView title=text("WEBTV FULL",22); title.setGravity(Gravity.CENTER); title.setTypeface(null,1); root.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        playerView=new PlayerView(this); playerView.setUseController(true); playerView.setBackgroundColor(Color.BLACK); root.addView(playerView,new LinearLayout.LayoutParams(-1,dp(215)));
        status=text("Carregando lista M3U...",14); root.addView(status,new LinearLayout.LayoutParams(-1,dp(44)));
        LinearLayout row=new LinearLayout(this); row.setPadding(dp(8),0,dp(8),0); category=new Spinner(this); row.addView(category,new LinearLayout.LayoutParams(-1,dp(52))); root.addView(row);
        epg=text("EPG: selecione um canal",13); epg.setTextColor(Color.LTGRAY); root.addView(epg,new LinearLayout.LayoutParams(-1,dp(45)));
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){currentGroup=String.valueOf(p.getItemAtPosition(pos)); renderList();}});
    }

    private void loadPlaylist(){
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                URL u=new URL(M3U_URL);
                HttpURLConnection c=(HttpURLConnection)u.openConnection();
                c.setConnectTimeout(20000); c.setReadTimeout(60000);
                c.setRequestProperty("User-Agent","VLC/3.0 WebTVFull/1.0");
                c.setRequestProperty("Accept","*/*");
                c.setRequestProperty("Accept-Encoding","gzip");
                int code=c.getResponseCode();
                if(code<200 || code>=300) throw new IOException("HTTP "+code);
                InputStream raw=c.getInputStream();
                String enc=c.getContentEncoding();
                if(enc!=null && enc.toLowerCase(Locale.US).contains("gzip")) raw=new java.util.zip.GZIPInputStream(raw);
                BufferedReader r=new BufferedReader(new InputStreamReader(raw,java.nio.charset.StandardCharsets.UTF_8),8192);
                String line; Item cur=null;
                while((line=r.readLine())!=null){
                    line=line.trim();
                    if(line.startsWith("#EXTINF:")){
                        cur=parseExtinf(line);
                    }else if(cur!=null && !line.isEmpty() && !line.startsWith("#")){
                        cur.url=line;
                        items.add(cur);
                        cur=null;
                    }
                }
                r.close(); c.disconnect();
                if(items.isEmpty()) throw new IOException("A lista foi recebida, mas nenhum canal M3U foi encontrado");
                runOnUiThread(()->setupCategories());
                // O EPG não pode bloquear a exibição da lista.
                Executors.newSingleThreadExecutor().execute(()->{ loadEpg(); runOnUiThread(()->{ if(epg!=null) epg.setText("EPG disponível • selecione um canal"); }); });
            }catch(Exception e){
                runOnUiThread(()->status.setText("Erro ao carregar M3U: "+e.getMessage()));
            }
        });
    }
    private Item parseExtinf(String l){ Item x=new Item(); int comma=l.indexOf(','); x.name=comma>=0?l.substring(comma+1).trim():"Canal"; x.id=attr(l,"tvg-id"); x.group=attr(l,"group-title"); x.logo=attr(l,"tvg-logo"); if(x.group.isEmpty())x.group="Outros"; return x; }
    private String attr(String s,String key){String p=key+"=\""; int a=s.indexOf(p); if(a<0)return ""; a+=p.length(); int b=s.indexOf('"',a); return b<0?"":s.substring(a,b);}
    private void setupCategories(){ LinkedHashSet<String> cats=new LinkedHashSet<>(); cats.add("Todos"); for(Item x:items)cats.add(x.group); ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new ArrayList<>(cats)){@Override public View getView(int p,View v,android.view.ViewGroup g){TextView t=(TextView)super.getView(p,v,g);t.setTextColor(Color.WHITE);return t;} }; a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); category.setAdapter(a); status.setText(items.size()+" canais carregados • toque em um canal para reproduzir"); renderList(); }
    private void renderList(){
        if(list==null)return;
        list.removeAllViews();
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); grid.setUseDefaultMargins(false);
        int n=0;
        for(Item x:items){
            if(!currentGroup.equals("Todos")&&!x.group.equals(currentGroup))continue;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER); card.setPadding(dp(7),dp(8),dp(7),dp(8));
            GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(43,47,53)); bg.setCornerRadius(dp(7)); card.setBackground(bg);
            ImageView logo=new ImageView(this); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); logo.setImageResource(android.R.drawable.ic_menu_gallery);
            card.addView(logo,new LinearLayout.LayoutParams(-1,dp(78)));
            TextView name=text(x.name,14); name.setGravity(Gravity.CENTER); name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END); card.addView(name,new LinearLayout.LayoutParams(-1,dp(48)));
            card.setOnClickListener(v->play(x));
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams(); gp.width=0; gp.height=dp(132); gp.columnSpec=GridLayout.spec(n%2,1f); gp.rowSpec=GridLayout.spec(n/2); gp.setMargins(dp(5),dp(5),dp(5),dp(5)); grid.addView(card,gp);
            loadLogo(x,logo); n++;
        }
        if(n==0){TextView t=text("Nenhum canal nesta categoria.",14);list.addView(t);} else list.addView(grid,new LinearLayout.LayoutParams(-1,-2));
    }

    private void loadLogo(Item x, ImageView target){
        if(x.logo==null||x.logo.isEmpty()){ target.setImageResource(android.R.drawable.ic_menu_gallery); return; }
        Bitmap cached=logoCache.get(x.logo); if(cached!=null){target.setImageBitmap(cached);return;}
        logoExecutor.execute(()->{
            try{ HttpURLConnection c=(HttpURLConnection)new URL(x.logo).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(12000); c.setRequestProperty("User-Agent","WebTVFull/1.0"); c.connect(); Bitmap b=BitmapFactory.decodeStream(c.getInputStream()); c.disconnect(); if(b!=null){logoCache.put(x.logo,b); runOnUiThread(()->target.setImageBitmap(b));} }
            catch(Exception ignored){}
        });
    }
    private void play(Item x){ status.setText("Abrindo: "+x.name); epg.setText("EPG: "+formatEpg(x)); if(player!=null)player.release(); player=new ExoPlayer.Builder(this).build(); playerView.setPlayer(player); MediaItem.Builder mb=new MediaItem.Builder().setUri(Uri.parse(x.url)); String low=x.url.toLowerCase(); if(low.contains(".m3u8"))mb.setMimeType(MimeTypes.APPLICATION_M3U8); else if(low.contains(".mpd"))mb.setMimeType(MimeTypes.APPLICATION_MPD); player.setMediaItem(mb.build()); player.prepare(); player.play(); }
    private String formatEpg(Item x){
        EpgInfo e=epgPrograms.get(x.id);
        if(e!=null){ String cur=e.current==null?"sem programa atual":e.current; String next=e.next==null?"": " • Próximo: "+e.next; return "EPG: "+cur+next; }
        return "EPG: "+epgMap.getOrDefault(x.id,"sem programação encontrada");
    }
    private void loadEpg(){
        try{
            URL u=new URL(EPG_URL); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setConnectTimeout(10000); c.setReadTimeout(30000);
            InputStream in=c.getInputStream(); XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser(); p.setInput(in,"UTF-8");
            int e; String channelId=null, channelName=null, progId=null, progTitle=null; Date progStart=null, progStop=null; long now=System.currentTimeMillis();
            while((e=p.next())!=XmlPullParser.END_DOCUMENT){
                if(e==XmlPullParser.START_TAG){
                    String n=p.getName();
                    if(n.equals("channel")){ channelId=p.getAttributeValue(null,"id"); }
                    else if(n.equals("display-name") && channelId!=null){ String v=p.nextText(); if(channelName==null)channelName=v; if(!epgMap.containsKey(channelId))epgMap.put(channelId,v); }
                    else if(n.equals("programme")){ progId=p.getAttributeValue(null,"channel"); progStart=parseXmlTvDate(p.getAttributeValue(null,"start")); progStop=parseXmlTvDate(p.getAttributeValue(null,"stop")); progTitle=null; }
                    else if(n.equals("title") && progId!=null){ progTitle=p.nextText(); }
                } else if(e==XmlPullParser.END_TAG){
                    String n=p.getName();
                    if(n.equals("programme") && progId!=null && progTitle!=null && progStart!=null){
                        EpgInfo info=epgPrograms.get(progId); if(info==null)info=new EpgInfo();
                        long st=progStart.getTime(), sp=progStop==null?Long.MAX_VALUE:progStop.getTime();
                        if(st<=now && now<sp) info.current=progTitle; else if(st>now && (info.nextStart==0 || st<info.nextStart)){ info.next=progTitle; info.nextStart=st; }
                        epgPrograms.put(progId,info);
                    }
                    if(n.equals("channel")){channelId=null;channelName=null;}
                }
            }
            in.close();
        }catch(Exception ignored){}
    }
    private Date parseXmlTvDate(String v){ if(v==null||v.isEmpty())return null; try{ String t=v.trim(); String[] fmts={"yyyyMMddHHmmss Z","yyyyMMddHHmmss","yyyyMMddHHmmssXXX"}; for(String f:fmts){try{java.text.SimpleDateFormat d=new java.text.SimpleDateFormat(f,Locale.US); if(!f.contains("Z")&&!f.contains("X"))d.setTimeZone(java.util.TimeZone.getDefault()); return d.parse(t);}catch(Exception ignored){}} }catch(Exception ignored){} return null; }
    static class EpgInfo{String current,next;long nextStart;}
    @Override protected void onDestroy(){if(player!=null)player.release(); logoExecutor.shutdownNow(); super.onDestroy();}
    static class Item{String name="",url="",group="",id="",logo="";}
}
