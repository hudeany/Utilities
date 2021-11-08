package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class SymmetricDecryptionWorker extends WorkerSimple<Boolean> {
	private InputStream encryptedDataInputStream = null;
	private OutputStream decryptedDataOutputStream = null;
	private char[] password = null;
	private byte[] salt = null;
	private byte[] initializationVector = null;
	private String encryptionMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Symmetric decrypt inputStream
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param password
	 */
	public SymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final char[] password) {
		this(parent, encryptedDataInputStream, decryptedDataOutputStream, password, null, null, null);
	}

	/**
	 * Symmetric decrypt inputStream
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param password
	 * @param encryptionMethod
	 * @param salt
	 * @param initalizationVector
	 */
	public SymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final char[] password, final String encryptionMethod, final byte[] salt, final byte[] initializationVector) {
		super(parent);
		this.encryptedDataInputStream = encryptedDataInputStream;
		this.decryptedDataOutputStream = decryptedDataOutputStream;
		this.password = password;
		this.encryptionMethod = encryptionMethod;
		this.salt = salt;
		this.initializationVector = initializationVector;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
		}

		if (this.salt == null) {
			this.salt = CryptographicUtilities.DEFAULT_SALT;
		}

		if (this.initializationVector == null) {
			this.initializationVector = CryptographicUtilities.DEFAULT_SYMMETRIC_INITALIZATION_VECTOR;
		}
	}

	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	@Override
	public Boolean work() throws Exception {
		if (encryptionMethod == null || "".equals(encryptionMethod.trim())) {
			throw new Exception("Empty symmetric decryption method");
		}
		if ("TripleDES".equalsIgnoreCase(encryptionMethod) || "3DES".equalsIgnoreCase(encryptionMethod)) {
			encryptionMethod = "DESede";
		}
		encryptionMethod = encryptionMethod.toLowerCase();

		showProgress();

		itemsToDo = Math.max(dataSizeHint, encryptedDataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		try (OutputStream outputStream = new BufferedOutputStream(decryptedDataOutputStream)) {
			final Cipher decryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
			final byte[] keyBytes = CryptographicUtilities.stretchPassword(password, 128, salt);
			final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, encryptionMethod);
			decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			try (InputStream inputStream = new CipherInputStream(new BufferedInputStream(encryptedDataInputStream), decryptCipher)) {
				final int bufferSize = decryptCipher.getBlockSize() <= 0 ? 4096 : decryptCipher.getBlockSize();
				final byte[] buffer = new byte[bufferSize];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) > -1) {
					if (cancel) {
						break;
					} else {
						outputStream.write(buffer, 0, bytesRead);
						itemsDone += bytesRead;
						showProgress();
					}
				}
			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		}
	}
}
