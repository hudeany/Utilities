package de.soderer.utilities.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encoder / Decoder for the ASN.1 format
 */
public class Asn1Codec {
	/**
	 * ASN.1 "INTEGER" (0x02 = 2)
	 */
	public static final int DER_TAG_INTEGER = 0x02;

	/**
	 * ASN.1 "BIT STRING" (0x03 = 3)
	 */
	public static final int DER_TAG_BIT_STRING = 0x03;

	/**
	 * ASN.1 "OCTET STRING" (0x04 = 4)
	 */
	public static final int DER_TAG_OCTET_STRING = 0x04;

	/**
	 * ASN.1 "OBJECT" (0x06 = 6)
	 */
	public static final int DER_TAG_OBJECT = 0x06;

	/**
	 * ASN.1 "SEQUENCE" (0x30 = 48)
	 */
	public static final int DER_TAG_SEQUENCE = 0x30;

	/**
	 * ASN.1 CONTEXT SPECIFIC "cont [ 0 ]" (0xA0 = -96 = unsigned 160)
	 */
	public static final int DER_TAG_CONTEXT_SPECIFIC_0 = 0xA0;

	/**
	 * ASN.1 CONTEXT SPECIFIC "cont [ 1 ]" (0xA1 = -95 = unsigned 161)
	 */
	public static final int DER_TAG_CONTEXT_SPECIFIC_1 = 0xA1;

	private Asn1Codec() {
		// Utility class
	}

