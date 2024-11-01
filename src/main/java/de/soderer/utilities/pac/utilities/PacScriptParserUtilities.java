package de.soderer.utilities.pac.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import javax.net.ssl.HttpsURLConnection;

public class PacScriptParserUtilities {
	public static String readPacData(final URL pacUrl) {
		try {
			final HttpsURLConnection pacConnection = (HttpsURLConnection) pacUrl.openConnection();
			pacConnection.connect();
			try (InputStream pacInputStream = (InputStream) pacConnection.getContent()) {
				final String pacData = new String(toByteArray(pacInputStream), StandardCharsets.UTF_8);
				return pacData.replace("\r\n", "\n").replace("\r", "\n");
			}
		} catch (final MalformedURLException e) {
			throw new RuntimeException("Invalid PAC URL: " + pacUrl.toString(), e);
		} catch (final IOException e) {
			throw new RuntimeException("Cannot read PAC data from URL: " + pacUrl.toString(), e);
		}
	}

	public static String removeComments(final String text) {
		final int outsideComment = 0;
		final int insideStringLiteral = 1;
		final int insideSinglelineComment = 2;
		final int insideMultilineComment = 3;

		int currentState = outsideComment;
		String result = "";
		try (Scanner textScanner = new Scanner(text).useDelimiter("")) {
			while (textScanner.hasNext()) {
				final String currentCharString = textScanner.next();
				final char currentChar = currentCharString.charAt(0);
				switch (currentState) {
					case outsideComment:
						if (currentChar == '\"') {
							currentState = insideStringLiteral;
							result += currentChar;
						} else if (currentChar == '/' && textScanner.hasNext()) {
							final String nextCharString = textScanner.next();
							final char nextChar = nextCharString.charAt(0);
							if (nextChar == '/') {
								currentState = insideSinglelineComment;
							} else if (nextChar == '*') {
								currentState = insideMultilineComment;
							} else
								result = result + currentChar + nextChar;
						} else {
							result += currentChar;
						}
						break;
					case insideStringLiteral:
						result += currentChar;
						if (currentChar == '\\' && textScanner.hasNext()) {
							final String nextChar = textScanner.next();
							result += nextChar;
						} else if (currentChar == '\"') {
							currentState = outsideComment;
						}
						break;
					case insideSinglelineComment:
						if (currentChar == '\n') {
							currentState = outsideComment;
							result += "\n";
						}
						break;
					case insideMultilineComment:
						if (currentChar == '*' && textScanner.hasNext()) {
							final String nextCharString = textScanner.next();
							final char nextChar = nextCharString.charAt(0);
							if (nextChar == '/') {
								currentState = outsideComment;
								break;
							}
						}
						break;
					default:
						throw new RuntimeException("Invalid state");
				}
			}
		}
		return result;
	}

	public static String getHostnameFromRequestString(String requestString) {
		if (requestString == null || !requestString.contains("/")) {
			return requestString;
		} else {
			if (requestString.toLowerCase().startsWith("http")) {
				requestString = requestString.substring(requestString.indexOf("//") + 2);

				if (!requestString.contains("/")) {
					return requestString;
				}
			}

			return requestString.substring(0, requestString.indexOf("/"));
		}
	}

