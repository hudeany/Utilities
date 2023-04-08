package de.soderer.utilities;

public class DatabaseConstraint {
	public enum ConstraintType {
		PrimaryKey,
		Unique,
		ForeignKey,
		Check
	}

	private String tableName;
	private String constraintName;
	private ConstraintType constraintType;

	public DatabaseConstraint(final String tableName, final String constraintName, final ConstraintType constraintType) {
		this.tableName = tableName;
		this.constraintName = constraintName;
		this.constraintType = constraintType;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}

	public String getConstraintName() {
		return constraintName;
	}

	public void setConstraintName(final String constraintName) {
		this.constraintName = constraintName;
	}

	public ConstraintType getConstraintType() {
		return constraintType;
	}

	public void setConstraintType(final ConstraintType constraintType) {
		this.constraintType = constraintType;
	}
}
