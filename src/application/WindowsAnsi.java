package application;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

// Turns on ANSI/VT escape-sequence processing in the legacy Windows console
// (conhost), where it is OFF by default — without this, double-clicking the
// .exe prints raw "ESC[33m" text instead of colors, which also throws the
// columns out of alignment. Uses the JDK Foreign Function API (stable since
// JDK 22), so there is no external dependency. No-op on non-Windows or when
// stdout is redirected to a file/pipe (no real console to configure).
public final class WindowsAnsi {

	private static final int STD_OUTPUT_HANDLE = -11;
	private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

	private WindowsAnsi() {
	}

	public static void enable() {
		if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
			return;
		}
		try {
			Linker linker = Linker.nativeLinker();
			SymbolLookup k32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

			MethodHandle getStdHandle = linker.downcallHandle(
					k32.find("GetStdHandle").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
			MethodHandle getConsoleMode = linker.downcallHandle(
					k32.find("GetConsoleMode").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
			MethodHandle setConsoleMode = linker.downcallHandle(
					k32.find("SetConsoleMode").orElseThrow(),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

			MemorySegment handle = (MemorySegment) getStdHandle.invoke(STD_OUTPUT_HANDLE);

			try (Arena arena = Arena.ofConfined()) {
				MemorySegment modePtr = arena.allocate(ValueLayout.JAVA_INT);
				if ((int) getConsoleMode.invoke(handle, modePtr) == 0) {
					return;
				}
				int mode = modePtr.get(ValueLayout.JAVA_INT, 0);
				setConsoleMode.invoke(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
			}
		} catch (Throwable t) {
			// Best effort: if enabling fails, the game still runs, just without
			// interpreted colors — so swallow rather than crash the program.
		}
	}
}
