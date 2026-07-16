# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [1.3.0] - 2026-07-16

### Adicionado
- **Devolução de foco à aplicação anterior** — ao clicar em qualquer botão de envio
  (M. Acesso, M. Recarga, 🎲 Aleatório, Enviar, recentes), o foco volta para a janela
  em que o usuário estava antes de clicar, e é lá que o RFID é digitado
- Rastreador de foreground (Win32 via JNA): um poller de 150ms memoriza a última janela
  de **outro processo** que teve o foco (compara o PID com o do próprio app). No envio,
  `SetForegroundWindow` devolve o foco a essa janela antes de injetar as teclas

### Alterado
- O envio **não minimiza mais** a própria janela nem a traz de volta ao foreground —
  o foco permanece na aplicação de destino. A janela do RFID Virtual continua visível
  no topo (always-on-top), apenas sem o foco
- **Fallback** para não-Windows (ou quando nenhuma janela anterior é conhecida):
  mantém o comportamento antigo (minimiza → digita → restaura)

### Dependências
- Adicionadas `net.java.dev.jna:jna` e `jna-platform` 5.14.0 (User32) — fat JAR passa de
  ~9MB para ~12MB pelos binários nativos da JNA

### Limitações conhecidas
- Se a aplicação anterior estiver **minimizada** no momento do envio, o foco não é
  restaurado (usa apenas `SetForegroundWindow`; `IsIconic`/`ShowWindow` não implementados
  — `IsIconic` não existe na interface `User32` da JNA). Detalhes e fix proposto em
  `PONTOS-ATENCAO.md` (item 3)

## [1.2.0] - 2026-07-15

### Adicionado
- **Modo mini widget** — barra compacta com botões **M. Acesso**, **M. Recarga** e **🎲 Aleatório**; é a tela inicial do app. Botões ➕/➖ alternam entre mini e expandido
- **Ícone na bandeja do sistema** (System Tray) — fechar a janela (❌ ou X) esconde para a bandeja; duplo clique no ícone reabre; menu com "Abrir" e "Sair" (só "Sair" encerra o app)
- Janela sem borda, fundo transparente e arrastável pela alça ⣿ (ou qualquer área); abre no canto inferior direito da tela
- Ícone do app desenhado em código (JavaFX Canvas para a janela, AWT para a bandeja)
- Duas seções de master no `favorites.txt`: `#Master Acesso` (vermelho) e `#Master Recarga` (laranja) — botões dedicados no mini widget enviam o primeiro RFID de cada seção

### Alterado
- Cartões master (acesso e recarga) não entram no histórico de recentes
- Parser do `favorites.txt`: cabeçalho contendo "acesso" ou "recarga" define a seção master; qualquer outro cabeçalho (`#Testes`, `#Demais`, ...) vai para o pool do aleatório

### Migração
- A seção antiga `#Master` **não é mais reconhecida** (cairia no pool do aleatório) — renomear para `#Master Acesso` no `favorites.txt`

## [1.1.0] - 2026-07-02

### Adicionado
- Seções categorizadas no `favorites.txt`: `#Master`, `#Testes`, `#Demais`
- Botões Master em vermelho, Testes em azul
- Botão **🎲 Aleatório** — sorteia e envia um RFID da seção `#Demais`
- Arquivo `favorites.example.txt` como template

### Removido
- Campo de busca (substituído pelo botão aleatório)
- Limite de 10 favoritos (agora suporta milhares no pool)

## [1.0.0] - 2026-07-02

### Adicionado
- App JavaFX standalone com janela always-on-top
- Campo de texto para RFID + botão Enviar
- Injeção de teclas via `java.awt.Robot` (keyboard wedge)
- Favoritos persistidos em `favorites.txt`
- Botão ★ para salvar favorito
- Clique no favorito envia direto
- Clique direito no favorito para remover
- Janela redimensionável
- `Launcher.java` como entry point (workaround JavaFX classpath)
