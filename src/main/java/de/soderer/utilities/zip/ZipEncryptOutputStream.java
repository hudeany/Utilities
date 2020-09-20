package de.soderer.utilities.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ZipEncryptOutputStream<br />
 * <br />
 * Example:<br />
 * OutputStream zipOutputStream = new ZipOutputStream(new ZipEncryptOutputStream(new FileOutputStream(new File("path to zip file")), "password".toCharArray()));<br />
 * ((ZipOutputStream) zipOutputStream).putNextEntry(new ZipEntry("zipped file name"));<br />
 * IOUtils.copy(new ByteArrayInputStream(testString.getBytes("UTF-8")), zipOutputStream);<br />
 * ((ZipOutputStream) zipOutputStream).closeEntry();<br />
 * zipOutputStream.close();<br />
 */
public class ZipEncryptOutputStream extends OutputStream {
	static final int[] CRC_TABLE = new int[256];
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

	static final int DECRYPT_HEADER_SIZE = 12;
	static final int[] CFH_SIGNATURE = {0x50, 0x4b, 0x01, 0x02};
	static final int[] LFH_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
	static final int[] ECD_SIGNATURE = {0x50, 0x4b, 0x05, 0x06};
	static final int[] DD_SIGNATURE = {0x50, 0x4b, 0x07, 0x08};

	private final OutputStream delegate;
	private final int keys[] = new int[3];
	private final int pwdKeys[] = new int[3];

	private int copyBytes;
	private int skipBytes;
	private State state = State.NEW_SECTION;
	private State futureState;
	private Section section;
	private byte[] decryptHeader;
	private final ArrayList<int[][]> crcAndSize = new ArrayList<>();
	private final ArrayList<Integer> localHeaderOffset = new ArrayList<>();
	private ArrayList<int[]> fileData;
	private int[][] condition;
	private int fileIndex;
	private int[] buffer;
	private int bufOffset;
	private int fileSize;
	private int bytesWritten;
	private int centralRepoOffset;

	private static final int ROW_SIZE = 65536;

	/**
	 * Safer version of the constructor. Takes password as a char array that can
	 * be nulled right after calling this constructor instead of a string that may
	 * stay visible on the heap for the duration of application run time.
	 *
	 * @param delegate Output stream to write the password-protected zip to.
	 * @param password Password to use for protecting the zip.
	 */
	public ZipEncryptOutputStream(final OutputStream delegate, final char[] password) {
		this.delegate = delegate;
		pwdKeys[0] = 305419896;
		pwdKeys[1] = 591751049;
		pwdKeys[2] = 878082192;
		for (int i = 0; i < password.length; i++) {
			updateKeys((byte) (password[i] & 0xff), pwdKeys);
		}
	}

	private enum State {NEW_SECTION, SECTION_HEADER, FLAGS, REPO_OFFSET, CRC, FILE_HEADER_OFFSET,
		COMPRESSED_SIZE_READ, HEADER, DATA, FILE_BUFFERED, BUFFER, BUFFER_COPY, BUFFER_UNTIL, TAIL}

	private enum Section {LFH, CFH, ECD}

