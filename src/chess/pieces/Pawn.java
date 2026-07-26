package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class Pawn extends ChessPiece {

	public Pawn(Board board, Color color) {
		super(board, color);
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

		return mat;
	}
}
