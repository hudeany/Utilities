package de.soderer.utilities.pac.utilities;

import java.util.List;
import java.util.Map;

public class Result implements Statement {
	private Expression expression;

	public Result(List<String> expressionTokens) {
		this.expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(Map<String, Object> environmentVariables, Map<String, Method> definedMethods) {
		return expression.execute(environmentVariables, definedMethods);
	}
}
