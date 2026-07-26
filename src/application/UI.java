package application;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.Color;

public class UI {

	// Built from the raw ESC code point (decimal 27) instead of a string-literal
	// escape sequence, so it never depends on how backslash escapes survive
	// through any intermediate text-processing layer.
	private static final char ESC = (char) 27;

	public static final String ANSI_RESET = ESC + "[0m";
	public static final String ANSI_YELLOW = ESC + "[33m";
	public static final String ANSI_BLUE_BACKGROUND = ESC + "[44m";

	// https://stackoverflow.com/questions/2979383/java-clear-the-console
	public static void clearScreen() {
		System.out.print(ESC + "[H" + ESC + "[2J");
		System.out.flush();
	}

	public static String readPromotionType(Scanner sc) {
		String type = sc.nextLine().trim().toUpperCase();
		while (!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
			System.out.print("Invalid value! Enter piece for promotion (B/N/R/Q): ");
			type = sc.nextLine().trim().toUpperCase();
		}
		return type;
	}

	public static ChessPosition readChessPosition(Scanner sc) {
		try {
			String s = sc.nextLine();
			char column = s.charAt(0);
			int row = Integer.parseInt(s.substring(1));
			return new ChessPosition(column, row);
		}
		// Deliberately NOT a catch-all RuntimeException: that would also swallow
		// Scanner's own NoSuchElementException (thrown when input is exhausted),
		// turning an end-of-stream into an infinite non-blocking retry loop
		// instead of letting the program terminate.
		catch (StringIndexOutOfBoundsException | NumberFormatException | ChessException e) {
			throw new InputMismatchException("Error reading ChessPosition. Valid values are from a1 to h8.");
		}
	}

	public static void printMatch(ChessMatch chessMatch) {
		printBoard(chessMatch.getPieces());
		System.out.println();
		printCapturedPieces(chessMatch);
		System.out.println();
		System.out.println("Turn: " + chessMatch.getTurn());
		if (!chessMatch.getCheckMate()) {
			System.out.println("Waiting player: " + chessMatch.getCurrentPlayer());
			if (chessMatch.getCheck()) {
				System.out.println("CHECK!");
			}
		} else {
			System.out.println("CHECKMATE!");
			System.out.println("Winner: " + chessMatch.getCurrentPlayer());
		}
	}

	private static void printCapturedPieces(ChessMatch chessMatch) {
		List<ChessPiece> white = chessMatch.getCapturedPieces(Color.WHITE);
		List<ChessPiece> black = chessMatch.getCapturedPieces(Color.BLACK);
		System.out.println("Captured pieces:");
		System.out.print("White: ");
		System.out.println(Arrays.toString(white.toArray()));
		System.out.print("Black: ");
		System.out.println(Arrays.toString(black.toArray()));
	}

	public static void printBoard(ChessPiece[][] pieces) {
		printBoard(pieces, new boolean[pieces.length][pieces.length]);
	}

	public static void printBoard(ChessPiece[][] pieces, boolean[][] possibleMoves) {
		for (int i = 0; i < pieces.length; i++) {
			System.out.print((8 - i) + " ");
			for (int j = 0; j < pieces.length; j++) {
				printPiece(pieces[i][j], possibleMoves[i][j]);
			}
			System.out.println();
		}
		System.out.println("  a b c d e f g h");
	}

	private static void printPiece(ChessPiece piece, boolean background) {
		if (background) {
			System.out.print(ANSI_BLUE_BACKGROUND);
		}
		if (piece == null) {
			System.out.print("-" + ANSI_RESET);
		} else {
			if (piece.getColor() == Color.WHITE) {
				System.out.print(piece + ANSI_RESET);
			} else {
				System.out.print(ANSI_YELLOW + piece + ANSI_RESET);
			}
		}
		System.out.print(" ");
	}
}
