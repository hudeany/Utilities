package de.soderer.utilities.sql.whereclause.token;

public class SuplementalPart extends RulePart {
	public enum Type {
		OpeningBracket,
		ClosingBracket,
		Separator
	}
	
	public Type type;
	
	public SuplementalPart(Type type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		switch(type) {
			case OpeningBracket:
				return "(";
			case ClosingBracket:
				return ")";
			default:
				return ", ";
		}
	}
	
	@Override
	public String toString(RulePart.StringType stringType) {
		switch(type) {
			case OpeningBracket:
				return "(";
			case ClosingBracket:
				return ")";
			default:
				return ", ";
		}
	}
}