	@Override
	public void write(int b) throws IOException {
		if (skipBytes > 0) {
			skipBytes--;
			return;
		}
		if (copyBytes == 0) {
			switch (state) {
				case NEW_SECTION:
					if (b != 0x50) {
						throw new IllegalStateException("Unexpected value read at offset " + bytesWritten + ": " + b + " (expected: " + 0x50 + ")");
					}
					buffer(new int[4], State.SECTION_HEADER, 0x50);
					return;
				case SECTION_HEADER:
					identifySectionHeader();
					break;
				case FLAGS:
					copyBytes = 7;
					state = State.CRC;
					if (section == Section.LFH) {
						if ((b & 1) == 1) {
							throw new IllegalStateException("ZIP already password protected.");
						}
						if ((b & 64) == 64) {
							throw new IllegalStateException("Strong encryption used.");
						}
						if ((b & 8) == 8) {
							bufferUntil(State.FILE_BUFFERED, CFH_SIGNATURE, LFH_SIGNATURE);
						}
					}
					b = b & 0xf7 | 1;
					break;
				case CRC:
					if (section == Section.CFH) {
						final int[][] cns = crcAndSize.get(fileIndex);
						for (int j = 0; j < 3; j++) {
							for (int i = 0; i < 4; i++) {
								writeToDelegate(cns[j][i]);
							}
						}
						skipBytes = 11;
						copyBytes = 14;
						state = State.FILE_HEADER_OFFSET;
					} else {
						final int[] cns = new int[16];
						buffer(cns, State.COMPRESSED_SIZE_READ, b);
					}
					return;
				case FILE_HEADER_OFFSET:
					writeAsBytes(localHeaderOffset.get(fileIndex));
					fileIndex++;
					skipBytes = 3;
					copyBytesUntil(State.SECTION_HEADER, CFH_SIGNATURE, ECD_SIGNATURE);
					return;
				case COMPRESSED_SIZE_READ:
					int[][] cns = new int[][] {
						{buffer[0], buffer[1], buffer[2], buffer[3]},
						{buffer[4], buffer[5], buffer[6], buffer[7]},
						{buffer[8], buffer[9], buffer[10], buffer[11]}
					};
					adjustSize(cns[1]);
					crcAndSize.add(cns);
					for (int j = 0; j < 3; j++) {
						for (int i = 0; i < 4; i++) {
							writeToDelegate(cns[j][i]);
						}
					}
					copyBytes = buffer[12] + buffer[14] + (buffer[13] + buffer[15]) * 256 - 1;
					state = State.HEADER;
					if (copyBytes < 0) {
						throw new IllegalStateException("No file name stored in the zip file.");
					}
					break;
				case HEADER:
					writeDecryptHeader();
					fileSize = decode(crcAndSize.get(crcAndSize.size() - 1)[1]);
					state = State.DATA;

					b = encrypt(b);
					fileSize--;
					if (fileSize == 0) {
						state = State.NEW_SECTION;
					}
					break;
				case DATA:
					b = encrypt(b);
					fileSize--;
					if (fileSize == 0) {
						state = State.NEW_SECTION;
					}
					break;
				case BUFFER:
					buffer[bufOffset] = b;
					bufOffset++;
					if (bufOffset == buffer.length) {
						state = futureState;
					}
					return;
				case BUFFER_COPY:
					buffer[bufOffset] = b;
					if (checkCondition()) {
						bufOffset = 0;
						state = futureState;
					}
					break;
				case BUFFER_UNTIL:
					final int col = fileSize % ROW_SIZE;
					if (col == 0) {
						fileData.add(new int[ROW_SIZE]);
					}
					int[] row = fileData.get(fileData.size() - 1);
					row[col] = b;
					buffer[bufOffset] = b;
					fileSize++;
					if (checkCondition()) {
						fileSize -= buffer.length;
						state = futureState;
					}
					return;
				case FILE_BUFFERED:
					row = fileData.get(0);
					int r = 0;
					int pointer = 16 + row[12] + row[14] + (row[13] + row[15]) * 256;
					cns = new int[3][4];
					readFromFileBuffer(fileSize - 12, cns[0]);
					readFromFileBuffer(fileSize - 8, cns[1]);
					readFromFileBuffer(fileSize - 4, cns[2]);
					fileSize = decode(cns[1]);
					adjustSize(cns[1]);
					crcAndSize.add(cns);
					for (int i = 0; i < 4; i++) {
						row[i] = cns[0][i];
						row[i + 4] = cns[1][i];
						row[i + 8] = cns[2][i];
					}
					for (int i = 0; i < pointer; i++) {
						writeToDelegate(row[i]);
					}
					writeDecryptHeader();
					for (int i = 0; i < fileSize; i++) {
						writeToDelegate(encrypt(row[pointer]));
						pointer++;
						if (pointer == ROW_SIZE) {
							pointer = 0;
							r++;
							row = fileData.get(r);
						}
					}
					fileData = null;
					identifySectionHeader();
					break;
				case REPO_OFFSET:
					writeAsBytes(centralRepoOffset);
					skipBytes = 3;
					state = State.TAIL;
					return;
				case TAIL:
					break;
				default:
					break;
			}
		} else {
			copyBytes--;
		}
		writeToDelegate(b);
	}

	private void writeToDelegate(final int b) throws IOException {
		delegate.write(b);
		bytesWritten++;
	}

	private static void adjustSize(final int[] values) {
		int inc = DECRYPT_HEADER_SIZE;
		for (int i = 0; i < 4; i++) {
			values[i] = values[i] + inc;
			inc = values[i] >> 8;
		values[i] &= 0xff;
		}
	}

	private static int decode(final int[] value) {
		return value[0] + (value[1] << 8) + (value[2] << 16) + (value[3] << 24);
	}

