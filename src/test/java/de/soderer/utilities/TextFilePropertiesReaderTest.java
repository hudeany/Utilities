package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.TextUtilities.LineBreak;

@SuppressWarnings("static-method")
public class TextFilePropertiesReaderTest {
	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

	private static File createTempFile(final byte[] content) throws Exception {
		final File file = File.createTempFile("TextFilePropertiesReaderTest", ".txt");
		file.deleteOnExit();
		Files.write(file.toPath(), content);
		return file;
	}

	private static Charset charsetOf(final Tuple<String, Boolean> encodingData) {
		return Charset.forName(encodingData.getFirst());
	}

	@Test
	public void testInvalidFiles() throws Exception {
		final File directory = createTempFile(new byte[0]).getParentFile();
		assertEquals("Path isn't a file", assertThrows(Exception.class, () -> new TextFilePropertiesReader(directory)).getMessage());
		final File missing = new File(directory, "does-not-exist-" + System.nanoTime());
		assertEquals("File does not exist", assertThrows(Exception.class, () -> new TextFilePropertiesReader(missing)).getMessage());
		assertThrows(Exception.class, () -> new TextFilePropertiesReader(missing.getAbsolutePath()));
	}

	@Test
	public void testReadFile() throws Exception {
		final File file = createTempFile("Gr\u00FC\u00DFe\r\naus M\u00FCnchen\r\n".getBytes(StandardCharsets.UTF_8));
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(file.getAbsolutePath());
		assertEquals(-1, reader.getFilesize());
		assertNull(reader.getEncoding());

		assertEquals("Gr\u00FC\u00DFe\r\naus M\u00FCnchen\r\n", reader.readFileToString());
		assertEquals(file.length(), reader.getFilesize());
		assertEquals(StandardCharsets.UTF_8, charsetOf(reader.getEncoding()));
		assertEquals(Boolean.FALSE, reader.getEncoding().getSecond());

		// Text properties are scanned as well
		assertEquals(2, reader.getLinesCount());
		assertEquals(3, reader.getWordsCount());
		assertEquals(LineBreak.Windows, reader.getLinebreakType());
	}

	@Test
	public void testReadFileWithGivenEncoding() throws Exception {
		final File file = createTempFile("Gr\u00FC\u00DFe".getBytes(StandardCharsets.ISO_8859_1));
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(file.getAbsolutePath(), new Tuple<>("ISO-8859-1", false));
		assertEquals("Gr\u00FC\u00DFe", reader.readFileToString());
		assertEquals("ISO-8859-1", reader.getEncoding().getFirst());
	}

	@Test
	public void testReadPropertiesWithoutException() throws Exception {
		final TextFilePropertiesReader reader = new TextFilePropertiesReader("a b c".getBytes(StandardCharsets.UTF_8));
		reader.readProperties();
		assertEquals(3, reader.getWordsCount());
		assertEquals(5, reader.getCharactersCount());
	}

	@Test
	public void testDetectEncodingWithBom() throws Exception {
		for (final String encodingString : new String[] { "UTF-8-BOM", "UTF-16BE-BOM", "UTF-16LE-BOM", "UTF-32BE-BOM", "UTF-32LE-BOM" }) {
			final Tuple<String, Boolean> encodingData = TextFilePropertiesReader.getEncodingData(encodingString);
			final byte[] data = TextFilePropertiesReader.encodeString("x\u00E4\u20AC", encodingData);
			final TextFilePropertiesReader reader = new TextFilePropertiesReader(data);
			// The BOM is not part of the text
			assertEquals("x\u00E4\u20AC", reader.readFileToString(), encodingString);
			assertEquals(charsetOf(encodingData), charsetOf(reader.getEncoding()), encodingString);
			assertEquals(Boolean.TRUE, reader.getEncoding().getSecond(), encodingString);
		}
	}

	@Test
	public void testDetectEncodingWithoutBom() throws Exception {
		final TextFilePropertiesReader utf8Reader = new TextFilePropertiesReader("Gr\u00FC\u00DFe".getBytes(StandardCharsets.UTF_8));
		assertEquals("Gr\u00FC\u00DFe", utf8Reader.readFileToString());
		assertEquals(StandardCharsets.UTF_8, charsetOf(utf8Reader.getEncoding()));

		final TextFilePropertiesReader windowsReader = new TextFilePropertiesReader("Gr\u00FC\u00DFe \u20AC".getBytes(WINDOWS_1252));
		assertEquals("Gr\u00FC\u00DFe \u20AC", windowsReader.readFileToString());
		assertEquals(WINDOWS_1252, charsetOf(windowsReader.getEncoding()));

		final TextFilePropertiesReader utf16Reader = new TextFilePropertiesReader("Hello World".getBytes(StandardCharsets.UTF_16LE));
		assertEquals("Hello World", utf16Reader.readFileToString());
		assertEquals(StandardCharsets.UTF_16LE, charsetOf(utf16Reader.getEncoding()));
	}

