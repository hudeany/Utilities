package de.soderer.utilities.sql;

import org.apache.commons.lang3.StringUtils;

public class SelectStatementFormatter {
	private final String indentation;
	private final String lineBreak;

	public SelectStatementFormatter() {
		this("\t", "\n");
	}

	public SelectStatementFormatter(final String indentation, final String lineBreak) {
		this.indentation = indentation;
		this.lineBreak = lineBreak;
	}

	public String format(final SelectStatement selectStatement) {
		if (StringUtils.isEmpty(indentation) && StringUtils.isEmpty(lineBreak)) {
			final StringBuilder returnValue = new StringBuilder();
			if (selectStatement.getWithClauses() != null && selectStatement.getWithClauses().size() > 0) {
				returnValue.append("WITH ");
				returnValue.append(StringUtils.join(selectStatement.getWithClauses(), ","));
				returnValue.append(" ");
			}
			returnValue.append("SELECT ");
			boolean isFirstNamedExpression = true;
			for (final NamedExpression namedFieldDefinition : selectStatement.getFields()) {
				if (!isFirstNamedExpression) {
					returnValue.append(",");
				}
				returnValue.append(format(namedFieldDefinition));
				isFirstNamedExpression = false;
			}
			returnValue.append(" FROM ");
			returnValue.append(StringUtils.join(selectStatement.getFromTables(), ","));
			if (selectStatement.getJoinClauses() != null && selectStatement.getJoinClauses().size() > 0) {
				returnValue.append(StringUtils.join(selectStatement.getJoinClauses(), " "));
			}
			if (StringUtils.isNotBlank(selectStatement.getWhereClause())) {
				returnValue.append(" WHERE ");
				returnValue.append(selectStatement.getWhereClause());
			}
			if (selectStatement.getGroupBy() != null && selectStatement.getGroupBy().size() > 0) {
				returnValue.append(" GROUP BY ");
				returnValue.append(StringUtils.join(selectStatement.getGroupBy(), ", "));
			}
			if (StringUtils.isNotBlank(selectStatement.getHavingClause())) {
				returnValue.append(" HAVING ");
				returnValue.append(selectStatement.getHavingClause());
			}
			if (selectStatement.getOrderBy() != null && selectStatement.getOrderBy().size() > 0) {
				returnValue.append(" SORT BY ");
				returnValue.append(StringUtils.join(selectStatement.getOrderBy(), ", "));
			}
			return returnValue.toString();
		} else {
			final StringBuilder returnValue = new StringBuilder();
			if (selectStatement.getWithClauses() != null && selectStatement.getWithClauses().size() > 0) {
				returnValue.append("WITH");
				returnValue.append(lineBreak);
				returnValue.append(indentation);
				returnValue.append(StringUtils.join(selectStatement.getWithClauses(), ",\n" + indentation));
				returnValue.append(lineBreak);
			}
			returnValue.append("SELECT");
			returnValue.append(lineBreak);
			returnValue.append(indentation);
			boolean isFirstNamedExpression = true;
			for (final NamedExpression namedFieldDefinition : selectStatement.getFields()) {
				if (!isFirstNamedExpression) {
					returnValue.append("," + lineBreak + indentation);
				}
				returnValue.append(format(namedFieldDefinition));
				isFirstNamedExpression = false;
			}
			returnValue.append(lineBreak);
			returnValue.append("FROM");
			returnValue.append(lineBreak);
			returnValue.append(indentation);
			returnValue.append(StringUtils.join(selectStatement.getFromTables(), "," + lineBreak + indentation));
			if (selectStatement.getJoinClauses() != null && selectStatement.getJoinClauses().size() > 0) {
				returnValue.append(lineBreak);
				returnValue.append(StringUtils.join(selectStatement.getJoinClauses(), lineBreak));
			}
			if (StringUtils.isNotBlank(selectStatement.getWhereClause())) {
				returnValue.append(lineBreak);
				returnValue.append("WHERE ");
				returnValue.append(selectStatement.getWhereClause());
			}
			if (selectStatement.getGroupBy() != null && selectStatement.getGroupBy().size() > 0) {
				returnValue.append(lineBreak);
				returnValue.append("GROUP BY");
				returnValue.append(lineBreak);
				returnValue.append(indentation);
				returnValue.append(StringUtils.join(selectStatement.getGroupBy(), "," + lineBreak + indentation));
			}
			if (StringUtils.isNotBlank(selectStatement.getHavingClause())) {
				returnValue.append(lineBreak);
				returnValue.append("HAVING ");
				returnValue.append(selectStatement.getHavingClause());
			}
			if (selectStatement.getOrderBy() != null && selectStatement.getOrderBy().size() > 0) {
				returnValue.append(lineBreak);
				returnValue.append("SORT BY");
				returnValue.append(lineBreak);
				returnValue.append(indentation);
				returnValue.append(StringUtils.join(selectStatement.getOrderBy(), "," + lineBreak + indentation));
			}
			return returnValue.toString();
		}
	}

	private String format(final NamedExpression namedFieldDefinition) {
		String expressionString;
		if (namedFieldDefinition.getExpression() instanceof SelectStatement) {
			expressionString = format((SelectStatement) namedFieldDefinition.getExpression());
		} else {
			expressionString = (String) namedFieldDefinition.getExpression();
		}
		final StringBuilder returnValue = new StringBuilder(expressionString);
		if (StringUtils.isNotBlank(namedFieldDefinition.getName())) {
			returnValue.append(" AS \"").append(namedFieldDefinition.getName()).append("\"");
		}
		return returnValue.toString();
	}
}
