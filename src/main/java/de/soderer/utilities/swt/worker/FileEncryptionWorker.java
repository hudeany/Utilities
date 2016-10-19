package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.Security;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerParentSimple;
import de.soderer.utilities.WorkerSimple;

public class FileEncryptionWorker extends WorkerSimple<Boolean> {
	private File fileToEncrypt = null;
	private File encryptedFile = null;
	private PublicKey publicKey = null;
	private char[] password = null;
	private byte[] salt = null;
	private byte[] initializationVector = null;
	private String encryptionMethod = null;
	private boolean zipBeforeEncryption = true;

	/**
	 * Encrypt file using RSA
	 *
	 * @param parent
	 * @param fileToEncrypt
	 * @param encryptedFile
	 * @param publicKey
	 */
	public FileEncryptionWorker(WorkerParentSimple parent, File fileToEncrypt, File encryptedFile, PublicKey publicKey) {
		this(parent, fileToEncrypt, encryptedFile, publicKey, null, true);
	}

	/**
	 * Encrypt file using RSA
	 *
	 * @param parent
	 * @param fileToEncrypt
	 * @param encryptedFile
	 * @param publicKey
	 * @param encryptionMethod
	 * @param zipBeforeEncryption
	 */
	public FileEncryptionWorker(WorkerParentSimple parent, File fileToEncrypt, File encryptedFile, PublicKey publicKey, String encryptionMethod, boolean zipBeforeEncryption) {
		super(parent);
		this.fileToEncrypt = fileToEncrypt;
		this.encryptedFile = encryptedFile;
		this.publicKey = publicKey;
		this.encryptionMethod = encryptionMethod;
		this.zipBeforeEncryption = zipBeforeEncryption;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD;
		}
	}

	/**
	 * Encrypt file using AES
	 *
	 * @param parent
	 * @param fileToEncrypt
	 * @param password
	 * @param publicKey
	 */
	public FileEncryptionWorker(WorkerParentSimple parent, File fileToEncrypt, File encryptedFile, char[] password) {
		this(parent, fileToEncrypt, encryptedFile, password, null, null, null, true);
	}

	/**
	 * Encrypt file using AES
	 *
	 * @param parent
	 * @param fileToEncrypt
	 * @param encryptedFile
	 * @param password
	 * @param encryptionMethod
	 * @param zipBeforeEncryption
	 */
	public FileEncryptionWorker(WorkerParentSimple parent, File fileToEncrypt, File encryptedFile, char[] password, String encryptionMethod, byte[] salt, byte[] initalizationVector,
			boolean zipBeforeEncryption) {
		super(parent);
		this.fileToEncrypt = fileToEncrypt;
		this.encryptedFile = encryptedFile;
		this.password = password;
		this.encryptionMethod = encryptionMethod;
		this.salt = salt;
		initializationVector = initalizationVector;
		this.zipBeforeEncryption = zipBeforeEncryption;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
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

		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			itemsToDo = fileToEncrypt.length();
			
			Security.addProvider(new BouncyCastleProvider());

			inputStream = new BufferedInputStream(new FileInputStream(fileToEncrypt));

			Cipher encryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
			if (publicKey != null) {
				encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			} else {
				byte[] keyBytes = CryptographicUtilities.stretchPassword(password, 128, salt);
				SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
				encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			}
			outputStream = new CipherOutputStream(new BufferedOutputStream(new FileOutputStream(encryptedFile)), encryptCipher);

			if (zipBeforeEncryption) {
				outputStream = new ZipOutputStream(outputStream);
				ZipEntry entry = new ZipEntry(fileToEncrypt.getName());
				entry.setTime(fileToEncrypt.lastModified());
				((ZipOutputStream) outputStream).putNextEntry(entry);
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
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		} finally {
			Utilities.closeQuietly(outputStream);
			Utilities.closeQuietly(inputStream);
		}
	}
}
