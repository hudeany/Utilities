package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Locale;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that verifies a signature of the data of a stream with a public key. The stream is closed after the work.
 */
public class AsymmetricVerificationWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private InputStream dataInputStream = null;
	private byte[] signatureData = null;
	private PublicKey publicKey = null;
	private String signatureMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Creates a verification worker with the default method {@link CryptographicUtilities#DEFAULT_SIGNATURE_METHOD_RSA}.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the signed data
	 * @param signatureData signature to verify
	 * @param publicKey public key to verify with
	 */
	public AsymmetricVerificationWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final byte[] signatureData, final PublicKey publicKey) {
		this(parent, dataInputStream, signatureData, publicKey, null);
	}

	/**
	 * Creates a verification worker.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the signed data
	 * @param signatureData signature to verify
	 * @param publicKey public key to verify with
	 * @param signatureMethod signature method like "SHA256withRSA", blank for {@link CryptographicUtilities#DEFAULT_SIGNATURE_METHOD_RSA}.
	 *        The suffixes "withEC" and "withECDH" are mapped to "withECDSA".
	 */
	public AsymmetricVerificationWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final byte[] signatureData, final PublicKey publicKey, final String signatureMethod) {
		super(parent);
		this.dataInputStream = dataInputStream;
		this.signatureData = signatureData;
		this.publicKey = publicKey;
		this.signatureMethod = signatureMethod;

		if (Utilities.isBlank(this.signatureMethod)) {
			this.signatureMethod = CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA;
		}
	}

	/**
	 * Sets the expected data size in bytes for the progress display, if the stream cannot tell its size.
	 *
	 * @param dataSizeHint expected data size in bytes
	 */
	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	@Override
	public Boolean work() throws Exception {
		signatureMethod = signatureMethod.toLowerCase(Locale.ROOT);
		if (signatureMethod.endsWith("withec")) {
			signatureMethod = signatureMethod.replace("withec", "withecdsa");
		} else if (signatureMethod.endsWith("withecdh")) {
			signatureMethod = signatureMethod.replace("withecdh", "withecdsa");
		}

		signalProgress();

		itemsToDo = Math.max(dataSizeHint, dataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		try (BufferedInputStream dataStream = new BufferedInputStream(dataInputStream)) {
			final Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
			signature.initVerify(publicKey);
			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = dataStream.read(buffer)) != EOF) {
				if (cancel) {
					break;
				} else {
					signature.update(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					signalProgress();
				}
			}
			Utilities.clear(buffer);

			return signature.verify(signatureData);
		} catch (final Exception e) {
			throw new Exception("Cannot verify signature", e);
		}
	}
}
