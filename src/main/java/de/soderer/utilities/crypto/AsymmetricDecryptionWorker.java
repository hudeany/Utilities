package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that decrypts a stream encrypted by {@link AsymmetricEncryptionWorker} with a private key.<br>
 * Supported methods are "ECIES" and RSA or ElGamal with PKCS1, OAEP or no padding (e.g. "RSA/NONE/OAEPPadding").
 * RSA/ElGamal data is decrypted block by block. Both streams are closed after the work.
 */
public class AsymmetricDecryptionWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private InputStream encryptedDataInputStream = null;
	private OutputStream decryptedDataOutputStream = null;
	private PrivateKey privateKey = null;
	private String encryptionMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Asymmetric decrypt inputStream with the default method {@link CryptographicUtilities#DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA}.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param encryptedDataInputStream stream with the encrypted data
	 * @param decryptedDataOutputStream stream to write the decrypted data to
	 * @param privateKey private key to decrypt with
	 */
	public AsymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final PrivateKey privateKey) {
		this(parent, encryptedDataInputStream, decryptedDataOutputStream, privateKey, null);
	}

	/**
	 * Asymmetric decrypt inputStream.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param encryptedDataInputStream stream with the encrypted data
	 * @param decryptedDataOutputStream stream to write the decrypted data to
	 * @param privateKey private key to decrypt with
	 * @param encryptionMethod encryption method used for encryption, blank for {@link CryptographicUtilities#DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA}
	 */
	public AsymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final PrivateKey privateKey, final String encryptionMethod) {
		super(parent);
		this.encryptedDataInputStream = encryptedDataInputStream;
		this.decryptedDataOutputStream = decryptedDataOutputStream;
		this.privateKey = privateKey;
		this.encryptionMethod = encryptionMethod;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA;
		}
	}

	/**
	 * Sets the expected data size in bytes for the progress display, if the stream cannot tell its size.
	 *
	 * @param dataSizeHint expected data size in bytes
	 */
	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	@Override
	public Boolean work() throws Exception {
		if (encryptionMethod == null || "".equals(encryptionMethod.trim())) {
			throw new Exception("Empty symmetric decryption method");
		}
		encryptionMethod = encryptionMethod.toLowerCase(Locale.ROOT);

		signalProgress();

		itemsToDo = Math.max(dataSizeHint, encryptedDataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		try (OutputStream outputStream = new BufferedOutputStream(decryptedDataOutputStream)) {
			if (encryptionMethod.equalsIgnoreCase("ecies")) {
				final Cipher decryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
				decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
				try (InputStream inputStream = new CipherInputStream(new BufferedInputStream(encryptedDataInputStream), decryptCipher)) {
					final int bufferSize = decryptCipher.getBlockSize() <= 0 ? 4096 : decryptCipher.getBlockSize();
					final byte[] buffer = new byte[bufferSize];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
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
			} else {
				AsymmetricBlockCipher decryptCipher;
				if (encryptionMethod.startsWith("rsa")) {
					decryptCipher = new RSAEngine();
				} else if (encryptionMethod.startsWith("elgamal")) {
					decryptCipher = new ElGamalEngine();
				} else {
					throw new Exception("Unknown asymmetric encryption cipher method: " + encryptionMethod);
				}
				if (encryptionMethod.contains("pkcs1")) {
					decryptCipher = new PKCS1Encoding(decryptCipher);
				} else if (encryptionMethod.contains("oaep")) {
					decryptCipher = new OAEPEncoding(decryptCipher);
				} else if (encryptionMethod.contains("nopadding")) {
					// do no padding
				} else {
					throw new Exception("Unknown asymmetric encryption padding method: " + encryptionMethod);
				}

				decryptCipher.init(false, PrivateKeyFactory.createKey(privateKey.getEncoded()));

				try (InputStream inputStream = new BufferedInputStream(encryptedDataInputStream)) {
					final int bufferSize = decryptCipher.getInputBlockSize() <= 0 ? 4096 : decryptCipher.getInputBlockSize();
					final byte[] buffer = new byte[bufferSize];
					int bytesRead;
					// Always read complete cipher blocks, a short read of the stream must not split a block
					while ((bytesRead = inputStream.readNBytes(buffer, 0, buffer.length)) > 0) {
						if (cancel) {
							break;
						} else {
							outputStream.write(decryptCipher.processBlock(buffer, 0, bytesRead));
							itemsDone += bytesRead;
							signalProgress();
						}
					}
					Utilities.clear(buffer);
				}
			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		}
	}
}
