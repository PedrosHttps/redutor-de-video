package com.redutor.video;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.format.Formatter;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EncoderUtil;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reduz a fila de vídeos em primeiro plano (continua com o app fechado ou a tela bloqueada). */
@OptIn(markerClass = UnstableApi.class)
public class CompressService extends Service {

    /** Fração do bitrate original para Baixa, Média e Alta. */
    static final float[] FACTORS = {0.7f, 0.5f, 0.3f};
    private static final int MIN_BITRATE = 300_000;
    private static final String CHANNEL = "progresso";

    // Estado compartilhado com a tela (tudo acessado na thread principal).
    static final List<Video> videos = new ArrayList<>();
    static int level = 1;
    static boolean running;
    static String progressText = "";
    static int progress;
    static Runnable listener;
    private static CompressService instance;

    static class Video {
        final Uri uri;
        String name = "video";
        long size, durationMs, bitrate;
        int width, height;
        Bitmap thumb;
        boolean loaded, finished;
        String status;
        Uri result;
        long resultSize;

        Video(Uri uri) {
            this.uri = uri;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService saver = Executors.newSingleThreadExecutor();
    private final ProgressHolder holder = new ProgressHolder();
    private NotificationManager nm;
    private NotificationCompat.Builder notif;
    private PowerManager.WakeLock wakeLock;
    private Transformer transformer;
    private Video current;
    private File output;
    private int total, doneCount, currentProgress;
    private boolean canceled;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (transformer != null && transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                currentProgress = holder.progress;
                current.status = "Reduzindo… " + currentProgress + "%";
            }
            update();
            handler.postDelayed(this, 500);
        }
    };

    static void start(Context c) {
        running = true;
        progress = 0;
        progressText = "Preparando…";
        ContextCompat.startForegroundService(c, new Intent(c, CompressService.class));
    }

