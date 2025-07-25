package de.soderer.utilities.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.utilities.UserError;
import de.soderer.utilities.Utilities;

public class FileDownloadWorker extends WorkerSimple<Boolean> {
	public static final int EOF = -1;

	private final String downloadUrl;
	private final File destinationFile;
	private ProxyConfiguration proxyConfiguration = null;

	public FileDownloadWorker(final WorkerParentSimple parent, final String downloadUrl, final File destinationFile) {
		super(parent);

		this.destinationFile = destinationFile;
		this.downloadUrl = downloadUrl;
	}

	public FileDownloadWorker setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
		return this;
	}

	@Override
	public Boolean work() throws Exception {
		signalProgress();

		try {
			if (destinationFile.exists()) {
				throw new Exception("File already exists: " + destinationFile.getAbsolutePath());
			}

			signalUnlimitedProgress();
			final URLConnection urlConnection = new URL(downloadUrl).openConnection(proxyConfiguration == null ? Proxy.NO_PROXY : proxyConfiguration.getProxy(downloadUrl));
			itemsToDo = urlConnection.getContentLengthLong();
			if (itemsToDo < 0) {
				itemsToDo = urlConnection.getContentLength();
			}
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
				if (e.getMessage().toLowerCase().contains("server returned http response code: 401")) {
					throw new UserError("error.userNotAuthenticatedOrNotAuthorized", UserError.Reason.UnauthenticatedOrUnauthorized);
				} else {
					throw new Exception("Cannot download file: " + e.getMessage(), e);
				}
			}

			if (cancel) {
				destinationFile.delete();
				return false;
			} else {
				signalProgress(true);
				return true;
			}
		} catch (final Exception e) {
			throw e;
		}
	}
}
