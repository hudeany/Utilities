package de.soderer.utilities.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: UNION ALL
 */
public class SelectStatement {
	private List<WithDefinition> withClauses = new ArrayList<>();
	private List<NamedExpression> fields = new ArrayList<>();
	private List<NamedTableDefinition> fromTables = new ArrayList<>();
	private List<JoinClause> joinClauses;
	private String whereClause;
	private List<String> groupBy;
	private String havingClause;
	private List<String> orderBy;

	public List<WithDefinition> getWithClauses() {
		return withClauses;
	}

	public SelectStatement setWithClauses(final List<WithDefinition> withClause) {
		withClauses = withClause;
		return this;
	}

	public SelectStatement addWithClause(final WithDefinition withDefinition) {
		withClauses.add(withDefinition);
		return this;
	}

	public SelectStatement addWithClause(final String name, final String fieldDefinition) {
		withClauses.add(new WithDefinition(name, fieldDefinition));
		return this;
	}

	public List<NamedExpression> getFields() {
		return fields;
	}

	public SelectStatement setFields(final List<NamedExpression> fields) {
		this.fields = fields;
		return this;
	}

	public SelectStatement addField(final NamedExpression namedFieldDefinition) {
		fields.add(namedFieldDefinition);
		return this;
	}

	public SelectStatement addField(final Object expression) throws Exception {
		fields.add(new NamedExpression(expression, null));
		return this;
	}

	public List<NamedTableDefinition> getFromTables() {
		return fromTables;
	}

	public SelectStatement setFromTables(final List<NamedTableDefinition> fromTables) {
		this.fromTables = fromTables;
		return this;
	}

	public SelectStatement addFromTable(final NamedTableDefinition fromTable) {
		fromTables.add(fromTable);
		return this;
	}

	public SelectStatement addFromTable(final String tableDefinition) {
		fromTables.add(new NamedTableDefinition(tableDefinition, null));
		return this;
	}

	public List<JoinClause> getJoinClauses() {
		return joinClauses;
	}

	public void setJoinClauses(final List<JoinClause> joinClauses) {
		this.joinClauses = joinClauses;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(final String whereClause) {
		this.whereClause = whereClause;
	}

	public List<String> getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(final List<String> groupBy) {
		this.groupBy = groupBy;
	}

	public String getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(final String havingClause) {
		this.havingClause = havingClause;
	}

	public List<String> getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(final List<String> orderBy) {
		this.orderBy = orderBy;
	}

	public SelectStatement() {
	}

	public SelectStatement(final List<NamedExpression> fields, final List<NamedTableDefinition> fromTables) {
		this.fields = fields;
		this.fromTables = fromTables;
	}

	@Override
	public String toString() {
		return new SelectStatementFormatter().format(this);
	}
}
