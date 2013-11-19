package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class PGPFileVerificationWorker extends Worker<Boolean> {
	private InputStream dataToVerify = null;
	private InputStream signatureData = null;
	private PGPPublicKey publicKey = null;
	private String signatureMethod = null;

	public PGPFileVerificationWorker(ProgressShowingModalDialog<?> parent, InputStream dataToVerify, PGPPublicKey publicKey, InputStream signatureData) throws IOException {
		this(parent, dataToVerify, publicKey, signatureData, null);
	}
	
	public PGPFileVerificationWorker(ProgressShowingModalDialog<?> parent, InputStream dataToVerify, PGPPublicKey publicKey, InputStream signatureData, String signatureMethod) throws IOException {
		super(parent);
		this.dataToVerify = dataToVerify;
		this.signatureData = signatureData;
		this.publicKey = publicKey;
		this.signatureMethod = signatureMethod;

		if (StringUtils.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD;
		}
		
    	todo = dataToVerify.available();
	}
	
	@Override
	public void run() {
		long position = 0L;
		result = null;
		
		try {
			PGPObjectFactory pgpFact = new PGPObjectFactory(PGPUtil.getDecoderStream(signatureData));
			PGPSignatureList p3;

			Object o = pgpFact.nextObject();
			if (o instanceof PGPCompressedData) {
				PGPCompressedData c1 = (PGPCompressedData)o;
				pgpFact = new PGPObjectFactory(c1.getDataStream());
				p3 = (PGPSignatureList)pgpFact.nextObject();
			} else {
				p3 = (PGPSignatureList)o;
			}

			PGPSignature signature = p3.get(0);
			signature.initVerify(publicKey, "BC");

			dataToVerify = new BufferedInputStream(dataToVerify);
			
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataToVerify.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					signature.update(buffer, 0, bytesRead);
					position += bytesRead;

					showItemsDone(position);
				}
			}

			result = signature.verify();
		} catch (Exception e) {
			error = new Exception("Cannot verify signature", e);
		} finally {
			showDone();
			isDone = true;
		}
	}
}
