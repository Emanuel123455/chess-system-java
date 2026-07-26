# Chess System Java — O que é e como jogar

## O que é este projeto

Um jogo de **xadrez completo, jogado no terminal**, escrito em Java puro (sem
framework, sem Maven). É um **projeto de estudo** de Orientação a Objetos,
baseado no curso *Programação Orientada a Objetos com Java* (Prof. Dr. Nelio
Alves / educandoweb.com.br).

Implementa o jogo de xadrez de verdade:
- Todas as peças com seus movimentos legais (torre, cavalo, bispo, dama, rei, peão)
- Detecção de **xeque** e **xeque-mate**
- As três jogadas especiais: **roque**, **en passant** e **promoção de peão**
- Tabuleiro colorido no terminal, com destaque dos lances possíveis
- Peças capturadas listadas por cor

## Como abrir o jogo

**Opção 1 — executável nativo (mais simples, não precisa Java instalado):**

Vá até a pasta `dist\ChessSystemJava\` e dê duplo clique em
`ChessSystemJava.exe` (ou rode pelo terminal). É uma pasta só — leve `app\` e
`runtime\` junto se for copiar para outro PC, não adianta copiar só o `.exe`.

**Opção 2 — arquivo `.jar` (precisa ter Java instalado):**

```
java -jar chess-system-java.jar
```

## Como jogar

O jogo pede duas coisas por rodada, sempre em **notação algébrica** (a letra da
coluna de "a" a "h", seguida do número da linha de "1" a "8" — ex.: `e2`, `a1`,
`h8`):

```
Source: e2      ← de onde a peça sai
Target: e4      ← para onde ela vai
```

Depois de digitar a origem, o tabuleiro é reimpresso com os destinos possíveis
daquela peça **destacados em azul**, antes de você confirmar o destino.

### O tabuleiro

- Cada **tipo** de peça tem uma cor própria:
  - `K` Rei — **vermelho**
  - `Q` Dama — **magenta**
  - `R` Torre — **azul**
  - `B` Bispo — **verde**
  - `N` Cavalo — **ciano**
  - `P` Peão — **cinza**
- O **lado** é dado pela caixa da letra: **MAIÚSCULA = brancas** (e em negrito),
  **minúscula = pretas**. Ex.: `R` é torre branca, `r` é torre preta.
- `-` é casa vazia.
- Embaixo do tabuleiro aparecem as peças já capturadas de cada lado.
- No topo: de quem é a vez (`Waiting player`) e o número do turno.

### Jogadas especiais

- **Roque**: mova o rei **duas casas** na direção da torre (ex.: `e1` → `g1`
  pro roque pequeno). A torre pula sozinha para o lado certo. Só funciona se
  nem rei nem torre já se moveram e não há peças no caminho.
- **En passant**: se um peão adversário acabou de avançar duas casas e passou
  ao lado do seu peão, você pode capturá-lo **na diagonal, na casa atrás
  dele** — mas só na jogada **imediatamente seguinte**.
- **Promoção**: quando um peão chega na última linha (linha 8 para brancas,
  linha 1 para pretas), o jogo pergunta:
  ```
  Enter piece for promotion (B/N/R/Q):
  ```
  Digite a letra da peça que quer (Bispo, Cavalo, Torre ou Dama).

### Xeque e xeque-mate

- Se o seu rei está em xeque, aparece `CHECK!` na tela — e o jogo não deixa
  você fazer uma jogada que deixe (ou mantenha) seu próprio rei em xeque.
- Quando não há mais saída, aparece `CHECKMATE!` e o vencedor, e o jogo
  termina.

### Erros de digitação

Se você digitar uma posição inválida (fora de a1-h8) ou tentar mover uma peça
que não é sua, o jogo avisa o erro e deixa você tentar de novo — nada trava
nem fecha o programa.
