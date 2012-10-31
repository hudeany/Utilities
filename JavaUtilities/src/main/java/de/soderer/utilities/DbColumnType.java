package de.soderer.utilities;

public class DbColumnType {
	private String name;
	private int jsqlType;
	private int length;
	private boolean nullable;
	
	public DbColumnType(String dbTypeName, int jsqlType, int length, boolean nullable) {
		this.name = dbTypeName;
		this.jsqlType = jsqlType;
		this.length = length;
		this.nullable = nullable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getJsqlType() {
		return jsqlType;
	}

	public void setJsqlType(int jsqlType) {
		this.jsqlType = jsqlType;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
}
