package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Counterpart to HybridEncryptionWorker: unwraps the AES session key with the
 * recipient's private key, then decrypts the bulk data with AES/GCM. GCM verifies
 * the authentication tag automatically on the final read, so tampered ciphertext
 * throws an exception instead of silently producing corrupted output.
 */
public class HybridDecryptionWorker extends WorkerSimple<Boolean> {
	public static final int EOF = -1;

	private static final int GCM_NONCE_LENGTH_BYTES = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int MAX_PLAUSIBLE_WRAPPED_KEY_LENGTH = 16 * 1024;
	private static final String SYMMETRIC_METHOD = "AES/GCM/NoPadding";

	private InputStream encryptedDataInputStream = null;
	private OutputStream decryptedDataOutputStream = null;
	private PrivateKey privateKey = null;
	private String asymmetricKeyWrapMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Hybrid decrypt inputStream
	 *
	 * @param parent
	 * @param encryptedDataInputStream
	 * @param decryptedDataOutputStream
	 * @param privateKey recipient's RSA or EC private key, used only to unwrap the AES session key
	 */
	public HybridDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final PrivateKey privateKey) {
		this(parent, encryptedDataInputStream, decryptedDataOutputStream, privateKey, null);
	}

	/**
	 * Hybrid decrypt inputStream
	 *
	 * @param parent
	 * @param encryptedDataInputStream
	 * @param decryptedDataOutputStream
	 * @param privateKey recipient's RSA or EC private key, used only to unwrap the AES session key
	 * @param asymmetricKeyWrapMethod must match the method used by HybridEncryptionWorker
	 */
	public HybridDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final PrivateKey privateKey, final String asymmetricKeyWrapMethod) {
		super(parent);
		this.encryptedDataInputStream = encryptedDataInputStream;
		this.decryptedDataOutputStream = decryptedDataOutputStream;
		this.privateKey = privateKey;
		this.asymmetricKeyWrapMethod = asymmetricKeyWrapMethod;

		if (Utilities.isBlank(this.asymmetricKeyWrapMethod)) {
			this.asymmetricKeyWrapMethod = CryptographicUtilities.DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA;
		}
	}

	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	@Override
	public Boolean work() throws Exception {
		signalProgress();

		itemsToDo = Math.max(dataSizeHint, encryptedDataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		SecretKey sessionKey = null;
		try (InputStream inputStream = new BufferedInputStream(encryptedDataInputStream);
				OutputStream outputStream = new BufferedOutputStream(decryptedDataOutputStream)) {
			// Read the wrapped session key
			final byte[] wrappedKeyLengthBytes = inputStream.readNBytes(4);
			if (wrappedKeyLengthBytes.length != 4) {
				throw new Exception("Cannot read wrapped key length prefix: Data is too short");
			}
			final int wrappedKeyLength = ByteBuffer.wrap(wrappedKeyLengthBytes).getInt();
			if (wrappedKeyLength <= 0 || wrappedKeyLength > MAX_PLAUSIBLE_WRAPPED_KEY_LENGTH) {
				throw new Exception("Implausible wrapped key length: " + wrappedKeyLength);
			}
			final byte[] wrappedSessionKey = inputStream.readNBytes(wrappedKeyLength);
			if (wrappedSessionKey.length != wrappedKeyLength) {
				throw new Exception("Cannot read wrapped key: Data is too short");
			}

			// Unwrap it with the recipient's private key
			final Cipher keyUnwrapCipher = Cipher.getInstance(asymmetricKeyWrapMethod, BouncyCastleProvider.PROVIDER_NAME);
			keyUnwrapCipher.init(Cipher.UNWRAP_MODE, privateKey);
			sessionKey = (SecretKey) keyUnwrapCipher.unwrap(wrappedSessionKey, "AES", Cipher.SECRET_KEY);

			// Read the nonce and decrypt the bulk data; GCM verifies the auth tag on the final read
			final byte[] nonce = inputStream.readNBytes(GCM_NONCE_LENGTH_BYTES);
			if (nonce.length != GCM_NONCE_LENGTH_BYTES) {
				throw new Exception("Cannot read nonce prefix: Data is too short");
			}

			final Cipher dataCipher = Cipher.getInstance(SYMMETRIC_METHOD, BouncyCastleProvider.PROVIDER_NAME);
			dataCipher.init(Cipher.DECRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));

			try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, dataCipher)) {
				final byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = cipherInputStream.read(buffer)) != EOF) {
					if (cancel) {
						break;
					} else {
						outputStream.write(buffer, 0, bytesRead);
						itemsDone += bytesRead;
						signalProgress();
					}
				}
				Utilities.clear(buffer);
			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			if (sessionKey != null) {
				try {
					sessionKey.destroy();
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Not all providers/key types support key destruction; best effort only
				}
			}
		}
	}
}
