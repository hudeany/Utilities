package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class PropertiesWriterTest {
	/** Emoji as surrogate pair (U+1F600) */
	private static final String EMOJI = "\uD83D\uDE00";

	/** Values and keys covering all escaping rules */
	private static Map<String, String> getCriticalProperties() {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("plain", "value");
		properties.put("umlauts", "Grüße aus München: ÄÖÜäöüß");
		properties.put("euro", "Preis: 5 €");
		properties.put("emoji", "Smile " + EMOJI);
		properties.put("leadingBlank", " leading");
		properties.put("trailingBlank", "trailing ");
		properties.put("backslash", "C:\\temp\\file");
		properties.put("lineBreaks", "line1\nline2\r\nline3\rline4");
		properties.put("tabAndFormfeed", "a\tb\fc");
		properties.put("controlChars", "a\u0001b\u007Fc");
		properties.put("byteOrderMark", "\uFEFFtext");
		properties.put("unpairedHighSurrogate", "x\uD800y");
		properties.put("unpairedLowSurrogate", "x\uDC00y");
		properties.put("separatorsInValue", "=a:b#c!d\"e");
		properties.put("empty", "");
		properties.put("key with blanks", "v");
		properties.put("key=with:separators#and!comment", "v");
		properties.put("schlüssel€", "v");
		return properties;
	}

	private static byte[] write(final boolean storeAsUtf8, final Map<String, String> properties) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, storeAsUtf8)) {
			propertiesWriter.writeProperties(properties);
		}
		return outputStream.toByteArray();
	}

	private static void assertAllContained(final Map<String, String> expected, final Properties loaded) {
		assertEquals(expected.size(), loaded.size());
		for (final Entry<String, String> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), loaded.getProperty(entry.getKey()), "Value of key '" + entry.getKey() + "'");
		}
	}

	// ---------------------------------------------------------------------
	// ISO-8859-1 mode (default)
	// ---------------------------------------------------------------------

	@Test
	public void testIsoDefaultOutput() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream)) {
			assertFalse(propertiesWriter.isStoreAsUtf8());

			propertiesWriter.writeProperty("key1", "value 1");
			propertiesWriter.writeComment("Comment for key2\nComment line2 for key2");
			propertiesWriter.writeProperty("key2", "Valü 2");
			propertiesWriter.writeProperty("key3", "value31\nvalue32");
			propertiesWriter.writeProperty("key4", "value 4");
			propertiesWriter.writeProperty("key5", " value 5 ");
		}

		final String expected = "key1=value 1\n"
				+ "# Comment for key2\n"
				+ "# Comment line2 for key2\n"
				+ "key2=Val\\u00FC 2\n"
				+ "key3=value31\\nvalue32\n"
				+ "key4=value 4\n"
				+ "key5=\\ value 5 \n";
		assertEquals(expected, new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testIsoOutputIsPureAscii() throws Exception {
		final byte[] data = write(false, getCriticalProperties());
		for (final byte b : data) {
			assertTrue(b >= 0, "Non-ASCII byte found in ISO-8859-1 output");
		}
	}

	@Test
	public void testIsoEscapesNonAsciiInKeysAndValues() throws Exception {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("schlüssel", "€ " + EMOJI);
		final String data = new String(write(false, properties), StandardCharsets.ISO_8859_1);
		assertEquals("schl\\u00FCssel=\\u20AC \\uD83D\\uDE00\n", data);
	}

	@Test
	public void testIsoCommentKeepsLatin1AndEscapesOthers() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, false)) {
			propertiesWriter.writeComment("Kommentar ü €");
		}
		assertEquals("# Kommentar ü \\u20AC\n", new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testIsoRoundTripWithJavaProperties() throws Exception {
		final Map<String, String> expected = getCriticalProperties();
		final Properties loaded = new Properties();
		loaded.load(new ByteArrayInputStream(write(false, expected)));
		assertAllContained(expected, loaded);
	}

	// ---------------------------------------------------------------------
	// UTF-8 mode
	// ---------------------------------------------------------------------

	@Test
	public void testUtf8Output() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			assertTrue(propertiesWriter.isStoreAsUtf8());

			propertiesWriter.writeProperty("key1", "value 1");
			propertiesWriter.writeComment("Kommentar für key2 €\nZeile 2 " + EMOJI);
			propertiesWriter.writeProperty("key2", "Valü 2");
			propertiesWriter.writeProperty("key3", "value31\nvalue32");
			propertiesWriter.writeProperty("schlüssel", "€ " + EMOJI);
			propertiesWriter.writeProperty("key5", " value 5 ");
		}

		final String expected = "key1=value 1\n"
				+ "# Kommentar für key2 €\n"
				+ "# Zeile 2 " + EMOJI + "\n"
				+ "key2=Valü 2\n"
				+ "key3=value31\\nvalue32\n"
				+ "schlüssel=€ " + EMOJI + "\n"
				+ "key5=\\ value 5 \n";
		assertEquals(expected, new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testUtf8HasNoByteOrderMark() throws Exception {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("ä", "ö");
		final byte[] data = write(true, properties);
		assertFalse(data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF, "UTF-8 output must not start with a BOM");
		assertEquals("ä=ö\n", new String(data, StandardCharsets.UTF_8));
	}

	@Test
	public void testUtf8StillEscapesControlCharsBomAndUnpairedSurrogates() throws Exception {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("control", "a\u0001b\u007Fc");
		properties.put("bom", "\uFEFFtext");
		properties.put("high", "x\uD800y");
		properties.put("low", "x\uDC00y");
		properties.put("highAtEnd", "x\uD800");
		final String data = new String(write(true, properties), StandardCharsets.UTF_8);
		assertEquals("control=a\\u0001b\\u007Fc\n"
				+ "bom=\\uFEFFtext\n"
				+ "high=x\\uD800y\n"
				+ "low=x\\uDC00y\n"
				+ "highAtEnd=x\\uD800\n", data);
	}

	@Test
	public void testUtf8CommentEscapesBomAndUnpairedSurrogatesOnly() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			propertiesWriter.writeComment("\uFEFFü\uD800");
		}
		assertEquals("# \\uFEFFü\\uD800\n", new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testUtf8RoundTripWithJavaPropertiesReader() throws Exception {
		final Map<String, String> expected = getCriticalProperties();
		final Properties loaded = new Properties();
		try (Reader reader = new InputStreamReader(new ByteArrayInputStream(write(true, expected)), StandardCharsets.UTF_8)) {
			loaded.load(reader);
		}
		assertAllContained(expected, loaded);
	}

	@Test
	public void testUtf8RoundTripWithPropertyResourceBundle() throws Exception {
		final Map<String, String> expected = getCriticalProperties();
		// PropertyResourceBundle reads UTF-8 by default since Java 9
		final PropertyResourceBundle bundle = new PropertyResourceBundle(new ByteArrayInputStream(write(true, expected)));
		assertEquals(expected.size(), bundle.keySet().size());
		for (final Entry<String, String> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), bundle.getString(entry.getKey()), "Value of key '" + entry.getKey() + "'");
		}
	}

	// ---------------------------------------------------------------------
	// Mode independent behavior
	// ---------------------------------------------------------------------

	@Test
	public void testKeyEscaping() {
		assertEquals("a\\ b\\=c\\:d\\#e\\!f", PropertiesWriter.escapeKey("a b=c:d#e!f"));
		assertEquals("schl\\u00FCssel", PropertiesWriter.escapeKey("schlüssel"));
		assertEquals("schlüssel", PropertiesWriter.escapeKey("schlüssel", false));
	}

	@Test
	public void testValueEscaping() {
		assertEquals("", PropertiesWriter.escapeValue(null));
		assertEquals("\\ a b ", PropertiesWriter.escapeValue(" a b "));
		assertEquals("=a:b#c!d\"e", PropertiesWriter.escapeValue("=a:b#c!d\"e"));
		assertEquals("a\\\\b\\r\\nc\\td\\fe", PropertiesWriter.escapeValue("a\\b\r\nc\td\fe"));
		assertEquals("Gr\\u00FC\\u00DFe", PropertiesWriter.escapeValue("Grüße"));
		assertEquals("Grüße", PropertiesWriter.escapeValue("Grüße", false));
	}

	@Test
	public void testNullValueIsWrittenAsEmpty() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			propertiesWriter.writeProperty("key", null);
		}
		assertEquals("key=\n", new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testBlankSeparatorEscapesLeadingSeparatorInValue() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			propertiesWriter.withKeyValueSeparator(" ");
			propertiesWriter.writeProperty("key1", "=value");
			propertiesWriter.writeProperty("key2", ":value");
			propertiesWriter.writeProperty("key3", "value");
		}
		final byte[] data = outputStream.toByteArray();
		assertEquals("key1 \\=value\nkey2 \\:value\nkey3 value\n", new String(data, StandardCharsets.UTF_8));

		final Properties loaded = new Properties();
		loaded.load(new ByteArrayInputStream(data));
		assertEquals("=value", loaded.getProperty("key1"));
		assertEquals(":value", loaded.getProperty("key2"));
	}

	@Test
	public void testInvalidKeyValueSeparator() {
		try (final PropertiesWriter propertiesWriter = new PropertiesWriter(new ByteArrayOutputStream())) {
			assertThrows(IllegalArgumentException.class, () -> propertiesWriter.setKeyValueSeparator(null));
			assertThrows(IllegalArgumentException.class, () -> propertiesWriter.setKeyValueSeparator("->"));
			assertThrows(IllegalArgumentException.class, () -> propertiesWriter.setKeyValueSeparator("=\n"));
		}
	}

	@Test
	public void testEmptyKeyIsRejected() {
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(new ByteArrayOutputStream(), true)) {
			assertThrows(IllegalArgumentException.class, () -> propertiesWriter.writeProperty(null, "value"));
			assertThrows(IllegalArgumentException.class, () -> propertiesWriter.writeProperty("", "value"));
			assertEquals(0, propertiesWriter.getWrittenProperties());
		}
	}

	@Test
	public void testWriteAfterCloseIsRejected() {
		try (final PropertiesWriter propertiesWriter = new PropertiesWriter(new ByteArrayOutputStream(), true)) {
			propertiesWriter.close();
			assertThrows(IllegalStateException.class, () -> propertiesWriter.writeProperty("key", "value"));
			assertThrows(IllegalStateException.class, () -> propertiesWriter.writeComment("comment"));
			assertThrows(IllegalStateException.class, propertiesWriter::writeEmptyLine);
			// Closing twice must not fail
			propertiesWriter.close();
		}
	}

	@Test
	public void testCommentLineBreaksAndEmptyLines() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			propertiesWriter.writeComment("line1\r\nline2\rline3\nline4");
			propertiesWriter.writeEmptyLine();
			propertiesWriter.writeProperty("key", "value");
			assertEquals(1, propertiesWriter.getWrittenProperties());
		}
		assertEquals("# line1\n# line2\n# line3\n# line4\n\nkey=value\n", new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	public void testWritePropertiesWithComments() throws Exception {
		final Map<String, String> properties = new LinkedHashMap<>();
		properties.put("key1", "value1");
		properties.put("key2", "value2");
		final Map<String, String> comments = new LinkedHashMap<>();
		comments.put("key2", "Kommentär");

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, true)) {
			propertiesWriter.writeProperties(properties, comments);
			assertEquals(2, propertiesWriter.getWrittenProperties());
		}
		assertEquals("key1=value1\n# Kommentär\nkey2=value2\n", new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
	}
}
