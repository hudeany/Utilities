package de.soderer.utilities.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import de.soderer.utilities.DbUtilities;
import de.soderer.utilities.DbUtilities.DbVendor;

public class SqlStatementFormatter {
	/**
	 * Oracle misses "dual" as preserved word
	 * MySQL and MariaDB miss "max" and "min" as preserved words
	 */
	public static final List<String> ADDITIONALLY_RESERVED_WORDS_FOR_FORMATING = Arrays.asList(new String[] { "dual", "max", "min", "count" });

	private enum IndentationType {
		Normal,
		Select,
		With
	}

	private DbVendor dbVendor;
	private String indentation;
	private String lineBreak;

	public SqlStatementFormatter() {
		this(DbVendor.Oracle, "\t", "\n");
	}

	public SqlStatementFormatter(final DbVendor dbVendor, final String indentation, final String lineBreak) {
		if (dbVendor == null) {
			this.dbVendor = DbVendor.Oracle;
		} else {
			this.dbVendor = dbVendor;
		}
		this.indentation = indentation;
		this.lineBreak = lineBreak;
	}

	public SqlStatementFormatter setDbVendor(final DbVendor dbVendor) {
		if (dbVendor == null) {
			throw new IllegalArgumentException("Invalid empty DbVendor");
		}
		this.dbVendor = dbVendor;
		return this;
	}

	public SqlStatementFormatter setIndentation(final String indentation) {
		this.indentation = indentation;
		return this;
	}

	public SqlStatementFormatter setLineBreak(final String lineBreak) {
		this.lineBreak = lineBreak;
		return this;
	}

	public String format(String selectStatement) {
		if (isBlank(selectStatement)) {
			return null;
		} else {
			selectStatement = selectStatement.replace("\r\n", "\n").replace("\r", "\n").trim();
			if (selectStatement.endsWith(";")) {
				selectStatement = selectStatement.substring(0, selectStatement.length() - 1).trim();
			}
			final List<String> lines = splitLines(selectStatement);
			formatAndJoinReservedWords(lines, dbVendor);
			joinAliasPrefixes(lines);
			joinTableAliases(lines);
			joinTrailingCommas(lines);
			joinOperators(lines);
			joinFunctionBrakets(lines);
			joinSpecialCombinations(lines);
			if (isEmpty(indentation) && isEmpty(lineBreak)) {
				return joinLines(lines);
			} else {
				return indentAndJoinLines(lines, indentation, lineBreak);
			}
		}
	}
	private static boolean isEmpty(final String value) {
		return value == null || value.length() == 0;
	}

	private static boolean isBlank(final String value) {
		if (value == null || value.length() == 0) {
			return true;
		} else {
			for (int i = 0; i < value.length(); i++) {
				if (!Character.isWhitespace(value.charAt(i))) {
					return false;
				}
			}
			return true;
		}
	}

