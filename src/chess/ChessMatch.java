package chess;

import java.util.ArrayList;
import java.util.List;
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
	private int halfmoveClock;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;

	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
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

		// promotion
		promoted = null;
		if (movedPiece instanceof Pawn) {
			if ((movedPiece.getColor() == Color.WHITE && target.getRow() == 0)
					|| (movedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
				promoted = movedPiece;
				promoted = replacePromotedPiece("Q");
			}
		}

		// fifty-move rule clock: a pawn move or a capture resets it, anything else
		// counts up. capturedPiece covers en passant too (it moved a pawn anyway).
		if (movedPiece instanceof Pawn || capturedPiece != null) {
			halfmoveClock = 0;
		} else {
			halfmoveClock++;
		}

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

		// en passant vulnerability: only a two-square pawn advance opens the window
		if (movedPiece instanceof Pawn
				&& (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
			enPassantVulnerable = movedPiece;
		} else {
			enPassantVulnerable = null;
		}

		return (ChessPiece) capturedPiece;
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

		// performChessMove auto-promotes to Queen just to compute check/checkmate
		// before the player is asked what they actually want; now that their real
		// choice is on the board, refresh the check flag so it isn't stale (e.g. a
		// Queen giving check on the back rank while a Knight, chosen instead, would not).
		check = testCheck(opponent(newPiece.getColor()));

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
