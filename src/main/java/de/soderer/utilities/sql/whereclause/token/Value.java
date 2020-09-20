package de.soderer.utilities.sql.whereclause.token;

import java.text.NumberFormat;

import de.soderer.utilities.Utilities;

public class Value extends RulePart {
	public enum Type {
		Bool, String, Number, Date
	}

	public Type type;

	public String stringValue;
	public double numberValue;

	public String fieldName;

	protected Value() {
	}

	public Value(final String stringValue) {
		type = Type.String;
		this.stringValue = stringValue;
	}

	public Value(final double numberValue) {
		type = Type.Number;
		this.numberValue = numberValue;
	}

	public Value(final String fieldName, final Type fieldType) {
		this.fieldName = fieldName.toLowerCase();
		type = fieldType;
	}

	@Override
	public String toString() {
		if (Utilities.isNotEmpty(fieldName)) {
			return fieldName;
		} else {
			switch (type) {
				case String:
					final StringBuilder returnValue = new StringBuilder();
					returnValue.append("'");
					returnValue.append(stringValue.replace("'", "''"));
					returnValue.append("'");
					return returnValue.toString();
				case Number:
					return NumberFormat.getNumberInstance().format(numberValue);
				case Bool:
					throw new RuntimeException("Invalid value type for output: " + type);
				case Date:
					throw new RuntimeException("Invalid value type for output: " + type);
				default:
					throw new RuntimeException("Invalid value type for output: " + type);
			}
		}
	}

	@Override
	public String toString(final RulePart.StringType stringType) {
		if (Utilities.isNotEmpty(fieldName) && Expression.SYSDATE_VALUES.contains(fieldName)) {
			if (stringType == StringType.Oracle) {
				return "SYSDATE";
			} else if (stringType == StringType.MySQL) {
				return "SYSDATE()";
			} else {
				return "sysdate";
			}
		} else {
			return toString();
		}
	}
}
