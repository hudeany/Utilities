package de.soderer.utilities;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Reader splitting an SQL script into single statements.
 * <p>
 * Statements are separated by semicolons. Semicolons within string literals (single quotes, a
 * doubled single quote is an escaped quote) and comments ("--" up to the end of the line and
 * "/* ... *&#47;") do not end a statement. Comments are removed from the statements, empty
 * statements are skipped. A line containing only "/" (Oracle's statement terminator) also ends a
 * statement.
 * </p>
 * <p>
 * Not supported: backslash escapes within string literals (like MySQL's 'It\'s'), quoted
 * identifiers containing semicolons or comment markers, dollar quoted strings (PostgreSQL) and
 * semicolons within PL/SQL blocks.
 * </p>
 */
public class SqlScriptReader extends BasicReader {
	/**
	 * Creates a new reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read the script from
	 * @throws Exception
	 *             if the input stream is null
	 */
	public SqlScriptReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
	}

	/**
	 * Creates a new reader using the given encoding.
	 *
	 * @param inputStream
	 *            the stream to read the script from
	 * @param encodingCharset
	 *            the encoding of the script, or null for UTF-8
	 * @throws Exception
	 *             if the input stream is null
	 */
	public SqlScriptReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
	}

	/**
	 * Reads the next statement of the script.
	 *
	 * @return the next statement without terminating semicolon and without leading and trailing
	 *         whitespace, or null if there are no more statements
	 * @throws Exception
	 *             if a string literal or a multi line comment is not closed, or reading fails
	 */
	public String readNextStatement() throws Exception {
		// Loop instead of recursion for skipped empty statements, so many empty statements cannot cause a StackOverflowError
		while (true) {
			final StringBuilder nextStatement = new StringBuilder();
			final boolean endOfData = readStatementText(nextStatement);
			final String statement = nextStatement.toString().trim();
			if (statement.length() > 0 && !"/".equals(statement)) {
				return statement;
			} else if (endOfData) {
				return null;
			}
			// else: skip empty statement
		}
	}

	/**
	 * Reads the text of the next statement up to its terminator.
	 *
	 * @param nextStatement
	 *            receives the statement text without comments and terminator
	 * @return true, if the end of data was reached
	 */
	private boolean readStatementText(final StringBuilder nextStatement) throws Exception {
		boolean withinString = false;
		boolean withinSingleLineComment = false;
		boolean withinMultiLineComment = false;
		// True as long as the current line contains only whitespace (and comments) so far
		boolean onlyWhitespaceInLine = true;

		Character nextCharacter;
		while ((nextCharacter = readNextCharacter()) != null) {
			if (withinString) {
				if (nextCharacter == '\'') {
					withinString = false;
				}
				nextStatement.append(nextCharacter);
			} else if (withinSingleLineComment) {
				if (nextCharacter == '\n' || nextCharacter == '\r') {
					withinSingleLineComment = false;
					// The comment separates tokens like whitespace, e.g. "SELECT 1--c\nFROM t"
					appendSeparator(nextStatement);
					onlyWhitespaceInLine = true;
				}
			} else if (withinMultiLineComment) {
				if (nextCharacter == '*') {
					nextCharacter = readNextCharacter();
					if (nextCharacter != null && nextCharacter == '/') {
						withinMultiLineComment = false;
						// The comment separates tokens like whitespace, e.g. "SELECT/*c*/1"
						appendSeparator(nextStatement);
					} else {
						reuseCurrentChar();
					}
				}
			} else if (nextCharacter == '\'') {
				withinString = true;
				onlyWhitespaceInLine = false;
				nextStatement.append(nextCharacter);
			} else if (nextCharacter == '-') {
				nextCharacter = readNextCharacter();
				if (nextCharacter != null && nextCharacter == '-') {
					withinSingleLineComment = true;
				} else {
					reuseCurrentChar();
					onlyWhitespaceInLine = false;
					nextStatement.append('-');
				}
			} else if (nextCharacter == '/') {
				final Character followingCharacter = peek();
				if (followingCharacter != null && followingCharacter == '*') {
					readNextCharacter();
					withinMultiLineComment = true;
				} else if (onlyWhitespaceInLine) {
					// Oracle's "/" terminator line, which is not necessarily preceded by a semicolon
					final StringBuilder blanksAfterSlash = new StringBuilder();
					Character lineCharacter = peek();
					while (lineCharacter != null && (lineCharacter == ' ' || lineCharacter == '\t')) {
						blanksAfterSlash.append(readNextCharacter());
						lineCharacter = peek();
					}
					if (lineCharacter == null || lineCharacter == '\n' || lineCharacter == '\r') {
						return lineCharacter == null;
					} else {
						// Something else follows on the same line, so the slash is a normal character (e.g. a division)
						nextStatement.append('/').append(blanksAfterSlash);
						onlyWhitespaceInLine = false;
					}
				} else {
					nextStatement.append('/');
				}
			} else if (nextCharacter == ';') {
				return false;
			} else {
				nextStatement.append(nextCharacter);
				if (nextCharacter == '\n' || nextCharacter == '\r') {
					onlyWhitespaceInLine = true;
				} else if (!Character.isWhitespace(nextCharacter)) {
					onlyWhitespaceInLine = false;
				}
			}
		}

		if (withinString) {
			throw new Exception("Unclosed sql string");
		} else if (withinMultiLineComment) {
			throw new Exception("Unclosed multiline comment");
		}
		return true;
	}

	/**
	 * Appends a blank as token separator, unless the statement is empty or already ends with
	 * whitespace.
	 *
	 * @param statement
	 *            the statement read so far
	 */
	private static void appendSeparator(final StringBuilder statement) {
		if (statement.length() > 0 && !Character.isWhitespace(statement.charAt(statement.length() - 1))) {
			statement.append(' ');
		}
	}
}
