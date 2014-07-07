package de.soderer.utilities.swt.worker;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import de.soderer.utilities.swt.ProgressShowingModalDialog;
import de.soderer.utilities.swt.Worker;

public class PGPFileSignatureWorker extends Worker<byte[]> {
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

		if (StringUtils.isBlank(this.signatureMethod)) {
			this.signatureMethod = PgpUtilities.DEFAULT_SIGNATURE_METHOD_PGP;
		}

		todo = dataToSign.available();
	}

	@Override
	public void run() {
		long position = 0L;
		result = null;

		PGPPrivateKey privateKey = null;
		ByteArrayOutputStream outputStream = null;
		ArmoredOutputStream outputStream2 = null;
		BufferedInputStream dataStream = null;
		BCPGOutputStream bcpgOutputStream = null;

		try {
			privateKey = secretKey.extractPrivateKey(password, "BC");
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				error = new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				error = new UserError("cannotReadPrivateKey", e);
			}
			showDone();
			isDone = true;
			return;
		} catch (Exception e) {
			error = new UserError("cannotReadPrivateKey", e);
			showDone();
			isDone = true;
			return;
		}

		try {
			showItemsDone(0L);

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

					position += bytesRead;

					showItemsDone(position);
				}
			}

			pgpSignatureGenerator.generate().encode(bcpgOutputStream);

			outputStream2.close();

			result = outputStream.toByteArray();
		} catch (Exception e) {
			error = new Exception("Cannot create signature", e);
		} finally {
			IOUtils.closeQuietly(dataStream);
			IOUtils.closeQuietly(bcpgOutputStream);
			IOUtils.closeQuietly(outputStream2);
			IOUtils.closeQuietly(outputStream);

			showDone();
			isDone = true;
		}
	}
}
