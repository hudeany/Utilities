package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.Security;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that encrypts a stream directly with a public key.<br>
 * Supported methods are "ECIES" and RSA or ElGamal with PKCS1, OAEP or no padding (e.g. "RSA/NONE/OAEPPadding").
 * RSA/ElGamal data is encrypted block by block, which is slow and unauthenticated; prefer {@link HybridEncryptionWorker}
 * for larger data. Both streams are closed after the work.
 */
public class AsymmetricEncryptionWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private InputStream dataInputStream = null;
	private OutputStream encryptedDataOutputStream = null;
	private PublicKey publicKey = null;
	private String encryptionMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Asymmetric encrypt inputStream with the default method {@link CryptographicUtilities#DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA}.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to encrypt
	 * @param encryptedDataOutputStream stream to write the encrypted data to
	 * @param publicKey public key to encrypt with
	 */
	public AsymmetricEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final PublicKey publicKey) {
		this(parent, dataInputStream, encryptedDataOutputStream, publicKey, null);
	}

	/**
	 * Asymmetric encrypt inputStream.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to encrypt
	 * @param encryptedDataOutputStream stream to write the encrypted data to
	 * @param publicKey public key to encrypt with
	 * @param encryptionMethod encryption method, blank for {@link CryptographicUtilities#DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA}
	 */
	public AsymmetricEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final PublicKey publicKey, final String encryptionMethod) {
		super(parent);
		this.dataInputStream = dataInputStream;
		this.encryptedDataOutputStream = encryptedDataOutputStream;
		this.publicKey = publicKey;
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
			throw new Exception("Empty asymmetric encryption method");
		}
		encryptionMethod = encryptionMethod.toLowerCase(Locale.ROOT);

		signalProgress();

		itemsToDo = Math.max(dataSizeHint, dataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		try (InputStream inputStream = new BufferedInputStream(dataInputStream)) {
			if (encryptionMethod.equalsIgnoreCase("ecies")) {
				// Used by ECDSA
				final Cipher eciesCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
				eciesCipher.init(Cipher.ENCRYPT_MODE, publicKey);

				// CipherOutputStream must be closed at end for correct final block data
				try (CipherOutputStream cipherOutputStream = new CipherOutputStream(new BufferedOutputStream(encryptedDataOutputStream), eciesCipher)) {
					final int bufferSize = eciesCipher.getBlockSize() <= 0 ? 4096 : eciesCipher.getBlockSize();
					final byte[] buffer = new byte[bufferSize];
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
				} catch (final Exception e) {
					e.printStackTrace();
					throw e;
				}
			} else {
				AsymmetricBlockCipher asymmetricEncryptCipher;
				if (encryptionMethod.startsWith("rsa")) {
					asymmetricEncryptCipher = new RSAEngine();
				} else if (encryptionMethod.startsWith("elgamal")) {
					asymmetricEncryptCipher = new ElGamalEngine();
				} else {
					throw new Exception("Unknown asymmetric encryption cipher method: " + encryptionMethod);
				}

				if (encryptionMethod.contains("pkcs1")) {
					asymmetricEncryptCipher = new PKCS1Encoding(asymmetricEncryptCipher);
				} else if (encryptionMethod.contains("oaep")) {
					asymmetricEncryptCipher = new OAEPEncoding(asymmetricEncryptCipher);
				} else if (encryptionMethod.contains("nopadding")) {
					// do no padding
				} else {
					throw new Exception("Unknown asymmetric encryption padding method: " + encryptionMethod);
				}

				asymmetricEncryptCipher.init(true, PublicKeyFactory.createKey(publicKey.getEncoded()));

				try (BufferedOutputStream bufferedEncryptedDataOutputStream = new BufferedOutputStream(encryptedDataOutputStream);) {
					final int bufferSize = asymmetricEncryptCipher.getInputBlockSize() <= 0 ? 4096 : asymmetricEncryptCipher.getInputBlockSize();
					final byte[] buffer = new byte[bufferSize];
					int bytesRead;
					// Always encrypt complete input blocks, a short read of the stream must not change the block layout
					while ((bytesRead = inputStream.readNBytes(buffer, 0, buffer.length)) > 0) {
						if (cancel) {
							break;
						} else {
							bufferedEncryptedDataOutputStream.write(asymmetricEncryptCipher.processBlock(buffer, 0, bytesRead));
							itemsDone += bytesRead;
							signalProgress();
						}
					}
					Utilities.clear(buffer);
				}
			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		}
	}
}
