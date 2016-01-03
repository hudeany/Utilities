package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.WorkerSimple;
import de.soderer.utilities.swt.ProgressShowingModalDialog;

public class FileVerificationWorker extends WorkerSimple<Boolean> {
	private File fileToVerify = null;
	private File signatureFile = null;
	private PublicKey publicKey = null;
	private String signatureMethod = null;

	public FileVerificationWorker(ProgressShowingModalDialog<?> parent, File fileToVerify, File signatureFile, PublicKey publicKey) {
		this(parent, fileToVerify, signatureFile, publicKey, null);
	}
	
	public FileVerificationWorker(ProgressShowingModalDialog<?> parent, File fileToVerify, File signatureFile, PublicKey publicKey, String signatureMethod) {
		super(parent);
		this.fileToVerify = fileToVerify;
		this.signatureFile = signatureFile;
		this.publicKey = publicKey;
		this.signatureMethod = signatureMethod;

		if (StringUtils.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}
		
    	itemsToDo = fileToVerify.length();
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
			
			dataStream = new BufferedInputStream(new FileInputStream(fileToVerify));
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initVerify(publicKey);
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

			byte[] signatureData = FileUtils.readFileToByteArray(signatureFile);
			
			result = signature.verify(signatureData);
			
			result = true;
		} catch (Exception e) {
			error = new Exception("Cannot verify signature", e);
		} finally {
			IOUtils.closeQuietly(dataStream);
			
			showDone();
		}
	}
}
