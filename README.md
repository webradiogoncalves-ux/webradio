# Web TV Full — integração dos 3 conteúdos

Projeto Android nativo com **um único player** e três áreas:
- Canais → fonte PlayTV
- Novelas/Filmes → fonte Novecalizando
- Jogos/Esportes → fonte GetFut

O aplicativo não abre os APKs originais dentro dele. A arquitetura separa **catálogo/fonte** do **player único**.

## Funcionamento

`Canais / Novelas / Jogos → item selecionado → resolver da fonte → player único`

O player Media3 aceita formatos compatíveis como HLS, MP4/progressivo e MPEG-TS quando o stream e o servidor permitem.

### Teste já confirmado

O projeto deixa disponível um item de teste usando a transmissão MPEG-TS que foi capturada anteriormente durante o teste autorizado do PlayTV:

`http://79.127.238.228:14551/`

Ela deve ser tratada como **Teste PlayTV / MPEG-TS**, não como SBT.

## O que ainda precisa das fontes

Os três APKs obtêm parte do conteúdo dinamicamente. O código deste projeto já deixa o ponto de integração pronto (`SourceResolver`), mas não inventa URLs de canais, novelas ou jogos que não foram encontradas.

Para produção, cada fonte deve fornecer/permitir obter, pelo fluxo autorizado, a URL do conteúdo selecionado. Depois disso o mesmo método `play()` reproduz tudo.

## GitHub

Envie a pasta inteira para o repositório. O workflow em `.github/workflows/build.yml` gera o APK debug automaticamente.
