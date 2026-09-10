# WEBTV FULL — projeto do zero

Projeto Android nativo com um único player Media3/ExoPlayer.

## Correções desta versão
- Interface principal em **2 colunas**, mais próxima do layout de referência.
- Título e abas ajustados para não ficarem cortados.
- Catálogo grande não é carregado inteiro na memória: listas usam **60 itens por página** + "Carregar mais".
- Player preparado para transmissões **MPEG-TS (.ts)** da M3U usando `video/mp2t`.
- Player abre em **paisagem, tela cheia e mantém a tela ligada**.
- Erro de reprodução mostra uma mensagem clara para testar outro canal.
- Ícone do app usa a arte WEBTV FULL fornecida.
- Conteúdo adulto não foi incluído.

## Teste recomendado
Abra **JOGOS → ESPORTES** e escolha um canal esportivo. O app usa o endereço da M3U diretamente; a reprodução depende de o servidor da lista estar disponível e autorizado no momento do teste.

## Build
O ambiente local desta sessão não possui Android SDK/Gradle para validar uma compilação aqui. O workflow `.github/workflows/build.yml` faz o build no GitHub Actions.
