package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class FileVerificationWorker extends WorkerSimple<Boolean> {
	private File fileToVerify = null;
	private File signatureFile = null;
	private PublicKey publicKey = null;
	private String signatureMethod = null;

	public FileVerificationWorker(final WorkerParentSimple parent, final File fileToVerify, final File signatureFile, final PublicKey publicKey) {
		this(parent, fileToVerify, signatureFile, publicKey, null);
	}

	public FileVerificationWorker(final WorkerParentSimple parent, final File fileToVerify, final File signatureFile, final PublicKey publicKey, final String signatureMethod) {
		super(parent);
		this.fileToVerify = fileToVerify;
		this.signatureFile = signatureFile;
		this.publicKey = publicKey;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}
	}

	@Override
	public Boolean work() throws Exception {
		showProgress();

		itemsToDo = fileToVerify.length();

		Security.addProvider(new BouncyCastleProvider());

		try (BufferedInputStream dataStream = new BufferedInputStream(new FileInputStream(fileToVerify))) {
			final Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
			signature.initVerify(publicKey);
			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataStream.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					signature.update(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					showProgress();
				}
			}

			final byte[] signatureData = FileUtilities.readFileToByteArray(signatureFile);

			return signature.verify(signatureData);
		} catch (final Exception e) {
			throw new Exception("Cannot verify signature", e);
		}
	}
}
