package com.redutor.video;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

public class HelpActivity extends AppCompatActivity {

    /** Formulário de críticas e sugestões (Google Forms). */
    private static final String FORM_URL = "https://forms.gle/juwkU6g8rt9yD6gH8";

    private static final String[][] FAQ = {
            {"Como reduzir um vídeo?",
                    "Toque em + Adicionar vídeos, escolha um ou vários na galeria, marque o nível e toque em Reduzir."},
            {"Qual nível escolher?",
                    "Baixa: para guardar com a melhor qualidade. Média: o equilíbrio, boa para a maioria. "
                            + "Alta: para mandar por WhatsApp ou e-mail; o vídeo vai para 720p."},
            {"Onde ficam os vídeos reduzidos?",
                    "Na galeria, na pasta Filmes/Redutor. O vídeo original não é apagado nem alterado."},
            {"Posso sair do app durante a redução?",
                    "Pode. A redução continua mesmo com a tela bloqueada, e o progresso aparece na notificação."},
            {"Apareceu \"Já estava bem compactado\"",
                    "O vídeo original já era bem leve, e regravar não deixaria menor. Tente o nível Alta."},
            {"O app fechou ou deu erro",
                    "Abra o app de novo. Se aparecer a janela com o erro, toque em Copiar erro e cole no formulário abaixo."},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        LinearLayout faq = findViewById(R.id.faq);
        for (String[] item : FAQ) {
            View card = getLayoutInflater().inflate(R.layout.item_faq, faq, false);
            ((TextView) card.findViewById(R.id.question)).setText(item[0]);
            ((TextView) card.findViewById(R.id.answer)).setText(item[1]);
            faq.addView(card);
        }

        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.feedback).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(FORM_URL)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Nenhum navegador para abrir o formulário", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            ((TextView) findViewById(R.id.version)).setText("Redutor de Vídeo, versão "
                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        ViewGroup content = findViewById(R.id.content);
        for (int i = 0; i < content.getChildCount(); i++) MainActivity.slideIn(content.getChildAt(i), i * 50L);
    }
}
