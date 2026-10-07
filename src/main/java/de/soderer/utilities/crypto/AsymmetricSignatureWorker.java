package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Locale;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that creates a signature of the data of a stream with a private key. The stream is closed after the work.
 */
public class AsymmetricSignatureWorker extends WorkerSimple<byte[]> {
	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private InputStream dataInputStream = null;
	private PrivateKey privateKey = null;
	private String signatureMethod = null;
	private long dataSizeHint = -1;

	/**
	 * Creates a signature worker with the default method {@link CryptographicUtilities#DEFAULT_SIGNATURE_METHOD_RSA}.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to sign
	 * @param privateKey private key to sign with
	 */
	public AsymmetricSignatureWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final PrivateKey privateKey) {
		this(parent, dataInputStream, privateKey, null);
	}

	/**
	 * Creates a signature worker.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to sign
	 * @param privateKey private key to sign with
	 * @param signatureMethod signature method like "SHA256withRSA", blank for {@link CryptographicUtilities#DEFAULT_SIGNATURE_METHOD_RSA}.
	 *        The suffixes "withEC" and "withECDH" are mapped to "withECDSA".
	 */
	public AsymmetricSignatureWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final PrivateKey privateKey, final String signatureMethod) {
		super(parent);
		this.dataInputStream = dataInputStream;
		this.privateKey = privateKey;
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
	public byte[] work() throws Exception {
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
			signature.initSign(privateKey);
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
			final byte[] signatureData = signature.sign();
			return signatureData;
		} catch (final Exception e) {
			throw new Exception("Cannot create signature: " + e.getMessage(), e);
		}
	}
}
