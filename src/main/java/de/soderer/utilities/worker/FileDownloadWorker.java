package de.soderer.utilities.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.util.Locale;

import de.soderer.utilities.UserError;
import de.soderer.utilities.Utilities;

/**
 * Worker that downloads a URL into a new file.
 * Its result is true on success and false if it was cancelled. An HTTP 401 response results in a
 * {@link UserError} with reason {@link UserError.Reason#UnauthenticatedOrUnauthorized}.
 */
public class FileDownloadWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of InputStream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	/**
	 * Maximum number of HTTP redirects followed.
	 */
	private static final int MAX_REDIRECTS = 5;

	private final String downloadUrl;
	private final File destinationFile;
	private Proxy proxy = null;

	/**
	 * Creates a download worker.
	 *
	 * @param parent parent receiving the progress signals, may be null
	 * @param downloadUrl URL to download
	 * @param destinationFile file to create, must not exist yet
	 */
	public FileDownloadWorker(final WorkerParentSimple parent, final String downloadUrl, final File destinationFile) {
		super(parent);

		this.destinationFile = destinationFile;
		this.downloadUrl = downloadUrl;
	}

	/**
	 * Sets the proxy for the download.
	 *
	 * @param proxy proxy configuration, or null for a direct connection
	 */
	public void setProxy(final Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Sets the proxy for the download.
	 *
	 * @param newProxy proxy configuration, or null for a direct connection
	 * @return this worker for chaining
	 */
	public FileDownloadWorker withProxy(final Proxy newProxy) {
		setProxy(newProxy);
		return this;
	}

	@Override
	public Boolean work() throws Exception {
		signalProgress();

		if (destinationFile.exists()) {
			throw new Exception("File already exists: " + destinationFile.getAbsolutePath());
		}

		signalUnlimitedProgress();
		final URLConnection urlConnection;
		try {
			urlConnection = openCheckedConnection();
		} catch (final UserError e) {
			throw e;
		} catch (final Exception e) {
			throw new Exception("Cannot download file: " + e.getMessage(), e);
		}
		itemsToDo = urlConnection.getContentLengthLong();
		// The announced size of the transferred data, which must be received completely
		final long contentLength = itemsToDo;
		if (itemsToDo < 0) {
			itemsToDo = urlConnection.getHeaderFieldLong("Original-Content-Length", 0);
		}
		signalProgress(true);
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
				FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
			final byte[] buffer = new byte[4096];
			int readLength;
			while ((readLength = bufferedInputStream.read(buffer)) != EOF) {
				if (cancel) {
					break;
				} else {
					fileOutputStream.write(buffer, 0, readLength);
					itemsDone += readLength;
					signalProgress();
				}
			}
			Utilities.clear(buffer);
		} catch (final Exception e) {
			// The destination did not exist before, so a partially written file can be removed
			destinationFile.delete();
			if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("server returned http response code: 401")) {
				throw new UserError("error.userNotAuthenticatedOrNotAuthorized", UserError.Reason.UnauthenticatedOrUnauthorized);
			} else {
				throw new Exception("Cannot download file: " + e.getMessage(), e);
			}
		}

		if (cancel) {
			destinationFile.delete();
			return false;
		} else if (contentLength >= 0 && itemsDone != contentLength) {
			// A connection closed early by the server or a proxy may end the data without an error
			destinationFile.delete();
			throw new Exception("Cannot download file: Download incomplete, received " + itemsDone + " of " + contentLength + " bytes");
		} else {
			signalProgress(true);
			return true;
		}
	}

	/**
	 * Opens the connection to the download URL. For HTTP the response code is checked and redirects are followed,
	 * also from http to https (which HttpURLConnection does not do itself), but not from https to http.
	 * Without this check the body of an error or redirect response would be stored as downloaded file.
	 *
	 * @return connection with a successful response
	 * @throws Exception if the server responds with an error, too many redirects or an insecure redirect
	 */
	private URLConnection openCheckedConnection() throws Exception {
		String currentUrl = downloadUrl;
		for (int redirectCount = 0;; redirectCount++) {
			final URLConnection urlConnection;
			if (proxy != null) {
				urlConnection = URI.create(currentUrl).toURL().openConnection(proxy);
			} else {
				urlConnection = URI.create(currentUrl).toURL().openConnection();
			}
			urlConnection.setConnectTimeout(30_000);
			urlConnection.setReadTimeout(30_000);
			if (!(urlConnection instanceof HttpURLConnection)) {
				return urlConnection;
			}

			final HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
			httpConnection.setInstanceFollowRedirects(false);
			final int responseCode = httpConnection.getResponseCode();
			if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
				final String location = httpConnection.getHeaderField("Location");
				httpConnection.disconnect();
				if (location == null) {
					throw new Exception("HTTP redirect " + responseCode + " without location");
				} else if (redirectCount >= MAX_REDIRECTS) {
					throw new Exception("Too many HTTP redirects");
				}
				final String nextUrl = URI.create(currentUrl).resolve(location).toString();
				if (currentUrl.toLowerCase(Locale.ROOT).startsWith("https:") && !nextUrl.toLowerCase(Locale.ROOT).startsWith("https:")) {
					throw new Exception("Insecure HTTP redirect from https to: " + nextUrl);
				}
				currentUrl = nextUrl;
			} else if (responseCode == 401) {
				httpConnection.disconnect();
				throw new UserError("error.userNotAuthenticatedOrNotAuthorized", UserError.Reason.UnauthenticatedOrUnauthorized);
			} else if (responseCode < 200 || responseCode >= 300) {
				httpConnection.disconnect();
				throw new Exception("Server returned HTTP response code: " + responseCode);
			} else {
				return httpConnection;
			}
		}
	}
}
