package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Reader for PuTTY ".ppk" keyfiles<br />
 * <br />
 * PuTTY file format:
 * <pre>
 * PuTTY-User-Key-File-2: xxx
 * Encryption: xxx
 * Comment: xxx
 * Public-Lines: 4
 * &lt;Base64 public key data lines>
 * Private-Lines: 8
 * &lt;Base64 private key data lines>
 * Private-MAC: xxx
 * </pre>
 */
public class PuttyKeyReader implements Closeable {
	private final BufferedReader dataReader;
	private byte[] passwordByteArray = null;

	public static boolean isPuTTYKeyFile(final File ppkFile) throws IOException {
		try (BufferedReader puttKeyReader = new BufferedReader(new FileReader(ppkFile))) {
			String line;
			while ((line = puttKeyReader.readLine()) != null) {
				if (line.startsWith("PuTTY-User-Key-File-")) {
					return true;
				}
			}
			return false;
		}
	}

	public PuttyKeyReader(final InputStream inputStream) throws IOException {
		this(new InputStreamReader(inputStream));
	}

	public PuttyKeyReader(final Reader reader) throws IOException {
		dataReader = new BufferedReader(reader);
	}

	public PuttyKeyReader setPassword(final char[] passwordCharArray) {
		final CharBuffer charBuffer = CharBuffer.wrap(passwordCharArray);
		final ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		passwordByteArray = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());

		return this;
	}

	public PuttyKey readKey() throws Exception {
		final Map<String, String> headers = new HashMap<>();
		final Map<String, StringBuilder> data = new HashMap<>();
		String latestHeaderName = null;
		String nextLine;
		while ((nextLine = dataReader.readLine()) != null) {
			final int indexOfHeaderSeparatore = nextLine.indexOf(":");
			if (indexOfHeaderSeparatore > 0) {
				final String headerName = nextLine.substring(0, indexOfHeaderSeparatore).trim();
				headers.put(headerName, nextLine.substring(indexOfHeaderSeparatore + 1).trim());
				latestHeaderName = headerName;
			} else {
				if (!data.containsKey(latestHeaderName)) {
					data.put(latestHeaderName, new StringBuilder());
				}
				data.get(latestHeaderName).append(nextLine);
			}
		}

		String puttyKeyType = null;
		for (final String headerName : headers.keySet()) {
			if (headerName.startsWith("PuTTY-User-Key-File-")) {
				puttyKeyType = headerName;
				break;
			}
		}
		if (puttyKeyType == null || data.size() < 1) {
			throw new Exception("No PuTTY key found");
		}
		if (!"PuTTY-User-Key-File-2".equals(puttyKeyType)) {
			throw new Exception("Unsupported PuTTY key type: " + puttyKeyType);
		}
		final String chipherName = headers.get("PuTTY-User-Key-File-2");
		if (chipherName == null || "".equals(chipherName)) {
			throw new Exception("Missing chipher name");
		}
		if (!"ssh-rsa".equalsIgnoreCase(chipherName) && !"ssh-dss".equalsIgnoreCase(chipherName)) {
			throw new Exception("Unsupported chipher: " + chipherName);
		}

		final Decoder base64Decoder = Base64.getDecoder();
		final byte[] publicKey = base64Decoder.decode(data.get("Public-Lines").toString());
		byte[] privateKey = base64Decoder.decode(data.get("Private-Lines").toString());

		final String encryptionmethod = headers.get("Encryption");
		if (encryptionmethod == null || "".equals(encryptionmethod) || "none".equalsIgnoreCase(encryptionmethod)) {
			return new PuttyKey(headers, privateKey, publicKey);
		} else if ("aes256-cbc".equalsIgnoreCase(encryptionmethod)) {
			try {
				final byte[] puttyKeyEncryptionKey = new byte[32];
				final MessageDigest digest = MessageDigest.getInstance("SHA-1");

				digest.update(new byte[] { 0, 0, 0, 0 });
				digest.update(passwordByteArray);
				final byte[] key1 = digest.digest();

				digest.update(new byte[] { 0, 0, 0, 1 });
				digest.update(passwordByteArray);
				final byte[] key2 = digest.digest();

				System.arraycopy(key1, 0, puttyKeyEncryptionKey, 0, 20);
				System.arraycopy(key2, 0, puttyKeyEncryptionKey, 20, 12);

				final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
				cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(puttyKeyEncryptionKey, 0, 32, "AES"), new IvParameterSpec(new byte[16])); // initial vector=0
				privateKey = cipher.doFinal(privateKey);

				return new PuttyKey(headers, privateKey, publicKey);
			} catch (final Exception e) {
				throw new Exception("Cannot decrypt PuTTY private key data", e);
			}
		} else {
			throw new Exception("Unsupported key encryption method: " + headers.get("Encryption"));
		}
	}

	@Override
	public void close() throws IOException {
		try {
			dataReader.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
