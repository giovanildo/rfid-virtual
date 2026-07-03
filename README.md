# RFID Virtual

Leitor RFID virtual para testes. Simula um leitor físico keyboard wedge — injeta os dígitos do cartão como teclas na janela ativa.

## Requisitos

- JDK 21+
- Maven

## Como usar

1. Abra o projeto no IntelliJ (File → Open → pasta `rfid-virtual`)
2. Rode `Launcher.main()`
3. A janela fica sempre por cima (always on top)
4. Digite o RFID e pressione Enter, ou clique **Enviar**
5. O app minimiza, injeta as teclas na janela anterior e volta

## Favoritos

Copie `favorites.example.txt` para `favorites.txt` e adicione os RFIDs:

```
#Master
0666885278

#Testes
2023097202
3574806287

#Demais
0167978640
0168517536
```

| Seção | Cor | Função |
|---|---|---|
| `#Master` | Vermelho | Cartões master (operador) |
| `#Testes` | Azul | Cartões de teste fixos |
| `#Demais` | -- | Pool para o botão aleatório |

- **Clique** num botão Master ou Teste → envia direto
- **🎲 Aleatório** → sorteia um RFID da seção `#Demais` e envia
- `favorites.txt` está no `.gitignore`

## Build

```bash
mvn compile
mvn exec:java -Dexec.mainClass=Launcher
```