	/**
	 * Encodes a non-negative value in the DER length format: values below 0x80 as a single byte (short form),
	 * larger values as a byte 0x80 + n followed by the n big-endian value bytes (long form).
	 *
	 * @param value non-negative value to encode
	 * @return DER encoded value bytes
	 * @throws Exception if the value is negative or needs 128 or more value bytes
	 */
	public static byte[] getAsnEncodedInteger(final long value) throws Exception {
		if (value < 0) {
			throw new Exception("Minimum ASN.1 encoded Integer underrun");
		} else if (value < 0x80) {
			return new byte[] { (byte) value };
		} else {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] data = BigInteger.valueOf(value).toByteArray();
			if (data[0] == 0) {
				// Removed the obsolete sign bit, which caused an additional byte
				final byte[] tmp = new byte[data.length - 1];
				System.arraycopy(data, 1, tmp, 0, tmp.length);
				data = tmp;
			}
			if (data.length >= 0x80) {
				throw new Exception("Maximum ASN.1 encoded Integer exceeded");
			}
			out.write(0x80 | data.length);
			out.write(data);
			return out.toByteArray();
		}
	}

	/**
	 * Parses a DER length-prefixed integer value (the content of an ASN.1 INTEGER without its tag byte).
	 * {@code offset} points to the length byte (short or long form DER length encoding), which is directly
	 * followed by the integer's value bytes in two's complement big-endian representation.
	 *
	 * @param data data containing the encoded integer
	 * @param offset index of the length byte within the data
	 * @return parsed (signed) integer value
	 * @throws Exception if the data is null, the offset is out of range or the data is truncated or invalid
	 */
	public static BigInteger parseAsnEncodedInteger(final byte[] data, final int offset) throws Exception {
		if (data == null || offset < 0 || offset >= data.length) {
			throw new Exception("Invalid data or offset for ASN.1 integer parsing");
		}
		try {
			int pos = offset;
			final int lengthIndicatingValue = data[pos++] & 0xFF;
			int length;
			if (lengthIndicatingValue < 0x80) {
				length = lengthIndicatingValue;
			} else {
				final int lengthBytesCount = lengthIndicatingValue & 0x7F;
				if (pos + lengthBytesCount > data.length) {
					throw new Exception("Truncated ASN.1 length encoding");
				}
				length = 0;
				for (int i = 0; i < lengthBytesCount; i++) {
					length = (length << 8) | (data[pos++] & 0xFF);
				}
			}
			if (length < 0 || pos + length > data.length) {
				throw new Exception("Invalid or truncated ASN.1 integer length: " + length);
			}
			final byte[] valueBytes = new byte[length];
			System.arraycopy(data, pos, valueBytes, 0, length);
			return new BigInteger(valueBytes);
		} catch (final Exception e) {
			throw new Exception("Block read error", e);
		}
	}

	/**
	 * Creates a DER encoded tag consisting of the tag id, the DER encoded length and the concatenated data items.
	 *
	 * @param derTagId tag id, e.g. {@link #DER_TAG_SEQUENCE}
	 * @param derDataItems data items to concatenate as the tag's content (must not be null)
	 * @return DER encoded tag data
	 * @throws IOException if the data cannot be written
	 */
	public static byte[] createDerTagData(final int derTagId, final byte[]... derDataItems) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(derTagId);
		int dataItemsLength = 0;
		for (final byte[] dataItem : derDataItems) {
			dataItemsLength += dataItem.length;
		}
		if (dataItemsLength < 0x80) {
			out.write(dataItemsLength);
		} else {
			final int bytes = getByteEncodedLength(dataItemsLength);
			out.write(0x80 | bytes);
			for (int i = bytes - 1; i >= 0; i--) {
				out.write((dataItemsLength >> (8 * i)) & 0xFF);
			}
		}
		for (final byte[] dataItem : derDataItems) {
			out.write(dataItem);
		}
		return out.toByteArray();
	}

	private static int getByteEncodedLength(int value) {
		int lengthInBytes = 0;
		while (value > 0) {
			lengthInBytes++;
			value = value >> 8;
		}
		return lengthInBytes;
	}

	/**
	 * Reads the first DER tag of the given data. Any data following that tag is ignored.
	 *
	 * @param data DER encoded data
	 * @return the first tag with its content data
	 * @throws Exception if the data is truncated or the encoded length does not match the available data
	 */
	public static DerTag readDerTag(final byte[] data) throws Exception {
		try {
			final ByteArrayInputStream input = new ByteArrayInputStream(data);

			final int tagId = input.read();

			int tagLength;
			final int lengthIndicatingValue = input.read();
			if (lengthIndicatingValue < 0x80) {
				tagLength = lengthIndicatingValue;
			} else {
				final int tagLengthBytesCount = lengthIndicatingValue - 0x80;
				tagLength = 0;
				for (int i = 0; i < tagLengthBytesCount; i++) {
					final int nextValue = input.read();
					tagLength = (tagLength << 8) + nextValue;
				}
			}

			// This tagLength might be invalid encoded, because of invalid decryption.
			// So to prevent out of memory exception, read from input up to tagLength and stop if input hits EOF.
			final ByteArrayOutputStream tagData = new ByteArrayOutputStream();
			final byte[] buffer = new byte[8192];
			int bytesRead = 0;
			int tagLengthLeftToRead = tagLength;
			while (tagLengthLeftToRead > 0 && (bytesRead = input.read(buffer, 0, Math.min(buffer.length, tagLengthLeftToRead))) > 0) {
				tagData.write(buffer, 0, bytesRead);
				tagLengthLeftToRead = tagLengthLeftToRead - bytesRead;
			}
			final byte[] blockData = tagData.toByteArray();

			if (blockData.length != tagLength) {
				throw new Exception("Block length read error. Blocksize: " + data.length + " Tagsize: " + tagLength);
			} else {
				return new DerTag(tagId, tagData.toByteArray());
			}
		} catch (final IOException e) {
			throw new Exception("Block read error", e);
		}
	}

	/**
	 * Reads all consecutive DER tags of the given data (e.g. the content of a SEQUENCE).
	 *
	 * @param data DER encoded data
	 * @return list of all tags with their content data in the order of the data
	 * @throws Exception if the data is truncated or contains an invalid length
	 */
	public static List<DerTag> readDerTags(final byte[] data) throws Exception {
		try {
			final ByteArrayInputStream input = new ByteArrayInputStream(data);
			final List<DerTag> returnList = new ArrayList<>();
			while (input.available() > 0) {
				final int tagId = input.read();

				int tagLength;
				final int lengthIndicatingValue = input.read();
				if (lengthIndicatingValue < 0) {
					throw new Exception("Unexpected end of data while reading tag length");
				} else if (lengthIndicatingValue < 0x80) {
					tagLength = lengthIndicatingValue;
				} else {
					final int tagLengthBytesCount = lengthIndicatingValue & 0x7F;
					tagLength = 0;
					for (int i = 0; i < tagLengthBytesCount; i++) {
						final int nextByte = input.read();
						if (nextByte < 0) {
							throw new Exception("Unexpected end of data while reading tag length");
						}
						tagLength = (tagLength << 8) + nextByte;
					}
				}
				if (tagLength < 0 || tagLength > input.available()) {
					throw new Exception("Invalid or truncated tag length: " + tagLength);
				}

				final byte[] dataBlock = new byte[tagLength];
				input.read(dataBlock);

				returnList.add(new DerTag(tagId, dataBlock));
			}
			return returnList;
		} catch (final IOException e) {
			throw new Exception("Block read error", e);
		}
	}

	/**
	 * A single DER tag: tag id and content data (without the tag id and length bytes).
	 */
	public static class DerTag {
		int tagId;
		byte[] data;

		/**
		 * Creates a DER tag.
		 *
		 * @param tagId tag id, e.g. {@link Asn1Codec#DER_TAG_SEQUENCE}
		 * @param data content data of the tag (stored by reference, not copied)
		 */
		public DerTag(final int tagId, final byte[] data) {
			this.tagId = tagId;
			this.data = data;
		}

		/**
		 * Returns the tag id.
		 *
		 * @return tag id
		 */
		public int getTagId() {
			return tagId;
		}

		/**
		 * Sets the tag id.
		 *
		 * @param tagId tag id
		 */
		public void setTagId(final int tagId) {
			this.tagId = tagId;
		}

		/**
		 * Returns the content data of the tag.
		 *
		 * @return content data (the internal array, not a copy)
		 */
		public byte[] getData() {
			return data;
		}

		/**
		 * Sets the content data of the tag.
		 *
		 * @param data content data (stored by reference, not copied)
		 */
		public void setData(final byte[] data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return tagId + " (length " + data.length + "): " + Arrays.toString(data);
		}
	}
}
