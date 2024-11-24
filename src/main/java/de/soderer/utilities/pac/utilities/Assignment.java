package de.soderer.utilities.pac.utilities;

import java.util.List;

public class Assignment implements Statement {
	public enum Scope {
		VAR,

		/**
		 * Declarations by 'let' keyword are block sensitive and may not be re-declarated
		 */
		LET,

		CONST
	}

	private final Scope scope;
	private final String variableName;
	private final Expression expression;

	public Assignment(final String variableName, final List<String> expressionTokens) {
		this(Scope.VAR, variableName, expressionTokens);
	}

	public Assignment(final Scope scope, final String variableName, final List<String> expressionTokens) {
		this.scope = scope;
		this.variableName = variableName;
		expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(final Context context) {
		if (scope == Scope.CONST && context.hasVariable(variableName)) {
			throw new RuntimeException("Multiple declaration of variablename by 'const' keyword: " + variableName);
		} else if (context.isConstVariable(variableName)) {
			throw new RuntimeException("Assignment to constant after declaration: " + variableName);
		} else if (scope == Scope.LET && context.hasVariable(variableName)) {
			throw new RuntimeException("Multiple declaration of variablename by 'let' keyword: " + variableName);
		} else {
			// Expression must be executed always for concurrency results of unary operators
			final Object result = expression.execute(context);
			context.setEnvironmentVariable(variableName, result);
			if (scope == Scope.CONST) {
				context.setConstVariable(variableName);
			}
			return null;
		}
	}

	@Override
	public String toString() {
		if (scope == Scope.CONST) {
			return "const " + variableName + " = " + expression.toString() + ";";
		} else if (scope == Scope.LET) {
			return "let " + variableName + " = " + expression.toString() + ";";
		} else {
			return "var " + variableName + " = " + expression.toString() + ";";
		}
	}
}
