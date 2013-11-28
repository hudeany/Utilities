package de.soderer.utilities.swt.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class OTPCryptographyWorker extends Worker<Boolean> {
	private InputStream clearDataToEncrypt;
	private OutputStream[] encryptedData;
	private InputStream[] encryptedDataToDecrypt;
	private OutputStream clearData;
	private boolean encrypt;
	
	/**
	 * OTP Encryption
	 * 
	 * @param parent
	 * @param clearData
	 * @param encryptedData
	 * @throws IOException
	 */
	public OTPCryptographyWorker(ProgressShowingModalDialog<?> parent, InputStream clearData, OutputStream... encryptedData) throws Exception {
		super(parent);
		
		this.clearDataToEncrypt = clearData;
		this.encryptedData = encryptedData;
		encrypt = true;
		
    	todo = clearData.available();
	}
	
	/**
	 * OTP Decryption
	 * 
	 * @param parent
	 * @param clearData
	 * @param encryptedData
	 * @throws Exception 
	 */
	public OTPCryptographyWorker(ProgressShowingModalDialog<?> parent, OutputStream clearData, InputStream... encryptedData) throws Exception {
		super(parent);

		this.clearData = clearData;
		this.encryptedDataToDecrypt = encryptedData;
		encrypt = false;
		
    	todo = encryptedDataToDecrypt[0].available();
    	for (int i = 1; i < encryptedDataToDecrypt.length; i++) {
			if (todo != encryptedDataToDecrypt[i].available()) {
				throw new Exception("Invalid read state");
			}
		}
	}

	@Override
	public void run() {
		long position = 0L;
		result = null;
		try {
			int bufferSize = 4096;
			if (encrypt) {
				byte[] inputBuffer = new byte[bufferSize];
				byte[][] outputBuffer = new byte[encryptedData.length][bufferSize];
				int bytesRead;
				while ((bytesRead = clearDataToEncrypt.read(inputBuffer)) >= 0) {
					otpWork(new byte[][]{ inputBuffer }, outputBuffer);
					
					for (int i = 0; i < encryptedData.length; i++) {
						encryptedData[i].write(outputBuffer[i], 0, bytesRead);
					}
					
					position += bytesRead;
	
					showItemsDone(position);
				}
			} else {
				byte[][] inputBuffer = new byte[encryptedDataToDecrypt.length][bufferSize];
				int[] bytesRead = new int[encryptedDataToDecrypt.length];
				byte[] outputBuffer = new byte[bufferSize];
				while (bytesRead[0] >= 0) {
					for (int i = 0; i < encryptedDataToDecrypt.length; i++) {
						bytesRead[i] = encryptedDataToDecrypt[i].read(inputBuffer[0]);
						if (i > 0 && bytesRead[0] != bytesRead[i]) {
							throw new Exception("Invalid read state");
						}
					}
					
					otpWork(inputBuffer, new byte[][]{ outputBuffer });
					
					clearData.write(outputBuffer, 0, bytesRead[0]);
					
					position += bytesRead[0];
	
					showItemsDone(position);
				}
			}
		} catch (Exception e) {
			error = e;
		} finally {
			showDone();
			isDone = true;
		}
	}
	
	public static void otpWork(byte[][] immutableData, byte[][] mutableData) {
		for (int i = 0; i < mutableData.length - 1; i++) {
			Utilities.getRandomByteArray(mutableData[i]);
		}
		byte[] lastArray = mutableData[mutableData.length - 1];
		for (int i = 0; i < immutableData[0].length; i++) {
			lastArray[i] = 0;
			for (int j = 0; j < immutableData.length; j++) {
				lastArray[i] = (byte) (lastArray[i] ^ immutableData[j][i]);
			}
			for (int j = 0; j < mutableData.length - 1; j++) {
				lastArray[i] = (byte) (lastArray[i] ^ mutableData[j][i]);
			}
		}
	}
}