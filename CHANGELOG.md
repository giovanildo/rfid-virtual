# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

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
