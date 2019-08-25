package de.soderer.utilities.textbook;

public class TextPosition {
	private long line;
	private int column;
	
	public TextPosition(long line, int column) {
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
