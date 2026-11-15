package com.redutor.video;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.redutor.video.CompressService.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String[] LEVEL_NAMES = {
            "Baixa · quase sem perda", "Média · perda mínima", "Alta · perda pequena"};
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();

    private final List<Video> videos = CompressService.videos;
    private final RadioButton[] levelButtons = new RadioButton[3];
    private LinearLayout list;
    private TextView empty, progressText, resultText;
    private RadioGroup levels;
    private MaterialButton add, reduce;
    private View progressCard, resultCard;
    private LinearProgressIndicator progressBar;

    // Seletor de fotos/vídeos do Android: abre direto na galeria.
    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(), this::onPicked);
    private final ActivityResultLauncher<String> askNotifications = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> startCompress());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        levels = findViewById(R.id.levels);
        levelButtons[0] = findViewById(R.id.baixa);
        levelButtons[1] = findViewById(R.id.media);
        levelButtons[2] = findViewById(R.id.alta);
        add = findViewById(R.id.add);
        reduce = findViewById(R.id.reduce);
        progressCard = findViewById(R.id.progress_card);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        resultCard = findViewById(R.id.result_card);
        resultText = findViewById(R.id.result_text);

        levels.check(levelButtons[CompressService.level].getId());
        levels.setOnCheckedChangeListener((g, id) -> {
            for (int k = 0; k < 3; k++) if (levelButtons[k].getId() == id) CompressService.level = k;
        });
        add.setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                .build()));
        reduce.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startCompress();
            }
        });
        findViewById(R.id.cancel).setOnClickListener(v -> CompressService.cancel());
        findViewById(R.id.share).setOnClickListener(v -> share());
        findViewById(R.id.again).setOnClickListener(v -> {
            videos.clear();
            rebuildRows();
        });
        rebuildRows();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CompressService.listener = this::refresh;
        refresh();
    }

    @Override
    protected void onStop() {
        CompressService.listener = null;
        super.onStop();
    }

    private void onPicked(List<Uri> uris) {
        for (Uri uri : uris) {
            Video v = new Video(uri);
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int s = c.getColumnIndex(OpenableColumns.SIZE);
                    if (n >= 0 && c.getString(n) != null) v.name = c.getString(n);
                    if (s >= 0) v.size = c.getLong(s);
                }
            } catch (Exception ignored) {
            }
            videos.add(v);
            LOADER.execute(() -> {
                CompressService.load(getApplicationContext(), v);
                runOnUiThread(this::refresh);
            });
        }
        rebuildRows();
    }

    private void startCompress() {
        CompressService.start(this);
        refresh();
    }

    private void rebuildRows() {
        list.removeAllViews();
        for (Video v : videos) {
            View row = getLayoutInflater().inflate(R.layout.item_video, list, false);
            row.findViewById(R.id.remove).setOnClickListener(x -> {
                videos.remove(v);
                rebuildRows();
            });
            row.findViewById(R.id.watch).setOnClickListener(x -> watch(v.result));
            list.addView(row);
        }
        refresh();
    }

    /** Atualiza a tela a partir do estado atual (chamado também pelo serviço a cada 0,5 s). */
    private void refresh() {
        boolean running = CompressService.running;
        long[] est = new long[3];
        long in = 0, out = 0;
        int pending = 0, saved = 0;
        boolean loaded = true;
        for (int i = 0; i < videos.size() && i < list.getChildCount(); i++) {
            Video v = videos.get(i);
            View row = list.getChildAt(i);
            ((ImageView) row.findViewById(R.id.thumb)).setImageBitmap(v.thumb);
            ((TextView) row.findViewById(R.id.name)).setText(v.name);
            ((TextView) row.findViewById(R.id.details)).setText(details(v));
            TextView status = row.findViewById(R.id.status);
            status.setText(v.status);
            status.setVisibility(v.status == null ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.watch).setVisibility(v.result != null ? View.VISIBLE : View.GONE);
            row.findViewById(R.id.remove).setVisibility(running ? View.GONE : View.VISIBLE);
            loaded &= v.loaded;
            if (!v.finished) {
                pending++;
                for (int k = 0; k < 3; k++) est[k] += CompressService.estimate(v, CompressService.FACTORS[k]);
            }
            if (v.result != null) {
                saved++;
                in += v.size;
                out += v.resultSize;
            }
        }

        empty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
        add.setEnabled(!running);
        for (int k = 0; k < 3; k++) {
            levelButtons[k].setText(est[k] > 0
                    ? LEVEL_NAMES[k] + "\n≈ " + CompressService.size(this, est[k]) + (pending > 1 ? " no total" : "")
                    : LEVEL_NAMES[k]);
            levelButtons[k].setEnabled(!running);
        }
        reduce.setVisibility(running || (pending == 0 && saved > 0) ? View.GONE : View.VISIBLE);
        reduce.setEnabled(pending > 0 && loaded);
        reduce.setText(!loaded ? "Lendo vídeos…" : pending > 1 ? "Reduzir " + pending + " vídeos" : "Reduzir vídeo");

        progressCard.setVisibility(running ? View.VISIBLE : View.GONE);
        progressText.setText(CompressService.progressText);
        progressBar.setProgressCompat(CompressService.progress, true);

        resultCard.setVisibility(!running && saved > 0 ? View.VISIBLE : View.GONE);
        if (saved > 0) {
            resultText.setText("Pronto! " + CompressService.size(this, in) + " → " + CompressService.size(this, out)
                    + (in > 0 ? " (" + (100 - out * 100 / in) + "% menor)" : "")
                    + "\nSalvo em Filmes/Redutor");
        }
    }

    private String details(Video v) {
        StringBuilder s = new StringBuilder();
        if (v.width > 0) s.append(v.width).append('×').append(v.height).append(" · ");
        if (v.durationMs > 0) {
            long t = v.durationMs / 1000;
            s.append(t >= 3600
                    ? String.format(Locale.ROOT, "%d:%02d:%02d", t / 3600, t / 60 % 60, t % 60)
                    : String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60)).append(" · ");
        }
        return s.append(CompressService.size(this, v.size)).toString();
    }

    private void watch(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum app para assistir vídeos", Toast.LENGTH_SHORT).show();
        }
    }

    private void share() {
        ArrayList<Uri> uris = new ArrayList<>();
        for (Video v : videos) if (v.result != null) uris.add(v.result);
        if (uris.isEmpty()) return;
        Intent i = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE)
                .setType("video/mp4")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uris.size() == 1) i.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        else i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(i, "Compartilhar vídeos"));
    }
}
