package de.soderer.utilities.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import de.soderer.utilities.Utilities;

/**
 * Reads single keys from a Linux terminal via libc/termios (JNA).
 *
 * @deprecated Duplicates the Linux part of {@link JnaKeyStrokeReader} (libc/termios based raw mode + read).
 * Nothing in this package calls it - the actually used runtime path is {@link ConsoleUtilities#readKey()}, which
 * handles Linux raw mode via an external 'stty' process call instead. Kept only for reference; consolidate onto
 * one approach and remove this class once confirmed unused elsewhere in the project.
 */
@Deprecated
public class LinuxKeyStrokeReader {
	private LinuxKeyStrokeReader() {
		// Only static methods
	}

	private static volatile boolean initDone;
	private static volatile boolean stdinIsConsole;
	private static volatile boolean consoleModeAltered;

	private static volatile Libc libc;
	private static volatile TerminalFlags originalTerminalFlags;
	private static volatile TerminalFlags rawTerminalFlags;
	private static volatile TerminalFlags intermediateTerminalFlags;

	/**
	 * Read a character from the console without echo.
	 *
	 * @param wait true to wait for the next key, false to return immediately if no key is available
	 * @return key code (multi byte sequences combined into one int), -2 if no key is available and wait is false
	 * @throws Exception if the terminal cannot be configured or read
	 */
	@Deprecated
	public static int readKey(final boolean wait) throws Exception {
		init();
		if (!stdinIsConsole) {
			return readSingleCharFromByteStream(System.in);
		} else {
			consoleModeAltered = true;
			setTerminalAttrs(0, rawTerminalFlags);
			try {
				if (!wait && System.in.available() == 0) {
					return -2;
				} else {
					return readSingleCharFromByteStream(System.in);
				}
			} finally {
				setTerminalAttrs(0, intermediateTerminalFlags);
			}
		}
	}

	private static synchronized void init() throws IOException {
		if (!initDone) {
			libc = Native.load("c", Libc.class);
			stdinIsConsole = libc.isatty(0) == 1;

			if (stdinIsConsole) {
				originalTerminalFlags = getTerminalAttrs(0);
				rawTerminalFlags = new TerminalFlags(originalTerminalFlags);
				rawTerminalFlags.c_lflag &= ~(LibcDefinitions.ICANON | LibcDefinitions.ECHO | LibcDefinitions.ECHONL | LibcDefinitions.ISIG);
				intermediateTerminalFlags = new TerminalFlags(rawTerminalFlags);
				intermediateTerminalFlags.c_lflag |= LibcDefinitions.ICANON;

				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							if (initDone && stdinIsConsole && consoleModeAltered) {
								setTerminalAttrs(0, originalTerminalFlags);
								consoleModeAltered = false;
							}
						} catch (@SuppressWarnings("unused") final Exception e) {
							// do nothing
						}
					}
				});
			}
			initDone = true;
		}
	}

	private static TerminalFlags getTerminalAttrs(final int fd) throws IOException {
		final TerminalFlags termios = new TerminalFlags();
		try {
			if (libc.tcgetattr(fd, termios) != 0) {
				throw new RuntimeException("tcgetattr failed");
			}
		} catch (final LastErrorException e) {
			throw new IOException("tcgetattr failed", e);
		}
		return termios;
	}

	private static void setTerminalAttrs(final int fd, final TerminalFlags termios) throws IOException {
		try {
			if (libc.tcsetattr(fd, LibcDefinitions.TCSANOW, termios) != 0) {
				throw new RuntimeException("tcsetattr failed");
			}
		} catch (final LastErrorException e) {
			throw new IOException("tcsetattr failed", e);
		}
	}

	private static int readSingleCharFromByteStream(final InputStream inputStream) throws Exception {
		// Poll with a short sleep instead of a hard busy-spin, to avoid pinning a CPU core while waiting for input
		while (inputStream.available() == 0) {
			Thread.sleep(1);
		}
		final byte[] buffer = new byte[4];
		int index = 3;
		while (inputStream.available() > 0) {
			final byte nextByte = (byte) inputStream.read();
			if (index >= 0) {
				buffer[index--] = nextByte;
			}
			// else: more than 4 bytes arrived in this burst; still drain them from the stream so they don't leak
			// into and corrupt the next readKey() call, even though they don't fit into the returned int
		}
		return Utilities.convertByteArrayToInt(buffer);
	}

	/**
	 * Native termios structure of Linux.
	 */
	@Deprecated
	protected static class TerminalFlags extends Structure {
		/**
		 * Input mode flags.
		 */
		@Deprecated
		public int c_iflag;
		/**
		 * Output mode flags.
		 */
		@Deprecated
		public int c_oflag;
		/**
		 * Control mode flags.
		 */
		@Deprecated
		public int c_cflag;
		/**
		 * Local mode flags.
		 */
		@Deprecated
		public int c_lflag;
		/**
		 * Line discipline.
		 */
		@Deprecated
		public byte c_line;
		/**
		 * Space for the remaining termios fields.
		 */
		@Deprecated
		public byte[] filler = new byte[64];

		@Deprecated
		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "filler");
		}

		@Deprecated
		TerminalFlags() {
		}

		@Deprecated
		TerminalFlags(final TerminalFlags t) {
			c_iflag = t.c_iflag;
			c_oflag = t.c_oflag;
			c_cflag = t.c_cflag;
			c_lflag = t.c_lflag;
			c_line = t.c_line;
			filler = t.filler.clone();
		}
	}

	private static class LibcDefinitions {
		static final int ISIG = 0000001;
		static final int ICANON = 0000002;
		static final int ECHO = 0000010;
		static final int ECHONL = 0000100;
		static final int TCSANOW = 0;
	}

	private interface Libc extends Library {
		int tcgetattr(int fd, TerminalFlags termios) throws LastErrorException;
		int tcsetattr(int fd, int opt, TerminalFlags termios) throws LastErrorException;
		int isatty(int fd);
	}
}
