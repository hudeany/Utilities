package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Security;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.PGPUtilities.PgpHashMethod;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class PGPFileSignatureWorker extends WorkerSimple<byte[]> {
	private InputStream dataToSign = null;
	private PGPSecretKey secretKey = null;
	private char[] password = null;
	private String hashingMethod = null;

	public PGPFileSignatureWorker(final WorkerParentSimple parent, final InputStream dataToSign, final PGPSecretKey secretKey, final char[] password) {
		this(parent, dataToSign, secretKey, password, null);
	}

	public PGPFileSignatureWorker(final WorkerParentSimple parent, final InputStream dataToSign, final PGPSecretKey secretKey, final char[] password, final String hashingMethod) {
		super(parent);
		this.dataToSign = dataToSign;
		this.secretKey = secretKey;
		this.password = password;
		this.hashingMethod = hashingMethod;

		if (Utilities.isBlank(this.hashingMethod)) {
			this.hashingMethod = PGPUtilities.DEFAULT_HASHING_METHOD;
		}
	}

	@SuppressWarnings("resource")
	@Override
	public byte[] work() throws Exception {
		showProgress();

		final PGPPrivateKey privateKey = PGPUtilities.readPGPPrivateKey(secretKey, password);
		ByteArrayOutputStream outputStream = null;
		ArmoredOutputStream outputStream2 = null;
		BufferedInputStream dataStream = null;
		BCPGOutputStream bcpgOutputStream = null;

		try {
			itemsToDo = dataToSign.available();

			showProgress();

			Security.addProvider(new BouncyCastleProvider());

			outputStream = new ByteArrayOutputStream();
			outputStream2 = new ArmoredOutputStream(outputStream);
			final JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(), PgpHashMethod.getByName(hashingMethod).getId());
			final PGPSignatureGenerator pgpSignatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
			pgpSignatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

			bcpgOutputStream = new BCPGOutputStream(outputStream2);
			dataStream = new BufferedInputStream(dataToSign);

			final byte[] buffer = new byte[4096];
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
			Utilities.clear(buffer);

			pgpSignatureGenerator.generate().encode(bcpgOutputStream);

			outputStream2.close();

			return outputStream.toByteArray();
		} catch (final Exception e) {
			throw new Exception("Cannot create signature", e);
		} finally {
			Utilities.closeQuietly(dataStream);
			Utilities.closeQuietly(bcpgOutputStream);
			Utilities.closeQuietly(outputStream2);
			Utilities.closeQuietly(outputStream);
		}
	}
}
