# Pontos de Atenção — v1.3.0

Registrado em 2026-07-15 (revisão da v1.2.0). Atualizado em 2026-07-16 (v1.3.0).

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

## 3. Janela alvo minimizada não é restaurada ao devolver o foco (v1.3.0)

A devolução de foco (v1.3.0) usa apenas `SetForegroundWindow(hwnd)`. A checagem
`IsIconic` + `ShowWindow(SW_RESTORE)` **não foi implementada** porque `IsIconic`
não existe na interface `User32` da JNA (`jna-platform` 5.14.0).

Consequência: se a aplicação anterior estiver **minimizada** no exato momento do
envio, `SetForegroundWindow` sozinho não a restaura — as teclas podem não chegar
ao destino esperado.

- **Na prática raramente acontece**: a janela anterior costuma estar visível
  (o usuário acabou de usá-la antes de clicar no widget).
- **Fix proposto**: declarar uma extensão própria da interface `User32` com
  `IsIconic(HWND)` e `ShowWindow(HWND, int)`, e chamar `ShowWindow(hwnd,
  SW_RESTORE)` antes do `SetForegroundWindow` quando a janela estiver minimizada.

## 4. `build.bat` acoplado à versão do JAR

O `build.bat` copia `target\rfid-virtual-<versão>.jar` com a versão **fixa no
script**. Ficou defasado (apontava para `1.1.0`) desde a v1.2.0 até ser corrigido
na v1.3.0.

- **Lembrar**: a cada bump de versão no `pom.xml`, atualizar também o nome do JAR
  no `build.bat`.
- **Fix proposto**: usar um curinga ou ler a versão do `pom.xml` para evitar o
  acoplamento manual.
