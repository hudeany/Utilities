package de.soderer.utilities.sql;

import de.soderer.utilities.Utilities;

public class NamedTableDefinition {
	private String definition;

	private String name;

	public NamedTableDefinition(final String definition, final String name) {
		setDefinition(definition);
		setName(name);
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(final String definition) {
		this.definition = Utilities.trim(definition);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = Utilities.trim(name);
	}

	@Override
	public String toString() {
		final StringBuilder returnValue = new StringBuilder(definition);
		if (Utilities.isNotBlank(name)) {
			returnValue.append(" ").append(name);
		}
		return returnValue.toString();
	}
}
