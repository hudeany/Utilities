package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * ZipDecryptInputStream<br />
 * <br />
 * Example:<br />
 * InputStream inputStream = new ZipInputStream(new ZipDecryptInputStream(new FileInputStream(new File("path to zip file")), "password".toCharArray()));<br />
 * ((ZipInputStream) inputStream).getNextEntry();<br />
 * byte[] decryptedUnzippedFileData = IOUtils.toByteArray(inputStream);<br />
 */
public class ZipDecryptInputStream extends InputStream {
	private enum State {
		SIGNATURE,
		FLAGS,
		COMPRESSED_SIZE,
		FN_LENGTH,
		EF_LENGTH,
		HEADER,
		DATA,
		TAIL
	}

	private static final int[] CRC_TABLE = new int[256];
	static {
		for (int i = 0; i < 256; i++) {
			int r = i;
			for (int j = 0; j < 8; j++) {
				if ((r & 1) == 1) {
					r = (r >>> 1) ^ 0xedb88320;
				} else {
					r >>>= 1;
				}
			}
			CRC_TABLE[i] = r;
		}
	}

	private static final int DECRYPT_HEADER_SIZE = 12;
	private static final int[] LFH_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};

	private final InputStream encryptedZipDataInputStream;
	private final char[] password;
	private final int keys[] = new int[3];

	private State state = State.SIGNATURE;
	private int skipBytes;
	private int compressedSize;
	private int value;
	private int valuePos;
	private int valueInc;

	/**
	 * Only supports ZipCrypto (PKZIP) (weak crypto algorithm).
	 * "Strong encryption" and "WinZip encryption" are not supported.
	 *
	 * @param stream
	 * @param password
	 */
	public ZipDecryptInputStream(final InputStream encryptedZipDataInputStream, final char[] password) {
		this.encryptedZipDataInputStream = encryptedZipDataInputStream;
		this.password = password;
	}

	@Override
	public int read() throws IOException {
		int result = encryptedZipDataInputStream.read();
		if (skipBytes == 0) {
			switch (state) {
				case SIGNATURE:
					if (result != LFH_SIGNATURE[valuePos]) {
						state = State.TAIL;
					} else {
						valuePos++;
						if (valuePos >= LFH_SIGNATURE.length) {
							skipBytes = 2;
							state = State.FLAGS;
						}
					}
					break;
				case FLAGS:
					if ((result & 1) == 0) {
						throw new IllegalStateException("ZIP not password protected.");
					}
					if ((result & 64) == 64) {
						throw new IllegalStateException("Strong encryption used.");
					}
					if ((result & 8) == 8) {
						throw new IllegalStateException("Unsupported ZIP format.");
					}
					result -= 1;
					compressedSize = 0;
					valuePos = 0;
					valueInc = DECRYPT_HEADER_SIZE;
					state = State.COMPRESSED_SIZE;
					skipBytes = 11;
					break;
				case COMPRESSED_SIZE:
					compressedSize += result << (8 * valuePos);
					result -= valueInc;
					if (result < 0) {
						valueInc = 1;
						result += 256;
					} else {
						valueInc = 0;
					}
					valuePos++;
					if (valuePos > 3) {
						valuePos = 0;
						value = 0;
						state = State.FN_LENGTH;
						skipBytes = 4;
					}
					break;
				case FN_LENGTH:
				case EF_LENGTH:
					value += result << 8 * valuePos;
					if (valuePos == 1) {
						valuePos = 0;
						if (state == State.FN_LENGTH) {
							state = State.EF_LENGTH;
						} else {
							state = State.HEADER;
							skipBytes = value;
						}
					} else {
						valuePos = 1;
					}
					break;
				case HEADER:
					initKeys(password);
					for (int i = 0; i < DECRYPT_HEADER_SIZE; i++) {
						updateKeys((byte) (result ^ decryptByte()));
						result = encryptedZipDataInputStream.read();
					}
					compressedSize -= DECRYPT_HEADER_SIZE;
					state = State.DATA;
					// intentionally no break
				case DATA:
					result = (result ^ decryptByte()) & 0xff;
					updateKeys((byte) result);
					compressedSize--;
					if (compressedSize == 0) {
						valuePos = 0;
						state = State.SIGNATURE;
					}
					break;
				case TAIL:
					// nothing to do
			}
		} else {
			skipBytes--;
		}

		return result;
	}

	@Override
	public void close() throws IOException {
		encryptedZipDataInputStream.close();
		super.close();
	}

	private void initKeys(final char[] password) {
		keys[0] = 305419896;
		keys[1] = 591751049;
		keys[2] = 878082192;
		for (int i = 0; i < password.length; i++) {
			updateKeys((byte) (password[i] & 0xff));
		}
	}

	private void updateKeys(final byte nextByte) {
		keys[0] = crc32(keys[0], nextByte);
		keys[1] += keys[0] & 0xff;
		keys[1] = keys[1] * 134775813 + 1;
		keys[2] = crc32(keys[2], (byte) (keys[1] >> 24));
	}

	private byte decryptByte() {
		final int temp = keys[2] | 2;
		return (byte) ((temp * (temp ^ 1)) >>> 8);
	}

	private int crc32(final int previousCrc, final byte nextByte) {
		return ((previousCrc >>> 8) ^ CRC_TABLE[(previousCrc ^ nextByte) & 0xff]);
	}
}