	private static List<String> splitLines(final String selectStatement) {
		if (isBlank(selectStatement)) {
			return null;
		} else {
			final List<String> lines = new ArrayList<>();
			StringBuilder nextLine = new StringBuilder();
			Character textStarter = null;
			final char[] data = selectStatement.toCharArray();
			for (int i = 0; i < data.length; i++) {
				final char nextChar = data[i];
				if (textStarter != null) {
					// within quoted texts
					if (textStarter == '"' && nextChar == '"') {
						// no double quotes in double quoted identifiers allowed
						nextLine.append(nextChar);
						lines.add(nextLine.toString());
						nextLine = new StringBuilder();
						textStarter = null;
					} else if (textStarter == '\'' && nextChar == '\'') {
						if (i + 1 < data.length && data[i + 1] == '\'') {
							// double-singlequote is the escape sequence for single quotes within single quoted texts
							i = i + 1;
							nextLine.append('\'').append('\'');
						} else {
							nextLine.append(nextChar);
							lines.add(nextLine.toString());
							nextLine = new StringBuilder();
							textStarter = null;
						}
					} else if ((textStarter == '´' || textStarter == '`') && (nextChar == '´' || nextChar == '`')) {
						nextLine.append(nextChar);
						lines.add(nextLine.toString());
						nextLine = new StringBuilder();
						textStarter = null;
					} else {
						nextLine.append(nextChar);
					}
				} else if (nextChar == '"' || nextChar == '\'' ||nextChar == '´' || nextChar == '`') {
					textStarter = nextChar;
					nextLine.append(nextChar);
				} else if (nextChar == '+' || nextChar == '-' || nextChar == '/' || nextChar == '*' || nextChar == '%' || nextChar == '=' || nextChar == '&' || nextChar == '|' || nextChar == '^' || nextChar == '<' || nextChar == '>') {
					if (i + 2 < data.length && DbUtilities.SQL_OPERATORS.contains(new StringBuilder(data[i]).append(data[i + 1]).append(data[i + 2]).toString())) {
						if (nextLine.length() > 0) {
							lines.add(nextLine.toString());
							nextLine = new StringBuilder();
						}
						lines.add(new StringBuilder().append(data[i]).append(data[i + 1]).append(data[i + 2]).toString());
						i = i + 2;
					} else if (i + 1 < data.length && DbUtilities.SQL_OPERATORS.contains(new StringBuilder(data[i]).append(data[i + 1]).toString())) {
						if (nextLine.length() > 0) {
							lines.add(nextLine.toString());
							nextLine = new StringBuilder();
						}
						lines.add(new StringBuilder().append(data[i]).append(data[i + 1]).toString());
						i = i + 1;
					} else if (DbUtilities.SQL_OPERATORS.contains(Character.toString(data[i]))) {
						if (nextLine.length() > 0) {
							lines.add(nextLine.toString());
							nextLine = new StringBuilder();
						}
						lines.add(Character.toString(data[i]));
					}
				} else if (nextChar == '(' || nextChar == ')' || nextChar == ',' || nextChar == '.') {
					if (nextLine.length() > 0) {
						lines.add(nextLine.toString());
						nextLine = new StringBuilder();
					}
					lines.add(Character.toString(nextChar));
				} else if (nextChar == ' ' || nextChar == '\n' || nextChar == '\t') {
					if (nextLine.length() > 0) {
						lines.add(nextLine.toString());
						nextLine = new StringBuilder();
					}
				} else {
					nextLine.append(nextChar);
				}
			}
			if (nextLine.length() > 0) {
				lines.add(nextLine.toString());
			}
			return lines;
		}
	}

