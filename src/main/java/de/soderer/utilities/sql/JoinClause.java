package de.soderer.utilities.sql;

import de.soderer.utilities.Utilities;

public class JoinClause {
	public enum JoinType {
		InnerJoin,
		LeftOutterJoin,
		RightOutterJoin,
		FullOutterJoin,
	}

	private String joinTable;
	private String joinTableAlias;
	private JoinType joinType;
	private String joinCondition;

	public String getJoinTable() {
		return joinTable;
	}

	public void setJoinTable(final String joinTable) {
		this.joinTable = joinTable;
	}

	public String getJoinTableAlias() {
		return joinTableAlias;
	}

	public void setJoinTableAlias(final String joinTableAlias) {
		this.joinTableAlias = joinTableAlias;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public void setJoinType(final JoinType joinType) {
		this.joinType = joinType;
	}

	public String getJoinCondition() {
		return joinCondition;
	}

	public void setJoinCondition(final String joinCondition) {
		this.joinCondition = joinCondition;
	}

	public JoinClause(final String joinTable, final String joinTableAlias, final JoinType joinType, final String joinCondition) {
		this.joinTable = joinTable;
		this.joinTableAlias = joinTableAlias;
		this.joinType = joinType;
		this.joinCondition = joinCondition;
	}

	@Override
	public String toString() {
		final StringBuilder returnValue = new StringBuilder();

		switch (joinType) {
			case InnerJoin:
				returnValue.append("JOIN");
				break;
			case LeftOutterJoin:
				returnValue.append("LEFT JOIN");
				break;
			case RightOutterJoin:
				returnValue.append("RIGHT JOIN");
				break;
			case FullOutterJoin:
				returnValue.append("FULL OUTTER JOIN");
				break;
			default:
				throw new RuntimeException("Invalid missing Join type");
		}

		returnValue.append(" ");
		returnValue.append(joinTable);
		if (Utilities.isNotBlank(joinTableAlias)) {
			returnValue.append(" ");
			returnValue.append(joinTableAlias);
		}
		if (Utilities.isNotBlank(joinCondition)) {
			returnValue.append(" ON ");
			returnValue.append(joinCondition);
		}

		return returnValue.toString();
	}
}
