package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class FileVerificationWorker extends Worker<Boolean> {
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
		
    	todo = fileToVerify.length();
	}

	@Override
	public void run() {
		long position = 0L;
		result = null;
		try {
			byte[] signatureData = FileUtils.readFileToByteArray(signatureFile);
			
			BufferedInputStream dataStream = null;
			try {
				showItemsDone(0L);
				
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
						position += bytesRead;

						showItemsDone(position);
					}
				}
				result = signature.verify(signatureData);
			} catch (Exception e) {
				throw new Exception("Cannot verify signature", e);
			} finally {
				IOUtils.closeQuietly(dataStream);
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
