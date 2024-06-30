package de.soderer.utilities.pac.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Expression implements Statement {
	private List<String> expressionTokens;

	private Expression expression1;
	private String operator;
	private Expression expression2;

	public Expression(final List<String> expressionTokens) {
		for (int tokenIndex = 0; tokenIndex < expressionTokens.size(); tokenIndex++) {
			final String currentToken = expressionTokens.get(tokenIndex);
			if ("+".equals(currentToken) || "-".equals(currentToken) || "*".equals(currentToken) || "/".equals(currentToken)
					|| "&".equals(currentToken) || "|".equals(currentToken)
					|| "++".equals(currentToken) || "--".equals(currentToken)
					|| "&&".equals(currentToken) || "||".equals(currentToken)) {
				this.expressionTokens = null;
				expression1 = new Expression(expressionTokens.subList(0, tokenIndex));
				operator = currentToken;
				expression2 = new Expression(expressionTokens.subList(tokenIndex + 1, expressionTokens.size()));
				break;
			} else if ("(".equals(currentToken)) {
				this.expressionTokens = null;
				final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, tokenIndex);
				expression1 = new Expression(expressionTokens.subList(tokenIndex + 1, bracketEnd));
				tokenIndex = bracketEnd + 1;
				if (tokenIndex < expressionTokens.size()) {
					operator = expressionTokens.get(tokenIndex);
					expression2 = new Expression(expressionTokens.subList(tokenIndex + 1, expressionTokens.size()));
				}
				break;
			} else {
				if (tokenIndex + 1 < expressionTokens.size() && expressionTokens.get(tokenIndex + 1).equals("(")) {
					tokenIndex++;
					final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, tokenIndex);
					tokenIndex = bracketEnd;
					tokenIndex++;
					if (this.expressionTokens != null) {
						throw new RuntimeException("Missing operator in expression at token index " + tokenIndex + ": " + currentToken);
					} else {
						this.expressionTokens = expressionTokens.subList(0, tokenIndex);
					}
				} else {
					this.expressionTokens = expressionTokens.subList(0, tokenIndex + 1);
				}
			}
		}
	}

	@Override
	public Object execute(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods) {
		Object result = null;
		if (expressionTokens == null) {
			final Object result1 = expression1 == null ? null : expression1.execute(environmentVariables, definedMethods);
			final Object result2 = expression2 == null ? null : expression2.execute(environmentVariables, definedMethods);
			if ("+".equals(operator)) {
				if (result1 instanceof String && result2 instanceof String) {
					result = ((String) result1) + (String) result2;
				} else if (result1 instanceof Integer && result2 instanceof Integer) {
					result = ((Integer) result1) + (Integer) result2;
				}  else if (result1 instanceof Float && result2 instanceof Float) {
					result = ((Float) result1) + (Float) result2;
				}  else if (result1 instanceof Double && result2 instanceof Double) {
					result = ((Double) result1) + (Double) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("-".equals(operator)) {
				if (result1 instanceof Integer && result2 instanceof Integer) {
					result = ((Integer) result1) - (Integer) result2;
				}  else if (result1 instanceof Float && result2 instanceof Float) {
					result = ((Float) result1) - (Float) result2;
				}  else if (result1 instanceof Double && result2 instanceof Double) {
					result = ((Double) result1) - (Double) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("*".equals(operator)) {
				if (result1 instanceof Integer && result2 instanceof Integer) {
					result = ((Integer) result1) * (Integer) result2;
				}  else if (result1 instanceof Float && result2 instanceof Float) {
					result = ((Float) result1) * (Float) result2;
				}  else if (result1 instanceof Double && result2 instanceof Double) {
					result = ((Double) result1) * (Double) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("/".equals(operator)) {
				if (result1 instanceof Integer && result2 instanceof Integer) {
					result = ((Integer) result1) / (Integer) result2;
				}  else if (result1 instanceof Float && result2 instanceof Float) {
					result = ((Float) result1) / (Float) result2;
				}  else if (result1 instanceof Double && result2 instanceof Double) {
					result = ((Double) result1) / (Double) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("&".equals(operator)) {
				if (result1 instanceof Boolean && result2 instanceof Boolean) {
					result = ((Boolean) result1) & (Boolean) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("&&".equals(operator)) {
				if (result1 instanceof Boolean && result2 instanceof Boolean) {
					result = ((Boolean) result1) && (Boolean) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("|".equals(operator)) {
				if (result1 instanceof Boolean && result2 instanceof Boolean) {
					result = ((Boolean) result1) | (Boolean) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else if ("||".equals(operator)) {
				if (result1 instanceof Boolean && result2 instanceof Boolean) {
					result = ((Boolean) result1) || (Boolean) result2;
				} else {
					throw new RuntimeException("Unsupported parameters for operator: " + operator);
				}
			} else {
				throw new RuntimeException("Unsupported operator: " + operator);
			}
		} else {
			for (int tokenIndex = 0; tokenIndex < expressionTokens.size(); tokenIndex++) {
				final String currentToken = expressionTokens.get(tokenIndex);
				if (tokenIndex + 1 < expressionTokens.size() && expressionTokens.get(tokenIndex + 1).equals("(")) {
					tokenIndex++;
					final int bracketEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, tokenIndex);
					final List<Object> methodCallParameters = readMethodParameters(environmentVariables, definedMethods, tokenIndex);
					tokenIndex = bracketEnd;

					if (definedMethods.containsKey(currentToken)) {
						result = definedMethods.get(currentToken).executeMethod(methodCallParameters, environmentVariables, definedMethods);
					} else if ("isPlainHostName".equals(currentToken)) {
						result = PacScriptMethods.isPlainHostName((String) methodCallParameters.get(0));
					} else if ("dnsDomainIs".equals(currentToken)) {
						result = PacScriptMethods.dnsDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
					} else if ("localHostOrDomainIs".equals(currentToken)) {
						result = PacScriptMethods.localHostOrDomainIs((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
					} else if ("isResolvable".equals(currentToken)) {
						result = PacScriptMethods.isResolvable((String) methodCallParameters.get(0));
					} else if ("isInNet".equals(currentToken)) {
						result = PacScriptMethods.isInNet((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
					} else if ("dnsResolve".equals(currentToken)) {
						result = PacScriptMethods.dnsResolve((String) methodCallParameters.get(0));
					} else if ("myIpAddress".equals(currentToken)) {
						result = PacScriptMethods.myIpAddress();
					} else if ("dnsDomainLevels".equals(currentToken)) {
						result = PacScriptMethods.dnsDomainLevels((String) methodCallParameters.get(0));
					} else if ("shExpMatch".equals(currentToken)) {
						result = PacScriptMethods.shExpMatch((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
					} else if ("weekdayRange".equals(currentToken)) {
						result = PacScriptMethods.weekdayRange((String) methodCallParameters.get(0), (String) methodCallParameters.get(1), (String) methodCallParameters.get(2));
					} else if ("dateRange".equals(currentToken)) {
						result = PacScriptMethods.dateRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
					} else if ("timeRange".equals(currentToken)) {
						result = PacScriptMethods.timeRange(methodCallParameters.get(0), methodCallParameters.get(1), methodCallParameters.get(2), methodCallParameters.get(3), methodCallParameters.get(4), methodCallParameters.get(5), methodCallParameters.get(6));
					} else if ("isResolvableEx".equals(currentToken)) {
						result = PacScriptMethods.isResolvableEx((String) methodCallParameters.get(0));
					} else if ("isInNetEx".equals(currentToken)) {
						result = PacScriptMethods.isInNetEx((String) methodCallParameters.get(0), (String) methodCallParameters.get(1));
					} else if ("dnsResolveEx".equals(currentToken)) {
						result = PacScriptMethods.dnsResolveEx((String) methodCallParameters.get(0));
					} else if ("myIpAddressEx".equals(currentToken)) {
						result = PacScriptMethods.myIpAddressEx();
					} else if ("sortIpAddressList".equals(currentToken)) {
						result = PacScriptMethods.sortIpAddressList((String) methodCallParameters.get(0));
					} else if ("getClientVersion".equals(currentToken)) {
						result = PacScriptMethods.getClientVersion();
					}
				} else {
					if (environmentVariables.containsKey(currentToken)) {
						result = environmentVariables.get(currentToken);
					} else if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
						result = currentToken.substring(1, currentToken.length() - 1);
					} else {
						try {
							result = Integer.parseInt(currentToken);
						} catch (@SuppressWarnings("unused") final NumberFormatException e1) {
							try {
								result = Float.parseFloat(currentToken);
							} catch (@SuppressWarnings("unused") final NumberFormatException e2) {
								try {
									result = Double.parseDouble(currentToken);
								} catch (@SuppressWarnings("unused") final NumberFormatException e3) {
									throw new RuntimeException("Unexpected code in expression at token index " + tokenIndex + ": " + currentToken);
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	private List<Object> readMethodParameters(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods, int tokenIndex) {
		final int parametersEnd = PacScriptParserUtilities.findClosingBracketToken(expressionTokens, tokenIndex);
		tokenIndex++;
		final List<Object> methodCallParameters = new ArrayList<>();
		while (tokenIndex < parametersEnd) {
			final int nextParameterStart = tokenIndex;
			int nextParameterEnd = tokenIndex;
			String parameterToken = expressionTokens.get(nextParameterEnd);
			while (!(",".equals(parameterToken) || ")".equals(parameterToken))) {
				nextParameterEnd++;
				parameterToken = expressionTokens.get(nextParameterEnd);
			}
			final Object parameterValue = new Expression(expressionTokens.subList(nextParameterStart, nextParameterEnd)).execute(environmentVariables, definedMethods);
			methodCallParameters.add(parameterValue);
			tokenIndex = nextParameterEnd + 1;
		}
		return methodCallParameters;
	}
}
