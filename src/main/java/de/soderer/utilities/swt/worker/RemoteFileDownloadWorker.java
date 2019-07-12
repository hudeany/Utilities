package de.soderer.utilities.swt.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.RemoteConnection;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class RemoteFileDownloadWorker extends WorkerSimple<RemoteFileDownloadWorker> {
	private RemoteConnection connection = null;

	private String remoteFilePath = null;
	private String localFilePath = null;

	public RemoteFileDownloadWorker(final WorkerParentSimple parent, final RemoteConnection connection, final String remoteFilePath, final String localFilePath) throws Exception {
		super(parent);

		this.connection = connection;
		this.remoteFilePath = remoteFilePath;
		this.localFilePath = localFilePath;

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (Utilities.isBlank(remoteFilePath)) {
			throw new Exception("No remoteFilePath");
		} else if (!connection.fileExists(remoteFilePath)) {
			throw new Exception("Remote file not found or is no directory: \"" + remoteFilePath + "\"");
		} else if (Utilities.isBlank(localFilePath)) {
			throw new Exception("No localFilePath");
		} else if (new File(localFilePath).exists()) {
			throw new Exception("Local file already exists: \"" + localFilePath + "\"");
		}
	}

	@Override
	public RemoteFileDownloadWorker work() throws Exception {
		showUnlimitedProgress();

		itemsToDo = connection.getFileInfo(remoteFilePath).getFileSize();

		showProgress();

		try (InputStream inputStream = connection.getFile(remoteFilePath);
				OutputStream outputStream = new FileOutputStream(localFilePath)) {

			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) >= 0) {
				if (cancel) {
					break;
				} else {
					outputStream.write(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					showProgress();
				}
			}
		}

		if (cancel && new File(localFilePath).exists()) {
			new File(localFilePath).delete();
		}

		return this;
	}
}
