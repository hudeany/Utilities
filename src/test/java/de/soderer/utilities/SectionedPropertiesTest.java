package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SectionedPropertiesTest {
	private static SectionedProperties load(final String text) throws Exception {
		return load(text, true);
	}

	private static SectionedProperties load(final String text, final boolean caseInsensitive) throws Exception {
		final SectionedProperties properties = new SectionedProperties(caseInsensitive);
		properties.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
		return properties;
	}

	private static String save(final SectionedProperties properties) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		properties.save(outputStream);
		return outputStream.toString(StandardCharsets.UTF_8);
	}

	private static SectionedProperties roundTrip(final SectionedProperties properties) throws Exception {
		return load(save(properties));
	}

	/**
	 * Input stream that remembers whether it was closed.
	 */
	private static class CloseTrackingInputStream extends ByteArrayInputStream {
		boolean closed = false;

		CloseTrackingInputStream(final byte[] data) {
			super(data);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	/**
	 * Output stream that remembers whether it was closed.
	 */
	private static class CloseTrackingOutputStream extends ByteArrayOutputStream {
		boolean closed = false;

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	@Test
	public void testLoad() throws Exception {
		final SectionedProperties properties = load("# comment\n"
				+ "global = g\n"
				+ "\n"
				+ "[First]\n"
				+ "  key1 =  value 1  \n"
				+ "key2=a=b\n"
				+ "keyWithoutValue\n"
				+ "   # indented comment\n"
				+ "[ Second Section ]\n"
				+ "key1 = other\n"
				+ "url = http://host/#anchor\n");

		assertEquals("g", properties.getValue(null, "global"));
		assertEquals("value 1", properties.getValue("First", "key1"));
		// Only the first '=' separates key and value
		assertEquals("a=b", properties.getValue("First", "key2"));
		assertEquals("", properties.getValue("First", "keyWithoutValue"));
		assertEquals("other", properties.getValue("Second Section", "key1"));
		// '#' within a value is no comment
		assertEquals("http://host/#anchor", properties.getValue("Second Section", "url"));

		assertNull(properties.getValue("First", "missing"));
		assertNull(properties.getValue("Missing", "key1"));
		assertNull(properties.getValue("First", null));
	}

	@Test
	public void testCaseInsensitiveByDefault() throws Exception {
		final SectionedProperties properties = load("[Section]\nKey = v\n");
		assertEquals("v", properties.getValue("SECTION", "key"));
		assertEquals("v", properties.getValue(" section ", " KEY "));
	}

	@Test
	public void testCaseInsensitiveKeepsSpellingWhenSaving() throws Exception {
		final SectionedProperties properties = load("GlobalKey = g\n[Database]\nUserName = admin\nPassWord = secret\n");
		assertEquals("GlobalKey=g\n\n[Database]\nUserName=admin\nPassWord=secret\n\n", save(properties));
		// Saving and loading again keeps the spelling
		assertEquals("GlobalKey=g\n\n[Database]\nUserName=admin\nPassWord=secret\n\n", save(roundTrip(properties)));
		assertEquals("admin", properties.getValue("DATABASE", "username"));
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testCaseInsensitiveNamesHaveOriginalSpelling() throws Exception {
		final SectionedProperties properties = load("GlobalKey = g\n[Database]\nUserName = admin\n");
		assertEquals(Arrays.asList(null, "Database"), new ArrayList<>(properties.getSectionNames()));
		assertEquals(Arrays.asList("UserName"), new ArrayList<>(properties.getSectionKeys("DATABASE")));
		assertEquals(Arrays.asList("GlobalKey"), new ArrayList<>(properties.getSectionKeys(null)));
		// Lookups in the sets ignore the case
		assertTrue(properties.getSectionNames().contains("database"));
		assertTrue(properties.getSectionNames().contains(null));
		assertTrue(properties.getSectionKeys("Database").contains("USERNAME"));
		assertFalse(properties.getSectionKeys("Database").contains("missing"));
		assertFalse(properties.getSectionKeys("Database").contains(Integer.valueOf(1)));
	}

	@Test
	public void testCaseInsensitiveFirstSpellingWins() throws Exception {
		// Repeated sections and keys in other spellings keep the first spelling, the last value wins
		final SectionedProperties loaded = load("[Db]\nKey = 1\n[DB]\nKEY = 2\nother = 3\n");
		assertEquals("[Db]\nKey=2\nother=3\n\n", save(loaded));

		final SectionedProperties properties = new SectionedProperties();
		properties.setValue("MySection", "MyKey", "1");
		properties.setValue(" MYSECTION ", "mykey", "2");
		properties.setValue("mysection", "SecondKey", "3");
		assertEquals("[MySection]\nMyKey=2\nSecondKey=3\n\n", save(properties));
		assertEquals(Arrays.asList("MySection"), new ArrayList<>(properties.getSectionNames()));

		// Values set after loading keep the loaded spelling
		loaded.setValue("db", "key", "4");
		assertEquals("[Db]\nKey=4\nother=3\n\n", save(loaded));
	}

	@Test
	public void testCaseInsensitiveRemoveViaLiveSets() throws Exception {
		final SectionedProperties properties = load("[Database]\nUserName = admin\nPassWord = secret\n[Other]\nx = 1\n");
		// Removal is case insensitive too
		assertTrue(properties.getSectionKeys("database").remove("USERNAME"));
		assertFalse(properties.getSectionKeys("database").remove("USERNAME"));
		assertNull(properties.getValue("Database", "UserName"));
		assertEquals("[Database]\nPassWord=secret\n\n[Other]\nx=1\n\n", save(properties));

		// A removed key or section set again gets its new spelling
		properties.setValue("Database", "USERNAME", "root");
		assertTrue(properties.getSectionNames().remove("OTHER"));
		properties.setValue("OTHER", "X", "2");
		assertEquals("[Database]\nPassWord=secret\nUSERNAME=root\n\n[OTHER]\nX=2\n\n", save(properties));

		// Removal by iterator
		final Iterator<String> iterator = properties.getSectionNames().iterator();
		assertEquals("Database", iterator.next());
		iterator.remove();
		assertEquals(Arrays.asList("OTHER"), new ArrayList<>(properties.getSectionNames()));
		assertEquals(1, properties.getSectionNames().size());
	}

	@Test
	public void testCaseSensitive() throws Exception {
		final SectionedProperties properties = load("[Section]\nKey = upper\nkey = lower\n", false);
		assertEquals("upper", properties.getValue("Section", "Key"));
		assertEquals("lower", properties.getValue("Section", "key"));
		assertNull(properties.getValue("section", "Key"));
		assertNull(properties.getValue("Section", "KEY"));

		// The original case is kept when saving
		final String saved = save(properties);
		assertTrue(saved.contains("[Section]\n"), saved);
		assertTrue(saved.contains("Key=upper\n"), saved);
	}

	@Test
	public void testDuplicateKeysLastWins() throws Exception {
		final SectionedProperties properties = load("[s]\nk = 1\nk = 2\n[s]\nk2 = 3\n");
		assertEquals("2", properties.getValue("s", "k"));
		// A repeated section continues the existing one
		assertEquals("3", properties.getValue("s", "k2"));
		assertEquals(1, properties.getSectionNames().size());
	}

	@Test
	public void testSectionAndKeyNames() throws Exception {
		final SectionedProperties properties = load("a = 1\n[s1]\nb = 2\nc = 3\n[empty]\n");
		assertEquals(Arrays.asList(null, "s1", "empty"), new ArrayList<>(properties.getSectionNames()));
		assertEquals(Arrays.asList("a"), new ArrayList<>(properties.getSectionKeys(null)));
		assertEquals(Arrays.asList("b", "c"), new ArrayList<>(properties.getSectionKeys("s1")));
		// Sections without keys are kept
		assertTrue(properties.getSectionKeys("empty").isEmpty());
		assertNull(properties.getSectionKeys("missing"));

		final SectionedProperties empty = new SectionedProperties();
		assertNull(empty.getSectionNames());
		assertNull(empty.getSectionKeys(null));
		assertNull(empty.getValue(null, "a"));
	}

	@Test
	public void testByteOrderMarkIsIgnored() throws Exception {
		assertEquals("v", load("\uFEFFkey = v\n").getValue(null, "key"));
		assertEquals("v", load("\uFEFF[section]\nkey = v\n").getValue("section", "key"));
		assertEquals(0, load("\uFEFF# comment\n").getSectionNames().size());
	}

	@Test
	public void testLineBreaksInFile() throws Exception {
		final SectionedProperties properties = load("a = 1\r\n[s]\r\nb = 2\rc = 3");
		assertEquals("1", properties.getValue(null, "a"));
		assertEquals("2", properties.getValue("s", "b"));
		assertEquals("3", properties.getValue("s", "c"));
	}

	@Test
	public void testInvalidLines() throws Exception {
		final Exception invalidSection = assertThrows(Exception.class, () -> load("a = 1\n[section\n"));
		assertTrue(invalidSection.getMessage().contains("(2)"), invalidSection.getMessage());
		final Exception emptyKey = assertThrows(Exception.class, () -> load("a = 1\n = value\n"));
		assertEquals("Invalid key at line 2", emptyKey.getMessage());
		assertThrows(IllegalArgumentException.class, () -> new SectionedProperties().load(null));
	}

	@Test
	public void testFailedLoadKeepsCurrentEntries() throws Exception {
		final SectionedProperties properties = load("a = 1\n");
		assertThrows(Exception.class, () -> properties.load(new ByteArrayInputStream("[broken\n".getBytes(StandardCharsets.UTF_8))));
		assertEquals("1", properties.getValue(null, "a"));
	}

	@Test
	public void testLoadReplacesEntries() throws Exception {
		final SectionedProperties properties = load("a = 1\n");
		properties.load(new ByteArrayInputStream("b = 2\n".getBytes(StandardCharsets.UTF_8)));
		assertNull(properties.getValue(null, "a"));
		assertEquals("2", properties.getValue(null, "b"));
	}

	@Test
	public void testSaveFormat() throws Exception {
		final SectionedProperties properties = new SectionedProperties(false);
		properties.setValue("Section", "key", "value");
		properties.setValue(null, "global", "g");
		properties.setValue("Section", "nullValue", null);
		// Keys without section are written first, so they are not read into a section
		assertEquals("global=g\n\n[Section]\nkey=value\nnullValue=\n\n", save(properties));
	}

	@Test
	public void testSaveWithoutEntries() throws Exception {
		assertEquals("", save(new SectionedProperties()));
		assertThrows(IllegalArgumentException.class, () -> new SectionedProperties().save(null));
	}

	@Test
	public void testStreamsAreNotClosed() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream("a = 1\n".getBytes(StandardCharsets.UTF_8));
		final SectionedProperties properties = new SectionedProperties();
		properties.load(inputStream);
		assertFalse(inputStream.closed);

		final CloseTrackingOutputStream outputStream = new CloseTrackingOutputStream();
		properties.save(outputStream);
		assertFalse(outputStream.closed);
		assertEquals("a=1\n\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testRoundTripOfValues() throws Exception {
		final String[] values = {
				"simple",
				"with spaces inside",
				"C:\\new\\dir\\",
				"line1\nline2",
				"line1\r\nline2\rline3",
				"literal \\n is no line break",
				"two backslashes \\\\n",
				"backslash before line break \\\nnext",
				"\nleading and trailing line breaks\n",
				"unicode \u00E4\u00F6\u00FC \u20AC \u4E2D",
				"a = b # c [d]",
				"",
		};
		final SectionedProperties properties = new SectionedProperties(false);
		for (int i = 0; i < values.length; i++) {
			properties.setValue("s", "key" + i, values[i]);
		}
		final SectionedProperties reloaded = load(save(properties), false);
		for (int i = 0; i < values.length; i++) {
			final String expected = values[i].replace("\r\n", "\n").replace('\r', '\n');
			assertEquals(expected, reloaded.getValue("s", "key" + i), "key" + i);
		}
	}

	@Test
	public void testValuesAreTrimmed() throws Exception {
		final SectionedProperties properties = new SectionedProperties();
		properties.setValue(null, "k", "  value  ");
		assertEquals("value", roundTrip(properties).getValue(null, "k"));
	}

	@Test
	public void testOldFilesWithSingleBackslashes() throws Exception {
		// Files of older versions did not escape backslashes, these values are read as before
		final SectionedProperties properties = load("path = C:\\temp\\dir\nbreak = a\\nb\n");
		assertEquals("C:\\temp\\dir", properties.getValue(null, "path"));
		assertEquals("a\nb", properties.getValue(null, "break"));
	}

	@Test
	public void testSetValueNormalizesNames() throws Exception {
		final SectionedProperties properties = new SectionedProperties(false);
		properties.setValue(" Section ", " key ", "v");
		assertEquals("v", properties.getValue("Section", "key"));
		assertEquals("v", properties.getValue(" Section", "key "));
		assertEquals("v", roundTrip(properties).getValue("section", "key"));
	}

	@Test
	public void testSetValueRejectsInvalidNames() {
		final SectionedProperties properties = new SectionedProperties();
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", null, "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "  ", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "a=b", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "a\nb", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "a\rb", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "#comment", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("s", "[section]", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("a\nb", "key", "v"));
		assertThrows(IllegalArgumentException.class, () -> properties.setValue("a\rb", "key", "v"));
		// Nothing was stored
		assertNull(properties.getSectionNames());
	}

	@Test
	public void testEmptySectionNameDiffersFromNoSection() throws Exception {
		final SectionedProperties properties = new SectionedProperties();
		properties.setValue(null, "k", "none");
		properties.setValue("", "k", "empty");
		final SectionedProperties reloaded = roundTrip(properties);
		assertEquals("none", reloaded.getValue(null, "k"));
		assertEquals("empty", reloaded.getValue("", "k"));
	}
}
