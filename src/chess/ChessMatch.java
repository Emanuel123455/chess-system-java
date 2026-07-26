package chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {

	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private boolean draw;
	private String drawReason;
	private boolean resigned;
	private int halfmoveClock;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;

	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();
	private Map<String, Integer> positionCounts = new HashMap<>();

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
		recordPosition();
	}

	public int getTurn() {
		return turn;
	}

	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean getCheck() {
		return check;
	}

	public boolean getCheckMate() {
		return checkMate;
	}

	public boolean getDraw() {
		return draw;
	}

	public String getDrawReason() {
		return drawReason;
	}

	public boolean getResigned() {
		return resigned;
	}

	// Winner of a decided game, or null while it is still going or drawn. On
	// checkmate the winner is the side that just moved (currentPlayer, since the
	// turn isn't passed after mate); on resignation it is the side to move's
	// opponent, because a player resigns on their own turn.
	public Color getWinner() {
		if (checkMate) {
			return currentPlayer;
		}
		if (resigned) {
			return opponent(currentPlayer);
		}
		return null;
	}

	public void resign() {
		resigned = true;
	}

	public void agreeDraw() {
		draw = true;
		drawReason = "Agreement";
	}

	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}

	public ChessPiece getPromoted() {
		return promoted;
	}

	public ChessPiece[][] getPieces() {
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i, j);
			}
		}
		return mat;
	}

	public List<ChessPiece> getCapturedPieces(Color color) {
		List<ChessPiece> list = new ArrayList<>();
		for (Piece p : capturedPieces) {
			ChessPiece cp = (ChessPiece) p;
			if (cp.getColor() == color) {
				list.add(cp);
			}
		}
		return list;
	}

	public boolean[][] possibleMoves(ChessPosition sourcePosition) {
		Position position = sourcePosition.toPosition();
		validateSourcePosition(position);
		return board.piece(position).possibleMoves();
	}

	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Position source = sourcePosition.toPosition();
		Position target = targetPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		validateCastleThroughCheck(source, target);
		Piece capturedPiece = makeMove(source, target);

		if (testCheck(currentPlayer)) {
			undoMove(source, target, capturedPiece);
			throw new ChessException("You can't put yourself in check");
		}

		ChessPiece movedPiece = (ChessPiece) board.piece(target);

		// These two facts depend only on the move itself, never on which piece a
		// pawn promotes to, so they are settled here for every move.
		// fifty-move clock: a pawn move or a capture resets it, otherwise it counts
		// up (capturedPiece covers en passant too, it moved a pawn anyway).
		if (movedPiece instanceof Pawn || capturedPiece != null) {
			halfmoveClock = 0;
		} else {
			halfmoveClock++;
		}
		// en passant window: only a two-square pawn advance opens it
		if (movedPiece instanceof Pawn
				&& (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
			enPassantVulnerable = movedPiece;
		} else {
			enPassantVulnerable = null;
		}

		// PROBLEM (promotion): a pawn's promotion piece is chosen by the player in
		// the UI *after* this method returns (Program then calls
		// replacePromotedPiece). Yet whether the move gives check/checkmate/
		// stalemate, and which position is stored for threefold, all depend on that
		// final piece. Judging the outcome here would have to assume a piece (the
		// old code forced a Queen), so a later under-promotion left the flags wrong.
		// SOLUTION: for a promotion, don't judge the outcome yet — flag the pawn as
		// pending and return. concludeTurn() then runs from replacePromotedPiece,
		// once the real piece is on the board. A normal move already has its final
		// piece, so it is concluded right away. Move-generation and check-detection
		// are unchanged; only *when* the outcome is evaluated moves.
		promoted = null;
		if (movedPiece instanceof Pawn
				&& ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0)
						|| (movedPiece.getColor() == Color.BLACK && target.getRow() == 7))) {
			promoted = movedPiece;
		} else {
			concludeTurn();
		}

		return (ChessPiece) capturedPiece;
	}

	// Evaluates the end of the turn against the position now on the board: check,
	// checkmate/stalemate, the three positional draw rules, and passing the turn.
	// Runs immediately for a normal move, or from replacePromotedPiece once a
	// promoted pawn's chosen piece is in place (see the note in performChessMove).
	private void concludeTurn() {
		check = testCheck(opponent(currentPlayer));

		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		} else if (testStaleMate(opponent(currentPlayer))) {
			draw = true;
			drawReason = "Stalemate";
		} else {
			nextTurn();
		}

		if (!checkMate && !draw && insufficientMaterial()) {
			draw = true;
			drawReason = "Insufficient material";
		}
		// 100 half-moves = 50 full moves by each side with no pawn move or capture
		if (!checkMate && !draw && halfmoveClock >= 100) {
			draw = true;
			drawReason = "Fifty-move rule";
		}
		// threefold repetition: same position (placement + side to move + castling
		// rights + en passant target) reached a third time
		if (!checkMate && !draw && recordPosition() >= 3) {
			draw = true;
			drawReason = "Threefold repetition";
		}
	}

	public ChessPiece replacePromotedPiece(String type) {
		if (promoted == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if (!type.equals("B") && !type.equals("N") && !type.equals("Q") && !type.equals("R")) {
			return promoted;
		}

		Position pos = promoted.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);

		ChessPiece newPiece = newPiece(type, promoted.getColor());
		board.placePiece(newPiece, pos);
		piecesOnTheBoard.add(newPiece);

		// The player's real piece is on the board now, so evaluate the end of the
		// turn against it. This is the deferred half of the promotion handling in
		// performChessMove: doing it here, instead of on an assumed Queen, is what
		// makes check/checkmate/stalemate/draw/threefold correct for any choice.
		concludeTurn();

		return newPiece;
	}

	private ChessPiece newPiece(String type, Color color) {
		if (type.equals("B")) {
			return new Bishop(board, color);
		}
		if (type.equals("N")) {
			return new Knight(board, color);
		}
		if (type.equals("R")) {
			return new Rook(board, color);
		}
		return new Queen(board, color);
	}

	private Piece makeMove(Position source, Position target) {
		ChessPiece p = (ChessPiece) board.removePiece(source);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target);

		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}

		// kingside castling: rook jumps to the king's other side
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}

		// queenside castling
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}

		// en passant: a pawn moved diagonally onto an empty square, so the piece it
		// captured isn't on the target square but on the one right beside it
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && capturedPiece == null) {
				Position pawnPosition;
				if (p.getColor() == Color.WHITE) {
					pawnPosition = new Position(target.getRow() + 1, target.getColumn());
				} else {
					pawnPosition = new Position(target.getRow() - 1, target.getColumn());
				}
				capturedPiece = board.removePiece(pawnPosition);
				capturedPieces.add(capturedPiece);
				piecesOnTheBoard.remove(capturedPiece);
			}
		}

		return capturedPiece;
	}

	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece) board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			capturedPieces.remove(capturedPiece);
			piecesOnTheBoard.add(capturedPiece);
		}

		// undo kingside castling
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
		}

		// undo queenside castling
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
		}

		// undo en passant: the generic block above placed the captured pawn back on
		// the target square; move it back beside the source square where it belongs
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
				ChessPiece pawn = (ChessPiece) board.removePiece(target);
				Position pawnPosition;
				if (p.getColor() == Color.WHITE) {
					pawnPosition = new Position(3, target.getColumn());
				} else {
					pawnPosition = new Position(4, target.getColumn());
				}
				board.placePiece(pawn, pawnPosition);
			}
		}
	}

	private void validateSourcePosition(Position position) {
		if (!board.thereIsAPiece(position)) {
			throw new ChessException("There is no piece on source position");
		}
		if (currentPlayer != ((ChessPiece) board.piece(position)).getColor()) {
			throw new ChessException("The chosen piece is not yours");
		}
		if (!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible moves for the chosen piece");
		}
	}

	private void validateTargetPosition(Position source, Position target) {
		if (!board.piece(source).possibleMove(target)) {
			throw new ChessException("The chosen piece can't move to target position");
		}
	}

	// A king may not castle across a square that is under attack. The starting
	// square is already handled (King.possibleMoves won't offer castling while in
	// check) and the landing square is caught by the general self-check guard
	// after the move; this fills the remaining gap — the square the king passes
	// over. We simulate the king one step toward the rook and test for check.
	private void validateCastleThroughCheck(Position source, Position target) {
		ChessPiece p = (ChessPiece) board.piece(source);
		if (!(p instanceof King) || Math.abs(target.getColumn() - source.getColumn()) != 2) {
			return;
		}
		int step = (target.getColumn() > source.getColumn()) ? 1 : -1;
		Position middle = new Position(source.getRow(), source.getColumn() + step);
		Piece king = board.removePiece(source);
		board.placePiece(king, middle);
		boolean middleAttacked = testCheck(currentPlayer);
		board.removePiece(middle);
		board.placePiece(king, source);
		if (middleAttacked) {
			throw new ChessException("You can't castle across a square that is under attack");
		}
	}

	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private ChessPiece king(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			if (p instanceof King) {
				return (ChessPiece) p;
			}
		}
		throw new IllegalStateException("There is no " + color + " king on the board");
	}

	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = piecesOnTheBoard.stream()
				.filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
		for (Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves();
			if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
				return true;
			}
		}
		return false;
	}

	// True if `color` has at least one move that does not leave its own king in
	// check. Shared by checkmate (in check + no legal move) and stalemate (not in
	// check + no legal move); the piece move generators produce pseudo-legal
	// moves, so each candidate is simulated and checked here.
	private boolean hasAnyLegalMove(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for (int i = 0; i < board.getRows(); i++) {
				for (int j = 0; j < board.getColumns(); j++) {
					if (mat[i][j]) {
						Position source = ((ChessPiece) p).getChessPosition().toPosition();
						Position target = new Position(i, j);
						Piece capturedPiece = makeMove(source, target);
						boolean stillInCheck = testCheck(color);
						undoMove(source, target, capturedPiece);
						if (!stillInCheck) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean testCheckMate(Color color) {
		return testCheck(color) && !hasAnyLegalMove(color);
	}

	private boolean testStaleMate(Color color) {
		return !testCheck(color) && !hasAnyLegalMove(color);
	}

	// Draw by "dead position": neither side has enough material to force mate.
	// Standard auto-draw set: K vs K, K + one minor (bishop/knight) vs K, and
	// K+B vs K+B with both bishops on same-colored squares. Any pawn, rook or
	// queen on the board means there is enough material, so it is not a draw.
	private boolean insufficientMaterial() {
		List<ChessPiece> minors = new ArrayList<>();
		for (Piece piece : piecesOnTheBoard) {
			ChessPiece p = (ChessPiece) piece;
			if (p instanceof Pawn || p instanceof Rook || p instanceof Queen) {
				return false;
			}
			if (p instanceof Bishop || p instanceof Knight) {
				minors.add(p);
			}
		}
		if (minors.size() <= 1) {
			return true;
		}
		if (minors.size() == 2) {
			ChessPiece a = minors.get(0);
			ChessPiece b = minors.get(1);
			if (a instanceof Bishop && b instanceof Bishop && a.getColor() != b.getColor()
					&& squareParity(a) == squareParity(b)) {
				return true;
			}
		}
		return false;
	}

	private int squareParity(ChessPiece piece) {
		Position pos = piece.getChessPosition().toPosition();
		return (pos.getRow() + pos.getColumn()) % 2;
	}

	// Records the current position and returns how many times it has now occurred.
	// Two positions are "the same" for the repetition rule when piece placement,
	// side to move, castling rights and en passant target all match.
	private int recordPosition() {
		String key = positionKey();
		int count = positionCounts.getOrDefault(key, 0) + 1;
		positionCounts.put(key, count);
		return count;
	}

	private String positionKey() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
				ChessPiece p = (ChessPiece) board.piece(i, j);
				if (p == null) {
					sb.append('.');
				} else {
					String letter = p.toString();
					sb.append(p.getColor() == Color.WHITE ? letter : letter.toLowerCase());
				}
			}
		}
		sb.append('|').append(currentPlayer);
		sb.append('|').append(castlingRights());
		sb.append('|').append(enPassantVulnerable != null ? enPassantVulnerable.getChessPosition() : "-");
		return sb.toString();
	}

	private String castlingRights() {
		StringBuilder sb = new StringBuilder();
		if (king(Color.WHITE).getMoveCount() == 0) {
			if (unmovedRook('h', 1)) {
				sb.append('K');
			}
			if (unmovedRook('a', 1)) {
				sb.append('Q');
			}
		}
		if (king(Color.BLACK).getMoveCount() == 0) {
			if (unmovedRook('h', 8)) {
				sb.append('k');
			}
			if (unmovedRook('a', 8)) {
				sb.append('q');
			}
		}
		return sb.length() == 0 ? "-" : sb.toString();
	}

	private boolean unmovedRook(char file, int rank) {
		ChessPiece p = (ChessPiece) board.piece(new ChessPosition(file, rank).toPosition());
		return p instanceof Rook && p.getMoveCount() == 0;
	}

	private void placeNewPiece(char column, int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		piecesOnTheBoard.add(piece);
	}

	private void initialSetup() {
		placeNewPiece('a', 1, new Rook(board, Color.WHITE));
		placeNewPiece('b', 1, new Knight(board, Color.WHITE));
		placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('d', 1, new Queen(board, Color.WHITE));
		placeNewPiece('e', 1, new King(board, Color.WHITE, this));
		placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('g', 1, new Knight(board, Color.WHITE));
		placeNewPiece('h', 1, new Rook(board, Color.WHITE));
		for (char column = 'a'; column <= 'h'; column++) {
			placeNewPiece(column, 2, new Pawn(board, Color.WHITE, this));
		}

		placeNewPiece('a', 8, new Rook(board, Color.BLACK));
		placeNewPiece('b', 8, new Knight(board, Color.BLACK));
		placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('d', 8, new Queen(board, Color.BLACK));
		placeNewPiece('e', 8, new King(board, Color.BLACK, this));
		placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('g', 8, new Knight(board, Color.BLACK));
		placeNewPiece('h', 8, new Rook(board, Color.BLACK));
		for (char column = 'a'; column <= 'h'; column++) {
			placeNewPiece(column, 7, new Pawn(board, Color.BLACK, this));
		}
	}
}
