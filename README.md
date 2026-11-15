# Redutor de Vídeo (Android)

Reduz o tamanho de vídeos regravando em H.265 (ou H.264, se o aparelho não tiver H.265). Baixa e Média mantêm a resolução; Alta reduz para 720p.

| Nível | Redução aproximada | Qualidade |
|-------|--------------------|-----------|
| Baixa | ~45% | quase sem perda |
| Média | ~65% | perda pequena |
| Alta  | ~85% | vai para 720p (ótimo no celular, menos nítido numa TV) |

- Escolha **um ou vários vídeos** direto da galeria; cada um mostra miniatura, resolução, duração e tamanho.
- Cada nível mostra o **tamanho final estimado**.
- A redução **continua em segundo plano** (pode sair do app ou bloquear a tela), com progresso na notificação e botão **Cancelar**.
- No fim: **Assistir**, **Compartilhar** e **Reduzir outros**.

Requer Android 10 ou mais novo. Os vídeos reduzidos vão para **Filmes/Redutor** (aparecem na galeria).

## Como pegar o .apk

1. No GitHub, abra a aba **Actions** deste repositório.
2. Clique na execução mais recente de **Build APK** (com ✓ verde).
3. Em **Artifacts**, baixe **RedutorDeVideo-apk** (vem em .zip) e extraia o `app-debug.apk`.

## Como instalar no celular

1. **Se já tiver a versão anterior instalada, desinstale-a antes** (cada build de teste tem uma assinatura diferente).
2. Passe o `app-debug.apk` para o celular (cabo USB, Google Drive, WhatsApp para você mesmo etc.).
3. Abra o arquivo no celular. Se aparecer "instalação bloqueada", toque em **Configurações** e ative **Permitir desta fonte**.
4. Toque em **Instalar**. Se o Play Protect avisar, toque em **Instalar mesmo assim** (o app não está na Play Store).

## Como usar

1. **+ Adicionar vídeos** e escolha na galeria (pode marcar vários)
2. Marque **Baixa**, **Média** ou **Alta**
3. **Reduzir vídeo** — na primeira vez o Android pergunta se pode mostrar notificações: permita para ver o progresso
