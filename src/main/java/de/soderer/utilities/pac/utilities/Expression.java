package de.soderer.utilities.pac.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

	public Expression(final List<String> expressionTokens) {
		if ("(".equals(expressionTokens.get(0))) {
			final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, 0);
			expression1 = new Expression(expressionTokens.subList(1, bracketEnd));
			if (bracketEnd < expressionTokens.size() - 1) {
				operator = expressionTokens.get(bracketEnd + 1);
				expression2 = new Expression(expressionTokens.subList(bracketEnd + 2, expressionTokens.size()));
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
	public Object execute(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods) {
		if (expressionTokens == null) {
			final Object result1 = expression1 == null ? null : expression1.execute(environmentVariables, definedMethods);
			final Object result2 = expression2 == null ? null : expression2.execute(environmentVariables, definedMethods);
			if (result1 == null) {
				if ("++".equals(operator) && result2 != null && result2 instanceof Integer) {
					final int value = ((Integer) result2);
					environmentVariables.put(expression2.getVariableName(), value);
					return value;
				} else if ("--".equals(operator) && result2 != null && result2 instanceof Integer) {
					final int value = ((Integer) result2);
					environmentVariables.put(expression2.getVariableName(), value);
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
					environmentVariables.put(expression1.getVariableName(), value);
					return value;
				} else if ("--".equals(operator) && result1 instanceof Integer) {
					final int value = ((Integer) result1) - 1;
					environmentVariables.put(expression2.getVariableName(), value);
					return value;
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
			final String currentToken = expressionTokens.get(0);
			if (expressionTokens.size() > 1 && expressionTokens.get(1).equals("(")) {
				final List<Object> methodCallParameters = readMethodParameters(environmentVariables, definedMethods, 1);

				if (definedMethods.containsKey(currentToken)) {
					return definedMethods.get(currentToken).executeMethod(methodCallParameters, environmentVariables, definedMethods);
				} else if ("isPlainHostName".equals(currentToken)) {
					return PacScriptMethods.isPlainHostName((String) methodCallParameters.get(0));
				} else if ("dnsDomainIs".equals(currentToken)) {
					return PacScriptMethods.dnsDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("localHostOrDomainIs".equals(currentToken)) {
					return PacScriptMethods.localHostOrDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("isResolvable".equals(currentToken)) {
					return PacScriptMethods.isResolvable((String) methodCallParameters.get(0));
				} else if ("isInNet".equals(currentToken)) {
					return PacScriptMethods.isInNet((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
				} else if ("dnsResolve".equals(currentToken)) {
					return PacScriptMethods.dnsResolve((String) methodCallParameters.get(0));
				} else if ("myIpAddress".equals(currentToken)) {
					return PacScriptMethods.myIpAddress();
				} else if ("dnsDomainLevels".equals(currentToken)) {
					return PacScriptMethods.dnsDomainLevels((String) methodCallParameters.get(0));
				} else if ("shExpMatch".equals(currentToken)) {
					return PacScriptMethods.shExpMatch((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("weekdayRange".equals(currentToken)) {
					return PacScriptMethods.weekdayRange((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
				} else if ("dateRange".equals(currentToken)) {
					return PacScriptMethods.dateRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
				} else if ("timeRange".equals(currentToken)) {
					return PacScriptMethods.timeRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
				} else if ("isResolvableEx".equals(currentToken)) {
					return PacScriptMethods.isResolvableEx((String) methodCallParameters.get(0));
				} else if ("isInNetEx".equals(currentToken)) {
					return PacScriptMethods.isInNetEx((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
				} else if ("dnsResolveEx".equals(currentToken)) {
					return PacScriptMethods.dnsResolveEx((String) methodCallParameters.get(0));
				} else if ("myIpAddressEx".equals(currentToken)) {
					return PacScriptMethods.myIpAddressEx();
				} else if ("sortIpAddressList".equals(currentToken)) {
					return PacScriptMethods.sortIpAddressList((String) methodCallParameters.get(0));
				} else if ("getClientVersion".equals(currentToken)) {
					return PacScriptMethods.getClientVersion();
				} else {
					throw new RuntimeException("Call of undefined method: " + currentToken);
				}
			} else {
				if ("true".equals(currentToken)) {
					return true;
				} else if ("false".equals(currentToken)) {
					return false;
				} else if (environmentVariables.containsKey(currentToken)) {
					return environmentVariables.get(currentToken);
				} else if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
					return currentToken.substring(1, currentToken.length() - 1);
				} else {
					try {
						return Integer.parseInt(currentToken);
					} catch (@SuppressWarnings("unused") final NumberFormatException e1) {
						try {
							return Float.parseFloat(currentToken);
						} catch (@SuppressWarnings("unused") final NumberFormatException e2) {
							try {
								return Double.parseDouble(currentToken);
							} catch (@SuppressWarnings("unused") final NumberFormatException e3) {
								throw new RuntimeException("Unexpected code in expression: " + currentToken);
							}
						}
					}
				}
			}
		}
	}

	private String getVariableName() {
		if (expressionTokens != null && expressionTokens.size() == 1) {
			return expressionTokens.get(0);
		} else {
			throw new RuntimeException("Unexpected expression when expecting variablename");
		}
	}

	private List<Object> readMethodParameters(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods, int bracketStartTokenIndex) {
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
			final Object parameterValue = new Expression(expressionTokens.subList(nextParameterStart, nextParameterEnd)).execute(environmentVariables, definedMethods);
			methodCallParameters.add(parameterValue);
			bracketStartTokenIndex = nextParameterEnd + 1;
		}
		return methodCallParameters;
	}

	@Override
	public String toString() {
		if (expressionTokens != null && !expressionTokens.isEmpty()) {
			String returnValue = "";
			for (final String expressionToken : expressionTokens) {
				if (returnValue.length() > 0) {
					returnValue += " ";
				}
				returnValue += expressionToken;
			}
			return returnValue;
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
			}
			if (expression2 != null) {
				returnValue = "(" + returnValue + " " + expression2.toString() + ")";
			}
			return returnValue;
		}
	}
}
