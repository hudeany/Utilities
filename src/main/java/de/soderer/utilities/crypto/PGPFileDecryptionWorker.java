package de.soderer.utilities.crypto;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * May need installed "US_export_policy.jar" and "local_policy.jar" for unlimited key strength Download: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
 */
public class PGPFileDecryptionWorker extends WorkerSimple<Boolean> {
	private InputStream encryptedData = null;
	private OutputStream decryptedData = null;
	private PGPSecretKey secretKey = null;
	private char[] password = null;

	public PGPFileDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedData, final OutputStream decryptedData, final PGPSecretKey secretKey, final char[] password) {
		super(parent);
		this.encryptedData = encryptedData;
		this.decryptedData = decryptedData;
		this.secretKey = secretKey;
		this.password = password;
	}

	@SuppressWarnings("resource")
	@Override
	public Boolean work() throws Exception {
		showProgress();

		InputStream decryptedPacketInputStream = null;
		InputStream decodedInputStream = null;
		InputStream interimInputStream = null;
		try {
			itemsToDo = encryptedData.available();

			Security.addProvider(new BouncyCastleProvider());

			final PGPPrivateKey privateKey = PGPUtilities.readPGPPrivateKey(secretKey, password);
			encryptedData = PGPUtil.getDecoderStream(encryptedData);

			final PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(encryptedData, new JcaKeyFingerprintCalculator());
			PGPEncryptedDataList encodedDataList;
			final Object encodedDataObject = pgpObjectFactory.nextObject();
			if (encodedDataObject instanceof PGPEncryptedDataList) {
				encodedDataList = (PGPEncryptedDataList) encodedDataObject;
			} else {
				encodedDataList = (PGPEncryptedDataList) pgpObjectFactory.nextObject();
			}

			final Iterator<PGPEncryptedData> dataIterator = encodedDataList.getEncryptedDataObjects();
			PGPPublicKeyEncryptedData publicKeyEncryptedData = null;

			while (dataIterator.hasNext()) {
				publicKeyEncryptedData = (PGPPublicKeyEncryptedData) dataIterator.next();
			}

			if (privateKey == null) {
				throw new IllegalArgumentException("secret key for message not found.");
			}

			if (publicKeyEncryptedData == null) {
				throw new IllegalArgumentException("encrypted data not found.");
			}

			// This execution checks for the JCE allowed key strength
			final JcePublicKeyDataDecryptorFactoryBuilder jcePublicKeyDataDecryptorFactoryBuilder = new JcePublicKeyDataDecryptorFactoryBuilder();
			decryptedPacketInputStream = publicKeyEncryptedData.getDataStream(jcePublicKeyDataDecryptorFactoryBuilder.build(privateKey));

			Object message = new PGPObjectFactory(decryptedPacketInputStream, new JcaKeyFingerprintCalculator()).nextObject();

			if (message instanceof PGPCompressedData) {
				interimInputStream = ((PGPCompressedData) message).getDataStream();
				message = new PGPObjectFactory(interimInputStream, new JcaKeyFingerprintCalculator()).nextObject();
			}

			if (message instanceof PGPLiteralData) {
				final PGPLiteralData literalData = (PGPLiteralData) message;
				@SuppressWarnings("unused")
				final
				String outFileName = literalData.getFileName();
				decodedInputStream = literalData.getInputStream();

				final byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = decodedInputStream.read(buffer)) >= 0) {
					if (cancel) {
						break;
					} else {
						decryptedData.write(buffer, 0, bytesRead);
						itemsDone += bytesRead;
						showProgress();
					}
				}
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("encrypted message contains a signed message - not literal data.");
			} else {
				throw new PGPException("message is not a simple encrypted file - type unknown.");
			}

			if (publicKeyEncryptedData.isIntegrityProtected()) {
				if (!publicKeyEncryptedData.verify()) {
					throw new Exception("message failed integrity check");
				}
			}

			return true;
		} catch (final PGPException pgpe) {
			if (pgpe.getUnderlyingException() != null && pgpe.getUnderlyingException() instanceof InvalidKeyException
					&& TextUtilities.containsIgnoreCase(((InvalidKeyException) pgpe.getUnderlyingException()).getMessage(), "Illegal key size") && !SystemUtilities.isUnlimitedKeyStrengthAllowed()) {
				throw new UserError(LangResources.get("keystrengthExceedsAllowedJCE"), pgpe);
			} else {
				throw pgpe;
			}
		} catch (final Exception e) {
			throw new Exception("Error while decrypting", e);
		} finally {
			Utilities.closeQuietly(interimInputStream);
			Utilities.closeQuietly(decodedInputStream);
			Utilities.closeQuietly(decryptedPacketInputStream);
		}
	}
}
