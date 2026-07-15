# Pontos de Atenção — v1.2.0

Registrado em 2026-07-15, durante a revisão da release v1.2.0.

## 1. Quebra de compatibilidade no `favorites.txt`

O parser da v1.2.0 só reconhece como seção master os cabeçalhos que contêm a
palavra **acesso** ou **recarga** (ex.: `#Master Acesso`, `#Master Recarga`).

A seção antiga `#Master` (formato v1.1.0) **não é mais reconhecida** — os RFIDs
dela caem no pool do botão 🎲 Aleatório. Consequência prática: um cartão master
poderia ser sorteado e enviado como se fosse cartão comum de teste.

**Migração**: em cada máquina que usa o app, renomear no `favorites.txt`:

```
#Master          →  #Master Acesso
```

## 2. Bug teórico: janela órfã quando não há bandeja do sistema

O app usa `Platform.setImplicitExit(false)` e consome o evento de fechamento da
janela (esconde em vez de encerrar), contando com o ícone da bandeja do sistema
para reabrir/sair.

Se `SystemTray.isSupported()` retornar `false` (ex.: Linux sem bandeja), o
`setupSystemTray()` retorna cedo **mas o comportamento de "fechar = esconder"
continua ativo**: fechar a janela deixa o processo rodando sem nenhuma forma de
reabrir ou encerrar pela UI (zumbi — só via gerenciador de tarefas).

- **No Windows não acontece** (bandeja sempre disponível) — por isso não foi
  corrigido na v1.2.0.
- **Fix proposto**: só interceptar o fechamento (`setOnCloseRequest` +
  `setImplicitExit(false)`) quando o ícone da bandeja for instalado com
  sucesso; caso contrário, fechar encerra o app normalmente.
