package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.time.LocalDateTime;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.PGPUtilities.PgpSymmetricEncryptionMethod;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that encrypts data with a PGP public key into an ASCII-armored, ZIP compressed PGP message.<br>
 * Old JREs may need the installed "US_export_policy.jar" and "local_policy.jar" for unlimited key strength.
 */
public class PGPFileEncryptionWorker extends WorkerSimple<Boolean> {
	private InputStream dataToEncrypt = null;
	private OutputStream encryptedData = null;
	private PGPPublicKey publicKey = null;
	private boolean useIntegrityPacket = true;
	private PgpSymmetricEncryptionMethod pgpSymmetricEncryptionMethod = PgpSymmetricEncryptionMethod.AES_256;

	/**
	 * Returns whether an integrity packet (modification detection code) is added.
	 *
	 * @return true if an integrity packet is added
	 */
	public boolean isUseIntegrityPacket() {
		return useIntegrityPacket;
	}

	/**
	 * Sets whether an integrity packet (modification detection code) is added (default true).
	 *
	 * @param useIntegrityPacket true to add an integrity packet
	 * @return this worker
	 */
	public PGPFileEncryptionWorker setUseIntegrityPacket(final boolean useIntegrityPacket) {
		this.useIntegrityPacket = useIntegrityPacket;
		return this;
	}

	/**
	 * Creates a PGP encryption worker. The data stream is not closed, the output stream is not closed but finished
	 * with the armor footer.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataToEncrypt stream with the data to encrypt
	 * @param encryptedData stream to write the encrypted message to
	 * @param publicKey public key of the recipient
	 */
	public PGPFileEncryptionWorker(final WorkerParentSimple parent, final InputStream dataToEncrypt, final OutputStream encryptedData, final PGPPublicKey publicKey) {
		super(parent);
		this.dataToEncrypt = dataToEncrypt;
		this.encryptedData = encryptedData;
		this.publicKey = publicKey;
	}

	/**
	 * Sets the symmetric encryption method by name (default AES-256).
	 *
	 * @param pgpSymmetricEncryptionMethod method name like "AES-256", see {@link PgpSymmetricEncryptionMethod#getByName(String)}
	 * @return this worker
	 * @throws Exception if the name is unknown
	 */
	public PGPFileEncryptionWorker setPgpSymmetricEncryptionMethod(final String pgpSymmetricEncryptionMethod) throws Exception {
		this.pgpSymmetricEncryptionMethod = PgpSymmetricEncryptionMethod.getByName(pgpSymmetricEncryptionMethod);
		return this;
	}

	/**
	 * Sets the symmetric encryption method (default AES-256).
	 *
	 * @param pgpSymmetricEncryptionMethod symmetric encryption method
	 * @return this worker
	 */
	public PGPFileEncryptionWorker setPgpSymmetricEncryptionMethod(final PgpSymmetricEncryptionMethod pgpSymmetricEncryptionMethod) {
		this.pgpSymmetricEncryptionMethod = pgpSymmetricEncryptionMethod;
		return this;
	}

	/**
	 * Returns the symmetric encryption method.
	 *
	 * @return symmetric encryption method
	 */
	public PgpSymmetricEncryptionMethod getPgpSymmetricEncryptionMethod() {
		return pgpSymmetricEncryptionMethod;
	}

	@Override
	public Boolean work() throws Exception {
		signalProgress();

		final boolean armor = true;

		PGPEncryptedDataGenerator encryptedDataGenerator = null;
		OutputStream encryptedDataGeneratorStream = null;
		PGPCompressedDataGenerator compressedDataGenerator = null;
		OutputStream compressedDataGeneratorStream = null;
		PGPLiteralDataGenerator literalDataGenerator = null;
		OutputStream literalDataGeneratorStream = null;
		try {
			itemsToDo = dataToEncrypt.available();

			Security.addProvider(new BouncyCastleProvider());

			dataToEncrypt = new BufferedInputStream(dataToEncrypt);

			if (armor) {
				encryptedData = new ArmoredOutputStream(encryptedData);
			}

			final JcePGPDataEncryptorBuilder builder = new JcePGPDataEncryptorBuilder(pgpSymmetricEncryptionMethod.getId()).setWithIntegrityPacket(useIntegrityPacket);
			encryptedDataGenerator = new PGPEncryptedDataGenerator(builder);
			final JcePublicKeyKeyEncryptionMethodGenerator encryptionMethodGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(publicKey);
			encryptedDataGenerator.addMethod(encryptionMethodGenerator);
			encryptedDataGeneratorStream = encryptedDataGenerator.open(encryptedData, new byte[4096]);

			compressedDataGenerator = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
			compressedDataGeneratorStream = compressedDataGenerator.open(encryptedDataGeneratorStream);

			literalDataGenerator = new PGPLiteralDataGenerator();
			// Use partial body lengths: available() is no reliable total size of a stream, and a literal data packet
			// with a fixed but wrong length produces a corrupt message
			literalDataGeneratorStream = literalDataGenerator.open(compressedDataGeneratorStream, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE, DateUtilities.getDateForLocalDateTime(LocalDateTime.now()), new byte[4096]);

			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataToEncrypt.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					literalDataGeneratorStream.write(buffer, 0, bytesRead);

					itemsDone += bytesRead;
					signalProgress();
				}
			}
			Utilities.clear(buffer);

			literalDataGenerator.close();
			compressedDataGenerator.close();
			encryptedDataGenerator.close();

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while encrypting", e);
		} finally {
			if (literalDataGenerator != null) {
				literalDataGenerator.close();
			}
			if (compressedDataGenerator != null) {
				compressedDataGenerator.close();
			}
			if (encryptedDataGenerator != null) {
				encryptedDataGenerator.close();
			}
			Utilities.closeQuietly(literalDataGeneratorStream);
			Utilities.closeQuietly(compressedDataGeneratorStream);
			Utilities.closeQuietly(encryptedDataGeneratorStream);
			if (armor) {
				Utilities.closeQuietly(encryptedData);
			}
		}
	}
}
