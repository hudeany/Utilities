package de.soderer.utilities.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.RemoteConnection;

public class RemoteFileDownloadWorker extends WorkerSimple<RemoteFileDownloadWorker> {
	private RemoteConnection connection = null;

	private String remoteFilePath = null;
	private File localFile = null;

	public RemoteFileDownloadWorker(final WorkerParentSimple parent, final RemoteConnection connection, final String remoteFilePath, final File localFile) throws Exception {
		super(parent);

		this.connection = connection;
		this.remoteFilePath = remoteFilePath;
		this.localFile = localFile;

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (Utilities.isBlank(remoteFilePath)) {
			throw new Exception("No remoteFilePath");
		} else if (connection.getFileInfo(remoteFilePath) == null) {
			throw new Exception("Remote file not found or is no file: \"" + remoteFilePath + "\"");
		} else if (localFile == null) {
			throw new Exception("No localFilePath");
		} else if (!localFile.getParentFile().exists()) {
			throw new Exception("Local file parent directory does not exist: \"" + localFile.getAbsolutePath() + "\"");
		} else if (localFile.exists()) {
			throw new Exception("Local file already exists: \"" + localFile.getAbsolutePath() + "\"");
		}
	}

	@Override
	public RemoteFileDownloadWorker work() throws Exception {
		showUnlimitedProgress();

		itemsToDo = connection.getFileInfo(remoteFilePath).getFileSize();

		showProgress();

		try (InputStream inputStream = connection.downloadFile(remoteFilePath);
				OutputStream outputStream = new FileOutputStream(localFile)) {
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
			Utilities.clear(buffer);
		}

		if (cancel && localFile.exists()) {
			localFile.delete();
		}

		return this;
	}
}
