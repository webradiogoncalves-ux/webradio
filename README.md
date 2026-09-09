# WebTV Full — 3 áreas / 1 player

Projeto Android nativo, sem WebView. A interface trabalha com três áreas:

- **Esportes** → jogos e transmissões esportivas
- **Novelas** → catálogo nativo de novelas
- **Canais** → canais ao vivo

## Próxima etapa

Os nomes e a interface já estão separados das fontes. O arquivo `StreamCatalog.java` é o ponto único onde entram os **links diretos de mídia autorizados** fornecidos pelo responsável pelos sinais (HLS `.m3u8`, MP4 ou outro formato suportado pelo player).

Não é usado o endereço de uma página HTML como se fosse um stream, e o app não abre os sites dentro de WebView.

## Build

O workflow em `.github/workflows/gerar-apk.yml` gera o APK pelo GitHub Actions com Java 17 e Gradle 8.7, sem depender do Android Studio local.


## Reprodução com dois motores, sem dois players na tela
A interface continua com um único espaço de vídeo. O projeto agora possui dois motores nativos: Media3/ExoPlayer e Android MediaPlayer. Em modo AUTO, o primeiro é tentado e, se houver erro de reprodução, o segundo é acionado automaticamente. Também é possível marcar um conteúdo como EXOPLAYER ou MEDIAPLAYER no catálogo.

Isso é visualmente um único player para o usuário; a troca de motor acontece por trás.


IMPORTANTE: no GitHub, os arquivos app/, .github/, build.gradle, settings.gradle e gradle.properties devem ficar diretamente na raiz do repositorio. Nao deixe a pasta WebTV_Full como uma subpasta do repositorio. O workflow usa Gradle 8.9.
