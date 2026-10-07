package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SqlScriptReaderTest {
	@Test
	public void test1() {
		ByteArrayInputStream input = null;
		SqlScriptReader sqlScriptReader = null;
		try {
			final String data =
					"insert into test_tbl (column_integer, column_varchar) VALUES (1, '');\n"
							+ "insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ');\n"
							+ ";\n"
							+ "insert into test_tbl (column_integer, column_varchar) VALUES (3, 'abc'); -- Comment;;\n"
							+ "insert into test_tbl (column_integer, column_varchar) VALUES (4, /* Comment ;; */ 'abc');";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			String nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (3, 'abc')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (4,  'abc')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertNull(nextStatement);
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
			final String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '')";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			String nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertNull(nextStatement);
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
			final String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '') -- Comment";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			String nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertNull(nextStatement);
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
			final String data =
					"insert into test_tbl (column_integer, column_varchar) VALUES (1, '');\n"
							+ "\n"
							+ " -- Comment \n"
							+ "\n"
							+ " /* Comment */ \n"
							+ "\n"
							+ "insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ');\n";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			String nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (1, '')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertEquals("insert into test_tbl (column_integer, column_varchar) VALUES (2, ' abc; -- abc /* Comment */ '' ')", nextStatement);
			nextStatement = sqlScriptReader.readNextStatement();
			assertNull(nextStatement);
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
			final String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, ')";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			sqlScriptReader.readNextStatement();
			fail("Missing expected exception");
		} catch (final Exception e) {
			assertEquals("Unclosed sql string", e.getMessage());
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
			final String data = "insert into test_tbl (column_integer, column_varchar) VALUES (1, '') /* Comment";
			input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			sqlScriptReader = new SqlScriptReader(input, StandardCharsets.UTF_8);
			sqlScriptReader.readNextStatement();
			fail("Missing expected exception");
		} catch (final Exception e) {
			assertEquals("Unclosed multiline comment", e.getMessage());
		} finally {
			Utilities.closeQuietly(input);
			Utilities.closeQuietly(sqlScriptReader);
		}
	}
}
