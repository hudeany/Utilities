package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.worker.WorkerSimple;

public class FileSignatureWorker extends WorkerSimple<Boolean> {
	private File fileToSign = null;
	private File signatureDestinationFile = null;
	private PrivateKey privateKey = null;
	private String signatureMethod = null;

	public FileSignatureWorker(final ProgressShowingModalDialog<?> parent, final File fileToSign, final File signatureDestinationFile, final PrivateKey privateKey) {
		this(parent, fileToSign, signatureDestinationFile, privateKey, null);
	}

	public FileSignatureWorker(final ProgressShowingModalDialog<?> parent, final File fileToSign, final File signatureDestinationFile, final PrivateKey privateKey, final String signatureMethod) {
		super(parent);
		this.fileToSign = fileToSign;
		this.signatureDestinationFile = signatureDestinationFile;
		this.privateKey = privateKey;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}
	}

	@Override
	public Boolean work() throws Exception {
		showProgress();

		itemsToDo = fileToSign.length();

		Security.addProvider(new BouncyCastleProvider());

		try (BufferedInputStream dataStream = new BufferedInputStream(new FileInputStream(fileToSign))) {
			final Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
			signature.initSign(privateKey);
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
			final byte[] signatureData = signature.sign();
			FileUtils.writeByteArrayToFile(signatureDestinationFile, signatureData);

			return true;
		} catch (final Exception e) {
			throw new Exception("Cannot create signature", e);
		}
	}
}
