package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CsvTest {

	@Test
	public void test1() {
		CsvReader reader = null;
		try {
			String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\r\n;\"\"56\"";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|", dataLines.get(1).get(0));
			Assert.assertEquals("jkl", dataLines.get(1).get(1));
			Assert.assertEquals("4\n\n;\"56", dataLines.get(1).get(2));
		} catch (Exception e) {
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
			String csvData = "abc,d\"ef,123";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ',', null);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("d\"ef", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (Exception e) {
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
			String csvData = "abc;def;123\nabc;def";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', null);
			reader.setFillMissingTrailingColumnsWithNull(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals(null, dataLines.get(1).get(2));
		} catch (Exception e) {
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
			String csvData = "abc;def;123\nabc;def";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', null);
			reader.setFillMissingTrailingColumnsWithNull(true);
			reader.readAll();
		} catch (Exception e) {
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
			String csvData = "abc;def;123\nabc;def";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', null);
			reader.readAll();
			Assert.fail("Exception expected");
		} catch (Exception e) {
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
			String csvData = "abc;def;123\nabc;def\n";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', null);
			reader.setFillMissingTrailingColumnsWithNull(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("def", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
			Assert.assertEquals("abc", dataLines.get(1).get(0));
			Assert.assertEquals("def", dataLines.get(1).get(1));
			Assert.assertEquals(null, dataLines.get(1).get(2));
		} catch (Exception e) {
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
			String csvData = "123;\"ab\\\"c\";456\n";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', '\"');
			reader.setStringQuoteEscapeCharacter('\\');
			reader.setFillMissingTrailingColumnsWithNull(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("123", dataLines.get(0).get(0));
			Assert.assertEquals("ab\"c", dataLines.get(0).get(1));
			Assert.assertEquals("456", dataLines.get(0).get(2));
		} catch (Exception e) {
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
			String csvData = "123; \"abc\" ;456\n";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';', '\"');
			reader.setStringQuoteEscapeCharacter('\\');
			reader.setFillMissingTrailingColumnsWithNull(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("123", dataLines.get(0).get(0));
			Assert.assertEquals("abc", dataLines.get(0).get(1));
			Assert.assertEquals("456", dataLines.get(0).get(2));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}
	
	@Test
	public void testCsvWriter1() {
		CsvWriter writer = null;
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer = new CsvWriter(byteArrayOutputStream, ';', '\"');
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
			String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\n;\"\"56\"\n";
			writer.close();
			writer = null;
			Assert.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	@Test
	public void testCsvWriter2() {
		CsvWriter writer = null;
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			writer = new CsvWriter(byteArrayOutputStream, ';', '\"');
			writer.setStringQuoteEscapeCharacter('\\');
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
			String csvData = "abc;def;123\n\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\\\"´`^°²³*#.,:=+-~_|\";jkl;\"4\n\n;\\\"56\"\n";
			writer.close();
			writer = null;
			Assert.assertEquals(csvData, new String(byteArrayOutputStream.toByteArray(), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	@Test
	public void testWhitespaceOnlyValue() {
		CsvReader reader = null;
		try {
			String csvData = "abc; ;123";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals(" ", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (Exception e) {
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
			String csvData = "abc; ;123";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			reader.setAlwaysTrim(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (Exception e) {
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
			String csvData = "abc; \" \" ;123";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			reader.setAlwaysTrim(true);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals("", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (Exception e) {
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
			String csvData = "abc; \" \" ;123";
			reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals("abc", dataLines.get(0).get(0));
			Assert.assertEquals(" ", dataLines.get(0).get(1));
			Assert.assertEquals("123", dataLines.get(0).get(2));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}
}
