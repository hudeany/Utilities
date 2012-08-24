package de.soderer.utilities;

import java.io.ByteArrayInputStream;
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
}
