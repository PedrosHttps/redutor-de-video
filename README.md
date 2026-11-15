# Redutor de Vídeo (Android)

Reduz o tamanho de vídeos .mp4 regravando em H.265 (ou H.264, se o aparelho não tiver H.265), sem mudar a resolução.

| Nível | Redução aproximada | Qualidade |
|-------|--------------------|-----------|
| Baixa | ~30% | quase sem perda |
| Média | ~50% | perda mínima |
| Alta  | ~70% | perda pequena |

Requer Android 10 ou mais novo. O vídeo reduzido vai para **Filmes/Redutor** (aparece na galeria).

## Como pegar o .apk

1. No GitHub, abra a aba **Actions** deste repositório.
2. Clique na execução mais recente de **Build APK** (com ✓ verde).
3. Em **Artifacts**, baixe **RedutorDeVideo-apk** (vem em .zip) e extraia o `app-debug.apk`.

## Como instalar no celular

1. Passe o `app-debug.apk` para o celular (cabo USB, Google Drive, WhatsApp para você mesmo etc.).
2. Abra o arquivo no celular. Se aparecer "instalação bloqueada", toque em **Configurações** e ative **Permitir desta fonte** para o app que está abrindo o arquivo (ex.: Arquivos, Chrome).
3. Volte e toque em **Instalar**. Se o Play Protect avisar, toque em **Instalar mesmo assim** (o app não está na Play Store, por isso o aviso).

## Como usar

1. **Escolher vídeo .mp4**
2. Marque **Baixa**, **Média** ou **Alta**
3. **Reduzir vídeo** e aguarde (deixe o app aberto; a tela fica ligada sozinha)
