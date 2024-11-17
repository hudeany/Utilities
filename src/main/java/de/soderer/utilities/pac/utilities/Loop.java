package de.soderer.utilities.pac.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.soderer.utilities.pac.utilities.exception.BreakLoopException;
import de.soderer.utilities.pac.utilities.exception.ContinueLoopException;

public class Loop implements Statement {
	private Assignment loopInit;
	private final Expression loopCondition;
	private final Statement loopStep;
	private final List<Statement> loopStatements;

	public Loop(final List<String> loopHeadTokens, final List<String> loopCodeBlockTokens) {
		List<String> loopInitTokens = null;
		List<String> loopConditionTokens = null;
		List<String> loopStepTokens = null;
		for (int i = 0; i < loopHeadTokens.size(); i++) {
			if (";".equals(loopHeadTokens.get(i)) ) {
				if (loopInitTokens == null) {
					loopInitTokens = loopHeadTokens.subList(0, i);
				} else {
					loopConditionTokens = loopHeadTokens.subList(loopInitTokens.size() + 1, i);
					loopStepTokens = new ArrayList<>(loopHeadTokens.subList(i + 1, loopHeadTokens.size()));
					if (loopStepTokens.contains(";")) {
						throw new RuntimeException("Unsupported loop step expression");
					}
					loopStepTokens.add(";");
					break;
				}
			}
		}

		if (loopInitTokens == null || loopInitTokens.size() == 0) {
			throw new RuntimeException("Unsupported loop empty init expression");
		} else if (loopConditionTokens == null || loopConditionTokens.size() == 0) {
			throw new RuntimeException("Unsupported loop empty condition expression");
		} else if (loopStepTokens == null || loopStepTokens.size() == 0) {
			throw new RuntimeException("Unsupported loop empty step expression");
		}

		if ("var".equals(loopInitTokens.get(0)) || "let".equals(loopInitTokens.get(0))) {
			final String variableName = loopInitTokens.get(1);

			if (!"=".equals(loopInitTokens.get(2))) {
				throw new RuntimeException("Unexpected code in loop init: " + loopInitTokens.get(2));
			}

			loopInit = new Assignment("let".equals(loopInitTokens.get(0)), variableName, loopInitTokens.subList(3, loopInitTokens.size()));
		} else {
			throw new RuntimeException("Unsupported loop init expression");
		}

		loopCondition = new Expression(loopConditionTokens);
		loopStep = PacScriptParserUtilities.parseCodeBlockTokens(loopStepTokens).get(0);
		loopStatements = PacScriptParserUtilities.parseCodeBlockTokens(loopCodeBlockTokens);
	}

	public Loop(final String loopConditionExpression, final List<String> loopCodeBlockTokens) {
		if (loopConditionExpression == null || loopConditionExpression.trim().length() == 0) {
			throw new RuntimeException("Unsupported loop empty condition expression");
		}

		loopCondition = new Expression(Collections.singletonList(loopConditionExpression));
		loopStep = null;
		loopStatements = PacScriptParserUtilities.parseCodeBlockTokens(loopCodeBlockTokens);
	}

	@Override
	public Object execute(final Map<String, Object> environmentVariables, final Map<String, Method> definedMethods) {
		if (loopInit != null) {
			loopInit.execute(environmentVariables, definedMethods);
			try {
				while (true) {
					final Object expressionResult = loopCondition.execute(environmentVariables, definedMethods);
					if (!(expressionResult instanceof Boolean)) {
						throw new RuntimeException("Unsupported loop init expression result type");
					} else if ((Boolean) expressionResult) {
						for (final Statement loopStatement : loopStatements) {
							try {
								loopStatement.execute(environmentVariables, definedMethods);
							} catch (@SuppressWarnings("unused") final ContinueLoopException e) {
								break;
							}
						}

						loopStep.execute(environmentVariables, definedMethods);
					} else {
						break;
					}
				}
			} catch (@SuppressWarnings("unused") final BreakLoopException e) {
				// Do nothing, just exit the while loop
			}
		} else {
			try {
				while (true) {
					final Object expressionResult = loopCondition.execute(environmentVariables, definedMethods);
					if (!(expressionResult instanceof Boolean)) {
						throw new RuntimeException("Unsupported loop init expression result type");
					} else if ((Boolean) expressionResult) {
						for (final Statement loopStatement : loopStatements) {
							try {
								loopStatement.execute(environmentVariables, definedMethods);
							} catch (@SuppressWarnings("unused") final ContinueLoopException e) {
								break;
							}
						}
					} else {
						break;
					}
				}
			} catch (@SuppressWarnings("unused") final BreakLoopException e) {
				// Do nothing, just exit the while loop
			}
		}
		return null;
	}

	@Override
	public String toString() {
		if (loopInit != null) {
			String loopStepString = loopStep.toString();
			if (loopStepString.endsWith(";")) {
				loopStepString = loopStepString.substring(0, loopStepString.length() - 1);
			}
			if (loopStepString.startsWith("var ")) {
				loopStepString = loopStepString.substring(4);
			}
			String returnValue = "for (" + loopInit.toString() + " " + loopCondition.toString() + "; " + loopStepString + ") {\n";
			for (final Statement loopStatement : loopStatements) {
				returnValue += PacScriptParserUtilities.indentLines(loopStatement.toString()) + "\n";
			}
			returnValue += "}";
			return returnValue;
		} else {
			String returnValue = "while (" + loopCondition.toString() + ") {\n";
			for (final Statement loopStatement : loopStatements) {
				returnValue += PacScriptParserUtilities.indentLines(loopStatement.toString()) + "\n";
			}
			returnValue += "}";
			return returnValue;
		}
	}
}
