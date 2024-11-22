package de.soderer.utilities.pac.utilities;

import java.util.List;

public class Result implements Statement {
	private final Expression expression;

	public Result(final List<String> expressionTokens) {
		expression = new Expression(expressionTokens);
	}

	@Override
	public Object execute(final Context context) {
		return expression.execute(context);
	}

	@Override
	public String toString() {
		return "return " + expression.toString() + ";";
	}
}
