package de.soderer.utilities.sql.whereclause.token;

public class SupplementalPart extends RulePart {
	public enum Type {
		OpeningBracket, ClosingBracket, Separator
	}

	public Type type;

	public SupplementalPart(final Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		switch (type) {
			case OpeningBracket:
				return "(";
			case ClosingBracket:
				return ")";
			case Separator:
				return ", ";
			default:
				return ", ";
		}
	}

	@Override
	public String toString(final RulePart.StringType stringType) {
		switch (type) {
			case OpeningBracket:
				return "(";
			case ClosingBracket:
				return ")";
			case Separator:
				return ", ";
			default:
				return ", ";
		}
	}
}
