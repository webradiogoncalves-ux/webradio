# WebTV Full

Projeto Android nativo com um único player Media3/ExoPlayer e categorias da playlist.

## Lista M3U otimizada para GitHub
A playlist original tem cerca de 84 MB descompactada. Para evitar o limite de upload de arquivo individual do GitHub, ela foi compactada como:

`app/src/main/assets/playlist.m3u.gz`

O arquivo compactado fica abaixo de 25 MB e o aplicativo descompacta a lista automaticamente em memória durante a leitura.

O app primeiro identifica as categorias e só carrega os itens completos quando uma categoria é selecionada. A tela mostra os itens em páginas de 150 para evitar travamentos com categorias muito grandes.

## Categorias
A lista mantém os grupos existentes no M3U, incluindo canais, jogos e séries. O conteúdo é somente o que já existe na playlist fornecida para este projeto.

## Player
Um único player nativo é usado para os itens. HLS (`.m3u8`), DASH (`.mpd`) e URLs progressivas são tratados pelo Media3.

## GitHub
Extraia este ZIP e envie o conteúdo para a raiz do repositório. Não é necessário enviar nenhuma M3U descompactada.
