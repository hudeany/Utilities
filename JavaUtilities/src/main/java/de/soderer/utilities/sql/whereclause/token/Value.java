package de.soderer.utilities.sql.whereclause.token;

import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;

public class Value extends RulePart {
	public enum Type {
		Bool,
		String,
		Number,
		Date
	}
	
	public Type type;
	
	public String stringValue;
	public double numberValue;
	
	public String fieldName;
	
	protected Value() {
	}
	
	public Value(String stringValue) {
		this.type = Type.String;
		this.stringValue = stringValue;
	}
	
	public Value(double numberValue) {
		this.type = Type.Number;
		this.numberValue = numberValue;
	}
	
	public Value(String fieldName, Type fieldType) {
		this.fieldName = fieldName.toLowerCase();
		this.type = fieldType;
	}

	@Override
	public String toString() {
		if (StringUtils.isNotEmpty(fieldName)) {
			return fieldName;
		} else {
			switch(type) {
				case String:
					StringBuilder returnValue = new StringBuilder();
					returnValue.append("'");
					returnValue.append(stringValue.replace("'", "''"));
					returnValue.append("'");
					return returnValue.toString();
				case Number:
					return DecimalFormat.getNumberInstance().format(numberValue);
				default:
					throw new RuntimeException("Invalid value type for output: " + type);
			}
		}
	}

	@Override
	public String toString(RulePart.StringType stringType) {
		if (StringUtils.isNotEmpty(fieldName) && Expression.SYSDATE_VALUES.contains(fieldName)) {
			if (stringType == StringType.Oracle)
				return "SYSDATE";
			else if (stringType == StringType.MySQL)
				return "SYSDATE()";
			else return "sysdate";
		} else {
			return toString();
		}
	}
}