    static void cancel() {
        if (instance != null) instance.cancelAll();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        canceled = false;
        total = 0;
        doneCount = 0;
        for (Video v : videos) {
            if (!v.finished) {
                total++;
                v.status = "Na fila";
            }
        }
        nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Redução de vídeos", NotificationManager.IMPORTANCE_LOW));
        notif = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_compress)
                .setContentTitle("Reduzindo vídeos")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp());
        ServiceCompat.startForeground(this, 1, notif.build(), Build.VERSION.SDK_INT >= 35
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                : ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        // Sem isto o processador dorme quando a tela apaga e a compressão para.
        wakeLock = getSystemService(PowerManager.class)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "redutor:compress");
        wakeLock.acquire(6 * 60 * 60 * 1000L);
        handler.post(poll);
        next();
        return START_NOT_STICKY;
    }

    private void next() {
        current = null;
        for (Video v : videos) {
            if (!v.finished) {
                current = v;
                break;
            }
        }
        if (current == null) {
            finish(true);
            return;
        }
        Video v = current;
        currentProgress = 0;
        v.status = "Reduzindo… 0%";

        // H.265 mantém mais qualidade no mesmo tamanho; se o aparelho não tiver, usa H.264.
        String mime = EncoderUtil.getSupportedEncoders(MimeTypes.VIDEO_H265).isEmpty()
                ? MimeTypes.VIDEO_H264 : MimeTypes.VIDEO_H265;
        output = new File(getCacheDir(), "saida.mp4");
        output.delete();
        transformer = new Transformer.Builder(this)
                .setVideoMimeType(mime)
                .setEncoderFactory(new DefaultEncoderFactory.Builder(this)
                        .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder()
                                .setBitrate(targetBitrate(v.bitrate, FACTORS[level])).build())
                        .setEnableFallback(true)
                        .build())
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition, ExportResult result) {
                        onExported(v);
                    }

                    @Override
                    public void onError(Composition composition, ExportResult result, ExportException e) {
                        transformer = null;
                        output.delete();
                        done(v, "Não foi possível reduzir (" + e.getErrorCodeName() + ")");
                    }
                })
                .build();
        transformer.start(MediaItem.fromUri(v.uri), output.getAbsolutePath());
        update();
    }

    private void onExported(Video v) {
        transformer = null;
        v.status = "Salvando…";
        update();
        File file = output;
        saver.execute(() -> {
            long out = file.length();
            String msg;
            if (v.size > 0 && out >= v.size) {
                msg = "Já estava bem compactado: não ficou menor, nada foi salvo";
            } else {
                try {
                    v.result = save(v, file);
                    v.resultSize = out;
                    msg = "Pronto: " + size(this, out)
                            + (v.size > 0 ? " (" + (100 - out * 100 / v.size) + "% menor)" : "");
                } catch (IOException e) {
                    msg = "Erro ao salvar: " + e.getMessage();
                }
            }
            file.delete();
            String m = msg;
            handler.post(() -> done(v, m));
        });
    }

    private void done(Video v, String msg) {
        v.status = msg;
        v.finished = true;
        doneCount++;
        if (canceled) finish(false);
        else next();
    }

    private void cancelAll() {
        canceled = true;
        if (transformer != null) { // se estiver salvando, done() encerra depois de salvar
            transformer.cancel();
            transformer = null;
            current.status = "Cancelado";
            output.delete();
            finish(false);
        }
    }

    private void finish(boolean completed) {
        handler.removeCallbacks(poll);
        running = false;
        instance = null;
        for (Video v : videos) if (!v.finished && v != current) v.status = null;
        if (wakeLock.isHeld()) wakeLock.release();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        if (completed && listener == null) {
            nm.notify(2, new NotificationCompat.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_compress)
                    .setContentTitle("Vídeos reduzidos")
                    .setContentText("Salvos em Filmes/Redutor")
                    .setAutoCancel(true)
                    .setContentIntent(openApp())
                    .build());
        }
        if (listener != null) listener.run();
        stopSelf();
    }

    private void update() {
        progress = total == 0 ? 0 : (doneCount * 100 + currentProgress) / total;
        String step = current != null && current.status != null ? current.status : "";
        progressText = total > 1 ? "Vídeo " + Math.min(doneCount + 1, total) + " de " + total + " · " + step : step;
        if (running) nm.notify(1, notif.setContentText(progressText).setProgress(100, progress, false).build());
        if (listener != null) listener.run();
    }

    /** Copia o resultado para a galeria (Filmes/Redutor). */
    private Uri save(Video v, File file) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, outputName(v.name));
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Redutor");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        ContentResolver cr = getContentResolver();
        Uri uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("não foi possível criar o arquivo");
        try (OutputStream out = cr.openOutputStream(uri)) {
            if (out == null) throw new IOException("não foi possível gravar o arquivo");
            Files.copy(file.toPath(), out);
        } catch (IOException e) {
            cr.delete(uri, null, null);
            throw e;
        }
        values.clear();
        values.put(MediaStore.Video.Media.IS_PENDING, 0);
        cr.update(uri, values, null, null);
        return uri;
    }

    private PendingIntent openApp() {
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(poll);
        if (transformer != null) transformer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        running = false;
        instance = null;
        saver.shutdown();
        super.onDestroy();
    }

    /** Lê miniatura, duração, resolução e bitrate (roda fora da thread principal). */
    static void load(Context c, Video v) {
        try (MediaMetadataRetriever r = new MediaMetadataRetriever()) {
            r.setDataSource(c, v.uri);
            v.durationMs = num(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            v.width = (int) num(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            v.height = (int) num(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            if (num(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)) % 180 != 0) {
                int w = v.width;
                v.width = v.height;
                v.height = w;
            }
            v.bitrate = num(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            if (v.bitrate == 0 && v.durationMs > 0) v.bitrate = v.size * 8000 / v.durationMs;
            v.thumb = r.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 256, 256);
        } catch (Exception ignored) {
        }
        if (v.bitrate == 0) v.bitrate = 8_000_000; // valor seguro caso o arquivo não informe
        v.loaded = true;
    }

    private static long num(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int targetBitrate(long source, float factor) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_BITRATE, (long) (source * factor)));
    }

    /** Tamanho final estimado em bytes (0 se a duração for desconhecida). */
    static long estimate(Video v, float factor) {
        return v.durationMs <= 0 ? 0 : (long) targetBitrate(v.bitrate, factor) * v.durationMs / 8000;
    }

    static String outputName(String inputName) {
        int dot = inputName.lastIndexOf('.');
        return (dot > 0 ? inputName.substring(0, dot) : inputName) + "_reduzido.mp4";
    }

    static String size(Context c, long bytes) {
        return bytes > 0 ? Formatter.formatShortFileSize(c, bytes) : "tamanho desconhecido";
    }
}
