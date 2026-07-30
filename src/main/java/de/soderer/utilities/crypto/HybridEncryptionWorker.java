package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Hybrid encryption: the bulk data is encrypted with a freshly generated, random
 * AES-GCM "session key" (fast, authenticated, works for arbitrarily large streams).
 * Only that small session key is encrypted ("wrapped") with the recipient's
 * RSA/EC public key.
 *
 * This avoids the two problems of encrypting bulk data directly with RSA/ElGamal
 * (see AsymmetricEncryptionWorker):
 * - RSA/ElGamal are far too slow to use as a bulk cipher for anything beyond a
 *   few hundred bytes.
 * - Plain block-by-block asymmetric encryption provides no built-in authentication,
 *   so tampering with the ciphertext goes undetected; AES/GCM does.
 *
 * Stream layout written by this worker:
 *   [4 bytes big-endian: wrapped session key length]
 *   [wrapped session key length bytes: RSA/EC-wrapped AES session key]
 *   [12 bytes: GCM nonce]
 *   [remaining bytes: AES/GCM ciphertext, including the 16 byte GCM authentication tag at the end]
 */
public class HybridEncryptionWorker extends WorkerSimple<Boolean> {
	public static final int EOF = -1;

	private static final int SESSION_KEY_LENGTH_BITS = 256;
	private static final int GCM_NONCE_LENGTH_BYTES = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final String SYMMETRIC_METHOD = "AES/GCM/NoPadding";

	private InputStream dataInputStream = null;
	private OutputStream encryptedDataOutputStream = null;
	private PublicKey publicKey = null;
	private String asymmetricKeyWrapMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Hybrid encrypt inputStream
	 *
	 * @param parent
	 * @param dataInputStream
	 * @param encryptedDataOutputStream
	 * @param publicKey recipient's RSA or EC public key, used only to wrap the AES session key
	 */
	public HybridEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final PublicKey publicKey) {
		this(parent, dataInputStream, encryptedDataOutputStream, publicKey, null);
	}

	/**
	 * Hybrid encrypt inputStream
	 *
	 * @param parent
	 * @param dataInputStream
	 * @param encryptedDataOutputStream
	 * @param publicKey recipient's RSA or EC public key, used only to wrap the AES session key
	 * @param asymmetricKeyWrapMethod transformation used to wrap the AES session key
	 *        (e.g. "RSA/NONE/OAEPWithSHA256AndMGF1Padding" or "ECIES"); the bulk data itself
	 *        is always encrypted with AES/GCM regardless of this setting
	 */
	public HybridEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final PublicKey publicKey, final String asymmetricKeyWrapMethod) {
		super(parent);
		this.dataInputStream = dataInputStream;
		this.encryptedDataOutputStream = encryptedDataOutputStream;
		this.publicKey = publicKey;
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

		itemsToDo = Math.max(dataSizeHint, dataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		// Generate a fresh random AES session key, used for this one encryption only
		final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", BouncyCastleProvider.PROVIDER_NAME);
		keyGenerator.init(SESSION_KEY_LENGTH_BITS, new SecureRandom());
		final SecretKey sessionKey = keyGenerator.generateKey();

		try (InputStream inputStream = new BufferedInputStream(dataInputStream);
				OutputStream outputStream = new BufferedOutputStream(encryptedDataOutputStream)) {
			// Wrap the session key with the recipient's public key
			final Cipher keyWrapCipher = Cipher.getInstance(asymmetricKeyWrapMethod, BouncyCastleProvider.PROVIDER_NAME);
			keyWrapCipher.init(Cipher.WRAP_MODE, publicKey);
			final byte[] wrappedSessionKey = keyWrapCipher.wrap(sessionKey);

			outputStream.write(ByteBuffer.allocate(4).putInt(wrappedSessionKey.length).array());
			outputStream.write(wrappedSessionKey);

			// Encrypt the bulk data with AES/GCM using the session key
			final byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTES];
			new SecureRandom().nextBytes(nonce);
			outputStream.write(nonce);

			final Cipher dataCipher = Cipher.getInstance(SYMMETRIC_METHOD, BouncyCastleProvider.PROVIDER_NAME);
			dataCipher.init(Cipher.ENCRYPT_MODE, sessionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));

			// CipherOutputStream must be closed at end for correct final block + GCM tag
			try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, dataCipher)) {
				final byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != EOF) {
					if (cancel) {
						break;
					} else {
						cipherOutputStream.write(buffer, 0, bytesRead);
						itemsDone += bytesRead;
						signalProgress();
					}
				}
				Utilities.clear(buffer);
			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		} finally {
			try {
				sessionKey.destroy();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Not all providers/key types support key destruction; best effort only
			}
		}
	}
}
