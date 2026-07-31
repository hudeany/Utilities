package de.soderer.utilities.console;

public class Position {
	private final int line;
	private final int column;

	public Position(final int line, final int column) {
		super();
		this.line = line;
		this.column = column;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return "line: " + line + ", column: " + column;
	}
}
