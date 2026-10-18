package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class PropertiesReaderTest {
	/** Emoji as surrogate pair (U+1F600) */
	private static final String EMOJI = "\uD83D\uDE00";

	/** UTF-8 byte order mark */
	private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	/** Result of reading properties data */
	private record ReadResult(Map<String, String> properties, Map<String, String> comments) {
	}

	private static ReadResult read(final byte[] data) throws Exception {
		try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream(data))) {
			final Map<String, String> properties = propertiesReader.read();
			return new ReadResult(properties, propertiesReader.getComments());
		}
	}

	private static ReadResult read(final String data, final Charset charset) throws Exception {
		return read(data.getBytes(charset));
	}

	private static Map<String, String> readProperties(final String data) throws Exception {
		return read(data, StandardCharsets.ISO_8859_1).properties();
	}

	/** Values and keys covering all escaping rules, same as in PropertiesWriterTest */
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

	private static byte[] writeWithPropertiesWriter(final boolean storeAsUtf8, final Map<String, String> properties, final Map<String, String> comments) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (PropertiesWriter propertiesWriter = new PropertiesWriter(outputStream, storeAsUtf8)) {
			propertiesWriter.writeProperties(properties, comments);
		}
		return outputStream.toByteArray();
	}

	private static void assertAllContained(final Map<String, String> expected, final Map<String, String> actual) {
		assertEquals(expected.size(), actual.size());
		for (final Entry<String, String> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), actual.get(entry.getKey()), "Value of key '" + entry.getKey() + "'");
		}
	}

	// ---------------------------------------------------------------------
	// Basic syntax (ISO-8859-1 data)
	// ---------------------------------------------------------------------

	@Test
	public void testMixedSyntax() throws Exception {
		final String data = "key1 value 1" + "\n"
				+ " key2=V\\al\u00FC 2" + "\r\n"
				+ "   ! Comment for key3" + "\n"
				+ "key3 = value 3 " + "\r"
				+ "key4=value 41 \\" + "\n"
				+ "    value 42" + "\n"
				+ " # Comment for key5" + "\n"
				+ " # Comment line2 for key5" + "\n"
				+ "key5=value5\\n" + "\n"
				+ "key6 : value6" + "\n"
				+ "key7=\\ value7\\ " + "\n";
		final ReadResult result = read(data, StandardCharsets.ISO_8859_1);

		assertEquals(7, result.properties().size());
		assertEquals("value 1", result.properties().get("key1"));
		assertEquals("Valü 2", result.properties().get("key2"));
		assertEquals("value 3 ", result.properties().get("key3"));
		assertEquals("value 41 value 42", result.properties().get("key4"));
		assertEquals("value5\n", result.properties().get("key5"));
		assertEquals("value6", result.properties().get("key6"));
		assertEquals(" value7 ", result.properties().get("key7"));

		assertEquals("Comment for key3", result.comments().get("key3"));
		assertEquals("Comment for key5\nComment line2 for key5", result.comments().get("key5"));
		assertNull(result.comments().get("key1"));
	}

	@Test
	public void testEscapedBackslash() throws Exception {
		assertEquals("C:\\My Dir", readProperties("key1=C:\\\\My Dir\n").get("key1"));
	}

	@Test
	public void testEmptyInput() throws Exception {
		assertTrue(readProperties("").isEmpty());
		assertTrue(readProperties("\n\n   \n").isEmpty());
		assertTrue(readProperties("# only a comment\n").isEmpty());
	}

	@Test
	public void testLastLineWithoutLineBreak() throws Exception {
		assertEquals("value", readProperties("key=value").get("key"));
	}

	@Test
	public void testSeparators() throws Exception {
		final Map<String, String> properties = readProperties("a=1\n"
				+ "b:2\n"
				+ "c 3\n"
				+ "d\t4\n"
				+ "e = 5\n"
				+ "f  :  6\n"
				+ "g=\n"
				+ "h\n"
				+ "i==9\n");
		assertEquals("1", properties.get("a"));
		assertEquals("2", properties.get("b"));
		assertEquals("3", properties.get("c"));
		assertEquals("4", properties.get("d"));
		assertEquals("5", properties.get("e"));
		assertEquals("6", properties.get("f"));
		assertEquals("", properties.get("g"));
		assertEquals("", properties.get("h"));
		// Only the first separator separates, the second one belongs to the value
		assertEquals("=9", properties.get("i"));
	}

	@Test
	public void testEscapedCharsInKey() throws Exception {
		final Map<String, String> properties = readProperties("a\\ b\\=c\\:d\\#e\\!f=value\n");
		assertEquals("value", properties.get("a b=c:d#e!f"));
	}

	@Test
	public void testEscapeSequencesInValue() throws Exception {
		final Map<String, String> properties = readProperties("key=a\\tb\\nc\\rd\\fe\\\\f\\=g\\:h\n");
		assertEquals("a\tb\nc\rd\fe\\f=g:h", properties.get("key"));
	}

	@Test
	public void testUnicodeEscapes() throws Exception {
		final Map<String, String> properties = readProperties("upper=\\u00DC\\u20AC\n"
				+ "lower=\\u00fc\\u20ac\n"
				+ "surrogates=\\uD83D\\uDE00\n"
				+ "schl\\u00FCssel=v\n");
		assertEquals("Ü€", properties.get("upper"));
		assertEquals("ü€", properties.get("lower"));
		assertEquals(EMOJI, properties.get("surrogates"));
		assertEquals("v", properties.get("schlüssel"));
	}

	@Test
	public void testLineContinuation() throws Exception {
		final Map<String, String> properties = readProperties("crlf=a \\\r\n   b\r\n"
				+ "multi=1\\\n 2\\\n\t3\n"
				+ "escapedBackslashAtEnd=x\\\\\n"
				+ "next=y\n");
		assertEquals("a b", properties.get("crlf"));
		assertEquals("123", properties.get("multi"));
		// An even number of backslashes at the line end is no continuation
		assertEquals("x\\", properties.get("escapedBackslashAtEnd"));
		assertEquals("y", properties.get("next"));
	}

	@Test
	public void testUnicodeEscapesInComments() throws Exception {
		final ReadResult result = read("# Preis in \\u20AC\nkey=value\n", StandardCharsets.ISO_8859_1);
		assertEquals("Preis in €", result.comments().get("key"));
	}

	// ---------------------------------------------------------------------
	// Round trips with PropertiesWriter
	// ---------------------------------------------------------------------

	@Test
	public void testRoundTripIso() throws Exception {
		final Map<String, String> expected = getCriticalProperties();
		final Map<String, String> comments = new LinkedHashMap<>();
		comments.put("umlauts", "Kommentar mit ü und €");
		comments.put("plain", "Line 1\nLine 2");

		final ReadResult result = read(writeWithPropertiesWriter(false, expected, comments));
		assertAllContained(expected, result.properties());
		assertEquals("Kommentar mit ü und €", result.comments().get("umlauts"));
		assertEquals("Line 1\nLine 2", result.comments().get("plain"));
	}

	// ---------------------------------------------------------------------
	// UTF-8 data
	// Expected behavior: data is read as UTF-8, invalid UTF-8 data is read as ISO-8859-1
	// (same as java.util.PropertyResourceBundle since Java 9)
	// ---------------------------------------------------------------------

	@Test
	public void testUtf8Data() throws Exception {
		final ReadResult result = read("# Kommentar für €\nschlüssel=Grüße " + EMOJI + "\n", StandardCharsets.UTF_8);
		assertEquals("Grüße " + EMOJI, result.properties().get("schlüssel"));
		assertEquals("Kommentar für €", result.comments().get("schlüssel"));
	}

	@Test
	public void testUtf8DataWithUnicodeEscapes() throws Exception {
		// Escapes are still decoded in UTF-8 files
		final Map<String, String> properties = read("key=ü \\u00FC\n", StandardCharsets.UTF_8).properties();
		assertEquals("ü ü", properties.get("key"));
	}

	@Test
	public void testUtf8DataWithByteOrderMark() throws Exception {
		final byte[] content = "key=Grüße\n".getBytes(StandardCharsets.UTF_8);
		final byte[] data = new byte[UTF8_BOM.length + content.length];
		System.arraycopy(UTF8_BOM, 0, data, 0, UTF8_BOM.length);
		System.arraycopy(content, 0, data, UTF8_BOM.length, content.length);

		final Map<String, String> properties = read(data).properties();
		// The BOM must not become part of the first key
		assertEquals(1, properties.size());
		assertEquals("Grüße", properties.get("key"));
	}

	@Test
	public void testIsoFallbackForInvalidUtf8() throws Exception {
		// "ü" in ISO-8859-1 is the single byte 0xFC, which is invalid UTF-8
		final Map<String, String> properties = read("key1=Grüße\nkey2=ÄÖÜ\n", StandardCharsets.ISO_8859_1).properties();
		assertEquals("Grüße", properties.get("key1"));
		assertEquals("ÄÖÜ", properties.get("key2"));
	}

	@Test
	public void testIsoFallbackForInvalidUtf8AtEndOfLargeFile() throws Exception {
		// The invalid byte appears late, so a reader must not decide on the first bytes only
		final StringBuilder data = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			data.append("key").append(i).append("=value").append(i).append('\n');
		}
		data.append("last=ü\n");
		final Map<String, String> properties = read(data.toString(), StandardCharsets.ISO_8859_1).properties();
		assertEquals(10001, properties.size());
		assertEquals("value0", properties.get("key0"));
		assertEquals("ü", properties.get("last"));
	}

	@Test
	public void testRoundTripUtf8() throws Exception {
		final Map<String, String> expected = getCriticalProperties();
		final Map<String, String> comments = new LinkedHashMap<>();
		comments.put("umlauts", "Kommentar mit ü, € und " + EMOJI);

		final ReadResult result = read(writeWithPropertiesWriter(true, expected, comments));
		assertAllContained(expected, result.properties());
		assertEquals("Kommentar mit ü, € und " + EMOJI, result.comments().get("umlauts"));
	}

	@Test
	public void testDetectedEncoding() throws Exception {
		try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream("key=ü\n".getBytes(StandardCharsets.UTF_8)))) {
			assertEquals(StandardCharsets.UTF_8, propertiesReader.getEncoding());
		}
		try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream("key=ü\n".getBytes(StandardCharsets.ISO_8859_1)))) {
			assertEquals(StandardCharsets.ISO_8859_1, propertiesReader.getEncoding());
		}
		// Pure ASCII data is valid UTF-8
		try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream("key=\\u00FC\n".getBytes(StandardCharsets.ISO_8859_1)))) {
			assertEquals(StandardCharsets.UTF_8, propertiesReader.getEncoding());
		}
	}

	@Test
	public void testExplicitEncodingSkipsByteOrderMark() throws Exception {
		final byte[] content = "key=Grüße\n".getBytes(StandardCharsets.UTF_8);
		final byte[] data = new byte[UTF8_BOM.length + content.length];
		System.arraycopy(UTF8_BOM, 0, data, 0, UTF8_BOM.length);
		System.arraycopy(content, 0, data, UTF8_BOM.length, content.length);

		try (PropertiesReader propertiesReader = new PropertiesReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
			final Map<String, String> properties = propertiesReader.read();
			assertEquals(1, properties.size());
			assertEquals("Grüße", properties.get("key"));
		}
	}
}
