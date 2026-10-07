package de.soderer.utilities;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Reader splitting an SQL script into single statements.
 * <p>
 * Statements are separated by semicolons. Semicolons within string literals (single quotes, a
 * doubled single quote is an escaped quote) and comments ("--" up to the end of the line and
 * "/* ... *&#47;") do not end a statement. Comments are removed from the statements, empty
 * statements and Oracle's "/" statement terminator lines are skipped.
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
		final StringBuilder nextStatement = new StringBuilder();
		boolean withinString = false;
		boolean withinSingleLineComment = false;
		boolean withinMultiLineComment = false;

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
				nextStatement.append(nextCharacter);
			} else if (nextCharacter == '-') {
				nextCharacter = readNextCharacter();
				if (nextCharacter != null && nextCharacter == '-') {
					withinSingleLineComment = true;
				} else {
					reuseCurrentChar();
					nextStatement.append('-');
				}
			} else if (nextCharacter == '/') {
				nextCharacter = readNextCharacter();
				if (nextCharacter != null && nextCharacter == '*') {
					withinMultiLineComment = true;
				} else {
					reuseCurrentChar();
					nextStatement.append('/');
				}
			} else if (nextCharacter == ';') {
				break;
			} else {
				nextStatement.append(nextCharacter);
			}
		}

		if (withinString) {
			throw new Exception("Unclosed sql string");
		} else if (withinMultiLineComment) {
			throw new Exception("Unclosed multiline comment");
		} else if ("/".equals(nextStatement.toString().trim())) {
			// Skip empty Oracle statement: "/" is a code sign for finalizing Oracle SQL scripts
			return readNextStatement();
		} else if (Utilities.isNotBlank(nextStatement.toString())) {
			return nextStatement.toString().trim();
		} else if (nextCharacter == null) {
			return null;
		} else {
			// Skip empty statement
			return readNextStatement();
		}
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