	private void writeAsBytes(int value) throws IOException {
		for (int i = 0; i < 4; i++) {
			writeToDelegate(value & 0xff);
			value >>= 8;
		}
	}

	private void identifySectionHeader() throws IllegalStateException, IOException {
		if (Arrays.equals(buffer, LFH_SIGNATURE)) {
			section = Section.LFH;
			copyBytes = 1;
			state = State.FLAGS;
			localHeaderOffset.add(bytesWritten);
		} else if (Arrays.equals(buffer, CFH_SIGNATURE)) {
			section = Section.CFH;
			copyBytes = 3;
			state = State.FLAGS;
			if (centralRepoOffset == 0) {
				centralRepoOffset = bytesWritten;
			}
		} else if (Arrays.equals(buffer, ECD_SIGNATURE)) {
			section = Section.ECD;
			copyBytes = 11;
			state = State.REPO_OFFSET;
		} else {
			throw new IllegalStateException("Unknown header: " + Arrays.asList(buffer).toString());
		}
		flushBuffer();
	}

	private void readFromFileBuffer(final int offset, final int[] target) {
		int r = offset / ROW_SIZE;
		int c = offset % ROW_SIZE;
		int[] row = fileData.get(r);
		for (int i = 0; i < target.length; i++) {
			target[i] = row[c];
			c++;
			if (c == ROW_SIZE) {
				c = 0;
				r++;
				row = fileData.get(r);
			}
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		delegate.close();
	}

	private void initKeys() {
		System.arraycopy(pwdKeys, 0, keys, 0, keys.length);
	}

	private void updateKeys(final byte charAt) {
		updateKeys(charAt, keys);
	}

	private byte encryptByte() {
		final int temp = keys[2] | 2;
		return (byte) ((temp * (temp ^ 1)) >>> 8);
	}

	private int encrypt(final int b) {
		final int newB = (b ^ encryptByte()) & 0xff;
		updateKeys((byte) b);
		return newB;
	}

	private void writeDecryptHeader() throws IOException {
		initKeys();
		final int[] crc = crcAndSize.get(crcAndSize.size() - 1)[0];
		final SecureRandom random = new SecureRandom();
		decryptHeader = new byte[DECRYPT_HEADER_SIZE];
		random.nextBytes(decryptHeader);
		decryptHeader[DECRYPT_HEADER_SIZE - 2] = (byte) crc[2];
		decryptHeader[DECRYPT_HEADER_SIZE - 1] = (byte) crc[3];
		for (int i = 0; i < DECRYPT_HEADER_SIZE; i++) {
			writeToDelegate(encrypt(decryptHeader[i]));
		}
	}

	private void buffer(final int[] values, final State bufferState, final int... knownValues) {
		System.arraycopy(knownValues, 0, values, 0, knownValues.length);
		buffer = values;
		bufOffset = knownValues.length;
		this.state = State.BUFFER;
		futureState = bufferState;
	}

	private void flushBuffer() throws IOException {
		for (int i = 0; i < bufOffset; i++) {
			writeToDelegate(buffer[i]);
		}
	}

	private void copyBytesUntil(final State bufferState, final int[]... bufferCondition) {
		futureState = bufferState;
		this.condition = bufferCondition;
		bufOffset = 0;
		buffer = new int[bufferCondition[0].length];
		this.state = State.BUFFER_COPY;
	}

	private void bufferUntil(final State bufferState, final int[]... bufferCondition) {
		copyBytesUntil(bufferState, bufferCondition);
		fileData = new ArrayList<>();
		fileSize = 0;
		this.state = State.BUFFER_UNTIL;
	}

	private boolean checkCondition() {
		boolean equals = true;
		for (int i = 0; i < condition.length; i++) {
			equals = true;
			for (int j = 0; j <= bufOffset; j++) {
				if (condition[i][j] != buffer[j]) {
					equals = false;
					break;
				}
			}
			if (equals) {
				bufOffset++;
				break;
			}
		}
		if (!equals) {
			bufOffset = 0;
		}
		return equals && (buffer.length == bufOffset);
	}

	private static void updateKeys(final byte charAt, final int[] keys) {
		keys[0] = crc32(keys[0], charAt);
		keys[1] += keys[0] & 0xff;
		keys[1] = keys[1] * 134775813 + 1;
		keys[2] = crc32(keys[2], (byte) (keys[1] >> 24));
	}

	private static int crc32(final int oldCrc, final byte charAt) {
		return ((oldCrc >>> 8) ^ CRC_TABLE[(oldCrc ^ charAt) & 0xff]);
	}
}