	@Test
	public void testDefaultEncodingForNonUtf8Data() throws Exception {
		final byte[] data = "Gr\u00FC\u00DFe".getBytes(StandardCharsets.ISO_8859_1);
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(data);
		reader.setDefaultEncodingData(new Tuple<>("ISO-8859-15", false));
		assertEquals("Gr\u00FC\u00DFe", reader.readFileToString());
		assertEquals("ISO-8859-15", reader.getEncoding().getFirst());

		// null is ignored, UTF-8 as default falls back to Windows-1252 for invalid UTF-8
		final TextFilePropertiesReader reader2 = new TextFilePropertiesReader(data);
		reader2.setDefaultEncodingData(null);
		reader2.setDefaultEncodingData(new Tuple<>("UTF-8", false));
		reader2.readFileToString();
		assertEquals(WINDOWS_1252, charsetOf(reader2.getEncoding()));
	}

	@Test
	public void testReadPart() throws Exception {
		final byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);
		assertEquals("234", new TextFilePropertiesReader(data).readFileToString(2, 3));
		assertEquals("0123", new TextFilePropertiesReader(data).readFileToString(0, 4));
		// The range is moved and shortened to fit into the data
		assertEquals("789", new TextFilePropertiesReader(data).readFileToString(100, 3));
		assertEquals("0123456789", new TextFilePropertiesReader(data).readFileToString(-5, 100));
		assertEquals("", new TextFilePropertiesReader(data).readFileToString(3, -1));

