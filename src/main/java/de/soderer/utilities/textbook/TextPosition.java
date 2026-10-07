package de.soderer.utilities.textbook;

/**
 * Position in a text of a {@link TextBookEditor}.
 */
public class TextPosition {
	private final long line;
	private final int column;

	/**
	 * Creates a position.
	 *
	 * @param line 1-based line number
	 * @param column 0-based column within the line
	 */
	public TextPosition(final long line, final int column) {
		this.line = line;
		this.column = column;
	}

	/**
	 * Returns the line of this position.
	 *
	 * @return 1-based line number
	 */
	public long getLine() {
		return line;
	}

	/**
	 * Returns the column of this position.
	 *
	 * @return 0-based column within the line
	 */
	public int getColumn() {
		return column;
	}
}
