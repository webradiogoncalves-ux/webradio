# Painel Web Rádio Vem Comigo no Glória

Projeto completo para GitHub, com APK gerado por GitHub Actions e motor de automação separado do app.

## O que já vem pronto
- mesma identidade visual do app Web Rádio Vem Comigo no Glória;
- painel de sinal: online/offline, ouvintes, bitrate e tocando agora;
- comandos de iniciar/parar o servidor Caster via API privada;
- motor de automação com a grade enviada;
- os 13 arquivos de programa já incluídos em `media/programas/`;
- troca automática de programa conforme horário;
- intervalo 23:55–00:00 com silêncio para manter o stream ativo;
- botão no APK para ligar/desligar o motor;
- grade de programação no painel;
- pasta `media/chamadas/` para futuras chamadas/vinhetas;
- workflow `.github/workflows/build-apk.yml` para gerar `app-debug.apk` como artifact;
- nenhum segredo é gravado no APK, nos arquivos de código ou no GitHub.

## Importante para o sinal real
O Caster exige credenciais privadas para comandos de servidor e para o envio de áudio. Por segurança, elas **não** estão incluídas neste ZIP.

No servidor onde o backend e o motor forem executados, crie `backend/.env` a partir de `backend/.env.example` e informe:
- `CASTER_PRIVATE_TOKEN` para os comandos Start/Stop da API;
- `CASTER_SOURCE_PASSWORD` com a senha do encoder/source para o motor transmitir áudio;
- `CASTER_HOST`, `CASTER_PORT` e `CASTER_MOUNT` já vêm apontados para o servidor informado no projeto.

Nunca coloque essas credenciais em GitHub, no APK ou em screenshots.

## Motor de automação
Requer Linux/Windows com Python 3.10+ e FFmpeg instalados. Não há dependências Python externas.

O motor lê `backend/programacao.json`, escolhe o programa correspondente ao horário e envia o áudio do vídeo para o mountpoint Icecast/Caster usando FFmpeg. Os vídeos enviados para este projeto são clipes de 15 segundos; por isso o motor os repete durante a faixa horária. Substitua os arquivos por programas completos mantendo os mesmos nomes, ou edite `programacao.json`.

Para Linux, há um serviço pronto em `automation/painel-web-radio.service`.

## APK
O GitHub Actions gera o APK debug automaticamente em cada push para `main`/`master` e também manualmente em **Actions > Gerar APK do Painel > Run workflow**.

O APK é somente o painel de controle. O motor de automação precisa ficar em uma máquina/servidor que permaneça ligada e conectada à internet para manter a transmissão contínua.

## Modo celular reserva — automação 24h

Esta versão pode usar um segundo Android como transmissor. Os 13 programas estão em `app/src/main/assets/programas/` e são executados conforme `app/src/main/assets/programacao.json`.

O serviço Android roda em primeiro plano, mantém a automação ativa com a tela apagada e envia AAC pelo protocolo Icecast para o servidor Caster. O Caster Cloud aceita AAC e o plano Free permite até 96 Kbps. A senha do transmissor é digitada no próprio celular e fica somente no armazenamento privado do aplicativo; ela não fica no GitHub.

Ao iniciar a automação no celular reserva:
1. permita as notificações;
2. informe a senha do transmissor/source do Caster;
3. mantenha o celular no carregador e com internet;
4. desative a otimização de bateria para o app;
5. deixe a automação ligada.

O PC não precisa permanecer ligado.

> Importante: o APK é gerado pelo GitHub Actions. Como o ambiente desta preparação não possui Android SDK, o APK final precisa ser produzido pelo workflow do GitHub; não é correto afirmar que ele foi compilado aqui.