		final TextFilePropertiesReader reader = new TextFilePropertiesReader(data);
		reader.readFileToString(5, 5);
		assertEquals(5, reader.getCharactersCount());
	}

	@Test
	public void testReadPartOfFile() throws Exception {
		final File file = createTempFile("Gr\u00FC\u00DFe aus M\u00FCnchen".getBytes(StandardCharsets.UTF_8));
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(file);
		// Starts at a character boundary: "aus" starts after "Gr\u00FC\u00DFe " (8 bytes in UTF-8)
		assertEquals("aus", reader.readFileToString(8, 3));
		assertEquals(file.length(), reader.getFilesize());
	}

	@Test
	public void testReadPartUtf8AlignsToCharacters() throws Exception {
		// "a" 61, "ae" C3 A4, "b" 62, "oe" C3 B6, "c" 63
		final byte[] data = "a\u00E4b\u00F6c".getBytes(StandardCharsets.UTF_8);
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(data);
		// The start within "ae" is moved back to its first byte, the part is detected as UTF-8 nevertheless
		assertEquals("\u00E4b\u00F6", reader.readFileToString(2, 4));
		assertEquals(StandardCharsets.UTF_8, charsetOf(reader.getEncoding()));

		// "oe" cut off at the end is left out
		assertEquals("\u00E4b", new TextFilePropertiesReader(data).readFileToString(1, 4));
		assertEquals("\u00E4", new TextFilePropertiesReader(data).readFileToString(2, 1));
		assertEquals("\u00F6c", new TextFilePropertiesReader(data).readFileToString(5, 2));
		// No replacement characters anywhere
		for (int start = 0; start < data.length; start++) {
			for (int length = 0; start + length <= data.length; length++) {
				final String part = new TextFilePropertiesReader(data, new Tuple<>("UTF-8", false)).readFileToString(start, length);
				assertTrue(part.indexOf('\uFFFD') < 0, start + "/" + length + ": " + part);
			}
		}
	}

	@Test
	public void testReadPartUtf8OfFile() throws Exception {
		final File file = createTempFile("a\u00E4b\u00F6c".getBytes(StandardCharsets.UTF_8));
		assertEquals("\u00E4b\u00F6", new TextFilePropertiesReader(file).readFileToString(2, 4));
	}

	@Test
	public void testWindows1252EndingWithMultibyteLeadByte() throws Exception {
		// 0xDF ("sharp s" in Windows-1252) at the end looks like the start of a UTF-8 character cut off by a sample,
		// but the complete data cannot continue it
		final byte[] data = "Fu\u00DF".getBytes(WINDOWS_1252);
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(data);
		assertEquals("Fu\u00DF", reader.readFileToString());
		assertEquals(WINDOWS_1252, charsetOf(reader.getEncoding()));

		final File file = createTempFile(data);
		assertEquals("Fu\u00DF", new TextFilePropertiesReader(file).readFileToString());

		// A part reaching the end of the data
		assertEquals("u\u00DF", new TextFilePropertiesReader(data).readFileToString(1, 2));
		// A part ending with 0xDF in the middle of the data: the following byte shows that it is no UTF-8 character
		final byte[] longerData = "Fu\u00DF und Stra\u00DFe".getBytes(WINDOWS_1252);
		assertEquals("Fu\u00DF", new TextFilePropertiesReader(longerData).readFileToString(0, 3));
		assertEquals(" Stra\u00DF", new TextFilePropertiesReader(longerData).readFileToString(7, 6));
	}

	@Test
	public void testReadPartUtf16AlignsToCharacters() throws Exception {
		// BOM (2 bytes) + "abcdef" (12 bytes)
		final byte[] withBom = TextFilePropertiesReader.encodeString("abcdef", new Tuple<>("UTF-16LE", true));
		// Odd start position 3 is moved back to the character "a" at position 2, the length is shortened to whole characters
		assertEquals("ab", new TextFilePropertiesReader(withBom).readFileToString(3, 5));
		assertEquals("bc", new TextFilePropertiesReader(withBom).readFileToString(4, 4));

		// Without BOM the byte order is detected from the start of the data
		final byte[] withoutBom = "abcdef".getBytes(StandardCharsets.UTF_16LE);
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(withoutBom);
		assertEquals("bc", reader.readFileToString(3, 4));
		assertEquals(StandardCharsets.UTF_16LE, charsetOf(reader.getEncoding()));

		// Generic "UTF-16" takes the byte order from the BOM at the start of the data
		final byte[] generic = TextFilePropertiesReader.encodeString("abcdef", new Tuple<>("UTF-16", true));
		assertEquals("bc", new TextFilePropertiesReader(generic, new Tuple<>("UTF-16", true)).readFileToString(4, 4));

		final byte[] utf32 = TextFilePropertiesReader.encodeString("abcd", new Tuple<>("UTF-32LE", true));
		assertEquals("b", new TextFilePropertiesReader(utf32).readFileToString(9, 6));
	}

	@Test
	public void testEncodingDataAndDisplayString() {
		assertEncodingData("UTF-8-BOM", "UTF-8", true, "UTF-8-BOM");
		assertEncodingData("UTF-16BE-BOM", "UTF-16BE", true, "UTF-16BE-BOM");
		assertEncodingData("UTF-16LE-BOM", "UTF-16LE", true, "UTF-16LE-BOM");
		assertEncodingData("UTF-32BE-BOM", "UTF-32BE", true, "UTF-32BE-BOM");
		assertEncodingData("UTF-32LE-BOM", "UTF-32LE", true, "UTF-32LE-BOM");
		assertEncodingData("UnicodeBig", "UTF-16BE", true, "UTF-16BE-BOM");
		assertEncodingData("unicodelittle", "UTF-16LE", true, "UTF-16LE-BOM");
		assertEncodingData("ISO-8859-1", "ISO-8859-1", false, "ISO-8859-1");
		assertEncodingData("UTF-16BE", "UTF-16BE", false, "UTF-16BE");

		assertEquals("", TextFilePropertiesReader.getEncodingDisplayString(null));
		assertEquals("UTF-16BE", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UTF-16", false)));
		assertEquals("UTF-16BE-BOM", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UTF-16", true)));
		assertEquals("UTF-16BE-BOM", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UnicodeBig", false)));
		assertEquals("UTF-16LE-BOM", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UnicodeLittle", false)));
		assertEquals("UTF-16LE", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UTF-16LE", false)));
		assertEquals("UTF-32BE-BOM", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UTF-32", true)));
		assertEquals("UTF-8", TextFilePropertiesReader.getEncodingDisplayString(new Tuple<>("UTF-8", false)));
	}

	private static void assertEncodingData(final String encodingString, final String expectedName, final boolean expectedBom, final String expectedDisplayString) {
		final Tuple<String, Boolean> encodingData = TextFilePropertiesReader.getEncodingData(encodingString);
		assertEquals(expectedName, encodingData.getFirst(), encodingString);
		assertEquals(Boolean.valueOf(expectedBom), encodingData.getSecond(), encodingString);
		assertEquals(expectedDisplayString, TextFilePropertiesReader.getEncodingDisplayString(encodingData), encodingString);
	}

	@Test
	public void testEncodeString() throws Exception {
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-8", true)), 0xEF, 0xBB, 0xBF, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-8", false)), 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-16BE", true)), 0xFE, 0xFF, 0, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-16LE", true)), 0xFF, 0xFE, 'x', 0);
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UnicodeBig", false)), 0xFE, 0xFF, 0, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UnicodeLittle", false)), 0xFF, 0xFE, 'x', 0);
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-16", true)), 0xFE, 0xFF, 0, 'x');
		// Java's UTF-16 encoder would write a BOM, but the encoding data says "without BOM"
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-16", false)), 0, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-32BE", true)), 0, 0, 0xFE, 0xFF, 0, 0, 0, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-32", true)), 0, 0, 0xFE, 0xFF, 0, 0, 0, 'x');
		assertBytes(TextFilePropertiesReader.encodeString("x", new Tuple<>("UTF-32LE", true)), 0xFF, 0xFE, 0, 0, 'x', 0, 0, 0);
		assertBytes(TextFilePropertiesReader.encodeString("\u00E4", new Tuple<>("ISO-8859-1", false)), 0xE4);

		assertThrows(Exception.class, () -> TextFilePropertiesReader.encodeString("x", null));
		assertThrows(Exception.class, () -> TextFilePropertiesReader.encodeString("x", new Tuple<>("NO-SUCH-ENCODING", false)));
	}

	private static void assertBytes(final byte[] actual, final int... expected) {
		final byte[] expectedBytes = new byte[expected.length];
		for (int i = 0; i < expected.length; i++) {
			expectedBytes[i] = (byte) expected[i];
		}
		assertEquals(java.util.Arrays.toString(expectedBytes), java.util.Arrays.toString(actual));
	}

	@Test
	public void testDecodeByteArray() throws Exception {
		final byte[] utf8WithBom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'x' };
		assertEquals("x", TextFilePropertiesReader.decodeByteArray(utf8WithBom, new Tuple<>("UTF-8", true)));
		// A BOM is only removed if it matches the encoding
		assertEquals("\u00EF\u00BB\u00BFx", TextFilePropertiesReader.decodeByteArray(utf8WithBom, new Tuple<>("ISO-8859-1", false)));

		// A UTF-32LE BOM starts with the bytes of a UTF-16LE BOM, it must not be removed partially
		final byte[] utf32Le = TextFilePropertiesReader.encodeString("ab", new Tuple<>("UTF-32LE", true));
		assertEquals("ab", TextFilePropertiesReader.decodeByteArray(utf32Le, new Tuple<>("UTF-32LE", true)));

		// The generic UTF-16 decoder evaluates the BOM itself
		final byte[] utf16Le = TextFilePropertiesReader.encodeString("ab", new Tuple<>("UTF-16LE", true));
		assertEquals("ab", TextFilePropertiesReader.decodeByteArray(utf16Le, new Tuple<>("UnicodeLittle", true)));
		assertEquals("ab", TextFilePropertiesReader.decodeByteArray(utf16Le, new Tuple<>("UTF-16", true)));

		assertThrows(IOException.class, () -> TextFilePropertiesReader.decodeByteArray(utf8WithBom, new Tuple<>("NO-SUCH-ENCODING", false)));
	}

	@Test
	public void testEncodeDecodeRoundTrip() throws Exception {
		final String text = "Gr\u00FC\u00DFe \u20AC " + new String(Character.toChars(0x1F600));
		for (final String encodingString : new String[] { "UTF-8", "UTF-8-BOM", "UTF-16BE", "UTF-16LE", "UTF-16BE-BOM", "UTF-16LE-BOM", "UTF-32BE-BOM", "UTF-32LE-BOM", "UnicodeBig", "UnicodeLittle" }) {
			final Tuple<String, Boolean> encodingData = TextFilePropertiesReader.getEncodingData(encodingString);
			final byte[] data = TextFilePropertiesReader.encodeString(text, encodingData);
			assertEquals(text, TextFilePropertiesReader.decodeByteArray(data, encodingData), encodingString);
		}
	}

	@Test
	public void testScanLineIndexes() throws Exception {
		final File file = createTempFile("a\nbc\nd".getBytes(StandardCharsets.US_ASCII));
		final TextFilePropertiesReader reader = new TextFilePropertiesReader(file);
		assertNotNull(reader.scanLineStartIndexes());
		assertNotNull(reader.scanLinebreakIndexes());

		final TextFilePropertiesReader dataReader = new TextFilePropertiesReader("a\nb".getBytes(StandardCharsets.US_ASCII));
		assertEquals("Not implemented", assertThrows(Exception.class, dataReader::scanLineStartIndexes).getMessage());
		assertEquals("Not implemented", assertThrows(Exception.class, dataReader::scanLinebreakIndexes).getMessage());

		// A file deleted after creating the reader cannot be scanned
		assertTrue(file.delete());
		assertNull(reader.scanLineStartIndexes());
		assertNull(reader.scanLinebreakIndexes());
	}
}
