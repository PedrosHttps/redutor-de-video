package com.redutor.video;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.OptIn;
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

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    private static final int PICK_VIDEO = 1;
    private static final int MIN_BITRATE = 300_000;

    private Button pick, reduce;
    private RadioGroup levels;
    private ProgressBar progress;
    private TextView info, status;

    private Uri input;
    private String inputName = "video";
    private long inputSize;
    private Transformer transformer;
    private File output;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ProgressHolder holder = new ProgressHolder();
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (transformer != null
                    && transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                progress.setProgress(holder.progress);
                status.setText("Reduzindo… " + holder.progress + "%");
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pick = findViewById(R.id.pick);
        reduce = findViewById(R.id.reduce);
        levels = findViewById(R.id.levels);
        progress = findViewById(R.id.progress);
        info = findViewById(R.id.info);
        status = findViewById(R.id.status);

        pick.setOnClickListener(v -> startActivityForResult(
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("video/mp4")
                        .addCategory(Intent.CATEGORY_OPENABLE),
                PICK_VIDEO));
        reduce.setOnClickListener(v -> startExport());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEO || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        input = data.getData();
        inputName = "video";
        inputSize = 0;
        try (Cursor c = getContentResolver().query(input, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int s = c.getColumnIndex(OpenableColumns.SIZE);
                if (n >= 0 && c.getString(n) != null) inputName = c.getString(n);
                if (s >= 0) inputSize = c.getLong(s);
            }
        }
        info.setText(inputName + "\nTamanho: " + size(inputSize));
        status.setText("");
        reduce.setEnabled(true);
    }

    private void startExport() {
        int checked = levels.getCheckedRadioButtonId();
        float factor = checked == R.id.baixa ? 0.7f : checked == R.id.alta ? 0.3f : 0.5f;
        int bitrate = targetBitrate(sourceBitrate(), factor);

        // H.265 mantém mais qualidade no mesmo tamanho; se o aparelho não tiver, usa H.264.
        String mime = EncoderUtil.getSupportedEncoders(MimeTypes.VIDEO_H265).isEmpty()
                ? MimeTypes.VIDEO_H264 : MimeTypes.VIDEO_H265;

        output = new File(getCacheDir(), "saida.mp4");
        output.delete();

        transformer = new Transformer.Builder(this)
                .setVideoMimeType(mime)
                .setEncoderFactory(new DefaultEncoderFactory.Builder(this)
                        .setRequestedVideoEncoderSettings(
                                new VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                        .setEnableFallback(true)
                        .build())
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition, ExportResult result) {
                        onExportDone();
                    }

                    @Override
                    public void onError(Composition composition, ExportResult result, ExportException e) {
                        finishBusy("Não foi possível reduzir este vídeo.\n(" + e.getMessage() + ")");
                    }
                })
                .build();

        setBusy(true);
        status.setText("Reduzindo… 0%");
        transformer.start(MediaItem.fromUri(input), output.getAbsolutePath());
        handler.post(poll);
    }

    static int targetBitrate(long source, float factor) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_BITRATE, (long) (source * factor)));
    }

    static String outputName(String inputName) {
        String base = inputName.toLowerCase().endsWith(".mp4")
                ? inputName.substring(0, inputName.length() - 4) : inputName;
        return base + "_reduzido.mp4";
    }

    /** Bitrate total do vídeo original, em bits por segundo. */
    private long sourceBitrate() {
        try (MediaMetadataRetriever r = new MediaMetadataRetriever()) {
            r.setDataSource(this, input);
            String b = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (b != null) return Long.parseLong(b);
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null && inputSize > 0) return inputSize * 8 * 1000 / Math.max(1, Long.parseLong(d));
        } catch (Exception ignored) {
        }
        return 8_000_000; // valor seguro caso o arquivo não informe
    }

    private void onExportDone() {
        handler.removeCallbacks(poll);
        progress.setProgress(100);
        status.setText("Salvando…");
        new Thread(() -> {
            long outSize = output.length();
            String msg;
            if (inputSize > 0 && outSize >= inputSize) {
                msg = "Este vídeo já está bem compactado: o resultado não ficou menor, "
                        + "então nada foi salvo. Tente o nível Alta.";
            } else {
                try {
                    save();
                    long pct = inputSize > 0 ? 100 - outSize * 100 / inputSize : 0;
                    msg = "Pronto! " + size(inputSize) + " → " + size(outSize)
                            + " (" + pct + "% menor)\nSalvo em Filmes/Redutor";
                } catch (IOException e) {
                    msg = "Erro ao salvar: " + e.getMessage();
                }
            }
            output.delete();
            String finalMsg = msg;
            runOnUiThread(() -> finishBusy(finalMsg));
        }).start();
    }

    /** Copia o resultado para a galeria (Filmes/Redutor). */
    private void save() throws IOException {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Video.Media.DISPLAY_NAME, outputName(inputName));
        v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Redutor");
        v.put(MediaStore.Video.Media.IS_PENDING, 1);
        ContentResolver cr = getContentResolver();
        Uri uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new IOException("não foi possível criar o arquivo");
        try (OutputStream out = cr.openOutputStream(uri)) {
            if (out == null) throw new IOException("não foi possível gravar o arquivo");
            Files.copy(output.toPath(), out);
        } catch (IOException e) {
            cr.delete(uri, null, null);
            throw e;
        }
        v.clear();
        v.put(MediaStore.Video.Media.IS_PENDING, 0);
        cr.update(uri, v, null, null);
    }

    private void finishBusy(String msg) {
        handler.removeCallbacks(poll);
        transformer = null;
        setBusy(false);
        status.setText(msg);
    }

    private void setBusy(boolean busy) {
        pick.setEnabled(!busy);
        reduce.setEnabled(!busy);
        for (int i = 0; i < levels.getChildCount(); i++) levels.getChildAt(i).setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        progress.setProgress(0);
        if (busy) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private String size(long bytes) {
        return bytes > 0 ? Formatter.formatShortFileSize(this, bytes) : "desconhecido";
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(poll);
        if (transformer != null) transformer.cancel();
        super.onDestroy();
    }
}
