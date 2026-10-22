package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.EncodingDetector.DetectionResult;

@SuppressWarnings("static-method")
public class EncodingDetectorTest {
	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
	private static final Charset UTF_32LE = Charset.forName("UTF-32LE");
	private static final Charset UTF_32BE = Charset.forName("UTF-32BE");

	private static Charset detect(final byte[] data) {
		return EncodingDetector.detectEncoding(data).charset;
	}

	private static Charset detectComplete(final byte[] data) {
		return EncodingDetector.detectEncoding(data, null, true).charset;
	}

	private static byte[] bytes(final int... values) {
		final byte[] result = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (byte) values[i];
		}
		return result;
	}

	private static byte[] concat(final byte[] first, final byte[] second) {
		final byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	@Test
	public void testEmptyData() {
		final DetectionResult nullResult = EncodingDetector.detectEncoding(null);
		assertEquals(StandardCharsets.UTF_8, nullResult.charset);
		assertFalse(nullResult.hasBom);
		assertEquals(0, nullResult.bomLength);
		assertEquals(StandardCharsets.UTF_8, detect(new byte[0]));
	}

	@Test
	public void testByteOrderMarks() {
		assertBom(bytes(0xEF, 0xBB, 0xBF, 'a'), StandardCharsets.UTF_8, 3);
		assertBom(bytes(0xFE, 0xFF, 0, 'a'), StandardCharsets.UTF_16BE, 2);
		assertBom(bytes(0xFF, 0xFE, 'a', 0), StandardCharsets.UTF_16LE, 2);
		assertBom(bytes(0, 0, 0xFE, 0xFF, 0, 0, 0, 'a'), UTF_32BE, 4);
		// Starts like a UTF-16LE BOM, but is the longer UTF-32LE BOM
		assertBom(bytes(0xFF, 0xFE, 0, 0, 'a', 0, 0, 0), UTF_32LE, 4);
	}

	private static void assertBom(final byte[] data, final Charset expectedCharset, final int expectedBomLength) {
		final DetectionResult result = EncodingDetector.detectEncoding(data);
		assertEquals(expectedCharset, result.charset);
		assertTrue(result.hasBom);
		assertEquals(expectedBomLength, result.bomLength);
	}

	@Test
	public void testDetectionResultToString() {
		assertEquals("UTF-8 (BOM, 3 bytes)", EncodingDetector.detectEncoding(bytes(0xEF, 0xBB, 0xBF, 'a')).toString());
		assertEquals("UTF-8 (no BOM)", EncodingDetector.detectEncoding("a".getBytes(StandardCharsets.US_ASCII)).toString());
	}

	@Test
	public void testAsciiIsUtf8() {
		assertEquals(StandardCharsets.UTF_8, detect("Plain ASCII text\r\nwith lines\tand tabs".getBytes(StandardCharsets.US_ASCII)));
	}

	@Test
	public void testValidUtf8() {
		assertEquals(StandardCharsets.UTF_8, detect("Gr\u00FC\u00DFe \u20AC \u4E2D\u6587".getBytes(StandardCharsets.UTF_8)));
		// 4 byte sequence (emoji)
		assertEquals(StandardCharsets.UTF_8, detect(("a" + new String(Character.toChars(0x1F600)) + "b").getBytes(StandardCharsets.UTF_8)));
		// Highest valid code point U+10FFFF
		assertEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xF4, 0x8F, 0xBF, 0xBF)));
	}

	@Test
	public void testWindows1252() {
		assertEquals(WINDOWS_1252, detect("Gr\u00FC\u00DFe aus M\u00FCnchen".getBytes(WINDOWS_1252)));
		// Smart quotes and euro sign are in the range 0x80-0x9F
		assertEquals(WINDOWS_1252, detect("\u201Equoted\u201C \u20AC".getBytes(WINDOWS_1252)));
		// A lone continuation byte where a character must start
		assertEquals(WINDOWS_1252, detect(bytes('a', 0x80, 'b')));
	}

	@Test
	public void testBytesUndefinedInWindows1252UseIso88591() {
		for (final int undefinedByte : new int[] { 0x81, 0x8D, 0x8F, 0x90, 0x9D }) {
			assertEquals(StandardCharsets.ISO_8859_1, detect(bytes('a', 0xE4, undefinedByte, 'b')), Integer.toHexString(undefinedByte));
		}
	}

	@Test
	public void testFallbackCharset() {
		final byte[] latin = "Gr\u00FC\u00DFe".getBytes(StandardCharsets.ISO_8859_1);
		final Charset iso885915 = Charset.forName("ISO-8859-15");
		assertEquals(iso885915, EncodingDetector.detectEncoding(latin, iso885915).charset);
		// A UTF fallback cannot decode invalid UTF-8 data, so it is ignored
		assertEquals(WINDOWS_1252, EncodingDetector.detectEncoding(latin, StandardCharsets.UTF_16LE).charset);
		assertEquals(WINDOWS_1252, EncodingDetector.detectEncoding(latin, StandardCharsets.UTF_8).charset);
		// The fallback is not used for valid UTF-8 or data with BOM
		assertEquals(StandardCharsets.UTF_8, EncodingDetector.detectEncoding("\u00E4".getBytes(StandardCharsets.UTF_8), iso885915).charset);
		assertEquals(StandardCharsets.UTF_16LE, EncodingDetector.detectEncoding(bytes(0xFF, 0xFE, 'a', 0), iso885915).charset);
	}

	@Test
	public void testInvalidUtf8Sequences() {
		// Overlong encodings
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xC0, 0x80, 'b')));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xC1, 0xBF, 'b')));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xE0, 0x80, 0x80, 'b')));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xF0, 0x80, 0x80, 0x80, 'b')));
		// Directly encoded surrogate U+D800
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xED, 0xA0, 0x80, 'b')));
		// Above U+10FFFF
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xF4, 0x90, 0x80, 0x80, 'b')));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xF5, 0x80, 0x80, 0x80, 'b')));
		// Missing continuation byte in the middle
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xC3, 'b', 'c')));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 0xE2, 0x82, 'b', 'c')));
	}

	@Test
	public void testTruncatedUtf8AtEndOfSample() {
		// A sample may be cut off within a character, the present bytes are valid
		final byte[] cutEuro = bytes('a', 'b', 0xE2, 0x82);
		assertEquals(StandardCharsets.UTF_8, detect(cutEuro));
		// For complete data such a sequence can never be completed
		assertEquals(WINDOWS_1252, detectComplete(cutEuro));

		// Windows-1252 text ending with an umlaut or sharp s looks like a truncated UTF-8 sequence
		final byte[] fuss = "Fu\u00DF".getBytes(WINDOWS_1252);
		assertEquals(StandardCharsets.UTF_8, detect(fuss));
		assertEquals(WINDOWS_1252, detectComplete(fuss));
	}

	@Test
	public void testTruncatedSequenceWithInvalidPresentBytes() {
		// 0xE4 ('ae' in Windows-1252) followed by an ASCII character is no truncated UTF-8 sequence
		assertEquals(WINDOWS_1252, detect(bytes('x', 0xE4, 'y')));
		// The second byte decides about overlong encodings even when the sequence is truncated
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xE0, 0x80)));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xED, 0xA0)));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xF0, 0x80)));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xF4, 0x90)));
		// Leading bytes that can never start a valid sequence
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xC0)));
		assertNotEquals(StandardCharsets.UTF_8, detect(bytes('a', 'b', 0xF8)));
	}

	@Test
	public void testUtf16WithoutBom() {
		final String text = "Hello World, this is some text\r\n";
		assertEquals(StandardCharsets.UTF_16LE, detect(text.getBytes(StandardCharsets.UTF_16LE)));
		assertEquals(StandardCharsets.UTF_16BE, detect(text.getBytes(StandardCharsets.UTF_16BE)));
		// Mostly non Latin text with some blanks
		final String cyrillic = "\u041F\u0440\u0438\u0432\u0435\u0442 \u043C\u0438\u0440 1 2 3";
		assertEquals(StandardCharsets.UTF_16LE, detect(cyrillic.getBytes(StandardCharsets.UTF_16LE)));
		// Surrogate pair
		final String emoji = "Smile " + new String(Character.toChars(0x1F600)) + " in some longer text";
		assertEquals(StandardCharsets.UTF_16BE, detect(emoji.getBytes(StandardCharsets.UTF_16BE)));
		// An odd last byte (cut off sample) is ignored
		final byte[] oddLength = Arrays.copyOf(text.getBytes(StandardCharsets.UTF_16LE), text.length() * 2 - 1);
		assertEquals(StandardCharsets.UTF_16LE, detect(oddLength));
	}

	@Test
	public void testDataWithZeroBytesThatIsNoUtf16() {
		// Control characters are typical for binary data
		assertNotEquals(StandardCharsets.UTF_16LE, detect(bytes('a', 0, 1, 0, 'b', 0, 2, 0, 'c', 0)));
		// A lone low surrogate
		assertNotEquals(StandardCharsets.UTF_16LE, detect(bytes('a', 0, 'b', 0, 0x00, 0xDC, 'c', 0, 'd', 0)));
		// Zero bytes on both sides
		assertNotEquals(StandardCharsets.UTF_16LE, detect(bytes('a', 0, 0, 'b', 'c', 0, 0, 'd', 'e', 0, 0, 'f')));
		// A single zero byte in a long 8 bit text
		final byte[] longText = new byte[400];
		Arrays.fill(longText, (byte) 'x');
		longText[201] = 0;
		assertEquals(StandardCharsets.UTF_8, detect(longText));
	}

	@Test
	public void testUtf32WithoutBom() {
		final String text = "Hello World";
		assertEquals(UTF_32LE, detect(text.getBytes(UTF_32LE)));
		assertEquals(UTF_32BE, detect(text.getBytes(UTF_32BE)));
		// Too short for UTF-32 detection
		assertNotEquals(UTF_32LE, detect("a".getBytes(UTF_32LE)));
	}

	@Test
	public void testDetectEncodingOfFile() throws Exception {
		final Path file = File.createTempFile("EncodingDetectorTest", ".txt").toPath();
		file.toFile().deleteOnExit();

		// The complete file fits into the sample, so a sequence truncated at its end is invalid
		Files.write(file, "Fu\u00DF".getBytes(WINDOWS_1252));
		assertEquals(WINDOWS_1252, EncodingDetector.detectEncoding(file, 3, null).charset);
		assertEquals(WINDOWS_1252, EncodingDetector.detectEncoding(file, 1000, null).charset);

		// The sample ends within a UTF-8 character, but the file continues
		Files.write(file, "ab\u20ACcd".getBytes(StandardCharsets.UTF_8));
		assertEquals(StandardCharsets.UTF_8, EncodingDetector.detectEncoding(file, 4, null).charset);

		Files.write(file, new byte[0]);
		assertEquals(StandardCharsets.UTF_8, EncodingDetector.detectEncoding(file, 10, null).charset);

		Files.write(file, "Gr\u00FC\u00DFe".getBytes(StandardCharsets.ISO_8859_1));
		assertEquals(Charset.forName("ISO-8859-15"), EncodingDetector.detectEncoding(file, 10, Charset.forName("ISO-8859-15")).charset);
	}

	@Test
	public void testDetectEncodingOfFileErrors() throws Exception {
		final Path file = File.createTempFile("EncodingDetectorTest", ".txt").toPath();
		file.toFile().deleteOnExit();
		assertThrows(IllegalArgumentException.class, () -> EncodingDetector.detectEncoding(file, 0, null));
		assertThrows(java.io.IOException.class, () -> EncodingDetector.detectEncoding(file.resolveSibling("does-not-exist-" + System.nanoTime()), 10, null));
	}

	@Test
	public void testDecodingWithDetectedCharsetRestoresText() {
		final String text = "Gr\u00FC\u00DFe \u201Equoted\u201C \u20AC";
		for (final Charset charset : new Charset[] { StandardCharsets.UTF_8, WINDOWS_1252, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE }) {
			final byte[] data = text.getBytes(charset);
			assertEquals(text, new String(data, detectComplete(data)), charset.name());
		}
		final byte[] withBom = concat(bytes(0xEF, 0xBB, 0xBF), text.getBytes(StandardCharsets.UTF_8));
		final DetectionResult result = EncodingDetector.detectEncoding(withBom);
		assertEquals(text, new String(withBom, result.bomLength, withBom.length - result.bomLength, result.charset));
	}
}
