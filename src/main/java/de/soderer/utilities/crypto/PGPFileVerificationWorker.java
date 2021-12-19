package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class PGPFileVerificationWorker extends WorkerSimple<Boolean> {
	private InputStream dataToVerify = null;
	private InputStream signatureData = null;
	private PGPPublicKey publicKey = null;
	private String signatureMethod = null;

	public PGPFileVerificationWorker(final WorkerParentSimple parent, final InputStream dataToVerify, final PGPPublicKey publicKey, final InputStream signatureData) {
		this(parent, dataToVerify, publicKey, signatureData, null);
	}

	public PGPFileVerificationWorker(final WorkerParentSimple parent, final InputStream dataToVerify, final PGPPublicKey publicKey, final InputStream signatureData, final String signatureMethod) {
		super(parent);
		this.dataToVerify = dataToVerify;
		this.signatureData = signatureData;
		this.publicKey = publicKey;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA;
		}
	}

	@SuppressWarnings("resource")
	@Override
	public Boolean work() throws Exception {
		showProgress();

		InputStream interimInputStream = null;
		try (InputStream decoderStream = PGPUtil.getDecoderStream(signatureData)) {
			itemsToDo = dataToVerify.available();

			PGPObjectFactory pgpFact = new PGPObjectFactory(decoderStream, new JcaKeyFingerprintCalculator());
			PGPSignatureList p3;

			final Object o = pgpFact.nextObject();
			if (o instanceof PGPCompressedData) {
				final PGPCompressedData c1 = (PGPCompressedData) o;
				interimInputStream = c1.getDataStream();
				pgpFact = new PGPObjectFactory(interimInputStream, new JcaKeyFingerprintCalculator());
				p3 = (PGPSignatureList) pgpFact.nextObject();
			} else {
				p3 = (PGPSignatureList) o;
			}

			final PGPSignature signature = p3.get(0);
			signature.init(new JcaPGPContentVerifierBuilderProvider(), publicKey);

			dataToVerify = new BufferedInputStream(dataToVerify);

			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataToVerify.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					signature.update(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					showProgress();
				}
			}
			Utilities.clear(buffer);

			return signature.verify();
		} catch (final Exception e) {
			throw new Exception("Cannot verify signature", e);
		} finally {
			Utilities.closeQuietly(interimInputStream);
		}
	}
}
