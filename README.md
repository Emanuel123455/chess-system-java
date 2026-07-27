# Chess System Java

Um jogo de **xadrez jogável no terminal**, escrito em Java — um exercício de programação orientada a objetos que cobre o regulamento completo: geração de lances legais por peça, detecção de xeque/xeque-mate e todos os lances especiais (roque, en passant, promoção de peão).

> ⚠️ **Projeto de estudo.** Criado para **treino** de design orientado a objetos e Java, baseado no roteiro do curso *Programação Orientada a Objetos com Java* (educandoweb.com.br / Prof. Dr. Nelio Alves). O material de referência documentava a versão C# deste projeto; aqui ele foi refeito para Java a partir do mesmo checklist e verificado ponta a ponta.

## 🎯 Por que este projeto existe

Praticar a aplicação dos conceitos centrais de OO na construção de um sistema completo e não trivial a partir de um modelo de domínio:

- Encapsulamento, herança, polimorfismo, classes/métodos abstratos
- Uma **arquitetura em camadas**: um motor genérico de jogo de tabuleiro (`boardgame`) por baixo das regras específicas de xadrez (`chess`, `chess.pieces`), com uma UI de console (`application`) por cima
- Programação defensiva com exceções próprias
- Representação do tabuleiro por matriz 2D
- Uma máquina de estado completa: turnos, xeque, xeque-mate e todos os lances especiais do xadrez

## 🗂️ Arquitetura

```
application/    Program (ponto de entrada + loop do jogo), UI (renderização no console)
boardgame/      Board, Piece, Position, BoardException — genérico, agnóstico ao xadrez
chess/          ChessMatch, ChessPiece, ChessPosition, Color, ChessException
chess/pieces/   King, Queen, Rook, Bishop, Knight, Pawn
```

Sem framework, sem ferramenta de build — Java puro, compilado direto com `javac`.

## ✅ Funcionalidades

- Posição inicial padrão completa, com 32 peças
- Cálculo de lances legais por peça (linhas retas, diagonais, saltos do cavalo, avanços/capturas do peão)
- Controle de turno ("a peça escolhida não é sua")
- Detecção de xeque e xeque-mate, incluindo "você não pode se colocar em xeque"
- Registro de peças capturadas, mostradas por cor
- Lances especiais: **roque** (respeitando todas as condições — não pode rocar em xeque, atravessando casa atacada, nem para dentro de xeque), **en passant**, **promoção de peão** (escolha Bispo/Cavalo/Torre/Dama)
- **Todas as condições de empate**: afogamento (stalemate), material insuficiente (dead position), repetição tripla e regra dos 50 lances
- Comandos de **desistência** (`resign`) e **empate por acordo** (`draw`)
- Cores por tipo de peça (rei vermelho, dama magenta, torre azul, bispo verde, cavalo ciano, peão cinza), com o lado indicado pela caixa da letra, além do destaque dos lances possíveis
- Tratamento de exceções limpo — entrada inválida nunca derruba o jogo

## ▶️ Como rodar

Requer um JRE (Java 17+).

**Opção 1 — o jar executável** (sem precisar compilar):

```bash
java -jar chess-system-java.jar
```

**Opção 2 — compilar a partir do código-fonte:**

```bash
# Linux/Mac
javac -d bin $(find src -name "*.java")
java -cp bin application.Program
```

```powershell
# Windows PowerShell
javac -d bin (Get-ChildItem -Recurse -Filter *.java src | % FullName)
java -cp bin application.Program
```

## 🎮 Como jogar

A cada turno: digite a casa de **origem** e depois a casa de **destino**, em notação algébrica (ex.: `e2`, depois `e4`). O tabuleiro é reimpresso com os destinos legais da peça destacados antes de você confirmar o destino.

- **Roque**: mova o rei duas casas em direção à torre (ex.: `e1` → `g1`).
- **En passant**: capture na diagonal, na casa vazia atrás de um peão adversário que acabou de avançar duas casas.
- **Promoção**: quando um peão chega à última linha, o jogo pergunta qual peça você quer — `B`/`N`/`R`/`Q`.

Veja também [COMO-JOGAR.md](COMO-JOGAR.md) para o guia detalhado em português.

## 🚀 Possíveis melhorias futuras

Ideias para estender além do escopo de estudo:

- Histórico de lances / log em notação algébrica (exportação PGN)
- Um oponente com IA simples (minimax + função de avaliação)
- Interface gráfica (JavaFX/Swing) em vez do console
- Salvar/carregar o estado do jogo
- Jogo em rede (dois jogadores por sockets)
- Testes unitários dedicados para a geração de lances de cada peça

## 📚 Créditos

Projeto de estudo baseado no curso **Programação Orientada a Objetos com Java** — educandoweb.com.br / Prof. Dr. Nelio Alves. Refeito para Java a partir do checklist do curso, adaptado e verificado ponta a ponta como exercício de aprendizado.
