package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TextTableTest {
	@SuppressWarnings("static-method")
	@Test
	public void constructor_withColumnNames() throws Exception {
		final TextTable table = new TextTable("A", "B");

		assertEquals(2, table.getColumnCount());
		assertEquals(1, table.getHeaderLineCount());
		assertEquals(0, table.getDataLineCount());
		assertFalse(table.hasData());
		assertEquals("A", table.getHeader(0, 0));
		assertEquals("B", table.getHeader(0, 1));
		assertNull(table.getHeaderColor(0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addColumn_nullNameIsStoredAsEmpty() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn(null);

		assertEquals("", table.getHeader(0, 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addColumn_secondHeaderLineIsPaddedForAllColumns() throws Exception {
		final TextTable table = new TextTable("A", "B");
		table.addColumn("C", "c2", false);
		table.addColumn("D");

		assertEquals(2, table.getHeaderLineCount());
		assertEquals("", table.getHeader(1, 0));
		assertEquals("", table.getHeader(1, 1));
		assertEquals("c2", table.getHeader(1, 2));
		assertEquals("", table.getHeader(1, 3));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addColumn_withColor() {
		final TextTable table = new TextTable();
		table.addColumn("A", null, true, "YELLOW");

		assertEquals("YELLOW", table.getHeaderColor(0));
		assertEquals(1, table.getHeaderLineCount());
	}

	@SuppressWarnings("static-method")
	@Test
	public void addColumn_existingLinesGetEmptyValue() throws Exception {
		final TextTable table = new TextTable("A");
		table.startNewLine();
		table.addValueToCurrentLine("1");
		table.addColumn("B");

		assertEquals("1", table.getData(0, 0));
		assertEquals("", table.getData(0, 1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void startNewLine_valuesAreEmptyInitially() throws Exception {
		final TextTable table = new TextTable("A", "B");
		table.startNewLine("red");

		assertTrue(table.hasData());
		assertEquals(1, table.getDataLineCount());
		assertEquals("", table.getData(0, 0));
		assertEquals("", table.getData(0, 1));
		assertEquals("red", table.getLineColor(0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addValueToCurrentLine_fillsColumnsInOrder() throws Exception {
		final TextTable table = new TextTable("A", "B", "C");
		table.startNewLine();
		table.addValueToCurrentLine("x");
		table.addValueToCurrentLine(42);
		table.addValueToCurrentLine((String) null);

		assertEquals("x", table.getData(0, 0));
		assertEquals("42", table.getData(0, 1));
		assertEquals("", table.getData(0, 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addValueToCurrentLine_withoutLine() {
		final TextTable table = new TextTable("A");

		assertThrows(Exception.class, () -> table.addValueToCurrentLine("x"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addValueToCurrentLine_tooManyValues() throws Exception {
		final TextTable table = new TextTable("A");
		table.startNewLine();
		table.addValueToCurrentLine("x");

		assertThrows(Exception.class, () -> table.addValueToCurrentLine("y"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void dropColumn_removesHeaderValuesAndColor() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn("A", "a2", false, "RED");
		table.addColumn("B", "b2", true, "BLUE");
		table.startNewLine();
		table.addValueToCurrentLine("1");
		table.addValueToCurrentLine("2");

		table.dropColumn(0);

		assertEquals(1, table.getColumnCount());
		assertEquals("B", table.getHeader(0, 0));
		assertEquals("b2", table.getHeader(1, 0));
		assertEquals("BLUE", table.getHeaderColor(0));
		assertEquals("2", table.getData(0, 0));
		assertTrue(table.columnRightAligned.get(0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void dropColumn_nextValueStaysInSameColumn() throws Exception {
		final TextTable table = new TextTable("A", "B", "C");
		table.startNewLine();
		table.addValueToCurrentLine("a");
		table.addValueToCurrentLine("b");

		table.dropColumn(0);
		table.addValueToCurrentLine("c");

		assertEquals("b", table.getData(0, 0));
		assertEquals("c", table.getData(0, 1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void dropColumn_invalidIndex() {
		final TextTable table = new TextTable("A");

		assertThrows(IndexOutOfBoundsException.class, () -> table.dropColumn(1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void setAlignmentForColumn_invalidIndex() {
		final TextTable table = new TextTable("A");

		assertThrows(Exception.class, () -> table.setAlignmentForColumn(-1, true));
		assertThrows(Exception.class, () -> table.setAlignmentForColumn(1, true));
	}

	@SuppressWarnings("static-method")
	@Test
	public void setNumericColumnsRightALigned() throws Exception {
		final TextTable table = new TextTable("Name", "Count", "Mixed");
		table.startNewLine();
		table.addValueToCurrentLine("a");
		table.addValueToCurrentLine("1");
		table.addValueToCurrentLine("2");
		table.startNewLine();
		table.addValueToCurrentLine("b");
		table.addValueToCurrentLine("");
		table.addValueToCurrentLine("x");

		table.setNumericColumnsRightALigned();

		assertEquals(List.of(false, true, false), table.columnRightAligned);
	}

	@SuppressWarnings("static-method")
	@Test
	public void toString_alignsColumns() throws Exception {
		final TextTable table = new TextTable("Name", "Age");
		table.setAlignmentForColumn(1, true);
		table.startNewLine();
		table.addValueToCurrentLine("Anna");
		table.addValueToCurrentLine("34");
		table.startNewLine();
		table.addValueToCurrentLine("Bob");
		table.addValueToCurrentLine("5");

		assertEquals(""
				+ "Name Age\n"
				+ "--------\n"
				+ "Anna  34\n"
				+ "Bob    5\n", table.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toString_withSecondHeaderLine() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn("Value", "EUR", true);
		table.startNewLine();
		table.addValueToCurrentLine("7");

		assertEquals(""
				+ "Value\n"
				+ "EUR  \n"
				+ "-----\n"
				+ "    7\n", table.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toString_withoutData() {
		final TextTable table = new TextTable("A", "BB");

		assertEquals("A BB\n----\n", table.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toString_withEmptyColumn() throws Exception {
		// A column without any text has width 0
		final TextTable table = new TextTable("A", "");
		table.startNewLine();
		table.addValueToCurrentLine("x");

		assertEquals("A \n--\nx \n", table.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toCsvString_quotesValues() throws Exception {
		final TextTable table = new TextTable("A", "B");
		table.startNewLine();
		table.addValueToCurrentLine("x");
		table.addValueToCurrentLine("y\"z");

		assertEquals("\"A\";\"B\";\n\"x\";\"y\"\"z\";\n", table.toCsvString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toCsvString_withoutData() {
		assertEquals("\"A\";\n", new TextTable("A").toCsvString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void toCsvStringWithMetaData() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn("A", "a2", true, "YELLOW");
		table.addColumn("B");
		table.startNewLine("red");
		table.addValueToCurrentLine("1");
		// Leading apostrophe (Excel text marker) is removed
		table.addValueToCurrentLine("'x");
		table.startNewLine();
		table.addValueToCurrentLine("2");
		// Value enclosed in apostrophes is kept
		table.addValueToCurrentLine("'y'");

		assertEquals(""
				+ "\"HEADER\";\"A\";\"B\";\n"
				+ "\"HEADER\";\"a2\";\"\";\n"
				+ "\"COLOR\";\"YELLOW\";\"null\";\n"
				+ "\"RIGHTALIGN\";\"true\";\"false\";\n"
				+ "\"red\";\"1\";\"x\";\n"
				+ "\"null\";\"2\";\"'y'\";\n", table.toCsvStringWithMetaData());
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_roundTrip() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn("A;1", "a\"2", true, "YELLOW");
		table.addColumn("B");
		table.startNewLine("red");
		table.addValueToCurrentLine("multi\nline");
		table.addValueToCurrentLine("semi;colon \"quoted\"");
		table.startNewLine();
		table.addValueToCurrentLine("");
		table.addValueToCurrentLine("ä");

		final TextTable loaded = new TextTable();
		loaded.loadCsvWithMetadataFile(table.toCsvStringWithMetaData().getBytes(StandardCharsets.UTF_8));

		assertEquals(2, loaded.getHeaderLineCount());
		assertEquals(2, loaded.getColumnCount());
		assertEquals(2, loaded.getDataLineCount());
		assertEquals("A;1", loaded.getHeader(0, 0));
		assertEquals("a\"2", loaded.getHeader(1, 0));
		assertEquals("", loaded.getHeader(1, 1));
		assertEquals("YELLOW", loaded.getHeaderColor(0));
		assertNull(loaded.getHeaderColor(1));
		assertEquals(List.of(true, false), loaded.columnRightAligned);
		assertEquals("red", loaded.getLineColor(0));
		assertNull(loaded.getLineColor(1));
		assertEquals("multi\nline", loaded.getData(0, 0));
		assertEquals("semi;colon \"quoted\"", loaded.getData(0, 1));
		assertEquals("", loaded.getData(1, 0));
		assertEquals("ä", loaded.getData(1, 1));
		assertEquals(table.toString(), loaded.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_newLinesAreAppended() throws Exception {
		final TextTable table = new TextTable("A");
		table.startNewLine();
		table.addValueToCurrentLine("1");

		final TextTable loaded = new TextTable();
		loaded.loadCsvWithMetadataFile(table.toCsvStringWithMetaData().getBytes(StandardCharsets.UTF_8));

		// Loaded lines are complete
		assertThrows(Exception.class, () -> loaded.addValueToCurrentLine("x"));

		loaded.startNewLine();
		loaded.addValueToCurrentLine("2");
		assertEquals(2, loaded.getDataLineCount());
		assertEquals("1", loaded.getData(0, 0));
		assertEquals("2", loaded.getData(1, 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_withBomAndCrLf() throws Exception {
		final String csv = "﻿\"HEADER\";\"A\";\r\n\"COLOR\";\"null\";\r\n\"RIGHTALIGN\";\"false\";\r\n\"null\";\"x\";\r\n";

		final TextTable loaded = new TextTable();
		loaded.loadCsvWithMetadataFile(csv.getBytes(StandardCharsets.UTF_8));

		assertEquals("A", loaded.getHeader(0, 0));
		assertEquals(1, loaded.getDataLineCount());
		assertEquals("x", loaded.getData(0, 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_normalizesIncompleteData() throws Exception {
		// No metadata lines, a data line longer than the header and three header lines
		final String csv = ""
				+ "\"HEADER\";\"A\";\n"
				+ "\"HEADER\";\"a2\";\n"
				+ "\"HEADER\";\"a3\";\n"
				+ "\n"
				+ "\"null\";\"1\";\"2\";\n";

		final TextTable loaded = new TextTable();
		loaded.loadCsvWithMetadataFile(csv.getBytes(StandardCharsets.UTF_8));

		assertEquals(2, loaded.getHeaderLineCount());
		assertEquals(2, loaded.getColumnCount());
		assertEquals("", loaded.getHeader(0, 1));
		assertEquals("", loaded.getHeader(1, 1));
		assertNull(loaded.getHeaderColor(1));
		assertEquals(List.of(false, false), loaded.columnRightAligned);
		assertEquals("2", loaded.getData(0, 1));
		// Rendering must work with the normalized data
		assertEquals("A   \na2  \n----\n1  2\n", loaded.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_emptyData() throws Exception {
		final TextTable loaded = new TextTable("A");
		loaded.startNewLine();
		loaded.loadCsvWithMetadataFile(new byte[0]);

		assertEquals(1, loaded.getHeaderLineCount());
		assertEquals(0, loaded.getColumnCount());
		assertEquals(0, loaded.getDataLineCount());
		assertFalse(loaded.hasData());
	}

	@SuppressWarnings("static-method")
	@Test
	public void loadCsvWithMetadataFile_null() {
		assertThrows(IOException.class, () -> new TextTable().loadCsvWithMetadataFile(null));
	}

	@SuppressWarnings("static-method")
	@Test
	public void toHtmlString() throws Exception {
		final TextTable table = new TextTable();
		table.addColumn("First Name", "2nd", false, "LIGHT_GREEN");
		table.addColumn("A-B");
		table.startNewLine();
		table.addValueToCurrentLine("<b>");
		table.addValueToCurrentLine("'007");
		table.startNewLine();
		table.addValueToCurrentLine("x y");
		table.addValueToCurrentLine("ä");
		table.startNewLine("#ff0000");
		table.addValueToCurrentLine("1");
		table.addValueToCurrentLine("2");

		final String html = table.toHtmlString("A & B");

		assertTrue(html.startsWith("A &amp; B<br><br>\n<table "), html);
		assertFalse(html.contains("<STYLE"), html);
		assertTrue(html.contains("<tr align='center'><td bgcolor='lime'>First&nbsp;Name<br/>2nd</td><td>A&#8209;B<br/></td></tr>\n"), html);
		assertTrue(html.contains("<tr align='right' bgcolor='cornflowerblue'><td>&lt;b&gt;</td><td>007</td></tr>\n"), html);
		assertTrue(html.contains("<tr align='right' bgcolor='lightblue'><td>x&nbsp;y</td><td>&auml;</td></tr>\n"), html);
		assertTrue(html.contains("<tr align='right' bgcolor='#ff0000'><td>1</td><td>2</td></tr>\n"), html);
		assertTrue(html.endsWith("</table>\n<br>\n<br>\n"), html);
	}

	@SuppressWarnings("static-method")
	@Test
	public void toHtmlString_withFontSize() {
		final TextTable table = new TextTable("A");

		assertTrue(table.toHtmlString("T", 10).contains("font-size: 10pt;"));
		assertFalse(table.toHtmlString("T", 0).contains("font-size"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getHtmlHeader_withoutTime() {
		assertEquals(""
				+ "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
				+ "<html>\n"
				+ "<head>\n<title>T &amp; U</title>\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "<h2>T &amp; U</h2>\n", TextTable.getHtmlHeader("T & U", false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getHtmlHeader_withTimeAndStyleSheet() {
		final String header = TextTable.getHtmlHeader("T", true, "<style>td{}</style>");

		assertTrue(header.contains("</title>\n<style>td{}</style></head>\n"), header);
		assertTrue(header.contains("<h4>Stand: "), header);
		assertTrue(TextTable.getHtmlHeader("T").contains("<h4>Stand: "));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getHtmlFooter() {
		assertEquals("</body>\n</html>\n", TextTable.getHtmlFooter());
	}

	@SuppressWarnings("static-method")
	@Test
	public void convertXlsColor2HtmlColor() {
		assertEquals("yellow", TextTable.convertXlsColor2HtmlColor("VERY_LIGHT_YELLOW"));
		assertEquals("lime", TextTable.convertXlsColor2HtmlColor("light_green"));
		assertEquals("fuchsia", TextTable.convertXlsColor2HtmlColor("LAVENDER"));
		assertEquals("fuchsia", TextTable.convertXlsColor2HtmlColor("TAN"));
		assertEquals("aqua", TextTable.convertXlsColor2HtmlColor("TURQUOISE"));
		assertEquals("yellow", TextTable.convertXlsColor2HtmlColor("YELLOW"));
		assertEquals("aqua", TextTable.convertXlsColor2HtmlColor("LIGHT_TURQUOISE"));
		assertEquals("red", TextTable.convertXlsColor2HtmlColor("LIGHT_ORANGE"));
		assertEquals("fuchsia", TextTable.convertXlsColor2HtmlColor("ROSE"));
		assertEquals("coral", TextTable.convertXlsColor2HtmlColor("Wochenende"));
		assertEquals("white", TextTable.convertXlsColor2HtmlColor("UNKNOWN"));
		assertEquals("white", TextTable.convertXlsColor2HtmlColor(null));
	}

	@SuppressWarnings("static-method")
	@Test
	public void htmlEncodeString() {
		assertEquals("", TextTable.htmlEncodeString(null));
		assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&#39;", TextTable.htmlEncodeString("<a href=\"x\">&'"));
		assertEquals("&Auml;&auml;&Ouml;&ouml;&Uuml;&uuml;&szlig;", TextTable.htmlEncodeString("ÄäÖöÜüß"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void keepOnlyLastRows() throws Exception {
		final TextTable table = createTableWithThreeColoredLines();

		table.keepOnlyLastRows(2);

		assertEquals(2, table.getDataLineCount());
		assertEquals("1", table.getData(0, 0));
		assertEquals("c1", table.getLineColor(0));
		assertEquals("2", table.getData(1, 0));
		assertEquals("c2", table.getLineColor(1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void keepOnlyFirstRows_thenAddLine() throws Exception {
		final TextTable table = createTableWithThreeColoredLines();

		table.keepOnlyFirstRows(1);
		table.startNewLine("new");
		table.addValueToCurrentLine("x");

		assertEquals(2, table.getDataLineCount());
		assertEquals("0", table.getData(0, 0));
		assertEquals("c0", table.getLineColor(0));
		assertEquals("x", table.getData(1, 0));
		assertEquals("new", table.getLineColor(1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void keepOnlyRows_negativeRemovesAll() throws Exception {
		final TextTable table1 = createTableWithThreeColoredLines();
		table1.keepOnlyLastRows(-1);
		assertEquals(0, table1.getDataLineCount());
		assertFalse(table1.hasData());

		final TextTable table2 = createTableWithThreeColoredLines();
		table2.keepOnlyFirstRows(0);
		assertEquals(0, table2.getDataLineCount());
		assertFalse(table2.hasData());
	}

	@SuppressWarnings("static-method")
	@Test
	public void getHeaderAndData_invalidIndexes() {
		final TextTable table = new TextTable("A");

		assertThrows(Exception.class, () -> table.getHeader(1, 0));
		assertThrows(Exception.class, () -> table.getHeader(0, 1));
		assertThrows(Exception.class, () -> table.getData(0, 0));
	}

	private static TextTable createTableWithThreeColoredLines() throws Exception {
		final TextTable table = new TextTable("A");
		for (int i = 0; i < 3; i++) {
			table.startNewLine("c" + i);
			table.addValueToCurrentLine(i);
		}
		return table;
	}
}
