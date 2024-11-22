package de.soderer.utilities.pac.utilities;

import java.util.List;

public class Assignment implements Statement {
	/**
	 * Declarations by 'let' keyword are block sensitive and may not be re-declarated
	 */
	private final boolean isLetDeclaration;
	private final String variableName;
	private final Expression expression;

	public Assignment(final boolean isLetDeclaration, final String variableName, final List<String> expressionTokens) {
		this.isLetDeclaration = isLetDeclaration;
		this.variableName = variableName;
		expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(final Context context) {
		if (isLetDeclaration && context.hasVariable(variableName)) {
			throw new RuntimeException("Multiple declaration of variablename by 'let' keyword");
		} else {
			// Expression must be executed always for concurrency results of unary operators
			final Object result = expression.execute(context);
			context.setEnvironmentVariable(variableName, result);
			return null;
		}
	}

	@Override
	public String toString() {
		if (isLetDeclaration) {
			return "let " + variableName + " = " + expression.toString() + ";";
		} else {
			return "var " + variableName + " = " + expression.toString() + ";";
		}
	}
}
