package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerParentSimple;
import de.soderer.utilities.WorkerSimple;
import de.soderer.utilities.swt.ProgressShowingModalDialog;

public class FileDecryptionWorker extends WorkerSimple<Boolean> {
	private File fileToDecrypt = null;
	private File decryptedFile = null;
	private PrivateKey privateKey = null;
	private char[] password = null;
	private byte[] salt = null;
	private byte[] initializationVector = null;
	private String decryptionMethod = null;
	private boolean unzipAfterDecryption = true;

	/**
	 * Decrypt file using RSA
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param privateKey
	 */
	public FileDecryptionWorker(WorkerParentSimple parent, File fileToDecrypt, File decryptedFile, PrivateKey privateKey) {
		this(parent, fileToDecrypt, decryptedFile, privateKey, null, true);
	}

	/**
	 * Decrypt file using RSA
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param privateKey
	 * @param decryptionMethod
	 * @param unzipAfterDecryption
	 */
	public FileDecryptionWorker(WorkerParentSimple parent, File fileToDecrypt, File decryptedFile, PrivateKey privateKey, String decryptionMethod, boolean unzipAfterDecryption) {
		super(parent);
		this.fileToDecrypt = fileToDecrypt;
		this.decryptedFile = decryptedFile;
		this.privateKey = privateKey;
		this.decryptionMethod = decryptionMethod;
		this.unzipAfterDecryption = unzipAfterDecryption;

		if (Utilities.isBlank(this.decryptionMethod)) {
			this.decryptionMethod = CryptographicUtilities.DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD;
		}
	}

	/**
	 * Decrypt file using AES
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param password
	 */
	public FileDecryptionWorker(ProgressShowingModalDialog<?> parent, File fileToDecrypt, File decryptedFile, char[] password) {
		this(parent, fileToDecrypt, decryptedFile, password, null, null, null, true);
	}

	/**
	 * Decrypt file using AES
	 *
	 * @param parent
	 * @param fileToDecrypt
	 * @param decryptedFile
	 * @param password
	 * @param decryptionMethod
	 * @param salt
	 * @param initalizationVector
	 * @param unzipAfterDecryption
	 */
	public FileDecryptionWorker(ProgressShowingModalDialog<?> parent, File fileToDecrypt, File decryptedFile, char[] password, String decryptionMethod, byte[] salt, byte[] initalizationVector,
			boolean unzipAfterDecryption) {
		super(parent);
		this.fileToDecrypt = fileToDecrypt;
		this.decryptedFile = decryptedFile;
		this.password = password;
		this.decryptionMethod = decryptionMethod;
		this.salt = salt;
		initializationVector = initalizationVector;
		this.unzipAfterDecryption = unzipAfterDecryption;

		if (Utilities.isBlank(this.decryptionMethod)) {
			this.decryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
		}

		if (this.salt == null) {
			this.salt = CryptographicUtilities.DEFAULT_SALT;
		}

		if (initializationVector == null) {
			initializationVector = CryptographicUtilities.DEFAULT_SYMMETRIC_INITALIZATION_VECTOR;
		}
	}

	@Override
	public Boolean work() throws Exception {
		showProgress();

		OutputStream outputStream = null;
		InputStream inputStream = null;
		try {
			itemsToDo = fileToDecrypt.length();
			
			Security.addProvider(new BouncyCastleProvider());

			outputStream = new BufferedOutputStream(new FileOutputStream(decryptedFile));

			Cipher decryptCipher = Cipher.getInstance(decryptionMethod, "BC");
			if (privateKey != null) {
				decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
			} else {
				byte[] keyBytes = CryptographicUtilities.stretchPassword(password, 128, salt);
				SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
				decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			}
			inputStream = new CipherInputStream(new BufferedInputStream(new FileInputStream(fileToDecrypt)), decryptCipher);

			if (unzipAfterDecryption) {
				inputStream = new ZipInputStream(inputStream);
				((ZipInputStream) inputStream).getNextEntry();
			}

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					outputStream.write(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					showProgress();
				}
			}
			
			return true;
		} catch (Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			Utilities.closeQuietly(outputStream);
			Utilities.closeQuietly(inputStream);
		}
	}
}