	public static byte[] toByteArray(final InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		} else {
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
				copy(inputStream, byteArrayOutputStream);
				return byteArrayOutputStream.toByteArray();
			}
		}
	}

	private static final int EOF = -1;

	public static long copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
		final byte[] buffer = new byte[4096];
		int lengthRead;
		long bytesCopied = 0;
		while ((lengthRead = inputStream.read(buffer)) != EOF) {
			outputStream.write(buffer, 0, lengthRead);
			bytesCopied += lengthRead;
		}
		outputStream.flush();
		return bytesCopied;
	}

	public static List<String> tokenize(final String pacScriptString) {
		final int outsideStringLiteral = 0;
		final int insideStringLiteral = 1;
		int currentState = outsideStringLiteral;
		try (Scanner textScanner = new Scanner(pacScriptString).useDelimiter("")) {
			final List<String> tokens = new ArrayList<>();

			String nextToken = "";
			Character previousCharacter = null;
			while (textScanner.hasNext()) {
				final String currentCharString = textScanner.next();
				final Character currentCharacter = currentCharString.charAt(0);
				switch (currentState) {
					case outsideStringLiteral:
						if (currentCharacter == '\"') {
							nextToken += currentCharString;
							currentState = insideStringLiteral;
						} else if (currentCharacter == '(' || currentCharacter == ')') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							tokens.add(currentCharString);
							nextToken = "";
						} else if (currentCharacter == '[' || currentCharacter == ']') { // TODO
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							tokens.add(currentCharString);
							nextToken = "";
						} else if (currentCharacter == '{' || currentCharacter == '}') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							tokens.add(currentCharString);
							nextToken = "";
						} else if (currentCharacter == ' ' || currentCharacter == '\n' || currentCharacter == '\t') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
						} else if (currentCharacter == ',' || currentCharacter == ';'
								|| currentCharacter == '/' || currentCharacter == '*' || currentCharacter == '^'
								|| currentCharacter == '>' || currentCharacter == '<' || currentCharacter == '%'
								|| currentCharacter == '!') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							tokens.add(currentCharString);
						} else if (currentCharacter == '+' || currentCharacter == '-'
								|| currentCharacter == '&' || currentCharacter == '|') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							if (previousCharacter == currentCharacter) {
								tokens.set(tokens.size() - 1, currentCharString + currentCharString);
							} else {
								tokens.add(currentCharString);
							}
						} else if (currentCharacter == '=') {
							if (nextToken.length() > 0) {
								tokens.add(nextToken);
								nextToken = "";
							}
							if (previousCharacter != null && (
									'=' == previousCharacter
									|| '!' == previousCharacter
									|| '>' == previousCharacter
									|| '<' == previousCharacter
									|| '+' == previousCharacter
									|| '-' == previousCharacter
									|| '*' == previousCharacter
									|| '/' == previousCharacter)) {
								tokens.set(tokens.size() - 1, previousCharacter + "=");
							} else {
								tokens.add(currentCharString);
							}
						} else {
							nextToken += currentCharString;
						}
						break;
					case insideStringLiteral:
						if (currentCharacter == '\\' && textScanner.hasNext()) {
							final String nextCharString = textScanner.next();
							nextToken += currentCharString + nextCharString;
						} else if (currentCharacter == '\"') {
							nextToken += currentCharString;
							tokens.add(nextToken);
							nextToken = "";
							currentState = outsideStringLiteral;
						} else {
							nextToken += currentCharString;
						}
						break;
					default:
						throw new RuntimeException("Invalid state");
				}
				previousCharacter = currentCharacter;
			}
			if (nextToken.trim().length() > 0) {
				tokens.add(nextToken);
			}
			return tokens;
		}
	}

	public static int findClosingBracketToken(final List<String> tokens, final int startIndex) {
		final Stack<Character> openBrackets = new Stack<>();
		String currentToken = tokens.get(startIndex);
		if (!(currentToken.equals("(") || currentToken.equals("[") || currentToken.equals("{"))) {
			throw new RuntimeException("Not a starting bracket at token index: " + startIndex);
		} else {
			openBrackets.push(currentToken.charAt(0));
		}
		for (int tokenIndex = startIndex + 1; tokenIndex < tokens.size(); tokenIndex++) {
			currentToken = tokens.get(tokenIndex);
			final Character firstTokenCharacter = currentToken.charAt(0);
			if (firstTokenCharacter == '(') {
				openBrackets.push(firstTokenCharacter);
			} else if (firstTokenCharacter == '[') {
				openBrackets.push(firstTokenCharacter);
			} else if (firstTokenCharacter == '{') {
				openBrackets.push(firstTokenCharacter);
			} else if (firstTokenCharacter == ')') {
				if ('(' != openBrackets.pop()) {
					throw new RuntimeException("Unbalanced closing brackets at token index: " + tokenIndex);
				} else if (openBrackets.size() == 0) {
					return tokenIndex;
				}
			} else if (firstTokenCharacter == ']') {
				if ('[' != openBrackets.pop()) {
					throw new RuntimeException("Unbalanced closing brackets at token index: " + tokenIndex);
				} else if (openBrackets.size() == 0) {
					return tokenIndex;
				}
			} else if (firstTokenCharacter == '}') {
				if ('{' != openBrackets.pop()) {
					throw new RuntimeException("Unbalanced closing brackets at token index: " + tokenIndex);
				} else if (openBrackets.size() == 0) {
					return tokenIndex;
				}
			}
		}
		throw new RuntimeException("Unbalanced brackets starting at index: " + startIndex);
	}

	public static List<Statement> parseCodeBlockTokens(final List<String> codeBlockTokens) {
		final List<Statement> statements = new ArrayList<>();
		for (int tokenIndex = 0; tokenIndex < codeBlockTokens.size(); tokenIndex++) {
			String nextToken = codeBlockTokens.get(tokenIndex);
			if (";".equals(nextToken)) {
				// empty statement: do nothing
			} else if ("if".equals(nextToken)) {
				tokenIndex++;
				nextToken = codeBlockTokens.get(tokenIndex);
				if (!"(".equals(nextToken)) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}
				final int conditionStart = tokenIndex;
				final int conditionEnd = PacScriptParserUtilities.findClosingBracketToken(codeBlockTokens, conditionStart);

				tokenIndex = conditionEnd + 1;
				nextToken = codeBlockTokens.get(tokenIndex);

				List<String> ifCodeBlockTokens;
				if ("{".equals(nextToken)) {
					final int ifCodeBlockStart = tokenIndex;
					final int ifCodeBlockEnd = PacScriptParserUtilities.findClosingBracketToken(codeBlockTokens, ifCodeBlockStart);
					ifCodeBlockTokens = codeBlockTokens.subList(ifCodeBlockStart + 1, ifCodeBlockEnd);
					tokenIndex = ifCodeBlockEnd + 1;
				} else {
					final int ifCodeBlockStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					} else {
						tokenIndex++;
						final int ifCodeBlockEnd = tokenIndex;
						ifCodeBlockTokens = codeBlockTokens.subList(ifCodeBlockStart, ifCodeBlockEnd);
						tokenIndex = ifCodeBlockEnd;
					}
				}

				Condition parentCondition = new Condition(codeBlockTokens.subList(conditionStart + 1, conditionEnd), ifCodeBlockTokens, null);
				statements.add(parentCondition);

				boolean foundElseIf = true;
				while (foundElseIf && tokenIndex < codeBlockTokens.size()) {
					foundElseIf = false;

					nextToken = codeBlockTokens.get(tokenIndex);
					if ("else".equals(nextToken)) {
						tokenIndex++;
						nextToken = codeBlockTokens.get(tokenIndex);
						List<String> endCodeBlockTokens;
						if ("{".equals(nextToken)) {
							final int elseCodeBlockStart = tokenIndex;
							final int elseCodeBlockEnd = PacScriptParserUtilities.findClosingBracketToken(codeBlockTokens, elseCodeBlockStart);
							endCodeBlockTokens = codeBlockTokens.subList(elseCodeBlockStart + 1, elseCodeBlockEnd);
							parentCondition.setElseCodeBlockTokens(endCodeBlockTokens);
							tokenIndex = elseCodeBlockEnd + 1;
						} else if ("if".equals(nextToken)) {
							foundElseIf = true;
							tokenIndex++;
							nextToken = codeBlockTokens.get(tokenIndex);
							if (!"(".equals(nextToken)) {
								throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
							}
							final int subConditionStart = tokenIndex;
							final int subConditionEnd = PacScriptParserUtilities.findClosingBracketToken(codeBlockTokens, subConditionStart);

							tokenIndex = subConditionEnd + 1;
							nextToken = codeBlockTokens.get(tokenIndex);

							List<String> subIfCodeBlockTokens;
							if ("{".equals(nextToken)) {
								final int ifCodeBlockStart = tokenIndex;
								final int ifCodeBlockEnd = PacScriptParserUtilities.findClosingBracketToken(codeBlockTokens, ifCodeBlockStart);
								subIfCodeBlockTokens = codeBlockTokens.subList(ifCodeBlockStart + 1, ifCodeBlockEnd);
								tokenIndex = ifCodeBlockEnd + 1;
							} else {
								final int ifCodeBlockStart = tokenIndex;
								while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
									tokenIndex++;
								}
								if (!";".equals(codeBlockTokens.get(tokenIndex))) {
									throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
								} else {
									tokenIndex++;
									final int ifCodeBlockEnd = tokenIndex;
									subIfCodeBlockTokens = codeBlockTokens.subList(ifCodeBlockStart, ifCodeBlockEnd);
									tokenIndex = ifCodeBlockEnd;
								}
							}

							final Condition subCondition = new Condition(codeBlockTokens.subList(subConditionStart + 1, subConditionEnd), subIfCodeBlockTokens, null);
							parentCondition.setElseCodeBlockStatements(Collections.singletonList(subCondition));
							parentCondition = subCondition;
						} else {
							final int elseCodeBlockStart = tokenIndex;
							while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
								tokenIndex++;
							}
							if (!";".equals(codeBlockTokens.get(tokenIndex))) {
								throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
							} else {
								tokenIndex++;
								final int elseCodeBlockEnd = tokenIndex;
								endCodeBlockTokens = codeBlockTokens.subList(elseCodeBlockStart, elseCodeBlockEnd);
								parentCondition.setElseCodeBlockTokens(endCodeBlockTokens);
								tokenIndex = elseCodeBlockEnd;
							}
						}
					} else {
						parentCondition.setElseCodeBlockTokens(null);
						tokenIndex--;
					}
				}
			} else if ("return".equals(nextToken)) {
				tokenIndex++;
				final int expressionStart = tokenIndex;
				while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
					tokenIndex++;
				}
				if (!";".equals(codeBlockTokens.get(tokenIndex))) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}

				final int expressionEnd = tokenIndex;
				statements.add(new Result(codeBlockTokens.subList(expressionStart, expressionEnd)));
			} else if ("++".equals(nextToken)) {
				if (codeBlockTokens.size() > tokenIndex + 2) {
					tokenIndex++;
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					statements.add(new Assignment(false, variableName, Arrays.asList(variableName, "+", "1")));
				} else {
					throw new RuntimeException("Unsupported code found: " + nextToken);
				}
			} else if ("--".equals(nextToken)) {
				if (codeBlockTokens.size() > tokenIndex + 2) {
					tokenIndex++;
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					statements.add(new Assignment(false, variableName, Arrays.asList(variableName, "-", "1")));
				} else {
					throw new RuntimeException("Unsupported code found: " + nextToken);
				}
			} else if ("var".equals(nextToken)) {
				tokenIndex++;
				final String variableName = codeBlockTokens.get(tokenIndex);

				tokenIndex++;
				nextToken = codeBlockTokens.get(tokenIndex);

				if (!"=".equals(nextToken)) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}

				tokenIndex++;
				final int assignmentStart = tokenIndex;
				while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
					tokenIndex++;
				}
				if (!";".equals(codeBlockTokens.get(tokenIndex))) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}

				statements.add(new Assignment(false, variableName, codeBlockTokens.subList(assignmentStart, tokenIndex)));
			} else if ("let".equals(nextToken)) {
				tokenIndex++;
				final String variableName = codeBlockTokens.get(tokenIndex);

				tokenIndex++;
				nextToken = codeBlockTokens.get(tokenIndex);

				if (!"=".equals(nextToken)) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}

				tokenIndex++;
				final int assignmentStart = tokenIndex;
				while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
					tokenIndex++;
				}
				if (!";".equals(codeBlockTokens.get(tokenIndex))) {
					throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
				}

				statements.add(new Assignment(true, variableName, codeBlockTokens.subList(assignmentStart, tokenIndex)));
			} else if ("[".equals(nextToken)) { // TODO
				final int arrayIndexEnd = findClosingBracketToken(codeBlockTokens, tokenIndex);
			} else {
				if (codeBlockTokens.size() > tokenIndex + 2 && "=".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					final int assignmentStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					statements.add(new Assignment(false, variableName, codeBlockTokens.subList(assignmentStart, tokenIndex)));
				} else if (codeBlockTokens.size() > tokenIndex + 2 && "+=".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					final int assignmentStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					final List<String> assignmentExpressionTokens = new ArrayList<>(codeBlockTokens.subList(assignmentStart, tokenIndex));
					assignmentExpressionTokens.add(0, variableName);
					assignmentExpressionTokens.add(1, "+");
					statements.add(new Assignment(false, variableName, assignmentExpressionTokens));
				} else if (codeBlockTokens.size() > tokenIndex + 2 && "-=".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					final int assignmentStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					final List<String> assignmentExpressionTokens = new ArrayList<>(codeBlockTokens.subList(assignmentStart, tokenIndex));
					assignmentExpressionTokens.add(0, variableName);
					assignmentExpressionTokens.add(1, "-");
					statements.add(new Assignment(false, variableName, assignmentExpressionTokens));
				} else if (codeBlockTokens.size() > tokenIndex + 2 && "*=".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					final int assignmentStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					final List<String> assignmentExpressionTokens = new ArrayList<>(codeBlockTokens.subList(assignmentStart, tokenIndex));
					assignmentExpressionTokens.add(0, variableName);
					assignmentExpressionTokens.add(1, "*");
					statements.add(new Assignment(false, variableName, assignmentExpressionTokens));
				} else if (codeBlockTokens.size() > tokenIndex + 2 && "/=".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					final int assignmentStart = tokenIndex;
					while (tokenIndex < codeBlockTokens.size() && !";".equals(codeBlockTokens.get(tokenIndex))) {
						tokenIndex++;
					}
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					final List<String> assignmentExpressionTokens = new ArrayList<>(codeBlockTokens.subList(assignmentStart, tokenIndex));
					assignmentExpressionTokens.add(0, variableName);
					assignmentExpressionTokens.add(1, "/");
					statements.add(new Assignment(false, variableName, assignmentExpressionTokens));
				} else if (codeBlockTokens.size() > tokenIndex + 1 && "++".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					statements.add(new Assignment(false, variableName, Arrays.asList(variableName, "+", "1")));
				} else if (codeBlockTokens.size() > tokenIndex + 1 && "--".equals(codeBlockTokens.get(tokenIndex + 1))) {
					final String variableName = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					nextToken = codeBlockTokens.get(tokenIndex);

					tokenIndex++;
					if (!";".equals(codeBlockTokens.get(tokenIndex))) {
						throw new RuntimeException("Unexpected code at token index " + tokenIndex + ": " + nextToken);
					}

					statements.add(new Assignment(false, variableName, Arrays.asList(variableName, "-", "1")));
				} else {
					throw new RuntimeException("Unsupported code found: " + nextToken);
				}
			}
		}
		return statements;
	}

	public static String join(final String[] dataArray, final String separator) {
		String result = "";
		for (int i = 0; i < dataArray.length; i++) {
			if (i > 0) {
				result += separator;
			}
			result += dataArray[i];
		}
		return result;
	}

	public static String join(final List<String> dataList, final String separator) {
		String result = "";
		for (int i = 0; i < dataList.size(); i++) {
			if (i > 0) {
				result += separator;
			}
			result += dataList.get(i);
		}
		return result;
	}

	public static int indexOfOperatorOutsideOfBrackets(final List<String> tokens, final List<String> operators) {
		int openBrackets = 0;
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			final String token = tokens.get(tokenIndex);
			if ("(".equals(token)) {
				openBrackets++;
			} else if (")".equals(token)) {
				openBrackets--;
			} else if ("[".equals(token)) {
				openBrackets++;
			} else if ("]".equals(token)) {
				openBrackets--;
			} else if (openBrackets == 0 && operators.contains(token)) {
				return tokenIndex;
			}
		}
		return -1;
	}

	public static String indentLines(final String text) {
		return "\t" + text.replace("\n", "\n" + "\t");
	}

	public static List<String> replaceAliases(final List<String> tokens) {
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			if ("not".equals(tokens.get(tokenIndex))) {
				tokens.set(tokenIndex, "!");
			}
		}
		return tokens;
	}
}
