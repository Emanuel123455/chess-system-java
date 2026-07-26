# Chess System Java

A playable, terminal-based chess game built in Java — an object-oriented programming exercise covering the full rulebook: legal move generation per piece, check/checkmate detection, and all special moves (castling, en passant, pawn promotion).

> ⚠️ **Study project.** Built for **training** in object-oriented design and Java, based on the roadmap of the course *Programação Orientada a Objetos com Java* (educandoweb.com.br / Prof. Dr. Nelio Alves). The reference material documented the C# version of this project; it was rebuilt here for Java from the same checklist and verified end-to-end.

## 🎯 Why this project exists

Practice applying core OOP concepts to build a complete, non-trivial system from a domain model:

- Encapsulation, inheritance, polymorphism, abstract classes/methods
- A **layered architecture**: a generic board-game engine (`boardgame`) underneath chess-specific rules (`chess`, `chess.pieces`), with a console UI (`application`) on top
- Defensive programming with custom exceptions
- A 2D-matrix board representation
- A full game state machine: turns, check, checkmate, and every special move in chess

## 🗂️ Architecture

```
application/    Program (entry point + game loop), UI (console rendering)
boardgame/      Board, Piece, Position, BoardException — generic, chess-agnostic
chess/          ChessMatch, ChessPiece, ChessPosition, Color, ChessException
chess/pieces/   King, Queen, Rook, Bishop, Knight, Pawn
```

No framework, no build tool — plain Java, compiled directly with `javac`.

## ✅ Features

- Full standard 32-piece starting position
- Legal move calculation per piece (straight lines, diagonals, knight jumps, pawn pushes/captures)
- Turn enforcement ("the chosen piece is not yours")
- Check and checkmate detection, including "you can't put yourself in check"
- Captured-pieces tracking, shown by color
- Special moves: **castling** (respecting all conditions — can't castle out of, through, or into check), **en passant**, **pawn promotion** (choose Bishop/Knight/Rook/Queen)
- **All draw conditions**: stalemate, insufficient material (dead position), threefold repetition, and the fifty-move rule
- **Resign** and **draw-by-agreement** commands
- Per-piece-type colors (king red, queen magenta, rook blue, bishop green, knight cyan, pawn gray) with side shown by letter case, plus move highlighting
- Clean exception handling — invalid input never crashes the game

## ▶️ How to run

Requires a JRE (Java 17+).

**Option 1 — the executable jar** (no compilation needed):

```bash
java -jar chess-system-java.jar
```

**Option 2 — compile from source:**

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

## 🎮 How to play

Each turn: type the **source** square, then the **target** square, in algebraic notation (e.g. `e2`, then `e4`). The board reprints with the piece's legal destinations highlighted before you confirm the target.

- **Castling**: move the king two squares toward the rook (e.g. `e1` → `g1`).
- **En passant**: capture diagonally onto the empty square behind an opponent pawn that just advanced two squares.
- **Promotion**: when a pawn reaches the last rank, you'll be prompted to choose `B`/`N`/`R`/`Q`.

## 🚀 Possible future improvements

Ideas for extending this beyond the training scope:

- Move history / algebraic notation log (PGN export)
- A simple AI opponent (minimax + evaluation function)
- Graphical UI (JavaFX/Swing) instead of the console
- Save/load game state
- Network play (two players over sockets)
- Dedicated unit tests for each piece's move generation

## 📚 Credits

Study project based on the course **Programação Orientada a Objetos com Java** — educandoweb.com.br / Prof. Dr. Nelio Alves. Rebuilt for Java from the course checklist, adapted, and verified end-to-end as a learning exercise.
