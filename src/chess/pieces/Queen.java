package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class Queen extends ChessPiece {

	public Queen(Board board, Color color) {
		super(board, color);
	}

	@Override
	public String toString() {
		return "Q";
	}

	@Override
	public boolean[][] possibleMoves() {
		boolean[][] mat = new boolean[getBoard().getRows()][getBoard().getColumns()];

		Position p = new Position(0, 0);

		int[][] directions = {
				{ -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, // rook-like
				{ -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } // bishop-like
		};

		for (int[] d : directions) {
			p.setValues(position.getRow() + d[0], position.getColumn() + d[1]);
			while (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;
				p.setValues(p.getRow() + d[0], p.getColumn() + d[1]);
			}
			if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;
			}
		}

		return mat;
	}
}
