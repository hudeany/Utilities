package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConfigurationProperties}. The system property "user.home" is redirected to a temporary directory
 * for each test, so the real home directory is never touched.
 */
@SuppressWarnings("static-method")
public class ConfigurationPropertiesTest {
	private static final String APPLICATION_NAME = "ConfigurationPropertiesTest";

	private interface HomeAction {
		void run(Path home) throws Exception;
	}

	/**
	 * Runs the action with "user.home" pointing to a new temporary directory, which is deleted afterwards.
	 */
	private static void withTemporaryHome(final HomeAction action) throws Exception {
		final String originalUserHome = System.getProperty("user.home");
		final Path home = Files.createTempDirectory("ConfigurationPropertiesTest");
		try {
			System.setProperty("user.home", home.toString());
			action.run(home);
		} finally {
			System.setProperty("user.home", originalUserHome);
			deleteRecursively(home);
		}
	}

	private static void deleteRecursively(final Path path) throws Exception {
		if (Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
			try (Stream<Path> children = Files.list(path)) {
				for (final Path child : (Iterable<Path>) children::iterator) {
					deleteRecursively(child);
				}
			}
		}
		Files.deleteIfExists(path);
	}

	private static Path getConfigFile(final Path home) {
		return home.resolve("." + APPLICATION_NAME + ".config");
	}

