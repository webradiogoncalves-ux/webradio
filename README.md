# Web Rádio Vem Comigo no Glória — Painel modificado

Projeto Android Studio reconstruído a partir do APK fornecido pelo usuário.

## O que esta versão altera
- remove a seção **PROGRAMAÇÃO • 13 PROGRAMAS**;
- remove os 13 cards de programação;
- mantém o painel de áudio;
- mantém a tela principal sem `ScrollView`;
- inclui a biblioteca `VemComigo/Musicas` e `VemComigo/Pregacoes`.

## Gerar o APK pelo GitHub
Este projeto já vem preparado para **GitHub Actions**.

1. Crie um repositório no GitHub.
2. Envie todos os arquivos deste projeto para o repositório.
3. Abra a aba **Actions**.
4. Selecione **Gerar APK**.
5. Clique em **Run workflow** (ou faça um push na branch `main`/`master`).
6. Quando terminar, abra a execução e baixe o artefato **VemComigo-Painel-debug**.

O workflow usa Java 17, Gradle 8.7 e o Android Gradle Plugin 8.6.1 já definido no projeto.

## Observação
Este é um projeto reconstruído, não o código-fonte original do APK. Portanto, ele não deve ser tratado como garantia de reprodução de 100% das funções internas do APK original.
