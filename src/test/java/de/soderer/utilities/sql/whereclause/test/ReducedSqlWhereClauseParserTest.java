package de.soderer.utilities.sql.whereclause.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;
import de.soderer.utilities.sql.whereclause.ReducedSqlWhereClauseParser;
import de.soderer.utilities.sql.whereclause.token.RulePart;
import de.soderer.utilities.sql.whereclause.token.Value;

public class ReducedSqlWhereClauseParserTest extends TestCase {
	public void testQuotes1Formula() {
		String formula = "text = 'abc'";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"text = 'abc'",
			rule.toString());
	}

	public void testQuotes2Formula() {
		String formula = "text = 'abc'''";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"text = 'abc'''",
			rule.toString());
	}

	public void testQuotes3Formula() {
		String formula = "text = '''abc'";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"text = '''abc'",
			rule.toString());
	}
	
	public void testEqualsFormula() {
		String formula = "Email = 'abc@test.de'";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"email = 'abc@test.de'",
			rule.toString());
	}
	
	public void testBracketFormula() {
		String formula = "(Email = 'abc@test.de')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"email = 'abc@test.de'",
			rule.toString());
	}
	
	public void testConditionFormula() {
		String formula = "Nummer < 5 or Nummer > 7 and Nummer != 6";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"nummer < 5 or (nummer > 7 and nummer != 6)",
			rule.toString());
	}
	
	public void testListFormula() {
		String formula = "TExt in ('abc', '1', 'ghi')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"text in ('abc', '1', 'ghi')",
			rule.toString());
	}
	
	public void testModFormula() {
		String formula = "Mod(Nummer, 3) = 2";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"mod(nummer, 3) = 2",
			rule.toString());
	}
	
	public void testLikeFormula() {
		String formula = "Email like '%teXt%'";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"email like '%teXt%'",
			rule.toString());
	}
	
	public void testNotLikeFormula() {
		String formula = "Text not like '%teXt%'";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"text not like '%teXt%'",
			rule.toString());
	}

	
	public void testDateFormula() {
		String formula = "Geburtstag = to_Date('23.01.1977', 'dd.mm.yyyy')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"geburtstag = date('23.01.1977', 'dd.mm.yyyy')",
			rule.toString());
		Assert.assertEquals(
			"geburtstag = TO_DATE('23.01.1977', 'dd.mm.yyyy')",
			rule.toString(RulePart.StringType.Oracle));
	}
	
	public void testFullFormula() {
		String formula = "Email not like '%teXt%' and Email != 'abc@soderer.de' and Email is not null OR TExt = 'ein Text, als ''Zitat''' AND (Nummer <= 2 OR Nummer = 3) anD Geburtstag = to_Date('23.01.1977', 'dd.mm.yyyy') or char(geburtstag, 'dd.mm') = '23.01' oR Mod(Nummer, 3) = 2 or TExt in ('abc', '1', 'ghi')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"(email not like '%teXt%' and email != 'abc@soderer.de' and email is not null) or (text = 'ein Text, als ''Zitat''' and (nummer <= 2 or nummer = 3) and geburtstag = date('23.01.1977', 'dd.mm.yyyy')) or char(geburtstag, 'dd.mm') = '23.01' or mod(nummer, 3) = 2 or text in ('abc', '1', 'ghi')",
			rule.toString());
		Assert.assertEquals(
			"(email NOT LIKE '%teXt%' AND email != 'abc@soderer.de' AND email IS NOT NULL) OR (text = 'ein Text, als ''Zitat''' AND (nummer <= 2 OR nummer = 3) AND geburtstag = TO_DATE('23.01.1977', 'dd.mm.yyyy')) OR TO_CHAR(geburtstag, 'dd.mm') = '23.01' OR MOD(nummer, 3) = 2 OR text IN ('abc', '1', 'ghi')",
			rule.toString(RulePart.StringType.Oracle));
		Assert.assertEquals(
			"(email NOT LIKE '%teXt%' AND email != 'abc@soderer.de' AND email IS NOT NULL) OR (text = 'ein Text, als ''Zitat''' AND (nummer <= 2 OR nummer = 3) AND geburtstag = STR_TO_DATE('23.01.1977', '%d.%m.%Y')) OR DATE_FORMAT(geburtstag, '%d.%m') = '23.01' OR MOD(nummer, 3) = 2 OR text IN ('abc', '1', 'ghi')",
			rule.toString(RulePart.StringType.MySQL));
	}
	
	public void testFullFormulaBeanShell() {
		String formula = "Email != 'abc@soderer.de' and Email is not null OR TExt = 'ein Text, als ''Zitat''' AND (Nummer <= 2 OR Nummer = 3) anD Geburtstag = to_Date('23.01.1977', 'dd.mm.yyyy') or TExt in ('abc', '1', 'ghi')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		Assert.assertEquals(
			"(email != 'abc@soderer.de' and email is not null) or (text = 'ein Text, als ''Zitat''' and (nummer <= 2 or nummer = 3) and geburtstag = date('23.01.1977', 'dd.mm.yyyy')) or text in ('abc', '1', 'ghi')",
			rule.toString(RulePart.StringType.BeanShell));
	}
	
	public void testLikeFormula_Negative() {
		String formula = "TExt like 1";
		try {
			new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
			fail("IllegalArgumentException was excepted");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Expected exception: ", "Invalid value types for string infix operator: like", ex.getMessage());
		}
	}
	
	public void testBracketsFormula_Negative() {
		String formula = "((TExt = 1)";
		try {
			new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
			fail("IllegalArgumentException was excepted");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Expected exception: ", "Too many opening brackets", ex.getMessage());
		}
	}
	
	public void testListFormula_Negative() {
		String formula = "TExt in ('abc', 1, 'ghi')";
		try {
			new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
			fail("IllegalArgumentException was excepted");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Expected exception: ", "Illegal list value: 1", ex.getMessage());
		}
	}
	
	public void testFullFormulaBeanShell_Negative() {
		String formula = "Email not like '%teXt%' and Email != 'abc@soderer.de' and Email is not null OR TExt = 'ein Text, als ''Zitat''' AND (Nummer <= 2 OR Nummer = 3) anD Geburtstag = to_Date('23.01.77', 'dd.mm.yy') oR Mod(Nummer, 3) = 2 or TExt in ('abc', '1', 'ghi')";
		RulePart rule = new ReducedSqlWhereClauseParser().parse(formula, getDefaultFields());
		try {
			rule.toString(RulePart.StringType.BeanShell);
			fail("IllegalArgumentException was excepted");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Expected exception: ", "No beanshell representation available for function: not like", ex.getMessage());
		}
	}
	
	private Map<String, Value.Type> getDefaultFields() {
		Map<String, Value.Type> fieldList = new HashMap<String, Value.Type>();
		fieldList.put("email", Value.Type.String);
		fieldList.put("text", Value.Type.String);
		fieldList.put("nummer", Value.Type.Number);
		fieldList.put("geburtstag", Value.Type.Date);
		return fieldList;
	}
}
