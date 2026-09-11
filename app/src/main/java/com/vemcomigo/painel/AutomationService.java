package com.vemcomigo.painel;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.*;
import java.util.*;
import org.json.*;

/** 24h local automation + Icecast AAC source client. */
public class AutomationService extends Service {
    private static final String CH="automation_radio";
    private Handler handler;
    private MediaExtractor extractor;
    private MediaCodec decoder, encoder;
    private Thread worker;
    private volatile boolean running=false;
    private IcecastAacStreamer streamer;
    private JSONObject schedule;
    private String currentName="";

    @Override public void onCreate(){ super.onCreate(); handler=new Handler(Looper.getMainLooper()); createChannel(); }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel(CH,"Automação da Web Rádio",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c);} }
    private Notification notification(String text){ Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH):new Notification.Builder(this); b.setContentTitle("Web Rádio Vem Comigo no Glória").setContentText(text).setSmallIcon(com.vemcomigo.painel.R.drawable.radio_logo).setOngoing(true); return b.build(); }

    @Override public int onStartCommand(Intent i,int flags,int id){
        String pass=i!=null?i.getStringExtra("password"):null;
        if(pass!=null) getSharedPreferences("radio",0).edit().putString("source_password",pass).apply();
        if(!running) startEngine();
        return START_STICKY;
    }
    private void startEngine(){
        running=true; startForeground(10,notification("Automação ligada"));
        worker=new Thread(this::runLoop,"radio-automation"); worker.start();
    }
    private void runLoop(){
        try{ schedule=loadSchedule(); while(running){ JSONObject item=currentSlot(); if(item!=null){ String n=item.optString("nome"); if(!n.equals(currentName) || streamer==null || !streamer.isAlive()){ stopMedia(); currentName=n; playAsset(item.optString("audio")); } } else { if(!"INTERVALO".equals(currentName) || streamer==null || !streamer.isAlive()){ stopMedia(); currentName="INTERVALO"; startSilence(); } } updateNotification(currentName); Thread.sleep(1500); } }catch(Exception e){ updateNotification("Erro: "+e.getMessage()); stopMedia(); } }
    private JSONObject loadSchedule() throws Exception{ InputStream in=getAssets().open("programacao.json"); byte[] b=readAll(in); return new JSONObject(new String(b,"UTF-8")); }
    private JSONObject currentSlot() throws JSONException { Calendar c=Calendar.getInstance(); int n=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE); JSONArray a=schedule.getJSONArray("programas"); for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i); if(n>=min(x.optString("inicio")) && n<min(x.optString("fim"))) return x;} return null; }
    private int min(String s){String[] p=s.split(":"); return Integer.parseInt(p[0])*60+Integer.parseInt(p[1]);}
    private void updateNotification(String n){ if(Build.VERSION.SDK_INT>=26) { ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(10,notification("● ON AIR • "+n)); } }

    private void playAsset(String name) throws Exception{
        AssetFileDescriptor afd=getAssets().openFd("programas/"+name);
        extractor=new MediaExtractor(); extractor.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength()); afd.close();
        int track=-1; for(int i=0;i<extractor.getTrackCount();i++){String m=extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME); if(m!=null&&m.startsWith("audio/")){track=i;break;}}
        if(track<0) throw new IOException("Sem faixa de áudio: "+name); extractor.selectTrack(track); MediaFormat in=extractor.getTrackFormat(track); String mime=in.getString(MediaFormat.KEY_MIME);
        decoder=MediaCodec.createDecoderByType(mime); decoder.configure(in,null,null,0); decoder.start();
        MediaFormat out=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,24000,1); out.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC); out.setInteger(MediaFormat.KEY_BIT_RATE,96000); out.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384);
        encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC); encoder.configure(out,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); encoder.start();
        streamer=new IcecastAacStreamer(getSharedPreferences("radio",0).getString("source_password","")); streamer.connect();
        decodeEncodeLoop();
    }
    private void startSilence() throws Exception{
        MediaFormat out=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,24000,1); out.setInteger(MediaFormat.KEY_AAC_PROFILE,2); out.setInteger(MediaFormat.KEY_BIT_RATE,96000); out.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384);
        encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC); encoder.configure(out,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); encoder.start();
        streamer=new IcecastAacStreamer(getSharedPreferences("radio",0).getString("source_password","")); streamer.connect();
        byte[] silence=new byte[4096]; while(running && "INTERVALO".equals(currentName)){ feedEncoder(silence,4096); Thread.sleep(20); }
    }
    private void decodeEncodeLoop() throws Exception{
        MediaCodec.BufferInfo di=new MediaCodec.BufferInfo(); MediaCodec.BufferInfo ei=new MediaCodec.BufferInfo(); boolean inputDone=false, outputDone=false;
        while(running && currentName!=null){
            if(!inputDone){int in=decoder.dequeueInputBuffer(10000); if(in>=0){ByteBufferWrapper buf=new ByteBufferWrapper(decoder.getInputBuffer(in)); int n=extractor.readSampleData(buf.b,0); if(n<0){decoder.queueInputBuffer(in,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone=true;} else {decoder.queueInputBuffer(in,0,n,extractor.getSampleTime(),0); extractor.advance();}}}
            int out=decoder.dequeueOutputBuffer(di,10000); if(out>=0){ByteBufferWrapper pcm=new ByteBufferWrapper(decoder.getOutputBuffer(out)); byte[] data=new byte[di.size]; pcm.b.position(di.offset); pcm.b.get(data); encoderInput(data); decoder.releaseOutputBuffer(out,false); if((di.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0) outputDone=true; }
            drainEncoder(ei);
            if(outputDone){ extractor.seekTo(0,MediaExtractor.SEEK_TO_CLOSEST_SYNC); inputDone=false; outputDone=false; }
            if(schedule!=null){ JSONObject s=currentSlot(); if(s==null || !s.optString("nome").equals(currentName)) break; }
        }
    }
    private void encoderInput(byte[] pcm) throws Exception{int in=encoder.dequeueInputBuffer(10000); if(in>=0){ByteBufferWrapper b=new ByteBufferWrapper(encoder.getInputBuffer(in)); b.b.clear(); b.b.put(pcm); encoder.queueInputBuffer(in,0,pcm.length,0,0);}}
    private void feedEncoder(byte[] pcm,int len) throws Exception{int in=encoder.dequeueInputBuffer(10000); if(in>=0){ByteBufferWrapper b=new ByteBufferWrapper(encoder.getInputBuffer(in)); b.b.clear(); b.b.put(pcm,0,len); encoder.queueInputBuffer(in,0,len,0,0);} MediaCodec.BufferInfo ei=new MediaCodec.BufferInfo(); drainEncoder(ei);}
    private void drainEncoder(MediaCodec.BufferInfo info) throws Exception{if(encoder==null)return; while(true){int o=encoder.dequeueOutputBuffer(info,0); if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){ if(streamer!=null) streamer.setFormat(encoder.getOutputFormat()); continue;} if(o<0) break; ByteBufferWrapper b=new ByteBufferWrapper(encoder.getOutputBuffer(o)); byte[] data=new byte[info.size]; b.b.position(info.offset); b.b.get(data); if(streamer!=null && info.size>0) streamer.send(data,encoder.getOutputFormat()); encoder.releaseOutputBuffer(o,false);}}
    private void stopMedia(){try{if(extractor!=null)extractor.release();}catch(Exception ignored){} try{if(decoder!=null)decoder.stop();decoder.release();}catch(Exception ignored){} try{if(encoder!=null)encoder.stop();encoder.release();}catch(Exception ignored){} try{if(streamer!=null)streamer.close();}catch(Exception ignored){} extractor=null;decoder=null;encoder=null;streamer=null;}
    @Override public void onDestroy(){running=false;stopMedia();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
    private static byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)o.write(b,0,n);return o.toByteArray();}
    static class ByteBufferWrapper{java.nio.ByteBuffer b; ByteBufferWrapper(java.nio.ByteBuffer x){b=x;}}

    static class IcecastAacStreamer{
        final String password; Socket socket; OutputStream out; String host="sapircast.caster.fm"; int port=19513; String mount="QKnuH"; boolean alive=false;
        IcecastAacStreamer(String p){password=p;}
        void connect() throws Exception{if(password==null||password.isEmpty())throw new IOException("Informe a senha do transmissor no painel"); socket=new Socket();socket.connect(new java.net.InetSocketAddress(host,port),8000); out=socket.getOutputStream(); String auth=android.util.Base64.encodeToString(("source:"+password).getBytes("UTF-8"),android.util.Base64.NO_WRAP); String h="PUT /"+mount+" HTTP/1.0\r\nAuthorization: Basic "+auth+"\r\nContent-Type: audio/aac\r\nUser-Agent: VemComigoAndroid/1.0\r\n\r\n"; out.write(h.getBytes("UTF-8"));out.flush(); alive=true; }
        void setFormat(MediaFormat f){ }
        void send(byte[] raw,MediaFormat f)throws Exception{if(!alive)return; byte[] adts=adtsHeader(raw.length,24000,1);out.write(adts);out.write(raw);out.flush();}
        boolean isAlive(){return alive && socket!=null&&!socket.isClosed();}
        byte[] adtsHeader(int len,int sr,int ch){int freq=4; if(sr==48000)freq=3; else if(sr==32000)freq=5; else if(sr==24000)freq=6; else if(sr==22050)freq=7; else if(sr==16000)freq=8; byte[] h=new byte[7];int frame=len+7;h[0]=(byte)0xFF;h[1]=(byte)0xF1;h[2]=(byte)(((2-1)<<6)|(freq<<2)|(ch>>2));h[3]=(byte)(((ch&3)<<6)|(frame>>11));h[4]=(byte)((frame>>3)&0xFF);h[5]=(byte)(((frame&7)<<5)|0x1F);h[6]=(byte)0xFC;return h;}
        void close(){alive=false;try{if(out!=null)out.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}
    }
}
