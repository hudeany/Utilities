package de.soderer.utilities.csv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CsvTest {

	@Test
	public void test1() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\r\n;\"\"56\"";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';');

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|", dataLines.get(1).get(0));
			Assert.assertEquals("jkl", dataLines.get(1).get(1));
			Assert.assertEquals("4\n\n;\"56", dataLines.get(1).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test2() {
		CsvReader reader = null;
		try {
			final String csvData = "abc,d\"ef,123";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(',')
					.setStringQuote(null);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("d\"ef", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test3() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;123\nabc;def";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote(null)
					.setFillMissingTrailingColumnsWithNull(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals(null, dataLines.get(1).get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test5() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;123\nabc;def";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote(null)
					.setFillMissingTrailingColumnsWithNull(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			reader.readAll();
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test5_Error() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;123\nabc;def";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote(null);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			reader.readAll();
			Assert.fail("Exception expected");
		} catch (final Exception e) {
			// Exception expected
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test4() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;123\nabc;def\n";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote(null)
					.setFillMissingTrailingColumnsWithNull(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals(null, dataLines.get(1).get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test6() {
		CsvReader reader = null;
		try {
			final String csvData = "123;\"ab\\\"c\";456\n";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote('\"')
					.setStringQuoteEscapeCharacter('\\')
					.setFillMissingTrailingColumnsWithNull(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("123", dataLines.get(0).get(0));
			Assert.assertEquals("ab\"c", dataLines.get(0).get(1));
			Assert.assertEquals("456", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void test7() {
		CsvReader reader = null;
		try {
			final String csvData = "123; \"abc\" ;456\n";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setStringQuote('\"')
					.setStringQuoteEscapeCharacter('\\')
					.setFillMissingTrailingColumnsWithNull(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("123", dataLines.get(0).get(0));
			Assert.assertEquals("abc", dataLines.get(0).get(1));
			Assert.assertEquals("456", dataLines.get(0).get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testCsvWriter1() {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (CsvWriter writer = new CsvWriter(byteArrayOutputStream, new CsvFormat().setSeparator(';').setStringQuote('\"'))) {
				writer.writeValues(new Object[] {
						"abc",
						"def",
						"123"
				});
				writer.writeValues(new Object[] {
						"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|",
						"jkl",
						"4\n\n;\"56"
				});
			}
			final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\\n\\n;\"\"56\"\n";
			Assert.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testCsvWriter2() {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try (CsvWriter writer = new CsvWriter(byteArrayOutputStream, new CsvFormat().setSeparator(';').setStringQuote('\"').setStringQuoteEscapeCharacter('\\'))) {
				writer.writeValues(new Object[] {
						"abc",
						"def",
						"123"
				});
				writer.writeValues(new Object[] {
						"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|",
						"jkl",
						"4\n\n;\"56"
				});
			}
			final String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°²³*#.,:=+-~_|\";jkl;\"4\\n\\n;\\\"56\"\n";
			Assert.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testWhitespaceOnlyValue() {
		CsvReader reader = null;
		try {
			final String csvData = "abc; ;123";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';');

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals(" ", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testWhitespaceOnlyValueTrimed() {
		CsvReader reader = null;
		try {
			final String csvData = "abc; ;123";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setAlwaysTrim(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testWhitespaceOnlyValueTrimedQuoted() {
		CsvReader reader = null;
		try {
			final String csvData = "abc; \" \" ;123";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';')
					.setAlwaysTrim(true);

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testWhitespaceOnlyValueQuoted() {
		CsvReader reader = null;
		try {
			final String csvData = "abc; \" \" ;123";

			final CsvFormat csvFormat = new CsvFormat()
					.setSeparator(';');

			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), csvFormat);

			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals(" ", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testSpecialQuotation() {
		try {
			final List<String> dataLine = CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "\"abc\\\"123\"");
			Assert.assertEquals("abc\"123", dataLine.get(0));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testQuotationError1() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "abc\\\"123");
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			Assert.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError2() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "\"abc\\\"\"123");
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			Assert.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError3() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('\\'), "abc\"");
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			Assert.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testQuotationError4() {
		try {
			CsvReader.parseCsvLine(new CsvFormat().setSeparator(';').setStringQuote('"').setStringQuoteEscapeCharacter('"'), "abc\\\"123");
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			Assert.assertTrue(e.getMessage().contains("line 1"));
		}
	}

	@Test
	public void testIgnoreEmptyRows() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;1\nabc;def;2\n\n;;\nabc;def;3\n \nabc;def;4\n";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), new CsvFormat().setSeparator(';').setIgnoreEmptyLines(true));
			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals(dataLines.size(), 4);
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("1", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals("2", dataLines.get(1).get(2));
			Assert.assertEquals("abc", dataLines.get(2).get(0));
			Assert.assertEquals("def", dataLines.get(2).get(1));
			Assert.assertEquals("3", dataLines.get(2).get(2));
			Assert.assertEquals("abc", dataLines.get(3).get(0));
			Assert.assertEquals("def", dataLines.get(3).get(1));
			Assert.assertEquals("4", dataLines.get(3).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	public void testDontIgnoreEmptyRows() {
		CsvReader reader = null;
		try {
			final String csvData = "abc;def;1\nabc;def;2\n;;\nabc;def;3\nabc;def;4\n";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)), new CsvFormat().setSeparator(';').setIgnoreEmptyLines(false));
			final List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals(dataLines.size(), 5);
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("1", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals("2", dataLines.get(1).get(2));
			Assert.assertEquals("", dataLines.get(2).get(0));
			Assert.assertEquals("", dataLines.get(2).get(1));
			Assert.assertEquals("", dataLines.get(2).get(2));
			Assert.assertEquals("abc", dataLines.get(3).get(0));
			Assert.assertEquals("def", dataLines.get(3).get(1));
			Assert.assertEquals("3", dataLines.get(3).get(2));
			Assert.assertEquals("abc", dataLines.get(4).get(0));
			Assert.assertEquals("def", dataLines.get(4).get(1));
			Assert.assertEquals("4", dataLines.get(4).get(2));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}
}
