# RFID Virtual

Leitor RFID virtual para testes. Simula um leitor físico keyboard wedge — injeta os dígitos do cartão como teclas na janela ativa.

## Requisitos

- JDK 21+
- Maven

## Como usar

1. Abra o projeto no IntelliJ (File → Open → pasta `rfid-virtual`) e rode `Launcher.main()`, ou execute o fat JAR (`build.bat` → `rfid-virtual.jar`)
2. O app abre como **mini widget** no canto inferior direito, sempre por cima (always on top):
   - **M. Acesso** / **M. Recarga** → envia o cartão master da seção correspondente
   - **🎲 Aleatório** → sorteia um RFID do pool e envia
   - **➕** expande para a janela completa (campo de texto, recentes); **➖ Recolher** volta ao mini
   - Arraste pela alça ⣿ (ou qualquer área) para reposicionar
3. Na janela expandida, digite o RFID e pressione Enter ou clique **Enviar**
4. Ao enviar, o app **devolve o foco para a aplicação em que você estava** e injeta as teclas lá — a janela do RFID Virtual continua visível no topo, sem roubar o foco (Windows). Em outros sistemas, cai no comportamento antigo (minimiza → digita → restaura)
5. Fechar a janela (❌) **não encerra o app** — ele fica na bandeja do sistema (perto do relógio). Duplo clique no ícone reabre; para encerrar, clique direito → **Sair**

## Favoritos

Copie `favorites.example.txt` para `favorites.txt` e adicione os RFIDs:

```
#Master Acesso
0666885278

#Master Recarga
2023097202

#Demais
0167978640
0168517536
```

| Seção | Cor | Função |
|---|---|---|
| `#Master Acesso` | Vermelho | Cartão master do terminal de acesso |
| `#Master Recarga` | Laranja | Cartão master do guichê de recarga |
| Demais seções (`#Testes`, `#Demais`, ...) | — | Pool para o botão aleatório |

- O cabeçalho da seção é reconhecido por conter a palavra **acesso** ou **recarga**; qualquer outro vai para o pool
- **Clique** num botão master → envia direto
- Cartões enviados aparecem em **Recentes** (últimos 10; masters não entram)
- `favorites.txt` e `history.txt` estão no `.gitignore` (contêm RFIDs reais — não versionar)

> ⚠️ **Migração da v1.1.0**: a seção antiga `#Master` não é mais reconhecida — renomeie para `#Master Acesso`.

## Build

```bash
mvn compile
mvn exec:java -Dexec.mainClass=Launcher

# fat JAR (gera rfid-virtual.jar na raiz)
build.bat
```