	private static void formatAndJoinReservedWords(final List<String> lines, final DbVendor dbVendor) {
		final List<String> reservedWords;
		if (dbVendor == DbVendor.MySQL || dbVendor == DbVendor.MariaDB) {
			reservedWords = DbUtilities.RESERVED_WORDS_MYSSQL_MARIADB;
		} else {
			reservedWords = DbUtilities.RESERVED_WORDS_ORACLE;
		}
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if (reservedWords.contains(line.toLowerCase()) || ADDITIONALLY_RESERVED_WORDS_FOR_FORMATING.contains(line.toLowerCase())) {
				lines.set(i, line.toUpperCase());
			}
		}
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if ("BY".equalsIgnoreCase(line) && i > 0 && isOneOf(lines.get(i - 1), "GROUP", "SORT", "ORDER")) {
				i = joinWithPreviousLine(lines, i, " ");
			} else if ("AS".equalsIgnoreCase(line) && i > 0 && (i + 1) < lines.size()) {
				i = joinWithEnclosingLines(lines, i, " ", " ");
			} else if ("JOIN".equalsIgnoreCase(line)) {
				if (i > 0 && "OUTER".equals(lines.get(i - 1))) {
					i = joinWithPreviousLine(lines, i, " ");
				}
				if (i > 0 && isOneOf(lines.get(i - 1), "FULL", "RIGHT", "LEFT", "INNER")) {
					i = joinWithPreviousLine(lines, i, " ");
				}
				while ((i + 1) < lines.size() && !"ON".equals(lines.get(i + 1))) {
					joinWithNextLine(lines, i, " ");
				}
				if ((i + 1) < lines.size() && "ON".equals(lines.get(i + 1))) {
					joinWithNextLine(lines, i, " ");
				}
			}
		}
	}

	private static void joinAliasPrefixes(final List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if (".".equals(line) && i > 0 && (i + 1) < lines.size()) {
				i = joinWithEnclosingLines(lines, i, "", "");
			}
		}
	}

	private static void joinTableAliases(final List<String> lines) {
		boolean isWithinFromClause = false;
		boolean isWithinJoinClause = false;
		boolean isWithinUpdateClause = false;
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if ("FROM".equals(line)) {
				isWithinFromClause = true;
				isWithinJoinClause = false;
				isWithinUpdateClause = false;
			} else if (isWithinFromClause && (isOneOf(line, "UNION", "WHERE", "GROUP BY", "SORT BY", "ORDER BY", "HAVING", ")") || startsWithOneOf(line, "JOIN", "INNER JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN", "RIGHT OUTER JOIN", "FULL JOIN", "FULL OUTER JOIN"))) {
				isWithinFromClause = false;
			} else if (isOneOf(line, "JOIN", "INNER JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN", "RIGHT OUTER JOIN", "FULL JOIN", "FULL OUTER JOIN")) {
				isWithinFromClause = false;
				isWithinJoinClause = true;
				isWithinUpdateClause = false;
			} else if (isWithinJoinClause && "ON".equals(line)) {
				isWithinJoinClause = false;
			} else if (isOneOf(line, "UPDATE")) {
				isWithinFromClause = false;
				isWithinJoinClause = false;
				isWithinUpdateClause = true;
			} else if (isWithinUpdateClause && "SET".equals(line)) {
				isWithinUpdateClause = false;
			} else if (isWithinFromClause && (i + 1) < lines.size() && !isOneOf(lines.get(i + 1), "UNION", "WHERE", "GROUP BY", "SORT BY", "ORDER BY", "HAVING") && DbUtilities.isValidIdentifier(line) && i > 0 && DbUtilities.isValidAlias(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			} else if (isWithinJoinClause && (i + 1) < lines.size() && !"ON".equals(lines.get(i + 1)) && DbUtilities.isValidIdentifier(line) && i > 0 && DbUtilities.isValidAlias(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			} else if (isWithinUpdateClause && (i + 1) < lines.size() && !"SET".equals(lines.get(i + 1)) && DbUtilities.isValidIdentifier(line) && i > 0 && DbUtilities.isValidAlias(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			}
		}
	}

	private static void joinTrailingCommas(final List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if (",".equals(line) && i > 0) {
				i = joinWithPreviousLine(lines, i, "");
			}
		}
	}

	private static void joinOperators(final List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if ("*".equals(line) && i > 0 && ("SELECT".equals(lines.get(i - 1)) || "(".equals(lines.get(i - 1)))) {
				// do nothing for this is no operator here
			} else if (DbUtilities.SQL_OPERATORS.contains(line) && i > 0 && (i + 1) < lines.size()) {
				i = joinWithEnclosingLines(lines, i, " ", " ");
			}
		}
	}

	private static void joinFunctionBrakets(final List<String> lines) {
		boolean nextIsInsertTableDefinition = false;
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if ("INTO".equals(line)) {
				nextIsInsertTableDefinition = true;
			} else if ("(".equals(line) && i > 0 && DbUtilities.isValidIdentifier(lines.get(i - 1))) {
				if ("IN".equals(lines.get(i - 1))) {
					i = joinWithPreviousLine(lines, i, " ");
					if (i > 0) {
						i = joinWithPreviousLine(lines, i, " ");
					}
				} else if (isOneOf(lines.get(i - 1), "VALUES", "WHERE", "FROM", "GROUP BY", "SORT BY", "ORDER BY", "HAVING")) {
					// do nothing for this is no function
				} else if (nextIsInsertTableDefinition){
					nextIsInsertTableDefinition = false;
					i = joinWithPreviousLine(lines, i, " ");
				} else {
					i = joinWithPreviousLine(lines, i, "");
				}

				if (i + 1 < lines.size() && lines.get(i + 1).startsWith(")")) {
					joinWithNextLine(lines, i, "");
				} else if (i + 2 < lines.size() && !lines.get(i + 1).contains("(") && lines.get(i + 2).startsWith(")")) {
					lines.set(i, new StringBuilder(lines.get(i)).append(lines.get(i + 1)).append(lines.get(i + 2)).toString());
					lines.remove(i + 2);
					lines.remove(i + 1);
				} else if (i + 3 < lines.size() && !lines.get(i + 1).contains("(") && !lines.get(i + 2).contains("(") && lines.get(i + 3).startsWith(")")) {
					lines.set(i, new StringBuilder(lines.get(i)).append(lines.get(i + 1)).append(" ").append(lines.get(i + 2)).append(lines.get(i + 3)).toString());
					lines.remove(i + 3);
					lines.remove(i + 2);
					lines.remove(i + 1);
				} else if (i + 4 < lines.size() && !lines.get(i + 1).contains("(") && !lines.get(i + 2).contains("(") && !lines.get(i + 3).contains("(") && lines.get(i + 4).startsWith(")")) {
					lines.set(i, new StringBuilder(lines.get(i)).append(lines.get(i + 1)).append(" ").append(lines.get(i + 2)).append(" ").append(lines.get(i + 3)).append(lines.get(i + 4)).toString());
					lines.remove(i + 4);
					lines.remove(i + 3);
					lines.remove(i + 2);
					lines.remove(i + 1);
				}
			}
		}
	}

	private static void joinSpecialCombinations(final List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if ("DELETE".equals(line) && (i + 1) < lines.size() && "FROM".equals(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			} else if ("INSERT".equals(line) && (i + 1) < lines.size() && "INTO".equals(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			} else if ("UNION".equals(line) && (i + 1) < lines.size() && "ALL".equals(lines.get(i + 1))) {
				joinWithNextLine(lines, i, " ");
			}
		}
	}

	private static void joinWithNextLine(final List<String> lines, final int lineIndex, final String glue) {
		lines.set(lineIndex, new StringBuilder(lines.get(lineIndex)).append(glue).append(lines.get(lineIndex + 1)).toString());
		lines.remove(lineIndex + 1);
	}

	private static int joinWithPreviousLine(final List<String> lines, final int lineIndex, final String glue) {
		lines.set(lineIndex - 1, new StringBuilder(lines.get(lineIndex - 1)).append(glue).append(lines.get(lineIndex)).toString());
		lines.remove(lineIndex);
		return lineIndex - 1;
	}

	private static int joinWithEnclosingLines(final List<String> lines, final int lineIndex, final String prefixGlue, final String suffixGlue) {
		lines.set(lineIndex - 1, new StringBuilder(lines.get(lineIndex - 1)).append(prefixGlue).append(lines.get(lineIndex)).append(suffixGlue).append(lines.get(lineIndex + 1)).toString());
		lines.remove(lineIndex + 1);
		lines.remove(lineIndex);
		return lineIndex - 1;
	}

	private static boolean isOneOf(final String item, final String... searchItems) {
		for (final String searchItem : searchItems) {
			if (searchItems != null && searchItem.equals(item)) {
				return true;
			}
		}
		return false;
	}

	private static boolean startsWithOneOf(final String item, final String... searchItems) {
		for (final String searchItem : searchItems) {
			if (item != null && item.startsWith(searchItem)) {
				return true;
			}
		}
		return false;
	}

	private static String joinLines(final List<String> lines) {
		final StringBuilder builder = new StringBuilder();
		String previousLine = null;
		for (final String line : lines) {
			if (builder.length() > 0 && (previousLine == null || !previousLine.endsWith("(")) && !line.startsWith(")")) {
				builder.append(" ");
			}
			builder.append(line);

			previousLine = line;
		}
		return builder.toString();
	}

	private static String indentAndJoinLines(final List<String> lines, String indentation, String lineBreak) {
		if (isEmpty(indentation)) {
			indentation = "";
		}
		if (isEmpty(lineBreak)) {
			lineBreak = "";
		}

		final StringBuilder builder = new StringBuilder();
		final Stack<IndentationType> indentationLevel = new Stack<>();
		for (final String line : lines) {
			if (builder.length() > 0) {
				builder.append(lineBreak);
			}

			if (line.startsWith(")") || startsWithOneOf(line, "UNION", "VALUES", "SET", "WHERE", "FROM", "JOIN", "INNER JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN", "RIGHT OUTER JOIN", "FULL JOIN", "FULL OUTER JOIN", "GROUP BY", "SORT BY", "ORDER BY", "HAVING")) {
				if (indentationLevel.size() > 0) {
					indentationLevel.pop();
					if (line.startsWith(")") && indentationLevel.size() > 0 && indentationLevel.peek() == IndentationType.Select) {
						indentationLevel.pop();
					}
				}
			} else if (line.equals("SELECT") && indentationLevel.size() > 0 && indentationLevel.peek() == IndentationType.With) {
				indentationLevel.pop();
			}

			for (int i = 0; i < indentationLevel.size(); i++) {
				builder.append(indentation);
			}
			builder.append(line);

			if (line.endsWith("(") || startsWithOneOf(line, "VALUES", "UPDATE", "DELETE", "DELETE FROM", "INSERT INTO", "SET", "WHERE", "FROM", "JOIN", "INNER JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN", "RIGHT OUTER JOIN", "FULL JOIN", "FULL OUTER JOIN", "GROUP BY", "SORT BY", "ORDER BY", "HAVING")) {
				indentationLevel.push(IndentationType.Normal);
			} else if ("SELECT".equals(line)) {
				if (indentationLevel.size() > 0) {
					indentationLevel.pop();
					indentationLevel.push(IndentationType.Select);
					indentationLevel.push(IndentationType.Normal);
				} else {
					indentationLevel.push(IndentationType.Select);
				}
			} else if ("WITH".equals(line)) {
				indentationLevel.push(IndentationType.With);
			}
		}
		return builder.toString();
	}
}
