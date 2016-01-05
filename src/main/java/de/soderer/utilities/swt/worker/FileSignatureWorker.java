package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerSimple;
import de.soderer.utilities.swt.ProgressShowingModalDialog;

public class FileSignatureWorker extends WorkerSimple<Boolean> {
	private File fileToSign = null;
	private File signatureDestinationFile = null;
	private PrivateKey privateKey = null;
	private String signatureMethod = null;

	public FileSignatureWorker(ProgressShowingModalDialog<?> parent, File fileToSign, File signatureDestinationFile, PrivateKey privateKey) {
		this(parent, fileToSign, signatureDestinationFile, privateKey, null);
	}

	public FileSignatureWorker(ProgressShowingModalDialog<?> parent, File fileToSign, File signatureDestinationFile, PrivateKey privateKey, String signatureMethod) {
		super(parent);
		this.fileToSign = fileToSign;
		this.signatureDestinationFile = signatureDestinationFile;
		this.privateKey = privateKey;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}

		itemsToDo = fileToSign.length();
	}

	@Override
	public void run() {
		startTime = new Date();
		itemsToDo = 0L;
		itemsDone = 0L;
		result = null;
		showProgress();

		BufferedInputStream dataStream = null;
		try {
			Security.addProvider(new BouncyCastleProvider());

			dataStream = new BufferedInputStream(new FileInputStream(fileToSign));
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initSign(privateKey);
			byte[] buffer = new byte[4096];
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
			byte[] signatureData = signature.sign();
			FileUtils.writeByteArrayToFile(signatureDestinationFile, signatureData);

			result = true;
		} catch (Exception e) {
			error = new Exception("Cannot create signature", e);
		} finally {
			Utilities.closeQuietly(dataStream);

			showDone();
		}
	}
}
