package com.vemcomigo.painel;

import android.app.*;
import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import android.net.Uri;
import java.util.*;
import org.json.*;

/**
 * Automação local do celular reserva.
 * Envia MP3 já codificado para o Caster via protocolo SOURCE/Icecast.
 */
public class AutomationService extends Service {
    private static final String CH = "automation_radio";
    private static final int NOTIFICATION_ID = 10;
    private volatile boolean running = false;
    private Thread worker;
    private PowerManager.WakeLock wakeLock;
    private IcecastMp3Streamer streamer;
    private JSONObject schedule;
    private String currentName = "";
    private String currentAudio = "";
    private String lastHourCue = "";
    private String sermonSlotKey = "";
    private boolean sermonPlaying = false;

    private final android.content.SharedPreferences prefs() {
        return getSharedPreferences("radio", MODE_PRIVATE);
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VemComigo:RadioAutomation");
        wakeLock.setReferenceCounted(false);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CH, "Automação da Web Rádio", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH)
                : new Notification.Builder(this);
        return b.setContentTitle("Web Rádio Vem Comigo no Glória")
                .setContentText(text)
                .setSmallIcon(com.vemcomigo.painel.R.drawable.radio_logo)
                .setOngoing(true)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String pass = intent != null ? intent.getStringExtra("password") : null;
        if (pass != null) prefs().edit().putString("source_password", pass).apply();
        if (!running) startEngine();
        return START_STICKY;
    }

    private void startEngine() {
        running = true;
        startForeground(NOTIFICATION_ID, notification("Automação ligada • conectando ao Caster..."));
        prefs().edit().putBoolean("automation_running", true).putString("last_error", "").apply();
        try{ if(wakeLock!=null && !wakeLock.isHeld()) wakeLock.acquire(); }catch(Exception ignored){}
        worker = new Thread(this::runLoop, "radio-automation");
        worker.start();
    }

    private void runLoop() {
        try {
            schedule = loadSchedule();
            while (running) {
                try {
                JSONObject item = currentSlot();
                if (item == null) {
                    stopStreamer();
                    setState("INTERVALO", "", "Sem programa neste horário");
                    sleepQuiet(1000);
                    continue;
                }

                String name = item.optString("nome", "PROGRAMA");
                String audio = item.optString("audio", "");
                if (audio.isEmpty()) throw new IOException("Áudio não definido para " + name);

                if (!name.equals(currentName) || !audio.equals(currentAudio) || streamer == null || !streamer.isAlive()) {
                    stopStreamer();
                    currentName = name;
                    currentAudio = audio;
                    lastHourCue = "";
                    connectStreamer();
                    setState(name, audio, "Transmitindo");

                    // Chamada de abertura do programa, quando existir no APK original.
                    String call = item.optString("chamada_inicio", "");
                    if (!call.isEmpty()) streamAsset("chamadas/" + call, "📢 " + name);
                }

                // Hora certa: entra no início de cada hora e depois volta ao programa.
                Calendar now = Calendar.getInstance();
                String hourKey = String.format(Locale.US, "%04d-%02d-%02d-%02d",
                        now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                        now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY));
                if (now.get(Calendar.MINUTE) == 0 && now.get(Calendar.SECOND) < 12 && !hourKey.equals(lastHourCue)) {
                    lastHourCue = hourKey;
                    String hourFile = String.format(Locale.US, "hora_certa/hrs%02d_o.mp3", now.get(Calendar.HOUR_OF_DAY));
                    streamAsset(hourFile, "⏰ HORA CERTA");
                }

                // Pregação automática: entra uma vez no meio de cada bloco da grade.
                if (!sermonPlaying && shouldPlaySermon(item)) {
                    String sermon = prefs().getString("sermon_uri", "");
                    if (!sermon.isEmpty()) {
                        sermonPlaying = true;
                        try {
                            streamUri(Uri.parse(sermon), "🙏 PREGAÇÃO • " + displayName(Uri.parse(sermon)), name);
                        } finally {
                            sermonSlotKey = slotKey(item);
                            sermonPlaying = false;
                            setState(name, audio, "Pregação concluída • programa continua");
                        }
                    }
                }

                // Se o usuário selecionou músicas em "PASTA DE ÁUDIOS", elas
                // substituem o clipe de teste da grade e ficam em sequência até a
                // virada do próximo programa. Sem músicas selecionadas, usa o áudio
                // padrão empacotado na programação.
                List<String> userUris = getUserAudioUris();
                if (!userUris.isEmpty()) {
                    for (String uri : userUris) {
                        if (!running) break;
                        JSONObject slotNow = currentSlot();
                        if (slotNow == null || !slotNow.optString("nome", "").equals(currentName)) break;
                        streamUri(Uri.parse(uri), displayName(Uri.parse(uri)), name);
                    }
                } else {
                    streamAsset("programas_mp3/" + audio, name);
                }
                sleepQuiet(150);
                } catch (Throwable e) {
                    String msg=safeMessage(e);
                    prefs().edit().putString("last_error", msg).putBoolean("caster_connected", false).apply();
                    setState(currentName.isEmpty() ? "CONECTANDO" : currentName, currentAudio, "Reconectando ao Caster: " + msg);
                    stopStreamer();
                    sleepQuiet(5000);
                }
            }
        } catch (Throwable e) {
            prefs().edit().putString("last_error", safeMessage(e)).apply();
            setState(currentName.isEmpty() ? "ERRO" : currentName, currentAudio, "ERRO: " + safeMessage(e));
        } finally {
            running = false;
            stopStreamer();
            prefs().edit().putBoolean("automation_running", false).apply();
            stopForeground(true);
            try{ if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release(); }catch(Exception ignored){}
        }
    }

    private void connectStreamer() throws Exception {
        String pass = prefs().getString("source_password", "");
        if (pass.isEmpty()) throw new IOException("Informe a senha do transmissor no painel.");
        streamer = new IcecastMp3Streamer(pass);
        streamer.connect();
        prefs().edit().putBoolean("caster_connected", true).putString("last_error", "").apply();
        updateNotification("● NO AR • " + currentName);
    }

    private String slotKey(JSONObject item) {
        Calendar c=Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d-%s", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH), item.optString("inicio",""));
    }

    private boolean shouldPlaySermon(JSONObject item) {
        if (prefs().getBoolean("sermon_enabled", false)==false) return false;
        if (prefs().getString("sermon_uri", "").isEmpty()) return false;
        String key=slotKey(item);
        if (key.equals(sermonSlotKey)) return false;
        int start=minutes(item.optString("inicio","00:00"));
        int end=minutes(item.optString("fim","00:00"));
        Calendar c=Calendar.getInstance();
        int now=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
        if (end<=start) end+=1440;
        int cur=now;
        if (now<start) cur+=1440;
        int middle=start+(end-start)/2;
        return cur>=middle && cur<middle+1;
    }

    private List<String> getUserAudioUris() {
        String raw = prefs().getString("user_audio_uris", "");
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        for (String x : raw.split("\\n")) {
            if (!x.trim().isEmpty()) out.add(x.trim());
        }
        return out;
    }

    private String displayName(Uri uri) {
        String s = uri.getLastPathSegment();
        if (s == null || s.isEmpty()) s = uri.toString();
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < s.length()) s = s.substring(slash + 1);
        return s;
    }

    /** Streams a user-selected MP3 through the same Icecast connection. */
    private void streamUri(Uri uri, String title, String programName) throws Exception {
        if (!running || streamer == null || !streamer.isAlive()) return;
        MediaExtractor ex = new MediaExtractor();
        try {
            ex.setDataSource(this, uri, null);
            int track = -1;
            for (int i=0;i<ex.getTrackCount();i++) {
                MediaFormat f=ex.getTrackFormat(i);
                String mime=f.getString(MediaFormat.KEY_MIME);
                if (mime != null && (mime.equals(MediaFormat.MIMETYPE_AUDIO_MPEG) || mime.startsWith("audio/"))) { track=i; break; }
            }
            if(track<0) throw new IOException("Arquivo sem faixa de áudio: "+title);
            ex.selectTrack(track);
            updateNotification("● NO AR • " + title);
            prefs().edit().putString("now_playing", title).putString("current_audio", uri.toString()).putString("state_detail", "Transmitindo • "+programName).apply();
            streamExtractor(ex, programName, false);
        } finally { ex.release(); }
    }

    private void streamExtractor(MediaExtractor ex, String programName, boolean builtIn) throws Exception {
        ByteBuffer buffer=ByteBuffer.allocateDirect(64*1024);
        long previousPts=-1;
        while(running){
            // A Hora Certa também interrompe uma música escolhida pelo usuário.
            Calendar hc = Calendar.getInstance();
            String hk = String.format(Locale.US, "%04d-%02d-%02d-%02d",
                    hc.get(Calendar.YEAR), hc.get(Calendar.MONTH)+1,
                    hc.get(Calendar.DAY_OF_MONTH), hc.get(Calendar.HOUR_OF_DAY));
            if (hc.get(Calendar.MINUTE)==0 && hc.get(Calendar.SECOND)<12 && !hk.equals(lastHourCue)) {
                lastHourCue = hk;
                String hf = String.format(Locale.US, "hora_certa/hrs%02d_o.mp3", hc.get(Calendar.HOUR_OF_DAY));
                streamAsset(hf, "⏰ HORA CERTA");
                continue;
            }
            int size=ex.readSampleData(buffer,0);
            if(size<0) break;
            long pts=ex.getSampleTime();
            buffer.position(0);
            byte[] frame=new byte[size];
            buffer.get(frame);
            streamer.send(frame);
            ex.advance();
            long nextPts=ex.getSampleTime();
            long waitUs=0;
            if(nextPts>=0 && pts>=0) waitUs=nextPts-pts;
            else if(previousPts>=0 && pts>=0) waitUs=pts-previousPts;
            if(waitUs>0 && waitUs<1000000) sleepQuiet(Math.max(1,waitUs/1000));
            previousPts=pts;
            JSONObject slot=currentSlot();
            if(slot==null || !slot.optString("nome","").equals(currentName)) break;
        }
    }

    /** Streams one MP3 asset at its real sample timestamps. */
    private void streamAsset(String assetPath, String label) throws Exception {
        if (!running || streamer == null || !streamer.isAlive()) return;
        AssetManager am = getAssets();
        MediaExtractor ex = new MediaExtractor();
        AssetFileDescriptor afd = am.openFd(assetPath);
        try {
            ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } finally {
            afd.close();
        }

        int track = -1;
        for (int i = 0; i < ex.getTrackCount(); i++) {
            MediaFormat f = ex.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.equals(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
                track = i;
                break;
            }
        }
        if (track < 0) {
            ex.release();
            throw new IOException("MP3 inválido: " + assetPath);
        }
        ex.selectTrack(track);
        updateNotification("● NO AR • " + label);
        prefs().edit().putString("now_playing", label).putString("current_audio", assetPath).apply();

        ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);
        long previousPts = -1;
        try {
            while (running) {
                int size = ex.readSampleData(buffer, 0);
                if (size < 0) break;
                long pts = ex.getSampleTime();
                buffer.position(0);
                byte[] frame = new byte[size];
                buffer.get(frame);
                streamer.send(frame);
                ex.advance();

                // Se for áudio de programa, verifica a virada da hora sem esperar
                // terminar os ~15 segundos do clipe.
                if (assetPath.startsWith("programas_mp3/")) {
                    Calendar hc = Calendar.getInstance();
                    String hk = String.format(Locale.US, "%04d-%02d-%02d-%02d",
                            hc.get(Calendar.YEAR), hc.get(Calendar.MONTH)+1,
                            hc.get(Calendar.DAY_OF_MONTH), hc.get(Calendar.HOUR_OF_DAY));
                    if (hc.get(Calendar.MINUTE)==0 && hc.get(Calendar.SECOND)<12 && !hk.equals(lastHourCue)) {
                        lastHourCue = hk;
                        String hf = String.format(Locale.US, "hora_certa/hrs%02d_o.mp3", hc.get(Calendar.HOUR_OF_DAY));
                        streamAsset(hf, "⏰ HORA CERTA");
                    }
                }

                long nextPts = ex.getSampleTime();
                long waitUs = 0;
                if (nextPts >= 0 && pts >= 0) waitUs = nextPts - pts;
                else if (previousPts >= 0 && pts >= 0) waitUs = pts - previousPts;
                if (waitUs > 0 && waitUs < 1000000) sleepQuiet(Math.max(1, waitUs / 1000));
                previousPts = pts;

                // Não deixa a grade avançar para outro programa durante um arquivo.
                JSONObject slot = currentSlot();
                if (slot == null || !slot.optString("nome", "").equals(currentName)) break;
            }
        } finally {
            ex.release();
        }
    }

    private JSONObject loadSchedule() throws Exception {
        InputStream in = getAssets().open("programacao.json");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        in.close();
        return new JSONObject(new String(out.toByteArray(), "UTF-8"));
    }

    private JSONObject currentSlot() throws JSONException {
        Calendar c = Calendar.getInstance();
        int minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        JSONArray a = schedule.getJSONArray("programas");
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.getJSONObject(i);
            int start = minutes(x.optString("inicio"));
            int end = minutes(x.optString("fim"));
            if (minutes >= start && minutes < end) return x;
        }
        return null;
    }

    private int minutes(String s) {
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private void setState(String name, String audio, String detail) {
        prefs().edit()
                .putString("now_playing", name)
                .putString("current_audio", audio)
                .putString("state_detail", detail)
                .apply();
        updateNotification("● NO AR • " + name);
    }

    private void updateNotification(String text) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID, notification(text));
        }
    }

    private void stopStreamer() {
        try { if (streamer != null) streamer.close(); } catch (Exception ignored) {}
        streamer = null;
        prefs().edit().putBoolean("caster_connected", false).apply();
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String safeMessage(Throwable e) {
        String s = e.getMessage();
        if (s == null || s.trim().isEmpty()) s = e.getClass().getSimpleName();
        return s.length() > 180 ? s.substring(0, 180) : s;
    }

    @Override public void onDestroy() {
        running = false;
        stopStreamer();
        prefs().edit().putBoolean("automation_running", false).apply();
        try{ if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release(); }catch(Exception ignored){}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    /** Minimal Icecast source client using SOURCE, as used by standard Icecast clients. */
    static class IcecastMp3Streamer {
        final String password;
        Socket socket;
        InputStream in;
        OutputStream out;
        boolean alive = false;
        final String host = "sapircast.caster.fm";
        final int port = 19513;
        final String mount = "/QKnuH";

        IcecastMp3Streamer(String password) { this.password = password; }

        void connect() throws Exception {
            if (password == null || password.isEmpty()) throw new IOException("Senha do transmissor vazia");
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000);
            socket.setSoTimeout(8000);
            out = socket.getOutputStream();
            in = socket.getInputStream();
            String auth = android.util.Base64.encodeToString(
                    ("source:" + password).getBytes("UTF-8"), android.util.Base64.NO_WRAP);
            String req = "SOURCE " + mount + " HTTP/1.0\r\n" +
                    "Authorization: Basic " + auth + "\r\n" +
                    "Content-Type: audio/mpeg\r\n" +
                    "Ice-Name: Web Radio Vem no Gloria\r\n" +
                    "Ice-Genre: Gospel\r\n" +
                    "Ice-Public: 1\r\n" +
                    "User-Agent: VemComigoAndroid/2.1\r\n\r\n";
            out.write(req.getBytes("UTF-8"));
            out.flush();

            String headers = readHeaders(in);
            if (!(headers.contains(" 200 ") || headers.contains(" 100 ") || headers.startsWith("ICY 200"))) {
                throw new IOException("Caster recusou a transmissão: " + firstLine(headers));
            }
            alive = true;
        }

        void send(byte[] data) throws Exception {
            if (!alive) throw new IOException("Transmissão desconectada");
            out.write(data);
            out.flush();
        }

        boolean isAlive() {
            return alive && socket != null && socket.isConnected() && !socket.isClosed();
        }

        void close() {
            alive = false;
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        }

        private static String readHeaders(InputStream in) throws Exception {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            int prev = -1, cur;
            while ((cur = in.read()) != -1) {
                b.write(cur);
                if (prev == '\r' && cur == '\n') {
                    byte[] x = b.toByteArray();
                    int n = x.length;
                    if (n >= 4 && x[n-4] == '\r' && x[n-3] == '\n' && x[n-2] == '\r' && x[n-1] == '\n') break;
                }
                prev = cur;
                if (b.size() > 8192) break;
            }
            return new String(b.toByteArray(), "ISO-8859-1");
        }

        private static String firstLine(String s) {
            int i = s.indexOf('\n');
            return (i >= 0 ? s.substring(0, i) : s).trim();
        }
    }
}
