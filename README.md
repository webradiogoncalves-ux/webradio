# WebTV Full — M3U + categorias + EPG + player único

Projeto Android nativo preparado para receber uma lista M3U, separar automaticamente por `group-title`, mostrar os canais sem depender de imagens e tocar tudo com um único player Media3/ExoPlayer.

Também tenta carregar EPG pelo endpoint XMLTV padrão do mesmo servidor. Se o provedor não fornecer XMLTV ou usar outro endereço, o app continua funcionando sem EPG.

## Estrutura

- `.github/workflows/build.yml` — compila automaticamente no GitHub Actions.
- `app/` — aplicativo Android nativo.
- `app/src/main/res/drawable/webtv_icon.png` — ícone WebTV Full.
- `app/src/main/java/com/webtvfull/app/MainActivity.java` — M3U, categorias, EPG e player único.

## Importante sobre a lista

A URL M3U fornecida pelo usuário está configurada no código para o teste. Ela contém usuário e senha do provedor. **Não deixe esse repositório público** e considere trocar a senha depois do teste.

O app não extrai nem contorna DRM; ele apenas reproduz URLs que a própria lista M3U fornecer.
