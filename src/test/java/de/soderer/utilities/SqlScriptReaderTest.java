package de.soderer.utilities;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;

public class SqlScriptReaderTest {
	@Test
	public void test1() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data =
				"insert into test_tbl (column_integer, column_varchar) VALUES (1, '');\n"
				+ "insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ');\n"
				+ ";\n"
				+ "insert into test_tbl (column_integer, column_varchar) VALUES (3, 'abc'); -- Comment;;\n"
				+ "insert into test_tbl (column_integer, column_varchar) VALUES (4, /* Comment ;; */ 'abc');";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			String nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (3, 'abc')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (4,  'abc')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertNull(nextStatement);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
	
	@Test
	public void test2() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '')";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			String nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertNull(nextStatement);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
	
	@Test
	public void test3() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '') -- Comment";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			String nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertNull(nextStatement);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
	@Test
	public void test4() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data =
				"insert into test_tbl (column_integer, column_varchar) VALUES (1, '');\n"
				+ "\n"
				+ " -- Comment \n"
				+ "\n"
				+ " /* Comment */ \n"
				+ "\n"
				+ "insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ');\n";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			String nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			Assert.assertNull(nextStatement);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
	
	@Test
	public void testError1() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, ')";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			sqlScriptReader.readNextStatement();
			Assert.fail("Missing expected exception");
		} catch (Exception e) {
			Assert.assertEquals("Unclosed sql string", e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}

	@Test
	public void testError2() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '') /* Comment";
			input = new ByteArrayInputStream(data.getBytes("UTF-8"));
			sqlScriptReader = new SqlScriptReader(input, "UTF-8");
			sqlScriptReader.readNextStatement();
			Assert.fail("Missing expected exception");
		} catch (Exception e) {
			Assert.assertEquals("Unclosed multiline comment", e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
}
