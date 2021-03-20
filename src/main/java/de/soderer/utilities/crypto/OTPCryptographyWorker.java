package de.soderer.utilities.crypto;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.BitUtilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class OTPCryptographyWorker extends WorkerSimple<Boolean> {
	private InputStream clearDataToEncrypt;
	private OTPKeyItem[] inputItems;
	private OutputStream clearData;
	private OTPKeyItem[] outputItems;
	private final boolean encrypt;

	/**
	 * OTP Encryption
	 */
	public OTPCryptographyWorker(final WorkerParentSimple parent, final InputStream clearDataToEncrypt, final long dataSize, final OTPKeyItem... outputItems) throws Exception {
		super(parent);

		this.clearDataToEncrypt = clearDataToEncrypt;
		this.outputItems = outputItems;
		encrypt = true;
		itemsToDo = dataSize;
	}

	/**
	 * OTP Decryption
	 */
	public OTPCryptographyWorker(final WorkerParentSimple parent, final OutputStream clearData, final OTPKeyItem... inputItems) throws Exception {
		super(parent);

		this.clearData = clearData;
		this.inputItems = inputItems;
		encrypt = false;
	}

	@Override
	public Boolean work() throws Exception {
		showProgress();

		final int bufferSize = 4096;
		if (encrypt) {
			final byte[] inputBuffer = new byte[bufferSize];
			final byte[][] outputBuffer = new byte[outputItems.length][bufferSize];
			int bytesRead;
			final MessageDigest messageDigest = MessageDigest.getInstance(outputItems[0].getHashmethod());
			while ((bytesRead = clearDataToEncrypt.read(inputBuffer)) >= 0) {
				CryptographicUtilities.otpWork(new byte[][] { inputBuffer }, outputBuffer);

				for (int i = 0; i < outputItems.length; i++) {
					outputItems[i].write(outputBuffer[i], 0, bytesRead);
				}

				messageDigest.update(inputBuffer, 0, bytesRead);

				itemsDone += bytesRead;
				showProgress();
			}
			final String dataHash = BitUtilities.getHexString(messageDigest.digest());
			for (int i = 0; i < outputItems.length; i++) {
				outputItems[i].setDataHash(dataHash);
			}
		} else {
			itemsToDo = inputItems[0].available();
			final MessageDigest messageDigest = MessageDigest.getInstance(inputItems[0].getHashmethod());
			final List<Integer> keyIds = new ArrayList<>();
			for (int i = 1; i < inputItems.length; i++) {
				if (itemsToDo != inputItems[i].available()) {
					throw new Exception("Invalid read state");
				}
				keyIds.add(inputItems[i].getID());
			}

			if (inputItems.length != inputItems[0].getKeyAmount()) {
				throw new Exception("Invalid number of keys. Needed keys: " + inputItems[0].getKeyAmount());
			}

			for (int i = 1; i <= inputItems[0].getKeyAmount(); i++) {
				if (!keyIds.contains(i)) {
					throw new Exception("Missing key id: " + i);
				}
			}

			final byte[][] inputBuffer = new byte[inputItems.length][bufferSize];
			final int[] bytesRead = new int[inputItems.length];
			final byte[] outputBuffer = new byte[bufferSize];
			while (bytesRead[0] >= 0) {
				for (int i = 0; i < inputItems.length; i++) {
					bytesRead[i] = inputItems[i].read(inputBuffer[i]);
					if (bytesRead[0] != bytesRead[i]) {
						throw new Exception("Invalid read state");
					}
				}

				if (bytesRead[0] > -1) {
					CryptographicUtilities.otpWork(inputBuffer, new byte[][] { outputBuffer });

					clearData.write(outputBuffer, 0, bytesRead[0]);

					messageDigest.update(outputBuffer, 0, bytesRead[0]);

					itemsDone += bytesRead[0];
					showProgress();
				}
			}

			final String dataHash = BitUtilities.getHexString(messageDigest.digest());
			if (!dataHash.equals(inputItems[0].getDataHash())) {
				throw new Exception("invalid data hash");
			}
		}
		return true;
	}
}