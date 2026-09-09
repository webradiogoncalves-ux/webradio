# Web Rádio Vem Comigo no Glória — Painel FINAL

Projeto Android reconstruído para o painel da rádio.

## Esta versão
- Mantém o painel fixo, sem programação e sem os 13 cards.
- Remove “TOCANDO AGORA”.
- Mantém Hora Certa, menu, volume, compartilhar, músicas e pregações.
- O botão **OUVIR RÁDIO** abre a **transmissão direta**, sem página oficial e sem WebView.
- Endereço usado na transmissão direta: `http://sapircast.caster.fm:19513/QKnuH`
- Pastas criadas no armazenamento: `VemComigo/Musicas` e `VemComigo/Pregacoes`.

## GitHub Actions
O workflow em `.github/workflows/build-apk.yml` gera o APK Debug automaticamente.
