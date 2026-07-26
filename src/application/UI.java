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
	public static final String ANSI_BOLD = ESC + "[1m";
	public static final String ANSI_BLUE_BACKGROUND = ESC + "[44m";

	// One color per piece type; the side (white/black) is told apart by letter
	// case (uppercase = white, lowercase = black), so color always means "which
	// piece" and never gets confused with "which player".
	private static final String C_PAWN = ESC + "[90m"; // gray
	private static final String C_ROOK = ESC + "[94m"; // blue
	private static final String C_KNIGHT = ESC + "[96m"; // cyan
	private static final String C_BISHOP = ESC + "[92m"; // green
	private static final String C_QUEEN = ESC + "[95m"; // magenta
	private static final String C_KING = ESC + "[91m"; // red

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

	public static String readLine(Scanner sc) {
		return sc.nextLine();
	}

	public static ChessPosition readChessPosition(Scanner sc) {
		return parseChessPosition(sc.nextLine());
	}

	public static ChessPosition parseChessPosition(String s) {
		try {
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
		if (chessMatch.getCheckMate()) {
			System.out.println("CHECKMATE!");
			System.out.println("Winner: " + chessMatch.getWinner());
		} else if (chessMatch.getResigned()) {
			System.out.println(chessMatch.getCurrentPlayer() + " resigned.");
			System.out.println("Winner: " + chessMatch.getWinner());
		} else if (chessMatch.getDraw()) {
			System.out.println("DRAW! (" + chessMatch.getDrawReason() + ")");
		} else {
			System.out.println("Waiting player: " + chessMatch.getCurrentPlayer());
			if (chessMatch.getCheck()) {
				System.out.println("CHECK!");
			}
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
			String letter = piece.toString();
			String color = colorForPiece(letter);
			if (piece.getColor() == Color.WHITE) {
				System.out.print(ANSI_BOLD + color + letter + ANSI_RESET);
			} else {
				System.out.print(color + letter.toLowerCase() + ANSI_RESET);
			}
		}
		System.out.print(" ");
	}

	private static String colorForPiece(String letter) {
		switch (letter) {
			case "P":
				return C_PAWN;
			case "R":
				return C_ROOK;
			case "N":
				return C_KNIGHT;
			case "B":
				return C_BISHOP;
			case "Q":
				return C_QUEEN;
			case "K":
				return C_KING;
			default:
				return ANSI_RESET;
		}
	}
}
