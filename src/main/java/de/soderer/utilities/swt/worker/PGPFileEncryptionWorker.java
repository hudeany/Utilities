package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerSimple;
import de.soderer.utilities.swt.ProgressShowingModalDialog;

public class PGPFileEncryptionWorker extends WorkerSimple<Boolean> {
	private InputStream dataToEncrypt = null;
	private OutputStream encryptedData = null;
	private PGPPublicKey publicKey = null;

	public PGPFileEncryptionWorker(ProgressShowingModalDialog<?> parent, InputStream dataToEncrypt, OutputStream encryptedData, PGPPublicKey publicKey) throws IOException {
		super(parent);
		this.dataToEncrypt = dataToEncrypt;
		this.encryptedData = encryptedData;
		this.publicKey = publicKey;
		
    	itemsToDo = dataToEncrypt.available();
	}
	
	//TODO: Encrypt big file
	@Override
	public void run() {
		startTime = new Date();
		itemsToDo = 0L;
		itemsDone = 0L;
		result = null;
		showProgress();
		
		boolean armor = true;
		
		OutputStream outputStream2 = null;
		OutputStream outputStream3 = null;
		OutputStream outputStream4 = null;
		try {
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
					
					itemsDone += bytesRead;
					showProgress();
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
			Utilities.closeQuietly(outputStream4);
			Utilities.closeQuietly(outputStream3);
			Utilities.closeQuietly(outputStream2);
			if (armor) {
				Utilities.closeQuietly(encryptedData);
			}
			showDone();
		}
	}
}
