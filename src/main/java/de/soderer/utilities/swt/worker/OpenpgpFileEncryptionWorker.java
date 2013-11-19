package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;

import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class OpenpgpFileEncryptionWorker extends Worker<Boolean> {
	private InputStream dataToEncrypt = null;
	private OutputStream encryptedData = null;
	private PGPPublicKey publicKey = null;

	public OpenpgpFileEncryptionWorker(ProgressShowingModalDialog<?> parent, InputStream dataToEncrypt, OutputStream encryptedData, PGPPublicKey publicKey) throws IOException {
		super(parent);
		this.dataToEncrypt = dataToEncrypt;
		this.encryptedData = encryptedData;
		this.publicKey = publicKey;
		
    	todo = dataToEncrypt.available();
	}
	
	//TODO: Encrypt big file
	@Override
	public void run() {
		long position = 0L;
		result = null;
		
		boolean armor = true;
		
		OutputStream outputStream2 = null;
		OutputStream outputStream3 = null;
		OutputStream outputStream4 = null;
		try {
			showItemsDone(0L);
			
			Security.addProvider(new BouncyCastleProvider());
			
			dataToEncrypt = new BufferedInputStream(dataToEncrypt);

			String fileName = PGPLiteralData.CONSOLE;

			if (armor) {
				encryptedData = new ArmoredOutputStream(encryptedData);
			}

			ByteArrayOutputStream bOut = new ByteArrayOutputStream();

			PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
			outputStream4 = comData.open(bOut);
			
			PGPLiteralDataGenerator ldata = new PGPLiteralDataGenerator();
			outputStream2 = ldata.open(outputStream4, PGPLiteralData.BINARY, fileName, dataToEncrypt.available(), new Date());

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataToEncrypt.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					outputStream2.write(buffer, 0, bytesRead);
					
					position += bytesRead;
					
					showItemsDone(position);
				}
			}

			comData.close();

			PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(PGPEncryptedData.CAST5, true, new SecureRandom(), "BC");

			cPk.addMethod(publicKey);

			byte[] bytes = bOut.toByteArray();

			outputStream3 = cPk.open(encryptedData, bytes.length);

			outputStream3.write(bytes);
			
			result = true;
		} catch (Exception e) {
			error = new Exception("Error while encrypting", e);
		} finally {
			IOUtils.closeQuietly(outputStream4);
			IOUtils.closeQuietly(outputStream3);
			IOUtils.closeQuietly(outputStream2);
			if (armor) {
				IOUtils.closeQuietly(encryptedData);
			}
			showDone();
			isDone = true;
		}
	}
}