	/**
	 * Writes a configuration file as an older application version would have, using java.util.Properties directly.
	 */
	private static void writeConfigFile(final Path home, final String... keysAndValues) throws Exception {
		final Properties properties = new Properties();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			properties.setProperty(keysAndValues[i], keysAndValues[i + 1]);
		}
		try (OutputStream outputStream = Files.newOutputStream(getConfigFile(home))) {
			properties.store(outputStream, null);
		}
	}

	@Test
	public void testNewConfiguration() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertNull(configuration.get("missing"));
			assertFalse(configuration.containsKey("missing"));
			assertEquals(Arrays.asList(ConfigurationProperties.CONFIG_KEY_FORMAT_VERSION), configuration.getKeyList());
			// Nothing is written before save
			assertFalse(Files.exists(getConfigFile(home)));
		});
	}

	@Test
	public void testSaveAndLoad() throws Exception {
		withTemporaryHome(home -> {
			final LocalDateTime date = LocalDateTime.of(2026, 10, 22, 20, 15, 30);
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("String", "value with \u00E4\u20AC and \\backslash\\");
			configuration.set("Integer", 42);
			configuration.set("Character", ';');
			configuration.set("Boolean", true);
			configuration.set("Date", date);
			configuration.set("List", Arrays.asList("a", "b;c", "C:\\new"));
			configuration.saveOrThrow();
			assertTrue(Files.exists(getConfigFile(home)));

			final ConfigurationProperties reloaded = new ConfigurationProperties(APPLICATION_NAME);
			assertEquals("value with \u00E4\u20AC and \\backslash\\", reloaded.get("String"));
			assertEquals(42, reloaded.getInteger("Integer"));
			assertEquals(Character.valueOf(';'), reloaded.getCharacter("Character"));
			assertTrue(reloaded.getBoolean("Boolean"));
			assertEquals(date, reloaded.getDate("Date"));
			assertEquals(Arrays.asList("a", "b;c", "C:\\new"), reloaded.getList("List"));
		});
	}

	@Test
	public void testSubDirectory() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME, true);
			configuration.set("key", "value");
			configuration.saveOrThrow();
			final Path configFile = home.resolve("." + APPLICATION_NAME).resolve("." + APPLICATION_NAME + ".config");
			assertTrue(Files.exists(configFile));
			assertFalse(Files.exists(getConfigFile(home)));
			assertEquals("value", new ConfigurationProperties(APPLICATION_NAME, true).get("key"));
			// The configuration without sub directory is a different one
			assertNull(new ConfigurationProperties(APPLICATION_NAME, false).get("key"));
		});
	}

	@Test
	public void testSaveIsAtomicAndPrivate() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("password", "secret");
			configuration.save();
			configuration.set("password", "changed");
			configuration.save();

			// No temporary files are left behind
			try (Stream<Path> files = Files.list(home)) {
				assertEquals(Arrays.asList(getConfigFile(home).getFileName().toString()), files.map(path -> path.getFileName().toString()).toList());
			}
			if (home.getFileSystem().supportedFileAttributeViews().contains("posix")) {
				assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(getConfigFile(home))));
			}
			assertEquals("changed", new ConfigurationProperties(APPLICATION_NAME).get("password"));
		});
	}

	@Test
	public void testSaveKeepsSymbolicLink() throws Exception {
		withTemporaryHome(home -> {
			final Path target = home.resolve("real-config-file");
			Files.write(target, new byte[0]);
			try {
				Files.createSymbolicLink(getConfigFile(home), target);
			} catch (@SuppressWarnings("unused") final UnsupportedOperationException | java.io.IOException e) {
				// Symbolic links are not available (e.g. Windows without permission), nothing to test
				return;
			}
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("key", "value");
			configuration.saveOrThrow();
			assertTrue(Files.isSymbolicLink(getConfigFile(home)));
			assertTrue(new String(Files.readAllBytes(target), StandardCharsets.ISO_8859_1).contains("key=value"));
		});
	}

	@Test
	public void testInvalidConfigFile() throws Exception {
		withTemporaryHome(home -> {
			Files.write(getConfigFile(home), "key=\\uZZZZ".getBytes(StandardCharsets.ISO_8859_1));
			assertThrows(Exception.class, () -> new ConfigurationProperties(APPLICATION_NAME));
		});
	}

	@Test
	public void testTypedValues() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertEquals(0, configuration.getInteger("missing"));
			configuration.set("int", " 42 ");
			assertEquals(42, configuration.getInteger("int"));
			configuration.set("int", "  ");
			assertEquals(0, configuration.getInteger("int"));
			configuration.set("int", "x");
			assertThrows(NumberFormatException.class, () -> configuration.getInteger("int"));

			assertNull(configuration.getCharacter("missing"));
			configuration.set("char", "abc");
			assertEquals(Character.valueOf('a'), configuration.getCharacter("char"));
			configuration.set("char", "");
			assertNull(configuration.getCharacter("char"));

			configuration.set("nullString", (String) null);
			assertEquals("", configuration.get("nullString"));
		});
	}

	@Test
	public void testBooleanValues() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertFalse(configuration.getBoolean("missing"));
			for (final String trueValue : new String[] { "true", "TRUE", " yes ", "on", "ja", "+" }) {
				configuration.set("bool", trueValue);
				assertTrue(configuration.getBoolean("bool"), trueValue);
			}
			for (final String falseValue : new String[] { "false", "no", "", "0", "anything" }) {
				configuration.set("bool", falseValue);
				assertFalse(configuration.getBoolean("bool"), falseValue);
			}
			configuration.set("bool", false);
			assertEquals("false", configuration.get("bool"));
		});
	}

	@Test
	public void testDateValues() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			final LocalDateTime date = LocalDateTime.of(2000, 1, 2, 3, 4, 5);
			configuration.set("date", date);
			assertEquals(date, configuration.getDate("date"));
			assertNull(configuration.getDate("missing"));
			configuration.set("date", "no date");
			assertNull(configuration.getDate("date"));
			configuration.set("date", (LocalDateTime) null);
			assertEquals("", configuration.get("date"));
			assertNull(configuration.getDate("date"));
		});
	}

	@Test
	public void testListValues() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			final List<String> values = Arrays.asList("plain", "with;separator", "with \"quotes\"", "line\nbreak", "C:\\new\\dir", " spaces ", "", "\u00E4\u20AC");
			configuration.set("list", values);
			assertEquals(values, configuration.getList("list"));
			// Marked as list in the current format
			assertTrue(ConfigurationProperties.parseListValue(configuration.get(ConfigurationProperties.CONFIG_KEY_PLAIN_LIST_KEYS), false).contains("list"));

			configuration.saveOrThrow();
			assertEquals(values, new ConfigurationProperties(APPLICATION_NAME).getList("list"));
		});
	}

	@Test
	public void testEmptyAndSpecialLists() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertTrue(configuration.getList("missing").isEmpty());

			configuration.set("empty", new ArrayList<>());
			assertTrue(configuration.getList("empty").isEmpty());

			configuration.set("nullList", (List<String>) null);
			assertTrue(configuration.getList("nullList").isEmpty());

			// A single empty value can be told apart from an empty list
			configuration.set("singleEmpty", Arrays.asList(""));
			assertEquals(Arrays.asList(""), configuration.getList("singleEmpty"));

			configuration.set("nullValues", Arrays.asList("a", null));
			assertEquals(Arrays.asList("a", ""), configuration.getList("nullValues"));

			// A new modifiable list is returned every time
			final List<String> list = configuration.getList("nullValues");
			list.add("x");
			assertEquals(2, configuration.getList("nullValues").size());
		});
	}

	@Test
	public void testUnreadableListValue() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("broken", "\"unclosed");
			assertTrue(configuration.getList("broken").isEmpty());
		});
	}

	@Test
	public void testFormatListValue() {
		assertEquals("a;b", ConfigurationProperties.formatListValue(Arrays.asList("a", "b")));
		assertEquals("", ConfigurationProperties.formatListValue(Collections.emptyList()));
		assertEquals("\"\"", ConfigurationProperties.formatListValue(Arrays.asList("")));
		assertEquals("\"\"", ConfigurationProperties.formatListValue(Arrays.asList((String) null)));
		assertEquals(";", ConfigurationProperties.formatListValue(Arrays.asList("", "")));
		assertEquals("\"a;b\";\"q\"\"q\";\"l\nb\";\"c\rr\";C:\\x", ConfigurationProperties.formatListValue(Arrays.asList("a;b", "q\"q", "l\nb", "c\rr", "C:\\x")));
	}

	@Test
	public void testParseListValue() throws Exception {
		assertEquals(Arrays.asList("a", "b"), ConfigurationProperties.parseListValue("a;b", false));
		assertEquals(Arrays.asList("a", "", "b", ""), ConfigurationProperties.parseListValue("a;;b;", false));
		assertEquals(Arrays.asList("", ""), ConfigurationProperties.parseListValue(";", false));
		assertEquals(Arrays.asList(""), ConfigurationProperties.parseListValue("\"\"", false));
		// Unquoted values are not trimmed, whitespace around quoted values is ignored
		assertEquals(Arrays.asList("a", " b "), ConfigurationProperties.parseListValue(" \"a\" ; b ", false));
		assertEquals(Arrays.asList("q\"q", "x;y"), ConfigurationProperties.parseListValue("\"q\"\"q\";\"x;y\"", false));
		assertEquals(Arrays.asList("line\r\nbreak"), ConfigurationProperties.parseListValue("\"line\r\nbreak\"", false));
		// Backslashes are no escape characters in the current format
		assertEquals(Arrays.asList("C:\\new"), ConfigurationProperties.parseListValue("C:\\new", false));
		// A leading byte order mark and a trailing line break are ignored
		assertEquals(Arrays.asList("a", "b"), ConfigurationProperties.parseListValue("\uFEFFa;b\n", false));
	}

	@Test
	public void testParseListValueErrors() {
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("", false));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\"unclosed", false));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\"a\"x", false));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\"a\" \"b\"", false));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("a\nb", false));
	}

	@Test
	public void testFormatAndParseRoundTrip() throws Exception {
		final List<List<String>> lists = Arrays.asList(
				Arrays.asList("a"),
				Arrays.asList(""),
				Arrays.asList("", ""),
				Arrays.asList("a", ""),
				Arrays.asList("", "a"),
				Arrays.asList(" ", "\t"),
				Arrays.asList("\"", ";", "\"\"", "\n", "\r\n"),
				Arrays.asList("C:\\temp\\new", "\\\\server\\share"));
		for (final List<String> list : lists) {
			assertEquals(list, ConfigurationProperties.parseListValue(ConfigurationProperties.formatListValue(list), false), list.toString());
		}
	}

	@Test
	public void testParseLegacyListValue() throws Exception {
		assertEquals(Arrays.asList("a\tb", "c\\d"), ConfigurationProperties.parseListValue("a\\tb;c\\\\d", true));
		assertEquals(Arrays.asList("AB" + new String(Character.toChars(0x1F600))), ConfigurationProperties.parseListValue("\\x41\\u0042\\U0001F600", true));
		// The escaped CR is normalized to LF as well
		assertEquals(Arrays.asList("x y\n\n'\"\b\f"), ConfigurationProperties.parseListValue("x\\ y\\n\\r\\'\\\"\\b\\f", true));
		// Line breaks are normalized to LF in legacy values
		assertEquals(Arrays.asList("a\nb"), ConfigurationProperties.parseListValue("\"a\r\nb\"", true));

		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("a\\q", true));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("a\\", true));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\\x4", true));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\\u00G1", true));
		assertThrows(Exception.class, () -> ConfigurationProperties.parseListValue("\\U00110000", true));
	}

	@Test
	public void testMigrationOfLegacyLists() throws Exception {
		withTemporaryHome(home -> {
			// File of an older version: no format version, list with backslash escape sequences
			writeConfigFile(home, "Legacy", "a\\tb;c", "NoBackslash", "x;y", "InvalidEscape", "C:\\path");
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertTrue(configuration.containsKey(ConfigurationProperties.CONFIG_KEY_FORMAT_VERSION));

			assertEquals(Arrays.asList("a\tb", "c"), configuration.getList("Legacy"));
			// Migrated to the current format
			assertEquals("a\tb;c", configuration.get("Legacy"));
			assertEquals(Arrays.asList("a\tb", "c"), configuration.getList("Legacy"));

			assertEquals(Arrays.asList("x", "y"), configuration.getList("NoBackslash"));
			// Not readable as legacy value, so it is read as current value
			assertEquals(Arrays.asList("C:\\path"), configuration.getList("InvalidEscape"));

			configuration.saveOrThrow();
			final ConfigurationProperties reloaded = new ConfigurationProperties(APPLICATION_NAME);
			assertEquals(Arrays.asList("a\tb", "c"), reloaded.getList("Legacy"));
			assertEquals(Arrays.asList("C:\\path"), reloaded.getList("InvalidEscape"));
		});
	}

	@Test
	public void testCurrentFormatIsNeverReadAsLegacy() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("Paths", Arrays.asList("C:\\new", "D:\\temp"));
			configuration.saveOrThrow();
			// "\n" and "\t" of the paths must not become line break and tab
			assertEquals(Arrays.asList("C:\\new", "D:\\temp"), new ConfigurationProperties(APPLICATION_NAME).getList("Paths"));
		});
	}

	@Test
	public void testOldFileWithPlainListKeys() throws Exception {
		withTemporaryHome(home -> {
			// Written by a version that already migrated "Paths", but did not know the format version key
			writeConfigFile(home, "Paths", "C:\\new", "Old", "a\\nb", ConfigurationProperties.CONFIG_KEY_PLAIN_LIST_KEYS, "Paths");
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			assertEquals(Arrays.asList("C:\\new"), configuration.getList("Paths"));
			assertEquals(Arrays.asList("a\nb"), configuration.getList("Old"));
		});
	}

	@Test
	public void testRecentList() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.ensureRecentList("Recent", 3);
			assertEquals("", configuration.get("Recent"));
			assertEquals(3, configuration.getInteger("Recent" + ConfigurationProperties.MAXSIZE_EXTENSION));
			assertNull(configuration.getLatestRecentValue("Recent"));

			configuration.addRecentListEntry("Recent", "a");
			configuration.addRecentListEntry("Recent", "b");
			configuration.addRecentListEntry("Recent", "c");
			configuration.addRecentListEntry("Recent", "d");
			assertEquals(Arrays.asList("b", "c", "d"), configuration.getList("Recent"));
			// An existing value is moved to the end
			configuration.addRecentListEntry("Recent", "b");
			assertEquals(Arrays.asList("c", "d", "b"), configuration.getList("Recent"));
			assertEquals("b", configuration.getLatestRecentValue("Recent"));

			// An existing maximum size is not changed
			configuration.ensureRecentList("Recent", 5);
			assertEquals(3, configuration.getInteger("Recent" + ConfigurationProperties.MAXSIZE_EXTENSION));
		});
	}

	@Test
	public void testRecentListWithInvalidMaxSize() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			for (final String invalidMaxSize : new String[] { "x", "0", "-1" }) {
				configuration.set("Recent" + ConfigurationProperties.MAXSIZE_EXTENSION, invalidMaxSize);
				configuration.set("Recent", new ArrayList<>());
				for (int i = 0; i < 15; i++) {
					configuration.addRecentListEntry("Recent", "value" + i);
				}
				assertEquals(ConfigurationProperties.DEFAULT_RECENT_LIST_MAX_SIZE, configuration.getList("Recent").size(), invalidMaxSize);
				assertEquals("value14", configuration.getLatestRecentValue("Recent"));
			}
		});
	}

	@Test
	public void testListEntries() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.addListEntry("List", "a");
			configuration.addListEntry("List", "b");
			configuration.addListEntry("List", "a");
			assertEquals(Arrays.asList("a", "b", "a"), configuration.getList("List"));

			assertTrue(configuration.removeFromList("List", "a"));
			assertEquals(Arrays.asList("b", "a"), configuration.getList("List"));
			assertFalse(configuration.removeFromList("List", "x"));

			configuration.addSetEntry("Set", "a");
			configuration.addSetEntry("Set", "b");
			configuration.addSetEntry("Set", "a");
			assertEquals(Arrays.asList("a", "b"), configuration.getList("Set"));
		});
	}

	@Test
	public void testKeys() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.set("b", "2");
			configuration.set("a", Arrays.asList("1"));
			final List<String> keys = configuration.getKeyList();
			assertEquals(keys.stream().sorted().toList(), keys);
			assertTrue(keys.containsAll(Arrays.asList("a", "b", ConfigurationProperties.CONFIG_KEY_PLAIN_LIST_KEYS)));

			configuration.removeKey("a");
			assertFalse(configuration.containsKey("a"));
			// Also removed from the internal list of list keys, which was the only one
			assertEquals("", configuration.get(ConfigurationProperties.CONFIG_KEY_PLAIN_LIST_KEYS));
			configuration.removeKey("missing");

			assertTrue(ConfigurationProperties.isInternalKey(ConfigurationProperties.CONFIG_KEY_FORMAT_VERSION));
			assertTrue(ConfigurationProperties.isInternalKey(ConfigurationProperties.CONFIG_KEY_PLAIN_LIST_KEYS));
			assertTrue(ConfigurationProperties.isInternalKey(ConfigurationProperties.CONFIG_KEY_LEGACY_LIST_KEYS));
			assertFalse(ConfigurationProperties.isInternalKey(ConfigurationProperties.CONFIG_KEY_LANGUAGE));
			assertFalse(ConfigurationProperties.isInternalKey("b"));
		});
	}

	@Test
	public void testEnsure() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.ensure("empty");
			assertEquals("", configuration.get("empty"));
			configuration.ensure("flag", true);
			assertTrue(configuration.getBoolean("flag"));

			// Existing values are not changed
			configuration.set("existing", "value");
			configuration.ensure("existing");
			configuration.ensure("existing", true);
			assertEquals("value", configuration.get("existing"));
		});
	}

	@Test
	public void testSetupDefaultConfig() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			configuration.setupDefaultConfig();
			assertNotEmpty(configuration.getLanguage());
			// Nothing obsolete was removed, so nothing was saved
			assertFalse(Files.exists(getConfigFile(home)));

			configuration.set(ConfigurationProperties.CONFIG_KEY_LANGUAGE, "xx");
			configuration.set(ConfigurationProperties.CONFIG_KEY_LANGUAGE + ConfigurationProperties.ENUM_EXTENSION, "de,en");
			configuration.set("TlsServerCertificateCheck", "true");
			configuration.setupDefaultConfig();
			assertEquals("xx", configuration.getLanguage());
			assertFalse(configuration.containsKey(ConfigurationProperties.CONFIG_KEY_LANGUAGE + ConfigurationProperties.ENUM_EXTENSION));
			assertFalse(configuration.containsKey("TlsServerCertificateCheck"));
			// Saved because obsolete keys were removed
			assertFalse(new ConfigurationProperties(APPLICATION_NAME).containsKey("TlsServerCertificateCheck"));
			assertTrue(Files.exists(getConfigFile(home)));
		});
	}

	private static void assertNotEmpty(final String value) {
		assertTrue(value != null && !value.isEmpty(), "Expected a non empty value");
	}

	@Test
	public void testYesNoAsk() throws Exception {
		withTemporaryHome(home -> {
			final ConfigurationProperties configuration = new ConfigurationProperties(APPLICATION_NAME);
			for (final YesNoAsk value : YesNoAsk.values()) {
				configuration.set("choice", value);
				assertEquals(value, configuration.getYesNoAsk("choice", null), value.name());
			}
			assertTrue(configuration.isYesNoAskKey("choice"));

			final YesNoAsk defaultValue = YesNoAsk.values()[YesNoAsk.values().length - 1];
			assertEquals(defaultValue, configuration.getYesNoAsk("missing", defaultValue));
			assertFalse(configuration.isYesNoAskKey("missing"));

			configuration.ensureYesNoAsk("ensured", defaultValue);
			assertTrue(configuration.isYesNoAskKey("ensured"));
			assertEquals(defaultValue, configuration.getYesNoAsk("ensured", null));
			// An existing value is not changed
			configuration.ensureYesNoAsk("ensured", YesNoAsk.values()[0]);
			assertEquals(defaultValue, configuration.getYesNoAsk("ensured", null));

			assertThrows(IllegalArgumentException.class, () -> configuration.ensureYesNoAsk("invalid", null));
		});
	}
}
