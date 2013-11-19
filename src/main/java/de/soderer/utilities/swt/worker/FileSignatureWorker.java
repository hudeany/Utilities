package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class FileSignatureWorker extends Worker<Boolean> {
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

		if (StringUtils.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}
		
    	todo = fileToSign.length();
	}

	@Override
	public void run() {
		long position = 0L;
		result = null;
		try {
			byte[] signatureData;
			BufferedInputStream dataStream = null;
			try {
				showItemsDone(0L);
				
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
						position += bytesRead;
						
						showItemsDone(position);
					}
				}
				signatureData = signature.sign();
				FileUtils.writeByteArrayToFile(signatureDestinationFile, signatureData);
			} catch (Exception e) {
				throw new Exception("Cannot create signature", e);
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
