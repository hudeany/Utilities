package de.soderer.utilities.pac.utilities;

import java.util.List;
import java.util.Map;

public class Assignment implements Statement {
	private String variableName;
	private Expression expression;
	
	public Assignment(String variableName, List<String> expressionTokens) {
		this.variableName = variableName;
		this.expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(Map<String, Object> environmentVariables, Map<String, Method> definedMethods) {
		environmentVariables.put(variableName, expression.execute(environmentVariables, definedMethods));
		return null;
	}
}
