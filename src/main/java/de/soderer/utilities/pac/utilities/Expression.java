package de.soderer.utilities.pac.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.soderer.utilities.pac.utilities.exception.BreakLoopException;
import de.soderer.utilities.pac.utilities.exception.ContinueLoopException;

public class Expression implements Statement {
	private final static List<String> twoParameterOperators = Arrays.asList(new String[] {
			"+", "-", "*", "/",
			"&", "|", "&&", "||",
			"==", "!=", ">", "<", ">=", "<="
	});

	private final static List<String> unaryOperators = Arrays.asList(new String[] {
			"++", "--", "!"
	});

	private List<String> expressionTokens;

	private Expression expression1;
	private String operator;
	private Expression expression2;

	public List<String> getExpressionTokens() {
		return expressionTokens;
	}

	public Expression(final List<String> expressionTokens) {
		if (expressionTokens == null || expressionTokens.size() == 0) {
			throw new RuntimeException("Unsupported empty expression");
		} else if ("(".equals(expressionTokens.get(0))) {
			final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, 0);
			expression1 = new Expression(expressionTokens.subList(1, bracketEnd));
			if (bracketEnd < expressionTokens.size() - 1) {
				operator = expressionTokens.get(bracketEnd + 1);
				expression2 = new Expression(expressionTokens.subList(bracketEnd + 2, expressionTokens.size()));
			}
		} else if ("[".equals(expressionTokens.get(0))) {
			final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, 0);
			this.expressionTokens = expressionTokens.subList(0, bracketEnd + 1);
			if (bracketEnd < expressionTokens.size() - 1) {
				throw new RuntimeException("Unsupported expression with operator for array found: " + expressionTokens.get(bracketEnd + 1));
			}
		} else {
			final int operatorIndex = PacScriptParserUtilities.indexOfOperatorOutsideOfBrackets(expressionTokens, twoParameterOperators);
			if (operatorIndex > -1) {
				expression1 = new Expression(expressionTokens.subList(0, operatorIndex));
				operator = expressionTokens.get(operatorIndex);
				expression2 = new Expression(expressionTokens.subList(operatorIndex + 1, expressionTokens.size()));
			} else {
				final int unaryOperatorIndex = PacScriptParserUtilities.indexOfOperatorOutsideOfBrackets(expressionTokens, unaryOperators);
				if (unaryOperatorIndex == -1) {
					this.expressionTokens = expressionTokens;
				} else if (unaryOperatorIndex == 0) {
					operator = expressionTokens.get(unaryOperatorIndex);
					expression2 = new Expression(expressionTokens.subList(1, expressionTokens.size()));
				} else if (unaryOperatorIndex == expressionTokens.size() - 1) {
					expression1 = new Expression(expressionTokens.subList(0, unaryOperatorIndex));
					operator = expressionTokens.get(unaryOperatorIndex);
				} else {
					throw new RuntimeException("Unsupported expression with unary operator found: " + expressionTokens.get(unaryOperatorIndex));
				}
			}
		}
	}

	@Override
	public Object execute(final Context context) {
		if (expressionTokens == null) {
			final Object result1 = expression1 == null ? null : expression1.execute(context);
			final Object result2 = expression2 == null ? null : expression2.execute(context);
			if (result1 == null) {
				if ("++".equals(operator) && result2 != null && result2 instanceof Integer) {
					final int value = ((Integer) result2) + 1;
					if (expression2.getExpressionTokens() != null && expression2.getExpressionTokens().size() == 1 && context.hasVariable(expression2.getExpressionTokens().get(0))) {
						context.setEnvironmentVariable(expression2.getExpressionTokens().get(0), value);
					}
					return value;
				} else if ("--".equals(operator) && result2 != null && result2 instanceof Integer) {
					final int value = ((Integer) result2) - 1;
					if (expression2.getExpressionTokens() != null && expression2.getExpressionTokens().size() == 1 && context.hasVariable(expression2.getExpressionTokens().get(0))) {
						context.setEnvironmentVariable(expression2.getExpressionTokens().get(0), value);
					}
					return value;
				} else if ("==".equals(operator) && result2 == null) {
					return true;
				} else if ("!".equals(operator) && result2 != null && result2 instanceof Boolean) {
					return !(Boolean) result2;
				} else if (operator != null) {
					throw new RuntimeException("Unsupported operator: " + operator);
				} else {
					throw new RuntimeException("Unsupported expression found");
				}
			} else if (result2 == null) {
				if ("++".equals(operator) && result1 instanceof Integer) {
					final int value = ((Integer) result1) + 1;
					if (expression1.getExpressionTokens() != null && expression1.getExpressionTokens().size() == 1 && context.hasVariable(expression1.getExpressionTokens().get(0))) {
						context.setEnvironmentVariable(expression1.getExpressionTokens().get(0), value);
					}
					return result1;
				} else if ("--".equals(operator) && result1 instanceof Integer) {
					final int value = ((Integer) result1) - 1;
					if (expression1.getExpressionTokens() != null && expression1.getExpressionTokens().size() == 1 && context.hasVariable(expression1.getExpressionTokens().get(0))) {
						context.setEnvironmentVariable(expression1.getExpressionTokens().get(0), value);
					}
					return result1;
				} else if (operator != null) {
					throw new RuntimeException("Unsupported operator: " + operator);
				} else {
					return result1;
				}
			} else {
				if ("==".equals(operator)) {
					if (result1 == result2) {
						return true;
					} else if (result1 instanceof String && result2 instanceof String) {
						return ((String) result1).equals(result2);
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) == (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) == (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) == (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("!=".equals(operator)) {
					if (result1 == result2) {
						return false;
					} else if (result1 instanceof String && result2 instanceof String) {
						return !((String) result1).equals(result2);
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) != (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) != (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) != (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if (">".equals(operator)) {
					if (result1 == result2) {
						return false;
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) > (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) > (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) > (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("<".equals(operator)) {
					if (result1 == result2) {
						return false;
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) < (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) < (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) < (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if (">=".equals(operator)) {
					if (result1 == result2) {
						return false;
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) >= (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) >= (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) >= (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("<=".equals(operator)) {
					if (result1 == result2) {
						return false;
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) <= (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) <= (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) <= (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("+".equals(operator)) {
					if (result1 instanceof String && result2 instanceof String) {
						return ((String) result1) + (String) result2;
					} else if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) + (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) + (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) + (Double) result2;
					} else if (result1 instanceof String) {
						return result1 + result2.toString();
					} else if (result2 instanceof String) {
						return result1.toString() + result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("-".equals(operator)) {
					if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) - (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) - (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) - (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("*".equals(operator)) {
					if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) * (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) * (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) * (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("/".equals(operator)) {
					if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) / (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) / (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) / (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("%".equals(operator)) {
					if (result1 instanceof Integer && result2 instanceof Integer) {
						return ((Integer) result1) % (Integer) result2;
					}  else if (result1 instanceof Float && result2 instanceof Float) {
						return ((Float) result1) % (Float) result2;
					}  else if (result1 instanceof Double && result2 instanceof Double) {
						return ((Double) result1) % (Double) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("&".equals(operator)) {
					if (result1 instanceof Boolean && result2 instanceof Boolean) {
						return ((Boolean) result1) & (Boolean) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("&&".equals(operator)) {
					if (result1 instanceof Boolean && result2 instanceof Boolean) {
						return ((Boolean) result1) && (Boolean) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("|".equals(operator)) {
					if (result1 instanceof Boolean && result2 instanceof Boolean) {
						return ((Boolean) result1) | (Boolean) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else if ("||".equals(operator)) {
					if (result1 instanceof Boolean && result2 instanceof Boolean) {
						return ((Boolean) result1) || (Boolean) result2;
					} else {
						throw new RuntimeException("Unsupported parameters for operator: " + operator);
					}
				} else {
					throw new RuntimeException("Unsupported operator: " + operator);
				}
			}
		} else {
			final String firstToken = expressionTokens.get(0);
			if ("break".equals(firstToken)) {
				throw new BreakLoopException();
			} else if ("continue".equals(firstToken)) {
				throw new ContinueLoopException();
			} else if (expressionTokens.size() > 1 && firstToken.equals("[") && expressionTokens.get(expressionTokens.size() - 1).equals("]")) {
				final List<Object> array = new ArrayList<>();
				final List<List<String>> arrayItemTokens = readArrayItems(expressionTokens.subList(1, expressionTokens.size() - 1));
				for (final List<String> itemTokens : arrayItemTokens) {
					final Object nextItem = new Expression(itemTokens).execute(context);
					array.add(nextItem);
				}
				return array;
			} else if (expressionTokens.size() > 1 && expressionTokens.get(1).equals("(")) {
				final List<Object> methodCallParameters = readMethodParameters(context, 1);

				if (context.hasDefinedMethod(firstToken)) {
					return context.getDefinedMethod(firstToken).executeMethod(context, methodCallParameters);
				} else if ("isPlainHostName".equals(firstToken)) {
					return PacScriptMethods.isPlainHostName((String) methodCallParameters.get(0));
				} else if ("dnsDomainIs".equals(firstToken)) {
					return PacScriptMethods.dnsDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("localHostOrDomainIs".equals(firstToken)) {
					return PacScriptMethods.localHostOrDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("isResolvable".equals(firstToken)) {
					return PacScriptMethods.isResolvable((String) methodCallParameters.get(0));
				} else if ("isInNet".equals(firstToken)) {
					return PacScriptMethods.isInNet((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
				} else if ("dnsResolve".equals(firstToken)) {
					return PacScriptMethods.dnsResolve((String) methodCallParameters.get(0));
				} else if ("myIpAddress".equals(firstToken)) {
					return PacScriptMethods.myIpAddress();
				} else if ("dnsDomainLevels".equals(firstToken)) {
					return PacScriptMethods.dnsDomainLevels((String) methodCallParameters.get(0));
				} else if ("shExpMatch".equals(firstToken)) {
					return PacScriptMethods.shExpMatch((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("weekdayRange".equals(firstToken)) {
					return PacScriptMethods.weekdayRange((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
				} else if ("dateRange".equals(firstToken)) {
					return PacScriptMethods.dateRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
				} else if ("timeRange".equals(firstToken)) {
					return PacScriptMethods.timeRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
				} else if ("isResolvableEx".equals(firstToken)) {
					return PacScriptMethods.isResolvableEx((String) methodCallParameters.get(0));
				} else if ("isInNetEx".equals(firstToken)) {
					return PacScriptMethods.isInNetEx((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("dnsResolveEx".equals(firstToken)) {
					return PacScriptMethods.dnsResolveEx((String) methodCallParameters.get(0));
				} else if ("myIpAddressEx".equals(firstToken)) {
					return PacScriptMethods.myIpAddressEx();
				} else if ("sortIpAddressList".equals(firstToken)) {
					return PacScriptMethods.sortIpAddressList((String) methodCallParameters.get(0));
				} else if ("getClientVersion".equals(firstToken)) {
					return PacScriptMethods.getClientVersion();
				} else {
					throw new RuntimeException("Call of undefined method: " + firstToken);
				}
			} else if (expressionTokens.size() > 1 && expressionTokens.get(1).equals("[")) {
				if (context.hasVariable(firstToken)) {
					final int arrayIndex = readArrayIndex(context, expressionTokens, 1);
					final Object arrayObject = context.getEnvironmentVariable(firstToken);
					if (arrayObject == null || !(arrayObject instanceof List)) {
						throw new RuntimeException("Invalid array reference: " + firstToken);
					} else {
						@SuppressWarnings("unchecked")
						final List<Object> array = (List<Object>) arrayObject;
						if (array.size() <= arrayIndex) {
							throw new RuntimeException("Invalid array index: " + arrayIndex);
						} else {
							return array.get(arrayIndex);
						}
					}
				} else {
					throw new RuntimeException("Array Index call for undefined variable: " + firstToken);
				}
			} else {
				if ("true".equals(firstToken)) {
					return true;
				} else if ("false".equals(firstToken)) {
					return false;
				} else if (context.hasVariable(firstToken)) {
					return context.getEnvironmentVariable(firstToken);
				} else if (firstToken.startsWith("\"") && firstToken.endsWith("\"")) {
					return firstToken.substring(1, firstToken.length() - 1);
				} else if (firstToken.endsWith(".length")) {
					final String arrayName = firstToken.substring(0, firstToken.length() - 7);
					if (context.hasVariable(arrayName)) {
						final Object arrayObject = context.getEnvironmentVariable(arrayName);
						if (arrayObject != null && arrayObject instanceof List<?>) {
							return ((List<?>) arrayObject).size();
						} else {
							throw new RuntimeException("Variable for length is not an array: " + firstToken);
						}
					} else {
						throw new RuntimeException("Undefined variable name in expression: " + firstToken);
					}
				} else {
					try {
						return Integer.parseInt(firstToken);
					} catch (@SuppressWarnings("unused") final NumberFormatException e1) {
						try {
							return Float.parseFloat(firstToken);
						} catch (@SuppressWarnings("unused") final NumberFormatException e2) {
							try {
								return Double.parseDouble(firstToken);
							} catch (@SuppressWarnings("unused") final NumberFormatException e3) {
								throw new RuntimeException("Unexpected code in expression: " + firstToken);
							}
						}
					}
				}
			}
		}
	}

	private List<Object> readMethodParameters(final Context context, int bracketStartTokenIndex) {
		final int parametersEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, bracketStartTokenIndex);
		bracketStartTokenIndex++;
		final List<Object> methodCallParameters = new ArrayList<>();
		while (bracketStartTokenIndex < parametersEnd) {
			final int nextParameterStart = bracketStartTokenIndex;
			int nextParameterEnd = bracketStartTokenIndex;
			String parameterToken = expressionTokens.get(nextParameterEnd);
			while (!(",".equals(parameterToken) || ")".equals(parameterToken))) {
				nextParameterEnd++;
				parameterToken = expressionTokens.get(nextParameterEnd);
			}
			final Object parameterValue = new Expression(expressionTokens.subList(nextParameterStart, nextParameterEnd)).execute(context);
			methodCallParameters.add(parameterValue);
			bracketStartTokenIndex = nextParameterEnd + 1;
		}
		return methodCallParameters;
	}

	private static int readArrayIndex(final Context context, final List<String> expressionTokens, final int bracketStartTokenIndex) {
		final int arrayIndexEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, bracketStartTokenIndex);
		if (arrayIndexEnd == -1) {
			throw new RuntimeException("Missing array index closing operator");
		}

		final Object arrayIndexObject = new Expression(expressionTokens.subList(bracketStartTokenIndex + 1, arrayIndexEnd)).execute(context);
		if (arrayIndexObject == null || !(arrayIndexObject instanceof Integer)) {
			throw new RuntimeException("Invalid array index value: " + arrayIndexObject);
		}

		final int arrayIndex = ((Integer) arrayIndexObject).intValue();
		if (arrayIndex < 0) {
			throw new RuntimeException("Invalid array index value: " + arrayIndexObject);
		}
		return arrayIndex;
	}

	private static List<List<String>> readArrayItems(final List<String> expressionTokens) {
		final List<List<String>> arrayItemTokens = new ArrayList<>();
		List<String> nextItemTokens = new ArrayList<>();
		for (final String nextToken : expressionTokens) {
			if (",".equals(nextToken)) {
				arrayItemTokens.add(nextItemTokens);
				nextItemTokens = new ArrayList<>();
			} else {
				nextItemTokens.add(nextToken);
			}
		}
		arrayItemTokens.add(nextItemTokens);
		return arrayItemTokens;
	}

	@Override
	public String toString() {
		if (expressionTokens != null && !expressionTokens.isEmpty()) {
			final String firstToken = expressionTokens.get(0);
			if ("break".equals(firstToken)) {
				return "break;";
			} else if ("continue".equals(firstToken)) {
				return "continue;";
			} else {
				String returnValue = "";
				for (final String expressionToken : expressionTokens) {
					if (returnValue.length() > 0) {
						if (!returnValue.endsWith("(") && !returnValue.endsWith("[") && !expressionToken.equals("(") && !expressionToken.equals(")") && !expressionToken.equals("]") && !expressionToken.equals(",")) {
							returnValue += " ";
						}
					}
					returnValue += expressionToken;
				}
				return returnValue;
			}
		} else {
			String returnValue = "";
			if (expression1 != null) {
				returnValue = expression1.toString();
			}
			if (operator != null) {
				if (expression1 != null) {
					returnValue += " ";
				}
				returnValue += operator;
				if ("!".equals(operator)) {
					returnValue += "(";
				}
			}
			if (expression2 != null) {
				if (operator != null && "!".equals(operator)) {
					returnValue = returnValue + expression2.toString() + ")";
				} else {
					returnValue = returnValue + " " + expression2.toString();
				}
			}
			return returnValue;
		}
	}
}
