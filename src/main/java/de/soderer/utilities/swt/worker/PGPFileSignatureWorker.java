package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;

import de.soderer.utilities.PgpUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerSimple;
import de.soderer.utilities.swt.ProgressShowingModalDialog;

public class PGPFileSignatureWorker extends WorkerSimple<byte[]> {
	private InputStream dataToSign = null;
	private PGPSecretKey secretKey = null;
	private char[] password = null;
	private String signatureMethod = null;

	public PGPFileSignatureWorker(ProgressShowingModalDialog<?> parent, InputStream dataToSign, PGPSecretKey secretKey, char[] password) throws IOException {
		this(parent, dataToSign, secretKey, password, null);
	}

	public PGPFileSignatureWorker(ProgressShowingModalDialog<?> parent, InputStream dataToSign, PGPSecretKey secretKey, char[] password, String signatureMethod) throws IOException {
		super(parent);
		this.dataToSign = dataToSign;
		this.secretKey = secretKey;
		this.password = password;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = PgpUtilities.DEFAULT_SIGNATURE_METHOD_PGP;
		}
	}

	@Override
	public byte[] work() throws Exception {
		showProgress();

		PGPPrivateKey privateKey = null;
		ByteArrayOutputStream outputStream = null;
		ArmoredOutputStream outputStream2 = null;
		BufferedInputStream dataStream = null;
		BCPGOutputStream bcpgOutputStream = null;

		try {
			privateKey = secretKey.extractPrivateKey(password, "BC");
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				throw new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				throw new UserError("cannotReadPrivateKey", e);
			}
		} catch (Exception e) {
			throw new UserError("cannotReadPrivateKey", e);
		}

		try {
			itemsToDo = dataToSign.available();
			
			showProgress();

			Security.addProvider(new BouncyCastleProvider());

			outputStream = new ByteArrayOutputStream();
			outputStream2 = new ArmoredOutputStream(outputStream);
			PGPSignatureGenerator pgpSignatureGenerator = new PGPSignatureGenerator(secretKey.getPublicKey().getAlgorithm(), PgpUtilities.getPGPSignatureMethod(signatureMethod), "BC");
			pgpSignatureGenerator.initSign(PGPSignature.BINARY_DOCUMENT, privateKey);

			bcpgOutputStream = new BCPGOutputStream(outputStream2);
			dataStream = new BufferedInputStream(dataToSign);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataStream.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					pgpSignatureGenerator.update(buffer, 0, bytesRead);

					itemsDone += bytesRead;
					showProgress();
				}
			}

			pgpSignatureGenerator.generate().encode(bcpgOutputStream);

			outputStream2.close();

			return outputStream.toByteArray();
		} catch (Exception e) {
			throw new Exception("Cannot create signature", e);
		} finally {
			Utilities.closeQuietly(dataStream);
			Utilities.closeQuietly(bcpgOutputStream);
			Utilities.closeQuietly(outputStream2);
			Utilities.closeQuietly(outputStream);
		}
	}
}
