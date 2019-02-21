package de.soderer.utilities.sql;

import org.apache.commons.lang3.StringUtils;

public class NamedExpression {
	private Object expression;

	private String name;

	public NamedExpression(final Object expression, final String name) throws Exception {
		setExpression(expression);
		setName(name);
	}

	public Object getExpression() {
		return expression;
	}

	public void setExpression(final Object expression) throws Exception {
		if (expression instanceof String) {
			this.expression = StringUtils.trim((String) expression);
		} else if (expression instanceof SelectStatement) {
			this.expression = expression;
		} else {
			throw new Exception("Invalid expression type. Only String and SelectStatement are allowed");
		}
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = StringUtils.trim(name);
	}
}
