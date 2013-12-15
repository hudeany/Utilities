package de.soderer.utilities.swt.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

/**
 * May need installed "US_export_policy.jar" and "local_policy.jar" for unlimited key strength Download: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
 */
public class PGPFileDecryptionWorker extends Worker<Boolean> {
	private InputStream encryptedData = null;
	private OutputStream decryptedData = null;
	private PGPSecretKey secretKey = null;
	private char[] password = null;

	public PGPFileDecryptionWorker(ProgressShowingModalDialog<?> parent, InputStream encryptedData, OutputStream decryptedData, PGPSecretKey secretKey, char[] password) throws IOException {
		super(parent);
		this.encryptedData = encryptedData;
		this.decryptedData = decryptedData;
		this.secretKey = secretKey;
		this.password = password;

		todo = encryptedData.available();
	}

	@Override
	public void run() {
		InputStream decryptedPacketInputStream = null;
		InputStream decodedInputStream = null;
		try {
			long position = 0L;
			result = null;

			PGPPrivateKey privateKey = null;
			encryptedData = PGPUtil.getDecoderStream(encryptedData);

			try {
				privateKey = secretKey.extractPrivateKey(password, "BC");
			} catch (PGPException e) {
				if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
					error = new UserError("keystrengthExceedsAllowedJCE", e);
				} else {
					error = new UserError("cannotReadPrivateKey", e);
				}
				showDone();
				isDone = true;
				return;
			} catch (Exception e) {
				error = new UserError("cannotReadPrivateKey", e);
				showDone();
				isDone = true;
				return;
			}

			PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(encryptedData);
			PGPEncryptedDataList encodedDataList;
			Object encodedDataObject = pgpObjectFactory.nextObject();
			if (encodedDataObject instanceof PGPEncryptedDataList) {
				encodedDataList = (PGPEncryptedDataList)encodedDataObject;
			} else {
				encodedDataList = (PGPEncryptedDataList)pgpObjectFactory.nextObject();
			}

			@SuppressWarnings("unchecked")
			Iterator<PGPPublicKeyEncryptedData> dataIterator = encodedDataList.getEncryptedDataObjects();
			PGPPublicKeyEncryptedData publicKeyEncryptedData = null;

			while (dataIterator.hasNext()) {
				publicKeyEncryptedData = dataIterator.next();
			}

			if (privateKey == null) {
				throw new IllegalArgumentException("secret key for message not found.");
			}

			// This execution checks for the JCE allowed key strength
			decryptedPacketInputStream = publicKeyEncryptedData.getDataStream(privateKey, "BC");

			Object message = new PGPObjectFactory(decryptedPacketInputStream).nextObject();

			if (message instanceof PGPCompressedData) {
				message = new PGPObjectFactory(((PGPCompressedData)message).getDataStream()).nextObject();
			}

			if (message instanceof PGPLiteralData) {
				PGPLiteralData literalData = (PGPLiteralData)message;
				@SuppressWarnings("unused")
				String outFileName = literalData.getFileName();
				decodedInputStream = literalData.getInputStream();

				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = decodedInputStream.read(buffer)) >= 0) {
					if (cancel) {
						break;
					} else {
						decryptedData.write(buffer, 0, bytesRead);
						position += bytesRead;

						showItemsDone(position);
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
			} else {
				throw new Exception("no message integrity check");
			}

			result = true;
		} catch (PGPException pgpe) {
			if (pgpe.getUnderlyingException() != null && pgpe.getUnderlyingException() instanceof InvalidKeyException
					&& TextUtilities.containsIgnoreCase(((InvalidKeyException)pgpe.getUnderlyingException()).getMessage(), "Illegal key size") && !SystemUtilities.isunlimitedKeyStrengthAllowed()) {
				error = new UserError(LangResources.get("keystrengthExceedsAllowedJCE"), pgpe);
			} else {
				error = pgpe;
			}
		} catch (Exception e) {
			error = new Exception("Error while decrypting", e);
		} finally {
			IOUtils.closeQuietly(decodedInputStream);
			IOUtils.closeQuietly(decryptedPacketInputStream);

			showDone();
			isDone = true;
		}
	}
}
