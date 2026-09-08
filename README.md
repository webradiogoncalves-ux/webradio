# Web Rádio Vem Comigo no Glória — Painel modificado

Projeto Android reconstruído a partir do APK fornecido, mantendo a proposta visual e as alterações solicitadas:

- remove a seção "PROGRAMAÇÃO • 13 PROGRAMAS";
- remove os 13 cards de programas;
- mantém o painel de áudio;
- tela principal fixa, sem ScrollView;
- biblioteca local em `VemComigo/Musicas` e `VemComigo/Pregacoes`;
- botão ÁUDIO abre a biblioteca local.

## Gerar o APK pelo GitHub

O projeto já contém o workflow `.github/workflows/build-apk.yml`.

Depois de colocar estes arquivos no repositório:

1. Abra a aba **Actions**.
2. Entre em **Gerar APK**.
3. Clique em **Run workflow** (ou faça um push na branch `main`).
4. Aguarde o build terminar.
5. Abra o workflow concluído e baixe o artefato **VemComigo-Painel-APK**.

O arquivo gerado será `app-debug.apk`.

> Observação: este é um projeto reconstruído, não o código-fonte original do APK. O endereço de transmissão usado no exemplo do projeto é um endereço candidato e precisa ser confirmado antes de uma versão de produção.
