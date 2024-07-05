package de.soderer.utilities.pac.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Method {
	private final String methodName;
	private final List<String> methodParameterNames;
	private final List<Statement> statements;

	public Method(final String methodName, final List<String> methodParameterNames, final List<String> methodBodyTokens) {
		this.methodName = methodName;
		this.methodParameterNames = methodParameterNames;
		statements = PacScriptParserUtilities.parseCodeBlockTokens(methodBodyTokens);
	}

	public List<String> getMethodParameterNames() {
		return methodParameterNames;
	}

	public Object executeMethod(final List<Object> methodParameters, final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods) {
		final Map<String, Object> subEnvironmentVariables = new HashMap<>(environmentVariables);
		for (int i = 0; i < methodParameterNames.size(); i++) {
			final String methodParameterName = methodParameterNames.get(i);
			final Object methodParameter = methodParameters.get(i);
			subEnvironmentVariables.put(methodParameterName, methodParameter);
		}
		for (final Statement statement : statements) {
			final Object result = statement.execute(subEnvironmentVariables, definedMethods);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		String returnValue = "";
		for (final String methodParameterName : methodParameterNames) {
			if (returnValue.length() > 0) {
				returnValue += ", ";
			}
			returnValue += methodParameterName;
		}
		returnValue = "function " + methodName + "(" + returnValue + ") {";
		for (final Statement statement : statements) {
			returnValue += "\n" + PacScriptParserUtilities.indentLines(statement.toString());
		}
		returnValue += "\n}";
		return returnValue;
	}
}
