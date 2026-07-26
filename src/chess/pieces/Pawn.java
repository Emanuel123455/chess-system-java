package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.Color;

public class Pawn extends ChessPiece {

	private ChessMatch chessMatch;

	public Pawn(Board board, Color color, ChessMatch chessMatch) {
		super(board, color);
		this.chessMatch = chessMatch;
	}

	@Override
	public String toString() {
		return "P";
	}

	@Override
	public boolean[][] possibleMoves() {
		boolean[][] mat = new boolean[getBoard().getRows()][getBoard().getColumns()];

		Position p = new Position(0, 0);

		int direction = (getColor() == Color.WHITE) ? -1 : 1;
		int enPassantRow = (getColor() == Color.WHITE) ? 3 : 4;

		// one step forward
		p.setValues(position.getRow() + direction, position.getColumn());
		if (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p)) {
			mat[p.getRow()][p.getColumn()] = true;
		}

		// two steps forward (only on the pawn's very first move)
		Position p2 = new Position(position.getRow() + direction, position.getColumn());
		p.setValues(position.getRow() + 2 * direction, position.getColumn());
		if (getMoveCount() == 0 && getBoard().positionExists(p) && !getBoard().thereIsAPiece(p)
				&& getBoard().positionExists(p2) && !getBoard().thereIsAPiece(p2)) {
			mat[p.getRow()][p.getColumn()] = true;
		}

		// diagonal captures
		p.setValues(position.getRow() + direction, position.getColumn() - 1);
		if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
			mat[p.getRow()][p.getColumn()] = true;
		}
		p.setValues(position.getRow() + direction, position.getColumn() + 1);
		if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
			mat[p.getRow()][p.getColumn()] = true;
		}

		// en passant
		if (position.getRow() == enPassantRow) {
			Position left = new Position(position.getRow(), position.getColumn() - 1);
			if (getBoard().positionExists(left) && isThereOpponentPiece(left)
					&& getBoard().piece(left) == chessMatch.getEnPassantVulnerable()) {
				mat[left.getRow() + direction][left.getColumn()] = true;
			}
			Position right = new Position(position.getRow(), position.getColumn() + 1);
			if (getBoard().positionExists(right) && isThereOpponentPiece(right)
					&& getBoard().piece(right) == chessMatch.getEnPassantVulnerable()) {
				mat[right.getRow() + direction][right.getColumn()] = true;
			}
		}

		return mat;
	}
}
