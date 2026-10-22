package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class ReaderInputStreamTest {
	private static final String EMOJI = new String(Character.toChars(0x1F600));

	private static byte[] readAllByteByByte(final InputStream inputStream) throws Exception {
		final ByteArrayOutputStream result = new ByteArrayOutputStream();
		int nextByte;
		while ((nextByte = inputStream.read()) != -1) {
			result.write(nextByte);
		}
		return result.toByteArray();
	}

	private static String createLongText() {
		final StringBuilder text = new StringBuilder();
		for (int i = 0; i < 5000; i++) {
			text.append(i % 7 == 0 ? EMOJI : i % 5 == 0 ? "\u00E4" : "x");
		}
		return text.toString();
	}

	@Test
	public void testEncodesLikeStringGetBytes() throws Exception {
		final String text = "a\u00E4\u20AC" + EMOJI + "z";
		for (final Charset charset : new Charset[] { StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE, Charset.forName("UTF-32") }) {
			for (final int bufferSize : new int[] { 2, 3, 1024 }) {
				try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader(text), charset, bufferSize)) {
					assertArrayEquals(text.getBytes(charset), inputStream.readAllBytes(), charset.name() + "/" + bufferSize);
				}
			}
		}
	}

	@Test
	public void testLongTextWithSurrogatePairsAtBufferBoundaries() throws Exception {
		final String text = createLongText();
		for (final int bufferSize : new int[] { 2, 3, 7, 1024 }) {
			try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader(text), StandardCharsets.UTF_8, bufferSize)) {
				assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), inputStream.readAllBytes(), "buffer size " + bufferSize);
			}
		}
	}

	@Test
	public void testReadByteByByte() throws Exception {
		final String text = createLongText();
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader(text), StandardCharsets.UTF_8)) {
			assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), readAllByteByByte(inputStream));
			assertEquals(-1, inputStream.read());
		}
	}

	@Test
	public void testRoundTripWithInputStreamReader() throws Exception {
		final String text = createLongText();
		try (Reader reader = new InputStreamReader(new ReaderInputStream(new StringReader(text), StandardCharsets.UTF_16LE, 5), StandardCharsets.UTF_16LE)) {
			final StringBuilder result = new StringBuilder();
			final char[] buffer = new char[333];
			int length;
			while ((length = reader.read(buffer)) != -1) {
				result.append(buffer, 0, length);
			}
			assertEquals(text, result.toString());
		}
	}

	@Test
	public void testStatefulEncoderIsFlushed() throws Exception {
		// ISO-2022-JP switches back to ASCII at the end, which needs a flush of the encoder
		final String text = "a\u65E5\u672C";
		final Charset charset = Charset.forName("ISO-2022-JP");
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader(text), charset, 2)) {
			assertArrayEquals(text.getBytes(charset), inputStream.readAllBytes());
		}
	}

	@Test
	public void testUnmappableCharactersAreReplaced() throws Exception {
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("a\u20ACb"), StandardCharsets.ISO_8859_1)) {
			assertArrayEquals("a?b".getBytes(StandardCharsets.US_ASCII), inputStream.readAllBytes());
		}
		// A lone high surrogate at the end is malformed
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("a\uD83D"), StandardCharsets.UTF_8)) {
			assertArrayEquals("a?".getBytes(StandardCharsets.US_ASCII), inputStream.readAllBytes());
		}
	}

	@Test
	public void testReportingEncoderThrows() throws Exception {
		final CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder().onUnmappableCharacter(CodingErrorAction.REPORT);
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("a\u20ACb"), encoder)) {
			assertThrows(CharacterCodingException.class, inputStream::readAllBytes);
		}
		final CharsetEncoder malformedEncoder = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT);
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("a\uDE00b"), malformedEncoder)) {
			assertThrows(CharacterCodingException.class, inputStream::readAllBytes);
		}
	}

	@Test
	public void testEncoderIsResetOnCreation() throws Exception {
		final CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("first"), encoder)) {
			inputStream.readAllBytes();
		}
		// The encoder was flushed by the first stream and can be used again
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("second"), encoder)) {
			assertArrayEquals("second".getBytes(StandardCharsets.UTF_8), inputStream.readAllBytes());
		}
	}

	@Test
	public void testEmptyReader() throws Exception {
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader(""), StandardCharsets.UTF_8)) {
			assertEquals(-1, inputStream.read());
			assertEquals(-1, inputStream.read(new byte[10]));
			assertEquals(-1, inputStream.read());
		}
	}

	@Test
	public void testReadIntoArray() throws Exception {
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("abcdef"), StandardCharsets.US_ASCII)) {
			final byte[] buffer = new byte[10];
			assertEquals(0, inputStream.read(buffer, 0, 0));
			assertEquals(2, inputStream.read(buffer, 3, 2));
			assertEquals('a', buffer[3]);
			assertEquals('b', buffer[4]);
			assertEquals(0, buffer[0]);
			assertEquals(4, inputStream.read(buffer));
			assertEquals("cdef", new String(buffer, 0, 4, StandardCharsets.US_ASCII));
			assertEquals(-1, inputStream.read(buffer));
		}
	}

	@Test
	public void testReadArgumentChecks() throws Exception {
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("abc"), StandardCharsets.UTF_8)) {
			final byte[] buffer = new byte[4];
			assertThrows(NullPointerException.class, () -> inputStream.read(null, 0, 1));
			assertThrows(IndexOutOfBoundsException.class, () -> inputStream.read(buffer, -1, 1));
			assertThrows(IndexOutOfBoundsException.class, () -> inputStream.read(buffer, 0, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> inputStream.read(buffer, 2, 3));
			assertThrows(IndexOutOfBoundsException.class, () -> inputStream.read(buffer, 1, Integer.MAX_VALUE));
			// Nothing was consumed by the invalid calls
			assertEquals('a', inputStream.read());
		}
	}

	@Test
	public void testAvailable() throws Exception {
		try (ReaderInputStream inputStream = new ReaderInputStream(new StringReader("abc"), StandardCharsets.UTF_8)) {
			assertEquals(0, inputStream.available());
			assertEquals('a', inputStream.read());
			// The rest is encoded already
			assertEquals(2, inputStream.available());
		}
	}

	@Test
	public void testInvalidConstructorArguments() {
		assertThrows(IllegalArgumentException.class, () -> new ReaderInputStream(null, StandardCharsets.UTF_8));
		assertThrows(IllegalArgumentException.class, () -> new ReaderInputStream(new StringReader(""), (Charset) null));
		assertThrows(IllegalArgumentException.class, () -> new ReaderInputStream(new StringReader(""), (CharsetEncoder) null));
		assertThrows(IllegalArgumentException.class, () -> new ReaderInputStream(new StringReader(""), StandardCharsets.UTF_8, 1));
	}

	@Test
	public void testCloseClosesReader() throws Exception {
		final boolean[] closed = { false };
		final Reader reader = new StringReader("abc") {
			@Override
			public void close() {
				closed[0] = true;
				super.close();
			}
		};
		new ReaderInputStream(reader, StandardCharsets.UTF_8).close();
		assertTrue(closed[0]);
	}
}
