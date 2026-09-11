# Painel Web Rádio Vem Comigo no Glória — Automação no celular

Esta versão é feita para o cenário combinado: **celular reserva = automação + transmissão**, sem depender de PC ou VPS.

## O que foi corrigido

- O motor agora transmite **MP3** para o Caster usando o protocolo **SOURCE/Icecast**, em vez de tentar enviar AAC com PUT.
- O aplicativo espera a resposta do Caster e mostra erro de autenticação/conexão no painel.
- O painel consulta diretamente o `admin/publicstats.json` do Caster, então não depende de `10.0.2.2` nem de um backend rodando no PC.
- `OUVINTES`, estado **NO AR/FORA DO AR**, título e bitrate passam a vir do status público quando a transmissão está conectada.
- `TOCANDO AGORA` e `PRÓXIMO NA GRADE` também aparecem localmente mesmo antes do Caster atualizar o status.
- A grade usa os 13 áudios convertidos para MP3 e repete cada programa dentro do horário definido.
- As 24 chamadas de **HORA CERTA** estão incluídas e entram automaticamente no início de cada hora.
- As chamadas de abertura disponíveis no APK original foram incluídas e são executadas nas trocas de programa correspondentes.
- O serviço é foreground, usa WakeLock parcial e tenta reconectar automaticamente a cada 5 segundos em caso de queda.
- A senha do transmissor é digitada no celular e fica no armazenamento privado do app; ela não é gravada no projeto/GitHub.

## Como usar

1. Deixe o servidor de streaming do Caster **On-line** no painel do Caster.
2. Instale este APK no celular reserva.
3. Abra o painel e toque em **INICIAR**.
4. Digite a senha do transmissor/source do Caster quando solicitado.
5. Deixe o celular conectado à internet e, de preferência, carregando.
6. No Android/Samsung, retire a restrição de bateria deste app para evitar que o sistema mate a automação em segundo plano.
7. Confira no Caster: **Transmissão** deve mudar de “Fora do ar” para “No ar”.

## Dados do canal usados pelo aplicativo

- Host: `sapircast.caster.fm`
- Porta: `19513`
- Usuário source: `source`
- Mount: `/QKnuH`
- Formato enviado: MP3
- Bitrate: 96 Kbps

A senha não está neste README.

## Grade

- 00:00–09:00 — MADRUGADA COM DEUS
- 09:00–09:30 — PROGRAMA REFLEXÃO
- 09:30–10:00 — PROGRAMA FÉ MEIA HORA
- 10:00–12:00 — VEM COMIGO MANHÃ
- 12:00–12:30 — PROGRAMA MEIA DIA — EDIÇÃO MEIO-DIA
- 12:30–13:30 — ALMOÇO COM DEUS
- 13:30–14:00 — REFLEXÃO TARDE
- 14:00–15:00 — PROGRAMA VEM COMIGO TARDE
- 15:00–16:00 — VEM COMIGO CAFÉ E GLÓRIA
- 16:00–19:00 — LOUVOR E REFLEXÃO
- 19:00–21:00 — TARDIZINHA COM DEUS
- 21:00–21:55 — PROGRAMA FÉ — EDIÇÃO NOITE
- 21:55–23:55 — PROGRAMA NOITE COM DEUS
- 23:55–00:00 — intervalo

## Build

O projeto foi preparado para GitHub Actions. O workflow `.github/workflows/build-apk.yml` instala Java 17, Android SDK 35 e Gradle 8.10 e gera o APK debug.

A compilação precisa ser validada no GitHub Actions/Android SDK; este ambiente não possui o Android SDK instalado.
