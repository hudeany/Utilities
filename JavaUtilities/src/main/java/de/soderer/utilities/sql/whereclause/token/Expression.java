package de.soderer.utilities.sql.whereclause.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Expression extends Value {
	public static final Set<String> SYSDATE_VALUES = new HashSet<String>(Arrays.asList(new String[]{
		"sysdate",
		"sysdate()"
	}));
	
	public static final Set<String> DATE_FUNCTION_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"date",
		"to_date",
		"str_to_date"
	}));

	public static final Set<String> CHAR_FUNCTION_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"char",
		"to_char",
		"date_format"
	}));
	
	public static final Set<String> BOOL_UNARY_POSTFIX_OPERATOR_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"is null",
		"is not null"
	}));
	
	public static final Set<String> NUMBER_ONLY_OPERATOR_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"mod"
	}));

	public static final Set<String> COMPARE_OPERATOR_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"<",
		"<=",
		"=",
		"!=",
		"<>",
		">=",
		">"
	}));
	
	public static final Set<String> STRING_ONLY_COMPARE_OPERATOR_SIGNS = new HashSet<String>(Arrays.asList(new String[]{
		"like",
		"not like"
	}));
	
	public static final List<String> BOOL_ONLY_OPERATOR_SIGNS = new ArrayList<String>(Arrays.asList(new String[]{
		"or", // lower arithmetic priority
		"and" // higher arithmetic priority
	}));
	
	public Operator functionOperator;
	public Value value1;
	public Operator infixOperator;
	public Value value2;
	
	public Expression(Value value, Operator unaryPostfixOperator) {
		if (BOOL_UNARY_POSTFIX_OPERATOR_SIGNS.contains(unaryPostfixOperator.sign)) {
			this.type = Type.Bool;
			this.value1 = value;
			this.infixOperator = unaryPostfixOperator;
		} else {
			throw new IllegalArgumentException("Invalid operator for unary postfix operator: " + unaryPostfixOperator.sign);
		}
	}

	public Expression(Value value1, Operator infixOperator, Value value2) {
		if (!COMPARE_OPERATOR_SIGNS.contains(infixOperator.sign)
				&& !STRING_ONLY_COMPARE_OPERATOR_SIGNS.contains(infixOperator.sign)
				&& !NUMBER_ONLY_OPERATOR_SIGNS.contains(infixOperator.sign)
				&& !BOOL_ONLY_OPERATOR_SIGNS.contains(infixOperator.sign)) {
			throw new IllegalArgumentException("Invalid operator: " + infixOperator.sign);
		} else if (value1.type != value2.type) {
			throw new IllegalArgumentException("Invalid value types for infix operator: " + infixOperator.sign);
		} else if (STRING_ONLY_COMPARE_OPERATOR_SIGNS.contains(infixOperator.sign) && value1.type != Type.String) {
			throw new IllegalArgumentException("Invalid value types for string infix operator: " + infixOperator.sign);
		} else if (BOOL_ONLY_OPERATOR_SIGNS.contains(infixOperator.sign) && value1.type != Type.Bool) {
			throw new IllegalArgumentException("Invalid value types for bool infix operator: " + infixOperator.sign);
		} else if (NUMBER_ONLY_OPERATOR_SIGNS.contains(infixOperator.sign) && value1.type != Type.Number) {
			throw new IllegalArgumentException("Invalid value types for bool infix operator: " + infixOperator.sign);
		} else {
			if (NUMBER_ONLY_OPERATOR_SIGNS.contains(infixOperator.sign)) {
				this.type = Type.Number;
			} else {
				this.type = Type.Bool;
			}
			this.value1 = value1;
			this.infixOperator = infixOperator;
			this.value2 = value2;
		}
	}

	public Expression(Operator functionOperator, Value value1, Value value2) {
		if (!DATE_FUNCTION_SIGNS.contains(functionOperator.sign)
				&& !CHAR_FUNCTION_SIGNS.contains(functionOperator.sign)) {
			throw new IllegalArgumentException("Invalid operator: " + functionOperator.sign);
		} else if (DATE_FUNCTION_SIGNS.contains(functionOperator.sign) && (value1.type != Type.String || value2.type != Type.String)) {
			throw new IllegalArgumentException("Invalid value types for function operator: " + functionOperator.sign);
		} else if (CHAR_FUNCTION_SIGNS.contains(functionOperator.sign) && (value1.type != Type.Date || value2.type != Type.String)) {
			throw new IllegalArgumentException("Invalid value types for function operator: " + functionOperator.sign);
		} else {
			if (DATE_FUNCTION_SIGNS.contains(functionOperator.sign)) {
				functionOperator.sign = "date";
				this.type = Type.Date;
			} else {
				functionOperator.sign = "char";
				this.type = Type.String;
			}
			this.functionOperator = functionOperator;
			this.value1 = value1;
			this.value2 = new Value(value2.stringValue.replace("%d", "dd").replace("%m", "mm").replace("%Y", "yyyy").replace("%y", "yy"));
		}
	}

	@Override
	public String toString() {
		boolean bracketizeValue1 = infixOperator != null && value1 instanceof Expression && ((Expression)value1).infixOperator != null && BOOL_ONLY_OPERATOR_SIGNS.contains(((Expression)value1).infixOperator.sign) && !((Expression)value1).infixOperator.sign.equalsIgnoreCase(infixOperator.sign);
		boolean bracketizeValue2 = infixOperator != null && value2 != null && value2 instanceof Expression && ((Expression)value2).infixOperator != null && BOOL_ONLY_OPERATOR_SIGNS.contains(((Expression)value2).infixOperator.sign) && !((Expression)value2).infixOperator.sign.equalsIgnoreCase(infixOperator.sign);
		
		StringBuilder returnValue = new StringBuilder();
		
		if (infixOperator != null) {
			if (bracketizeValue1) returnValue.append("(");
			returnValue.append(value1.toString());
			if (bracketizeValue1) returnValue.append(")");
			returnValue.append(" ");
			returnValue.append(infixOperator.toString());
			if (value2 != null) {
				returnValue.append(" ");
				if (bracketizeValue2) returnValue.append("(");
				returnValue.append(value2.toString());
				if (bracketizeValue2) returnValue.append(")");
			}
		} else {
			returnValue.append(functionOperator.toString());
			returnValue.append("(");
			returnValue.append(value1.toString());
			returnValue.append(", ");
			returnValue.append(value2.toString());
			returnValue.append(")");
		}
		
		return returnValue.toString();
	}
	
	@Override
	public String toString(RulePart.StringType stringType) {
		if (stringType == StringType.BeanShell && infixOperator != null && infixOperator.sign.equalsIgnoreCase("mod")) {
			throw new IllegalArgumentException("No beanshell representation available for operator: " + infixOperator.sign);
		}
		
		boolean bracketizeValue1 = infixOperator != null && value1 instanceof Expression && ((Expression)value1).infixOperator != null && BOOL_ONLY_OPERATOR_SIGNS.contains(((Expression)value1).infixOperator.sign) && !((Expression)value1).infixOperator.sign.equalsIgnoreCase(infixOperator.sign);
		boolean bracketizeValue2 = infixOperator != null && value2 != null && value2 instanceof Expression && ((Expression)value2).infixOperator != null && BOOL_ONLY_OPERATOR_SIGNS.contains(((Expression)value2).infixOperator.sign) && !((Expression)value2).infixOperator.sign.equalsIgnoreCase(infixOperator.sign);
		
		StringBuilder returnValue = new StringBuilder();
		
		if (infixOperator != null) {
			if (bracketizeValue1) returnValue.append("(");
			returnValue.append(value1.toString(stringType));
			if (bracketizeValue1) returnValue.append(")");
			returnValue.append(" ");
			returnValue.append(infixOperator.toString(stringType));
			if (value2 != null) {
				returnValue.append(" ");
				if (bracketizeValue2) returnValue.append("(");
				returnValue.append(value2.toString(stringType));
				if (bracketizeValue2) returnValue.append(")");
			}
		} else {
			returnValue.append(functionOperator.toString(stringType));
			returnValue.append("(");
			returnValue.append(value1.toString(stringType));
			returnValue.append(", ");
			if (stringType == StringType.MySQL) {					
				returnValue.append(value2.toString(stringType).replace("dd", "%d").replace("mm", "%m").replace("yyyy", "%Y").replace("yy", "%y"));
			}
			else {
				returnValue.append(value2.toString(stringType));
			}
			returnValue.append(")");
		}
		
		return returnValue.toString();
	}
}
