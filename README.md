# Painel Web Rádio Vem Comigo no Glória — Automação Celular v3

## Correção desta versão
- O botão **PASTA DE ÁUDIOS** agora realmente carrega músicas escolhidas no celular.
- As músicas escolhidas ficam salvas no aplicativo e são usadas pela automação.
- Durante cada faixa da programação, as músicas selecionadas são tocadas em sequência até a próxima troca de programa.
- O **TOCANDO AGORA** passa a mostrar o nome da música que está sendo enviada.
- A chamada de abertura continua entrando apenas na troca do programa e depois a música começa.
- **HORA CERTA** interrompe a música no início da hora e depois a automação continua.
- Se nenhuma música for selecionada, o aplicativo usa os áudios padrão da programação.

## Como testar
1. Instale o APK no celular reserva.
2. Abra **PASTA DE ÁUDIOS** e selecione uma ou mais músicas MP3.
3. Confira no painel a mensagem **Músicas carregadas**.
4. Pressione **INICIAR** e informe a senha do transmissor no próprio celular.
5. No Caster, o servidor precisa estar ligado; a transmissão só fica no ar quando o aplicativo de transmissão se conecta.
6. Aguarde alguns segundos e confira **TOCANDO AGORA** e **TRANSMISSÃO NO AR**.

A senha do Caster não é incluída neste projeto nem no GitHub.

## Limite de transmissão
O Caster desta rádio está configurado para no máximo 96 Kbps. O painel aceita para a playlist do celular somente MP3 cujo bitrate identificado seja de até 96 Kbps. Arquivos acima desse limite não são adicionados. As vinhetas incluídas no projeto também foram normalizadas para 96 Kbps.


## NOVO — Pregação automática e entrada ao vivo
- **Pregação automática:** escolha um arquivo de áudio de até 96 Kbps no botão `PREGAÇÃO AUTOMÁTICA`. A pregação fica separada das músicas e entra uma vez no meio de cada bloco da grade. Depois, o programa continua. É possível ativar, desativar, trocar ou remover a pregação.
- **Entrada ao vivo:** o botão `ENTRADA AO VIVO` interrompe a automação e usa o microfone do celular. O áudio é codificado em AAC-LC 44,1 kHz / 64 Kbps e enviado ao mesmo mount via Icecast SOURCE. Ao sair do ao vivo, o motor de automação pode ser iniciado novamente.
- **Limite:** músicas e pregações selecionadas pelo painel são aceitas somente se o arquivo reportar bitrate de até 96 Kbps.
- **Observação:** a entrada ao vivo depende de o Caster aceitar AAC no mount. O projeto mantém a automação original em MP3.
