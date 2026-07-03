package de.soderer.utilities.textbook;

public class TextPosition {
	private final long line;
	private final int column;

	public TextPosition(final long line, final int column) {
		this.line = line;
		this.column = column;
	}

	public long getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}
}
