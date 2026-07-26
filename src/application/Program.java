package application;

import java.util.InputMismatchException;
import java.util.Scanner;

import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;

public class Program {

	public static void main(String[] args) {
		WindowsAnsi.enable();
		Scanner sc = new Scanner(System.in);
		ChessMatch chessMatch = new ChessMatch();

		while (true) {
			try {
				UI.clearScreen();
				UI.printMatch(chessMatch);

				if (chessMatch.getCheckMate() || chessMatch.getDraw() || chessMatch.getResigned()) {
					break;
				}

				System.out.println();
				System.out.println("(type 'resign' to give up, or 'draw' to offer a draw)");
				System.out.print("Source: ");
				String sourceInput = UI.readLine(sc);
				String command = sourceInput.trim().toLowerCase();

				if (command.equals("resign")) {
					chessMatch.resign();
					UI.clearScreen();
					UI.printMatch(chessMatch);
					break;
				}
				if (command.equals("draw")) {
					System.out.print("Opponent, accept draw? (y/n): ");
					String answer = UI.readLine(sc).trim().toLowerCase();
					if (answer.equals("y") || answer.equals("yes")) {
						chessMatch.agreeDraw();
						UI.clearScreen();
						UI.printMatch(chessMatch);
						break;
					}
					continue;
				}

				ChessPosition source = UI.parseChessPosition(sourceInput);

				boolean[][] possibleMoves = chessMatch.possibleMoves(source);
				UI.clearScreen();
				UI.printBoard(chessMatch.getPieces(), possibleMoves);
				System.out.println();
				System.out.print("Target: ");
				ChessPosition target = UI.readChessPosition(sc);

				ChessPiece capturedPiece = chessMatch.performChessMove(source, target);

				if (chessMatch.getPromoted() != null) {
					System.out.print("Enter piece for promotion (B/N/R/Q): ");
					String type = UI.readPromotionType(sc);
					chessMatch.replacePromotedPiece(type);
				}
			} catch (ChessException e) {
				System.out.println(e.getMessage());
			} catch (InputMismatchException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
