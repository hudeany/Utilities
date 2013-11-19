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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class FileEncryptionWorker extends Worker<Boolean> {
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
	public FileEncryptionWorker(ProgressShowingModalDialog<?> parent, File fileToEncrypt, File encryptedFile, PublicKey publicKey) {
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
	public FileEncryptionWorker(ProgressShowingModalDialog<?> parent, File fileToEncrypt, File encryptedFile, PublicKey publicKey, String encryptionMethod, boolean zipBeforeEncryption) {
		super(parent);
		this.fileToEncrypt = fileToEncrypt;
		this.encryptedFile = encryptedFile;
		this.publicKey = publicKey;
		this.encryptionMethod = encryptionMethod;
		this.zipBeforeEncryption = zipBeforeEncryption;
		
		if (StringUtils.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD;
		}
		
    	todo = fileToEncrypt.length();
	}

	/**
	 * Encrypt file using AES
	 * 
	 * @param parent
	 * @param fileToEncrypt
	 * @param password
	 * @param publicKey
	 */
	public FileEncryptionWorker(ProgressShowingModalDialog<?> parent, File fileToEncrypt, File encryptedFile, char[] password) {
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
	public FileEncryptionWorker(ProgressShowingModalDialog<?> parent, File fileToEncrypt, File encryptedFile, char[] password, String encryptionMethod, byte[] salt, byte[] initalizationVector, boolean zipBeforeEncryption) {
		super(parent);
		this.fileToEncrypt = fileToEncrypt;
		this.encryptedFile = encryptedFile;
		this.password = password;
		this.encryptionMethod = encryptionMethod;
		this.salt = salt;
		this.initializationVector = initalizationVector;
		this.zipBeforeEncryption = zipBeforeEncryption;
		
		if (StringUtils.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
		}
		
		if (this.salt == null) {
			this.salt = CryptographicUtilities.DEFAULT_SALT;
		}
		
		if (this.initializationVector == null) {
			this.initializationVector = CryptographicUtilities.DEFAULT_SYMMETRIC_INITALIZATION_VECTOR;
		}
		
    	todo = fileToEncrypt.length();
	}

	@Override
	public void run() {
		long position = 0L;
		result = null;
		try {
			InputStream inputStream = null;
			OutputStream outputStream = null;
			try {
				showItemsDone(0L);
				
				Security.addProvider(new BouncyCastleProvider());
				
				inputStream = new BufferedInputStream(new FileInputStream(fileToEncrypt));
				
				Cipher encryptCipher = Cipher.getInstance(encryptionMethod, "BC");
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
						position += bytesRead;
						
						showItemsDone(position);
					}
				}
			} catch (Exception e) {
				throw new Exception("Error while encrypting: " + e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(outputStream);
				IOUtils.closeQuietly(inputStream);
			}
			
			result = true;
		} catch (Exception e) {
			error = e;
		} finally {
			showDone();
			isDone = true;
		}
	}
}
