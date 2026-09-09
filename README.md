# Web Rádio Vem Comigo no Glória — Ouvinte

Aplicativo Android para o ouvinte da rádio.

## Esta versão
- Microfone fornecido pelo projeto como fundo da tela principal.
- Sem página, imagem ou WebView da Caster.fm dentro do aplicativo.
- O botão **OUVIR RÁDIO** usa somente a transmissão direta.
- Reprodução em segundo plano.
- Volume + e volume -.
- Compartilhar.
- Palavra do Dia.

## Transmissão direta
Servidor: `sapircast.caster.fm`
Porta: `19513`
Mount Point: `QKnuH`

Endereço usado pelo aplicativo:
`http://sapircast.caster.fm:19513/QKnuH`

O aplicativo não abre nem exibe a página do player da Caster.fm.

## GitHub Actions
O workflow `.github/workflows/build-apk.yml` gera o APK Debug automaticamente.
