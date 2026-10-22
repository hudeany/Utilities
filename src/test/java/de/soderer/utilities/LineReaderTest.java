package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.TextUtilities.LineBreak;

@SuppressWarnings("static-method")
public class LineReaderTest {
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

	private static List<String> readAllLines(final LineReader reader) throws IOException {
		final List<String> lines = new ArrayList<>();
		String line;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		return lines;
	}

	private static byte[] withBom(final BOM bom, final String text, final Charset charset) {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.writeBytes(bom.getBytes());
		outputStream.writeBytes(text.getBytes(charset));
		return outputStream.toByteArray();
	}

	@Test
	public void testReadLines() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("first\nsecond\n\nfourth"))) {
			assertEquals(Arrays.asList("first", "second", "", "fourth"), readAllLines(reader));
			assertNull(reader.readLine());
		}
	}

	@Test
	public void testTrailingLineBreakStartsNoLine() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("a\n"))) {
			assertEquals(Arrays.asList("a"), readAllLines(reader));
		}
		try (LineReader reader = new LineReader(new StringReader(""))) {
			assertNull(reader.readLine());
		}
		try (LineReader reader = new LineReader(new StringReader("\n"))) {
			assertEquals(Arrays.asList(""), readAllLines(reader));
		}
	}

	@Test
	public void testLineBreakTypes() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("a\r\nb\rc\nd\re"))) {
			assertEquals(LineBreak.Unknown, reader.getGlobalLineBreakType());

			assertEquals("a", reader.readLine());
			assertEquals(LineBreak.Windows, reader.getLatestLineBreakType());
			assertEquals(LineBreak.Windows, reader.getGlobalLineBreakType());

			assertEquals("b", reader.readLine());
			assertEquals(LineBreak.Mac, reader.getLatestLineBreakType());
			assertEquals(LineBreak.Mixed, reader.getGlobalLineBreakType());

			assertEquals("c", reader.readLine());
			assertEquals(LineBreak.Unix, reader.getLatestLineBreakType());

			assertEquals("d", reader.readLine());
			assertEquals(LineBreak.Mac, reader.getLatestLineBreakType());

			// Last line without line break
			assertEquals("e", reader.readLine());
			assertEquals(LineBreak.Unknown, reader.getLatestLineBreakType());
			assertEquals(LineBreak.Mixed, reader.getGlobalLineBreakType());
		}
	}

	@Test
	public void testUniformLineBreakTypes() throws Exception {
		for (final LineBreak lineBreak : new LineBreak[] { LineBreak.Unix, LineBreak.Windows, LineBreak.Mac }) {
			final String separator = lineBreak.toString();
			try (LineReader reader = new LineReader(new StringReader("a" + separator + "b" + separator + separator + "c" + separator))) {
				assertEquals(Arrays.asList("a", "b", "", "c"), readAllLines(reader), lineBreak.name());
				assertEquals(lineBreak, reader.getGlobalLineBreakType(), lineBreak.name());
			}
		}
	}

	@Test
	public void testCrAtEndOfData() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("a\r"))) {
			assertEquals("a", reader.readLine());
			assertEquals(LineBreak.Mac, reader.getLatestLineBreakType());
			assertNull(reader.readLine());
		}
	}

	@Test
	public void testStreamWithEncoding() throws Exception {
		final String text = "Gr\u00FC\u00DFe\r\n\u20AC " + new String(Character.toChars(0x1F600));
		for (final Charset charset : new Charset[] { StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, Charset.forName("UTF-32BE") }) {
			try (LineReader reader = new LineReader(new ByteArrayInputStream(text.getBytes(charset)), charset)) {
				assertEquals(BOM.NONE, reader.getBom());
				assertEquals(Arrays.asList("Gr\u00FC\u00DFe", "\u20AC " + new String(Character.toChars(0x1F600))), readAllLines(reader), charset.name());
			}
		}
		try (LineReader reader = new LineReader(new ByteArrayInputStream("Gr\u00FC\u00DFe".getBytes(StandardCharsets.ISO_8859_1)), StandardCharsets.ISO_8859_1)) {
			assertEquals("Gr\u00FC\u00DFe", reader.readLine());
		}
	}

	@Test
	public void testBomIsSkipped() throws Exception {
		try (LineReader reader = new LineReader(new ByteArrayInputStream(withBom(BOM.UTF_8, "a\nb", StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			assertEquals(BOM.UTF_8, reader.getBom());
			assertEquals(Arrays.asList("a", "b"), readAllLines(reader));
		}
		try (LineReader reader = new LineReader(new ByteArrayInputStream(withBom(BOM.UTF_16_LE, "x\u00E4", StandardCharsets.UTF_16LE)), StandardCharsets.UTF_16LE)) {
			assertEquals(BOM.UTF_16_LE, reader.getBom());
			assertEquals("x\u00E4", reader.readLine());
		}
	}

	@Test
	public void testGenericCharsetTakesByteOrderFromBom() throws Exception {
		assertGenericCharset(BOM.UTF_16_LE, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16);
		assertGenericCharset(BOM.UTF_16_BE, StandardCharsets.UTF_16BE, StandardCharsets.UTF_16);
		assertGenericCharset(BOM.UTF_32_LE, Charset.forName("UTF-32LE"), Charset.forName("UTF-32"));
		assertGenericCharset(BOM.UTF_32_BE, Charset.forName("UTF-32BE"), Charset.forName("UTF-32"));
	}

	private static void assertGenericCharset(final BOM bom, final Charset dataCharset, final Charset readerCharset) throws Exception {
		try (LineReader reader = new LineReader(new ByteArrayInputStream(withBom(bom, "line1\nline2", dataCharset)), readerCharset)) {
			assertEquals(bom, reader.getBom());
			assertEquals(Arrays.asList("line1", "line2"), readAllLines(reader), bom.name());
		}
	}

	@Test
	public void testBomNotFittingCharsetIsRejectedAndStreamClosed() {
		assertRejected(withBom(BOM.UTF_8, "a", StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		assertRejected(withBom(BOM.UTF_16_LE, "a", StandardCharsets.UTF_16LE), StandardCharsets.UTF_16BE);
		assertRejected(withBom(BOM.UTF_16_BE, "a", StandardCharsets.UTF_16BE), StandardCharsets.UTF_8);
		assertRejected(withBom(BOM.UTF_32_LE, "a", Charset.forName("UTF-32LE")), StandardCharsets.UTF_16LE);
		assertRejected(withBom(BOM.UTF_32_BE, "a", Charset.forName("UTF-32BE")), Charset.forName("UTF-32LE"));
	}

	private static void assertRejected(final byte[] data, final Charset charset) {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream(data);
		assertThrows(IOException.class, () -> new LineReader(inputStream, charset));
		assertTrue(inputStream.closed, charset.name());
	}

	@Test
	public void testInvalidArguments() {
		assertThrows(IllegalArgumentException.class, () -> new LineReader(null, StandardCharsets.UTF_8));
		assertThrows(IllegalArgumentException.class, () -> new LineReader((java.io.Reader) null));

		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[0]);
		assertThrows(IllegalArgumentException.class, () -> new LineReader(inputStream, null));
		assertTrue(inputStream.closed);
	}

	@Test
	public void testReadCharAndSkipChars() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("abc\r\ndef"))) {
			assertEquals('a', reader.readChar());
			reader.skipChars(1);
			assertEquals('c', reader.readChar());
			// CRLF counts as two characters
			reader.skipChars(2);
			assertEquals("def", reader.readLine());
			assertEquals(-1, reader.readChar());
			reader.skipChars(0);
			reader.skipChars(-5);
			assertThrows(IOException.class, () -> reader.skipChars(1));
		}
	}

	@Test
	public void testSkipCharsBeyondEnd() throws Exception {
		try (LineReader reader = new LineReader(new StringReader("ab"))) {
			assertThrows(IOException.class, () -> reader.skipChars(3));
		}
	}

	@Test
	public void testSkipManyChars() throws Exception {
		final StringBuilder text = new StringBuilder();
		for (int i = 0; i < 20000; i++) {
			text.append('x');
		}
		text.append("\nend");
		try (LineReader reader = new LineReader(new StringReader(text.toString()))) {
			reader.skipChars(20001);
			assertEquals("end", reader.readLine());
		}
	}

	@Test
	public void testClose() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream("a\nb".getBytes(StandardCharsets.UTF_8));
		try (final LineReader reader = new LineReader(inputStream, StandardCharsets.UTF_8)) {
			assertEquals("a", reader.readLine());
			reader.close();
			assertTrue(inputStream.closed);
			// Closing twice is allowed
			reader.close();

			assertThrows(IOException.class, reader::readLine);
			assertThrows(IOException.class, reader::readChar);
			assertThrows(IOException.class, () -> reader.skipChars(1));
			// The line break information remains available
			assertEquals(LineBreak.Unix, reader.getGlobalLineBreakType());
		}
	}
}
