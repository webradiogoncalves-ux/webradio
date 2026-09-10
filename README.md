# WebTV Full

Projeto Android nativo com uma lista M3U Plus local embutida em `app/src/main/assets/playlist.m3u`.

- A lista é carregada localmente na abertura, sem depender do download da M3U para mostrar o catálogo pela primeira vez.
- Categorias e logos vêm dos campos `group-title` e `tvg-logo` da própria lista.
- O catálogo remoto continua configurado para atualização quando houver conexão.
- Player nativo Media3/ExoPlayer para reprodução dos URLs presentes na lista.

**Importante:** a playlist embutida contém credenciais de acesso. Não publique este projeto em um repositório GitHub público. Para distribuição, prefira repositório privado ou uma forma segura de configurar a playlist.


Segurança: esta versão não contém a URL de login da lista no código-fonte. O catálogo M3U é local. Como os próprios links de transmissão podem carregar credenciais, mantenha este projeto em repositório privado.
