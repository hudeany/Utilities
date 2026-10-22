package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class BOMTest {
	private static byte[] bytes(final int... values) {
		final byte[] result = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (byte) values[i];
		}
		return result;
	}

	private static InputStream stream(final int... values) {
		return new BufferedInputStream(new ByteArrayInputStream(bytes(values)));
	}

	@Test
	public void testDetect() {
		assertEquals(BOM.UTF_8, BOM.detect(bytes(0xEF, 0xBB, 0xBF, 'a')));
		assertEquals(BOM.UTF_16_BE, BOM.detect(bytes(0xFE, 0xFF, 0, 'a')));
		assertEquals(BOM.UTF_16_LE, BOM.detect(bytes(0xFF, 0xFE, 'a', 0)));
		assertEquals(BOM.UTF_32_BE, BOM.detect(bytes(0, 0, 0xFE, 0xFF)));
		// UTF-32LE starts with the UTF-16LE signature and wins
		assertEquals(BOM.UTF_32_LE, BOM.detect(bytes(0xFF, 0xFE, 0, 0)));
		// Too short for UTF-32LE
		assertEquals(BOM.UTF_16_LE, BOM.detect(bytes(0xFF, 0xFE, 0)));
		// Exactly the signature
		assertEquals(BOM.UTF_8, BOM.detect(bytes(0xEF, 0xBB, 0xBF)));
	}

	@Test
	public void testDetectWithoutBom() {
		assertEquals(BOM.NONE, BOM.detect(null));
		assertEquals(BOM.NONE, BOM.detect(new byte[0]));
		assertEquals(BOM.NONE, BOM.detect("abc".getBytes(StandardCharsets.US_ASCII)));
		assertEquals(BOM.NONE, BOM.detect(bytes(0xEF, 0xBB)));
		assertEquals(BOM.NONE, BOM.detect(bytes(0xEF, 0xBB, 0xBE)));
		assertEquals(BOM.NONE, BOM.detect(bytes(0, 0, 0xFE)));
		// A BOM later in the data is no BOM
		assertEquals(BOM.NONE, BOM.detect(bytes('a', 0xEF, 0xBB, 0xBF)));
	}

	@Test
	public void testDetectWithLength() {
		final byte[] data = bytes(0xFF, 0xFE, 0, 0);
		assertEquals(BOM.UTF_32_LE, BOM.detect(data, 4));
		assertEquals(BOM.UTF_16_LE, BOM.detect(data, 3));
		assertEquals(BOM.UTF_16_LE, BOM.detect(data, 2));
		assertEquals(BOM.NONE, BOM.detect(data, 1));
		assertEquals(BOM.NONE, BOM.detect(data, -1));
		// A length beyond the data is limited to the data
		assertEquals(BOM.UTF_32_LE, BOM.detect(data, 100));
		assertEquals(BOM.NONE, BOM.detect(null, 4));
	}

	@Test
	public void testProperties() {
		assertEquals(3, BOM.UTF_8.getLength());
		assertEquals(2, BOM.UTF_16_LE.getLength());
		assertEquals(4, BOM.UTF_32_BE.getLength());
		assertEquals(0, BOM.NONE.getLength());
		assertEquals(0, BOM.NONE.getBytes().length);
		assertNull(BOM.NONE.getCharset());
		assertEquals(StandardCharsets.UTF_8, BOM.UTF_8.getCharset());
		assertEquals(StandardCharsets.UTF_16BE, BOM.UTF_16_BE.getCharset());
		assertEquals(Charset.forName("UTF-32LE"), BOM.UTF_32_LE.getCharset());
		assertEquals("UTF-16 little-endian", BOM.UTF_16_LE.toString());
		assertTrue(BOM.MAX_LENGTH >= BOM.UTF_32_LE.getLength());

		for (final BOM bom : BOM.values()) {
			assertTrue(bom.getLength() <= BOM.MAX_LENGTH, bom.name());
			if (bom != BOM.NONE) {
				// The signature is the encoded BOM character U+FEFF (encoded, because some decoders remove a leading BOM)
				assertEquals(java.util.Arrays.toString(bom.getBytes()), java.util.Arrays.toString(String.valueOf(BOM.BOM_UTF_8_CHAR).getBytes(bom.getCharset())), bom.name());
				assertEquals(bom, BOM.detect(bom.getBytes()), bom.name());
			}
		}
		assertEquals(BOM.BOM_UTF_8_CHAR_ISO_8859, new String(BOM.UTF_8.getBytes(), StandardCharsets.ISO_8859_1).charAt(0));
	}

	@Test
	public void testGetBytesReturnsCopy() {
		final byte[] bomBytes = BOM.UTF_8.getBytes();
		bomBytes[0] = 0;
		assertEquals((byte) 0xEF, BOM.UTF_8.getBytes()[0]);
		assertEquals(BOM.UTF_8, BOM.detect(bytes(0xEF, 0xBB, 0xBF)));
	}

	@Test
	public void testIsCompatibleWith() {
		assertTrue(BOM.UTF_8.isCompatibleWith(StandardCharsets.UTF_8));
		assertFalse(BOM.UTF_8.isCompatibleWith(StandardCharsets.ISO_8859_1));
		assertTrue(BOM.UTF_16_LE.isCompatibleWith(StandardCharsets.UTF_16LE));
		assertTrue(BOM.UTF_16_LE.isCompatibleWith(StandardCharsets.UTF_16));
		assertFalse(BOM.UTF_16_LE.isCompatibleWith(StandardCharsets.UTF_16BE));
		assertTrue(BOM.UTF_16_BE.isCompatibleWith(StandardCharsets.UTF_16));
		assertTrue(BOM.UTF_32_LE.isCompatibleWith(Charset.forName("UTF-32")));
		assertTrue(BOM.UTF_32_BE.isCompatibleWith(Charset.forName("UTF-32BE")));
		assertFalse(BOM.UTF_32_BE.isCompatibleWith(StandardCharsets.UTF_16));
		assertFalse(BOM.UTF_8.isCompatibleWith(null));
		assertTrue(BOM.NONE.isCompatibleWith(null));
		assertTrue(BOM.NONE.isCompatibleWith(StandardCharsets.UTF_16LE));
		if (Charset.isSupported("x-UTF-16LE-BOM")) {
			assertTrue(BOM.UTF_16_LE.isCompatibleWith(Charset.forName("x-UTF-16LE-BOM")));
		}
	}

	@Test
	public void testDetectAndSkip() throws Exception {
		final InputStream utf8 = stream(0xEF, 0xBB, 0xBF, 'x', 'y');
		assertEquals(BOM.UTF_8, BOM.detectAndSkip(utf8));
		assertEquals('x', utf8.read());

		final InputStream utf32 = stream(0xFF, 0xFE, 0, 0, 'x', 0, 0, 0);
		assertEquals(BOM.UTF_32_LE, BOM.detectAndSkip(utf32));
		assertEquals('x', utf32.read());

		// Without BOM nothing is consumed
		final InputStream noBom = stream('a', 'b', 'c', 'd', 'e');
		assertEquals(BOM.NONE, BOM.detectAndSkip(noBom));
		assertEquals('a', noBom.read());

		// Data shorter than the longest BOM
		final InputStream shortData = stream(0xFE, 0xFF, 'z');
		assertEquals(BOM.UTF_16_BE, BOM.detectAndSkip(shortData));
		assertEquals('z', shortData.read());

		final InputStream empty = stream();
		assertEquals(BOM.NONE, BOM.detectAndSkip(empty));
		assertEquals(-1, empty.read());
	}

	@Test
	public void testDetectAndSkipWithStreamReturningSingleBytes() throws Exception {
		// A single read() may return fewer bytes than requested
		final InputStream trickleStream = new FilterInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes(0xFF, 0xFE, 0, 0, 'x')))) {
			@Override
			public int read(final byte[] buffer, final int offset, final int length) throws java.io.IOException {
				return super.read(buffer, offset, Math.min(1, length));
			}
		};
		assertEquals(BOM.UTF_32_LE, BOM.detectAndSkip(trickleStream));
		assertEquals('x', trickleStream.read());
	}

	@Test
	public void testDetectAndSkipInvalidStreams() {
		assertThrows(IllegalArgumentException.class, () -> BOM.detectAndSkip(null));
		// ByteArrayInputStream supports mark, a FileInputStream like stream does not
		final InputStream noMarkSupport = new FilterInputStream(new ByteArrayInputStream(new byte[0])) {
			@Override
			public boolean markSupported() {
				return false;
			}
		};
		assertThrows(IllegalArgumentException.class, () -> BOM.detectAndSkip(noMarkSupport));
	}
}
