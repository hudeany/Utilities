package de.soderer.utilities.sql;

import org.apache.commons.lang3.StringUtils;

public class WithDefinition {
	private String name;

	private String definition;

	public WithDefinition(final String name, final String definition) {
		setName(name);
		setDefinition(definition);
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(final String definition) {
		this.definition = StringUtils.trim(definition);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = StringUtils.trim(name);
	}

	@Override
	public String toString() {
		final StringBuilder returnValue = new StringBuilder(name);
		returnValue.append(" AS (").append(definition).append(")");
		return returnValue.toString();
	}
}
