package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class BitUtilitiesTest {
	private static byte[] bytes(final int... values) {
		final byte[] result = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (byte) values[i];
		}
		return result;
	}

	private static byte[] allByteValues() {
		final byte[] data = new byte[256];
		for (int i = 0; i < 256; i++) {
			data[i] = (byte) i;
		}
		return data;
	}

	@Test
	public void testGetUnsignedValue() {
		assertEquals(0, BitUtilities.getUnsignedValue((byte) 0));
		assertEquals(127, BitUtilities.getUnsignedValue((byte) 127));
		assertEquals(128, BitUtilities.getUnsignedValue((byte) -128));
		assertEquals(255, BitUtilities.getUnsignedValue((byte) -1));
		assertEquals(Arrays.toString(new int[] { 0, 1, 128, 255 }), Arrays.toString(BitUtilities.getUnsignedValue(bytes(0, 1, 0x80, 0xFF))));
		assertEquals(0, BitUtilities.getUnsignedValue(new byte[0]).length);
	}

	@Test
	public void testGetUnsignedAsByte() {
		assertEquals((byte) 0, BitUtilities.getUnsignedAsByte(0));
		assertEquals((byte) 127, BitUtilities.getUnsignedAsByte(127));
		assertEquals((byte) -128, BitUtilities.getUnsignedAsByte(128));
		assertEquals((byte) -1, BitUtilities.getUnsignedAsByte(255));
		for (int i = 0; i < 256; i++) {
			assertEquals(i, BitUtilities.getUnsignedValue(BitUtilities.getUnsignedAsByte(i)));
		}
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.getUnsignedAsByte(-1));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.getUnsignedAsByte(256));
	}

	@Test
	public void testGetBitStringByte() {
		assertEquals("00000000", BitUtilities.getBitString((byte) 0));
		assertEquals("00001010", BitUtilities.getBitString((byte) 10));
		assertEquals("10000000", BitUtilities.getBitString((byte) 0x80));
		assertEquals("11111111", BitUtilities.getBitString((byte) -1));
	}

	@Test
	public void testGetBitStringInt() {
		assertEquals("0".repeat(29) + "101", BitUtilities.getBitString(5));
		assertEquals("1".repeat(32), BitUtilities.getBitString(-1));
		assertEquals("1" + "0".repeat(31), BitUtilities.getBitString(Integer.MIN_VALUE));
		assertEquals("0" + "1".repeat(31), BitUtilities.getBitString(Integer.MAX_VALUE));
	}

	@Test
	public void testGetBitStringByteArray() {
		assertEquals("00000001 11111111", BitUtilities.getBitString(bytes(1, 0xFF)));
		assertEquals("00000001|11111111", BitUtilities.getBitString(bytes(1, 0xFF), "|"));
		assertEquals("0000000111111111", BitUtilities.getBitString(bytes(1, 0xFF), null));
		assertEquals("10101010", BitUtilities.getBitString(bytes(0xAA)));
		assertEquals("", BitUtilities.getBitString(new byte[0]));
	}

	@Test
	public void testGetByteFromBitString() throws Exception {
		assertEquals((byte) 0, BitUtilities.getByteFromBitString("0"));
		assertEquals((byte) 5, BitUtilities.getByteFromBitString("101"));
		assertEquals((byte) 10, BitUtilities.getByteFromBitString("00001010"));
		assertEquals((byte) -1, BitUtilities.getByteFromBitString("11111111"));
		assertEquals((byte) -128, BitUtilities.getByteFromBitString("10000000"));
		assertEquals((byte) 5, BitUtilities.getByteFromBitString(" 101\t"));
		for (int i = 0; i < 256; i++) {
			assertEquals((byte) i, BitUtilities.getByteFromBitString(BitUtilities.getBitString((byte) i)));
		}

		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString(null));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString(""));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString("  "));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString("111111111"));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString("102"));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString("1 0"));
		assertThrows(Exception.class, () -> BitUtilities.getByteFromBitString("-1"));
	}

	@Test
	public void testGetIntegerFromBitString() throws Exception {
		assertEquals(5, BitUtilities.getIntegerFromBitString("101"));
		assertEquals(-1, BitUtilities.getIntegerFromBitString("1".repeat(32)));
		assertEquals(Integer.MIN_VALUE, BitUtilities.getIntegerFromBitString("1" + "0".repeat(31)));
		assertEquals(Integer.MAX_VALUE, BitUtilities.getIntegerFromBitString("1".repeat(31)));
		assertEquals(5, BitUtilities.getIntegerFromBitString(" 101 "));
		for (final int value : new int[] { 0, 1, -1, 123456789, -987654321, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
			assertEquals(value, BitUtilities.getIntegerFromBitString(BitUtilities.getBitString(value)));
		}

		assertThrows(Exception.class, () -> BitUtilities.getIntegerFromBitString(null));
		assertThrows(Exception.class, () -> BitUtilities.getIntegerFromBitString(""));
		assertThrows(Exception.class, () -> BitUtilities.getIntegerFromBitString("1".repeat(33)));
		assertThrows(Exception.class, () -> BitUtilities.getIntegerFromBitString("12"));
	}

	@Test
	public void testFromHexString() {
		assertNull(BitUtilities.fromHexString(null));
		assertArrayEquals(new byte[0], BitUtilities.fromHexString(""));
		assertArrayEquals(new byte[0], BitUtilities.fromHexString("0x"));
		assertArrayEquals(bytes(0x0A, 0xFF), BitUtilities.fromHexString("0AFF"));
		assertArrayEquals(bytes(0x0A, 0xFF), BitUtilities.fromHexString("0aff"));
		assertArrayEquals(bytes(0x0A, 0xFF), BitUtilities.fromHexString("0x0aFf"));
		assertArrayEquals(bytes(0x0A, 0xFF), BitUtilities.fromHexString("0X0AFF"));
		assertArrayEquals(allByteValues(), BitUtilities.fromHexString(BitUtilities.toHexString(allByteValues())));
	}

	@Test
	public void testFromHexStringInvalid() {
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0A1"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0G"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0A 1B"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0A:1B"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("x0A"));
		// Only ASCII hex digits, Character.digit would accept fullwidth and Arabic-Indic digits
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("\uFF10\uFF11"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("\u0661\u0662"));
	}

	@Test
	public void testFromHexStringIgnoringNonHexCharacters() {
		assertNull(BitUtilities.fromHexString(null, true));
		assertArrayEquals(bytes(0x0A, 0x1B, 0x2C), BitUtilities.fromHexString("0A:1B:2C", true));
		assertArrayEquals(bytes(0x0A, 0x1B, 0x2C), BitUtilities.fromHexString("0x0A, 0x1B, 0X2C", true));
		assertArrayEquals(bytes(0x0A, 0x1B, 0x2C), BitUtilities.fromHexString("0a 1b\n2c", true));
		assertArrayEquals(bytes(0x0A, 0x1B), BitUtilities.fromHexString("0x0A1B", true));
		// An 'x' directly after a hex digit is a separator, not a prefix
		assertArrayEquals(bytes(0x10, 0x20), BitUtilities.fromHexString("10x20", true));
		// Single digit bytes cannot be told apart after removing the separators
		assertArrayEquals(bytes(0x12), BitUtilities.fromHexString("1:2", true));
		assertArrayEquals(new byte[0], BitUtilities.fromHexString("::", true));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0A:1", true));

		// Without ignoring, the separators are invalid
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.fromHexString("0A:1B", false));
		assertArrayEquals(bytes(0x0A, 0x1B), BitUtilities.fromHexString("0A1B", false));
	}

	@Test
	public void testToHexString() {
		assertNull(BitUtilities.toHexString(null));
		assertEquals("", BitUtilities.toHexString(new byte[0]));
		assertEquals("000AFF7F80", BitUtilities.toHexString(bytes(0, 0x0A, 0xFF, 0x7F, 0x80)));
	}

	@Test
	public void testToHexStringWithSeparator() {
		assertNull(BitUtilities.toHexString(null, ":"));
		assertEquals("", BitUtilities.toHexString(new byte[0], ":"));
		assertEquals("0a:ff:00", BitUtilities.toHexString(bytes(0x0A, 0xFF, 0), ":"));
		assertEquals("0aff00", BitUtilities.toHexString(bytes(0x0A, 0xFF, 0), null));
		assertEquals("0a, ff", BitUtilities.toHexString(bytes(0x0A, 0xFF), ", "));
		assertEquals("7f", BitUtilities.toHexString(bytes(0x7F), ":"));
		// Separated output can be decoded again
		assertArrayEquals(allByteValues(), BitUtilities.fromHexString(BitUtilities.toHexString(allByteValues(), ":"), true));
	}

	@Test
	public void testGetHexStringSimple() {
		assertNull(BitUtilities.getHexString(null, " ", 16, true, true));
		assertEquals("", BitUtilities.getHexString(new byte[0], " ", 16, true, true));
		assertEquals("41 42 43", BitUtilities.getHexString(bytes(0x41, 0x42, 0x43), " ", 0, false, false));
		assertEquals("414243", BitUtilities.getHexString(bytes(0x41, 0x42, 0x43), null, 0, false, false));
		assertEquals("0AFF", BitUtilities.getHexString(bytes(0x0A, 0xFF), null, 0, false, false));
		assertEquals("41 42 43 ; ABC", BitUtilities.getHexString(bytes(0x41, 0x42, 0x43), " ", 0, false, true));
		assertEquals("00000000h: 41 42 43", BitUtilities.getHexString(bytes(0x41, 0x42, 0x43), " ", 0, true, false));
	}

	@Test
	public void testGetHexStringWithLines() {
		final byte[] data = "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII);
		assertEquals("41 42 43 44\n45 46 47 48\n49 4A", BitUtilities.getHexString(data, " ", 4, false, false));
		assertEquals("00000000h: 41 42 43 44\n00000004h: 45 46 47 48\n00000008h: 49 4A", BitUtilities.getHexString(data, " ", 4, true, false));
		// The ASCII part of the last line is aligned with the lines before
		assertEquals("00000000h: 41 42 43 44 ; ABCD\n"
				+ "00000004h: 45 46 47 48 ; EFGH\n"
				+ "00000008h: 49 4A       ; IJ", BitUtilities.getHexString(data, " ", 4, true, true));
		assertEquals("41424344 ; ABCD\n"
				+ "45464748 ; EFGH\n"
				+ "494A     ; IJ", BitUtilities.getHexString(data, null, 4, false, true));
		assertEquals("41, 42, 43, 44 ; ABCD\n"
				+ "45, 46, 47, 48 ; EFGH\n"
				+ "49, 4A         ; IJ", BitUtilities.getHexString(data, ", ", 4, false, true));
		// Full last line needs no padding
		assertEquals("41 42 ; AB\n43 44 ; CD", BitUtilities.getHexString(Arrays.copyOf(data, 4), " ", 2, false, true));
	}

	@Test
	public void testGetHexStringLineNumbersAreHexadecimalOffsets() {
		final String dump = BitUtilities.getHexString(Arrays.copyOf(allByteValues(), 33), " ", 16, true, false);
		final String[] lines = dump.split("\n");
		assertEquals(3, lines.length);
		assertTrue(lines[0].startsWith("00000000h: 00 01 02"));
		assertTrue(lines[0].endsWith("0E 0F"));
		assertTrue(lines[1].startsWith("00000010h: 10 11"));
		assertEquals("00000020h: 20", lines[2]);
	}

	@Test
	public void testGetHexStringAsciiOfNonPrintableBytes() {
		// Control characters, DEL and bytes >= 0x80 are shown as '.'
		assertEquals("00 1F 20 7E 7F 80 FF ; .. ~...", BitUtilities.getHexString(bytes(0x00, 0x1F, 0x20, 0x7E, 0x7F, 0x80, 0xFF), " ", 0, false, true));
	}

	@Test
	public void testGetByteArrayFromHexString() {
		assertArrayEquals(bytes(0x0A, 0xFF), BitUtilities.getByteArrayFromHexString("0AFF"));
		assertArrayEquals(bytes(0x0A), BitUtilities.getByteArrayFromHexString("0x0a"));
		assertArrayEquals(new byte[0], BitUtilities.getByteArrayFromHexString(""));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.getByteArrayFromHexString(null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.getByteArrayFromHexString("0A:FF"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.getByteArrayFromHexString("ABC"));
	}

	@Test
	public void testXorArrays() {
		assertArrayEquals(bytes(0x02, 0x06), BitUtilities.xor(bytes(1, 2), bytes(3, 4)));
		assertArrayEquals(bytes(0x01 ^ 0x10 ^ 0xFF), BitUtilities.xor(bytes(0x01), bytes(0x10), bytes(0xFF)));
		assertArrayEquals(new byte[0], BitUtilities.xor(new byte[0], new byte[0]));

		// XOR with itself is zero, XOR twice with the same key restores the data
		final byte[] data = "secret".getBytes(StandardCharsets.UTF_8);
		final byte[] key = "keykey".getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(new byte[data.length], BitUtilities.xor(data, data));
		assertArrayEquals(data, BitUtilities.xor(BitUtilities.xor(data, key), key));
	}

	@Test
	public void testXorDoesNotModifyOrReturnInput() {
		final byte[] a = bytes(1, 2);
		final byte[] b = bytes(3, 4);
		BitUtilities.xor(a, b);
		assertArrayEquals(bytes(1, 2), a);
		assertArrayEquals(bytes(3, 4), b);

		final byte[] single = bytes(5, 6);
		final byte[] result = BitUtilities.xor(single, new byte[2]);
		assertArrayEquals(single, result);
		assertNotSame(single, result);
	}

	@Test
	public void testXorList() {
		final List<byte[]> arrays = new ArrayList<>();
		arrays.add(bytes(0xF0));
		arrays.add(bytes(0x0F));
		assertArrayEquals(bytes(0xFF), BitUtilities.xor(arrays));
		assertArrayEquals(bytes(0xAB), BitUtilities.xor(Arrays.asList(bytes(0xAB))));
	}

	@Test
	public void testXorInvalidArguments() {
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor((byte[][]) null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(new byte[0][]));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor((List<byte[]>) null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(new ArrayList<>()));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(bytes(1), bytes(1, 2)));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(bytes(1, 2), bytes(1)));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(bytes(1), null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.xor(null, bytes(1)));
	}

	@Test
	public void testXorBytes() {
		assertEquals((byte) 7, BitUtilities.xor((byte) 1, (byte) 2, (byte) 4));
		assertEquals((byte) 0, BitUtilities.xor((byte) 0x5A, (byte) 0x5A));
		assertEquals((byte) 0xFF, BitUtilities.xor((byte) 0xF0, (byte) 0x0F));
		assertEquals((byte) 0x42, BitUtilities.xor((byte) 0x42));
		// A single byte array argument selects xor(byte...)
		final byte noBytesResult = BitUtilities.xor(new byte[0]);
		assertEquals((byte) 0, noBytesResult);
		final byte arrayResult = BitUtilities.xor(bytes(1, 2, 4, 8));
		assertEquals((byte) 15, arrayResult);
	}

	@Test
	public void testJoinByteArrays() {
		assertArrayEquals(bytes(1, 2, 3), BitUtilities.joinByteArrays(bytes(1), new byte[0], bytes(2, 3)));
		assertArrayEquals(new byte[0], BitUtilities.joinByteArrays());
		assertArrayEquals(bytes(9), BitUtilities.joinByteArrays(bytes(9)));
		final byte[] single = bytes(9);
		assertNotSame(single, BitUtilities.joinByteArrays(single));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.joinByteArrays((byte[][]) null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.joinByteArrays(bytes(1), null));
	}

	@Test
	public void testAddLengthCodedPadding() {
		assertArrayEquals(bytes(1, 2, 3, 1), BitUtilities.addLengthCodedPadding(bytes(1, 2, 3), 4));
		assertArrayEquals(bytes(1, 2, 2), BitUtilities.addLengthCodedPadding(bytes(1), 3));
		// Aligned data gets a full block
		assertArrayEquals(bytes(1, 2, 3, 4, 4, 4, 4, 4), BitUtilities.addLengthCodedPadding(bytes(1, 2, 3, 4), 4));
		assertArrayEquals(bytes(8, 8, 8, 8, 8, 8, 8, 8), BitUtilities.addLengthCodedPadding(new byte[0], 8));
		assertArrayEquals(bytes(7, 1), BitUtilities.addLengthCodedPadding(bytes(7), 1));

		final byte[] padded255 = BitUtilities.addLengthCodedPadding(new byte[10], 255);
		assertEquals(255, padded255.length);
		assertEquals(245, padded255[254] & 0xFF);

		assertThrows(IllegalArgumentException.class, () -> BitUtilities.addLengthCodedPadding(bytes(1), 0));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.addLengthCodedPadding(bytes(1), -1));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.addLengthCodedPadding(bytes(1), 256));
	}

	@Test
	public void testAddLengthCodedPaddingDoesNotModifyInput() {
		final byte[] data = bytes(1, 2, 3);
		BitUtilities.addLengthCodedPadding(data, 8);
		assertArrayEquals(bytes(1, 2, 3), data);
	}

	@Test
	public void testLengthCodedPaddingRoundTrip() {
		for (final int paddingSize : new int[] { 1, 2, 8, 16, 255 }) {
			for (int length = 0; length <= 40; length++) {
				final byte[] data = Arrays.copyOf(allByteValues(), length);
				final byte[] padded = BitUtilities.addLengthCodedPadding(data, paddingSize);
				assertEquals(0, padded.length % paddingSize);
				assertTrue(padded.length > data.length);
				assertArrayEquals(data, BitUtilities.removeLengthCodedPadding(padded, paddingSize), "Size " + paddingSize + ", length " + length);
			}
		}
	}

	@Test
	public void testRemoveLengthCodedPaddingInvalid() {
		// Padding byte 0 and padding longer than the block size are invalid
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(1, 2, 3, 0), 4));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(5, 5, 5, 5), 4));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(1, 2, 3, 4, 5, 5, 5, 5), 4));
		// Inconsistent padding bytes
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(1, 2, 3, 3, 2, 3), 3));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(9, 4, 4, 4), 4));
		// Invalid lengths
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(1, 2, 1), 2));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(new byte[0], 4));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(null, 4));
		// Invalid padding sizes
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(bytes(1), 0));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.removeLengthCodedPadding(new byte[256], 256));
	}

	@Test
	public void testRemoveLengthCodedPaddingWholeBlock() {
		assertArrayEquals(new byte[0], BitUtilities.removeLengthCodedPadding(bytes(4, 4, 4, 4), 4));
		assertArrayEquals(bytes(1, 2, 3, 4), BitUtilities.removeLengthCodedPadding(bytes(1, 2, 3, 4, 4, 4, 4, 4), 4));
		// Only the last block is checked, earlier data looking like padding stays
		assertArrayEquals(bytes(2, 2, 1, 1), BitUtilities.removeLengthCodedPadding(bytes(2, 2, 1, 1, 2, 2), 2));
	}

	@Test
	public void testAddRandomPadding() {
		final byte[] data = bytes(1, 2, 3);
		final byte[] padded = BitUtilities.addRandomPadding(data, 8);
		assertEquals(8, padded.length);
		assertArrayEquals(data, Arrays.copyOf(padded, 3));
		assertArrayEquals(bytes(1, 2, 3), data);

		assertEquals(16, BitUtilities.addRandomPadding(new byte[9], 8).length);
		assertEquals(1000, BitUtilities.addRandomPadding(new byte[1], 1000).length);
	}

	@Test
	public void testAddRandomPaddingAlignedDataReturnsCopy() {
		final byte[] data = bytes(1, 2, 3, 4);
		final byte[] padded = BitUtilities.addRandomPadding(data, 4);
		assertArrayEquals(data, padded);
		assertNotSame(data, padded);
		assertArrayEquals(new byte[0], BitUtilities.addRandomPadding(new byte[0], 8));
		assertArrayEquals(bytes(7), BitUtilities.addRandomPadding(bytes(7), 1));
	}

	@Test
	public void testAddRandomPaddingIsRandom() {
		final byte[] first = BitUtilities.addRandomPadding(new byte[1], 33);
		final byte[] second = BitUtilities.addRandomPadding(new byte[1], 33);
		// 32 random bytes are equal with probability 2^-256
		assertFalse(Arrays.equals(first, second));
	}

	@Test
	public void testAddRandomPaddingInvalidSize() {
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.addRandomPadding(bytes(1), 0));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.addRandomPadding(bytes(1), -8));
	}

	@Test
	public void testHexToByteString() {
		assertEquals((byte) 0x0A, BitUtilities.hexToByte("0a"));
		assertEquals((byte) 0x0A, BitUtilities.hexToByte("0A"));
		assertEquals((byte) -1, BitUtilities.hexToByte("FF"));
		assertEquals((byte) 0x7F, BitUtilities.hexToByte("7F"));
		// Further characters are ignored
		assertEquals((byte) 0x12, BitUtilities.hexToByte("12xyz"));

		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte(null));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte(""));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte("A"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte("0G"));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte("-1"));
	}

	@Test
	public void testHexToByteChars() {
		assertEquals((byte) 0x7F, BitUtilities.hexToByte('7', 'f'));
		assertEquals((byte) 0x80, BitUtilities.hexToByte('8', '0'));
		assertEquals((byte) 0, BitUtilities.hexToByte('0', '0'));
		for (int i = 0; i < 256; i++) {
			final String hex = BitUtilities.byteToHex((byte) i);
			assertEquals((byte) i, BitUtilities.hexToByte(hex.charAt(0), hex.charAt(1)));
			assertEquals((byte) i, BitUtilities.hexToByte(hex.toLowerCase().charAt(0), hex.toLowerCase().charAt(1)));
		}
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte('G', '0'));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte('0', 'g'));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte(' ', '0'));
		assertThrows(IllegalArgumentException.class, () -> BitUtilities.hexToByte('\uFF11', '0'));
	}

	@Test
	public void testByteToHex() {
		assertEquals("00", BitUtilities.byteToHex((byte) 0));
		assertEquals("0A", BitUtilities.byteToHex((byte) 10));
		assertEquals("7F", BitUtilities.byteToHex((byte) 127));
		assertEquals("80", BitUtilities.byteToHex((byte) -128));
		assertEquals("FF", BitUtilities.byteToHex((byte) -1));
		assertEquals(BitUtilities.toHexString(allByteValues()), joinByteToHex(allByteValues()));
	}

	private static String joinByteToHex(final byte[] data) {
		final StringBuilder result = new StringBuilder();
		for (final byte value : data) {
			result.append(BitUtilities.byteToHex(value));
		}
		return result.toString();
	}
}
