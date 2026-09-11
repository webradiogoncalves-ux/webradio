# Arquitetura

Android (Painel) -> HTTPS -> Backend -> Caster.fm API

O painel consulta `/api/status` e envia `/api/server/start` e `/api/server/stop`.

A autenticação privada fica exclusivamente no backend.

A grade de Hora Certa e chamadas é separada do controle do servidor. Para execução automática no ar, é necessário um motor de automação que faça a saída de áudio para o mount-point do Caster.fm.
