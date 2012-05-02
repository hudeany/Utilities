package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CsvTest {

	@Test
	public void test1() {
		try {
			String csvData = "abc;def;123\nabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"\"´`^°²³*#.,:=+-~_|;jkl;\"4\n\r\n;\"\"56\"";
			CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ';');
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals(dataLines.get(0).get(0), "abc");
			Assert.assertEquals(dataLines.get(0).get(1), "def");
			Assert.assertEquals(dataLines.get(0).get(2), "123");
			Assert.assertEquals(dataLines.get(1).get(0), "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,:=+-~_|");
			Assert.assertEquals(dataLines.get(1).get(1), "jkl");
			Assert.assertEquals(dataLines.get(1).get(2), "4\n\n;\"56");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test2() {
		try {
			String csvData = "abc,d\"ef,123";
			CsvReader reader = new CsvReader(new ByteArrayInputStream(csvData.getBytes("UTF-8")), ',', null);
			List<List<String>> dataLines = reader.readAll();
			Assert.assertEquals(dataLines.get(0).get(0), "abc");
			Assert.assertEquals(dataLines.get(0).get(1), "d\"ef");
			Assert.assertEquals(dataLines.get(0).get(2), "123");
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
