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


## PADRÃO FIXO: 96 Kbps

A automação usa 96 Kbps como limite rígido. Os áudios internos do projeto foram normalizados para 96 Kbps.
Músicas escolhidas pelo celular são verificadas antes da transmissão e qualquer arquivo acima de 96 Kbps é recusado.
O cliente Icecast também informa `Ice-Bitrate: 96`. Esse cabeçalho não converte áudio; a proteção real é a validação do bitrate do arquivo antes do envio.
