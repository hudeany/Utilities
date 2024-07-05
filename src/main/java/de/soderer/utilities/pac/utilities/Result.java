package de.soderer.utilities.pac.utilities;

import java.util.List;
import java.util.Map;

public class Result implements Statement {
	private final Expression expression;

	public Result(final List<String> expressionTokens) {
		expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods) {
		return expression.execute(environmentVariables, definedMethods);
	}

	@Override
	public String toString() {
		return "return " + expression.toString() + ";";
	}
}
