<div align="center">

<img src="docs/icon.svg" width="112" alt="Ícone do Redutor de Vídeo">

# Redutor de Vídeo

**Deixe seus vídeos até 85% menores, sem perder a qualidade que importa.**
No celular Android ou direto no navegador. Seus vídeos nunca saem do seu aparelho.

[![Baixar APK](https://img.shields.io/badge/Baixar_APK-Android_10+-5b4be8?style=for-the-badge&logo=android&logoColor=white)](https://github.com/PedrosHttps/redutor-de-video/releases/latest/download/RedutorDeVideo.apk)
&nbsp;
[![Usar no navegador](https://img.shields.io/badge/Usar_no_navegador-grátis-9333ea?style=for-the-badge&logo=googlechrome&logoColor=white)](https://pedroshttps.github.io/redutor-de-video/)

[![Versão](https://img.shields.io/github/v/release/PedrosHttps/redutor-de-video?label=versão&color=4f46e5)](https://github.com/PedrosHttps/redutor-de-video/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/PedrosHttps/redutor-de-video/build.yml?label=build)](https://github.com/PedrosHttps/redutor-de-video/actions)
![Privacidade](https://img.shields.io/badge/vídeos-100%25_no_seu_aparelho-15803d)

</div>

---

## ✨ Por que usar

<table>
<tr>
<td width="33%" valign="top">

### 🎯 Qualidade preservada
Regrava em **H.265 (HEVC)**, que guarda a mesma imagem com bem menos bytes. Sem H.265 no aparelho, usa H.264.

</td>
<td width="33%" valign="top">

### 📦 Vários de uma vez
Escolha **vários vídeos** da galeria, veja a miniatura, a duração e o **tamanho final estimado** antes de começar.

</td>
<td width="33%" valign="top">

### 🔒 Privado de verdade
Nada é enviado para servidor. Tudo é processado **no seu celular ou no seu navegador**.

</td>
</tr>
<tr>
<td valign="top">

### 🌙 Em segundo plano
No Android, a redução **continua com a tela bloqueada**, com progresso na notificação e botão Cancelar.

</td>
<td valign="top">

### 🎉 Resultado na hora
Progresso circular, comparação antes → depois e quanto você **economizou**. Assista, compartilhe ou baixe.

</td>
<td valign="top">

### 🔄 Atualizações por cima
Assinatura fixa: cada nova versão **instala por cima** da anterior, sem perder nada.

</td>
</tr>
</table>

## 🎚️ Três níveis

| Nível | Redução | Resolução | Ideal para |
|:--|:--:|:--:|:--|
| **Baixa** | ~45% | original | guardar com a melhor qualidade |
| **Média** | ~65% | original | o dia a dia: equilíbrio entre tamanho e imagem |
| **Alta** | ~85% | 720p | mandar por WhatsApp ou e-mail |

<sub>Os números são aproximados e variam conforme o vídeo original. Vídeos já leves podem não diminuir.</sub>

## 📲 Instalar no Android

1. No celular, toque em **[Baixar APK](https://github.com/PedrosHttps/redutor-de-video/releases/latest/download/RedutorDeVideo.apk)**.
2. Abra o `RedutorDeVideo.apk`. Se aparecer *instalação bloqueada*, toque em **Configurações** e ative **Permitir desta fonte**.
3. Toque em **Instalar**. Se o Play Protect avisar, toque em **Instalar mesmo assim** (o app não está na Play Store).

> Requer **Android 10 ou mais novo**. Os vídeos reduzidos ficam na galeria, em **Filmes/Redutor**.

## 🌐 Versão web

Abra **[pedroshttps.github.io/redutor-de-video](https://pedroshttps.github.io/redutor-de-video/)** no Chrome, Edge ou Safari atualizados. Arraste os vídeos, escolha o nível e baixe o resultado. Funciona no computador e no celular, sem instalar nada.

## ❓ Perguntas frequentes

<details>
<summary><b>Qual nível escolher?</b></summary>

**Média** serve para quase tudo. Use **Baixa** quando a qualidade for prioridade e **Alta** para mandar vídeos por mensagem.
</details>

<details>
<summary><b>O vídeo original é apagado?</b></summary>

Não. O original continua igual; o reduzido é salvo como um novo arquivo (`nome_reduzido.mp4`).
</details>

<details>
<summary><b>Apareceu "Já estava bem compactado"</b></summary>

O vídeo já era leve, e regravar não deixaria menor. Tente o nível **Alta**.
</details>

<details>
<summary><b>O app fechou ou deu erro</b></summary>

Abra o app de novo: ele mostra o erro com um botão **Copiar**. Cole no formulário de sugestões abaixo.
</details>

## 💬 Críticas e sugestões

Encontrou um problema ou tem uma ideia? **[Responda o formulário](https://forms.gle/juwkU6g8rt9yD6gH8)**. Leva 1 minuto e ajuda a melhorar o app.

## 👤 Sobre o criador

<table>
<tr>
<td><img src="https://github.com/PedrosHttps.png?size=192" width="96" alt="Foto de PedrosHttps"></td>
<td>

**PedrosHttps**<br>
Criador do Redutor de Vídeo.<br>
[![GitHub](https://img.shields.io/badge/GitHub-PedrosHttps-181717?logo=github)](https://github.com/PedrosHttps)

</td>
</tr>
</table>

---

<details>
<summary>🛠️ <b>Para desenvolvedores</b></summary>

- **Android:** Java, [Media3 Transformer](https://developer.android.com/media/media3/transformer) (codificador de hardware do aparelho), Material 3. Código em [`app/`](app/).
- **Web:** HTML + [Mediabunny](https://mediabunny.dev) (WebCodecs), sem build. Código em [`docs/`](docs/), publicado pelo GitHub Pages.
- **Build:** o GitHub Actions roda os testes, compila o `RedutorDeVideo.apk` e publica em [Releases](https://github.com/PedrosHttps/redutor-de-video/releases) a cada `versionName` novo.
- **Assinatura:** chave fixa nos Secrets do repositório (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, alias `redutor`). A chave original fica fora do Git, em `chave/redutor.p12`. **Guarde uma cópia**: sem ela, as próximas versões não instalam por cima.

</details>
