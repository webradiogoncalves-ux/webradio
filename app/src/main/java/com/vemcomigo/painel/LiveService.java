package com.vemcomigo.painel;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

/** Entrada ao vivo pelo microfone. Codifica AAC-LC 44.1 kHz/64 kbps e envia ADTS ao Caster. */
public class LiveService extends Service {
    private static final String CH="live_radio";
    private static final int ID=21;
    private volatile boolean running=false;
    private Thread worker;
    private AudioRecord recorder;
    private MediaCodec encoder;
    private LiveCaster caster;

    private android.content.SharedPreferences prefs(){return getSharedPreferences("radio",MODE_PRIVATE);}

    @Override public void onCreate(){super.onCreate();
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CH,"Entrada ao vivo",NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    private Notification notification(String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH):new Notification.Builder(this);
        return b.setContentTitle("Web Rádio Vem Comigo no Glória").setContentText(text)
                .setSmallIcon(R.drawable.radio_logo).setOngoing(true).build();
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!running) startLive();
        return START_NOT_STICKY;
    }
    private void startLive(){
        running=true;
        startForeground(ID,notification("🎙️ AO VIVO • preparando microfone..."));
        prefs().edit().putBoolean("live_running",true).putString("now_playing","🎙️ AO VIVO").putString("state_detail","Microfone ao vivo").apply();
        worker=new Thread(this::runLive,"radio-live"); worker.start();
    }
    private void runLive(){
        try{
            String pass=prefs().getString("source_password","");
            if(pass.isEmpty()) throw new IOException("Informe a senha do transmissor no painel primeiro.");
            caster=new LiveCaster(pass); caster.connect();
            int sampleRate=44100, channels=1, bitrate=64000;
            int min=AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0) min=4096;
            recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min*2,8192));
            MediaFormat fmt=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,channels);
            fmt.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            fmt.setInteger(MediaFormat.KEY_BIT_RATE,bitrate);
            fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384);
            encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            encoder.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            recorder.startRecording();
            update("🎙️ AO VIVO • microfone ligado");
            byte[] pcm=new byte[4096];
            long ptsUs=0;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(running){
                int n=recorder.read(pcm,0,pcm.length);
                if(n<=0) continue;
                int inIndex=encoder.dequeueInputBuffer(10000);
                if(inIndex>=0){
                    ByteBuffer in=encoder.getInputBuffer(inIndex);
                    if(in!=null){in.clear(); in.put(pcm,0,n); encoder.queueInputBuffer(inIndex,0,n,ptsUs,0);}
                    ptsUs += (long)n*1000000L/(sampleRate*2);
                }
                drain(info,false);
            }
            try{ if(encoder!=null){int idx=encoder.dequeueInputBuffer(10000); if(idx>=0) encoder.queueInputBuffer(idx,0,0,ptsUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM); drain(info,true);} }catch(Exception ignored){}
        }catch(Throwable e){
            prefs().edit().putString("last_error","Ao vivo: "+safe(e)).apply();
            update("Ao vivo parou: "+safe(e));
        }finally{ cleanup(); }
    }
    private void drain(MediaCodec.BufferInfo info,boolean eos)throws Exception{
        while(running || eos){
            int out=encoder.dequeueOutputBuffer(info,1000);
            if(out==MediaCodec.INFO_TRY_AGAIN_LATER) break;
            if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) continue;
            if(out<0) continue;
            ByteBuffer b=encoder.getOutputBuffer(out);
            if(b!=null && info.size>0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                b.position(info.offset); b.limit(info.offset+info.size);
                byte[] a=new byte[info.size]; b.get(a); caster.send(adts(a,44100,1,2));
            }
            encoder.releaseOutputBuffer(out,false);
            if((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0) break;
            if(!eos) break;
        }
    }
    private byte[] adts(byte[] a,int sr,int ch,int profile){
        int[] rates={96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
        int fi=4; for(int i=0;i<rates.length;i++) if(rates[i]==sr){fi=i;break;}
        int len=a.length+7; byte[] h=new byte[len];
        h[0]=(byte)0xFF; h[1]=(byte)0xF1;
        h[2]=(byte)(((profile-1)<<6)|(fi<<2)|(ch>>2));
        h[3]=(byte)(((ch&3)<<6)|(len>>11));
        h[4]=(byte)((len>>3)&0xFF); h[5]=(byte)(((len&7)<<5)|0x1F); h[6]=(byte)0xFC;
        System.arraycopy(a,0,h,7,a.length); return h;
    }
    private void update(String text){
        prefs().edit().putString("state_detail",text).putString("now_playing",text).putBoolean("live_running",running).apply();
        if(Build.VERSION.SDK_INT>=26) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(ID,notification(text));
    }
    private void cleanup(){
        running=false; try{if(recorder!=null){recorder.stop();recorder.release();}}catch(Exception ignored){} recorder=null;
        try{if(encoder!=null){encoder.stop();encoder.release();}}catch(Exception ignored){} encoder=null;
        try{if(caster!=null)caster.close();}catch(Exception ignored){} caster=null;
        prefs().edit().putBoolean("live_running",false).apply(); stopForeground(true);
    }
    private String safe(Throwable e){String s=e.getMessage();return s==null?e.getClass().getSimpleName():s;}
    @Override public void onDestroy(){cleanup();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}

    static class LiveCaster{
        final String password; Socket socket; OutputStream out; InputStream in; boolean alive;
        final String host="sapircast.caster.fm"; final int port=19513; final String mount="/QKnuH";
        LiveCaster(String p){password=p;}
        void connect()throws Exception{
            socket=new Socket(); socket.connect(new InetSocketAddress(host,port),10000); socket.setSoTimeout(8000);
            out=socket.getOutputStream(); in=socket.getInputStream();
            String auth=android.util.Base64.encodeToString(("source:"+password).getBytes("UTF-8"),android.util.Base64.NO_WRAP);
            String req="SOURCE "+mount+" HTTP/1.0\r\nAuthorization: Basic "+auth+"\r\nContent-Type: audio/aac\r\nIce-Name: Web Radio Vem no Gloria - AO VIVO\r\nIce-Genre: Gospel\r\nIce-Public: 1\r\nIce-Audio-Info: ice-samplerate=44100;ice-bitrate=64;ice-channels=1\r\nUser-Agent: VemComigoAndroid-Live/1.0\r\n\r\n";
            out.write(req.getBytes("UTF-8"));out.flush(); String hs=readHeaders(in);
            if(!(hs.contains(" 200 ")||hs.contains(" 100 ")||hs.startsWith("ICY 200"))) throw new IOException("Caster recusou ao vivo: "+first(hs)); alive=true;
        }
        void send(byte[] b)throws Exception{if(!alive)throw new IOException("Ao vivo desconectado");out.write(b);out.flush();}
        void close(){alive=false;try{if(out!=null)out.close();}catch(Exception ignored){}try{if(in!=null)in.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}
        static String readHeaders(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();int p=-1,c;while((c=in.read())!=-1){b.write(c);if(p=='\r'&&c=='\n'){byte[] x=b.toByteArray();int n=x.length;if(n>=4&&x[n-4]=='\r'&&x[n-3]=='\n'&&x[n-2]=='\r'&&x[n-1]=='\n')break;}p=c;if(b.size()>8192)break;}return b.toString("UTF-8");}
        static String first(String s){int i=s.indexOf('\n');return (i<0?s:s.substring(0,i)).trim();}
    }
}
