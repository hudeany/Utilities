package de.soderer.utilities.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.RemoteConnection;

public class RemoteFileUploadWorker extends WorkerSimple<RemoteFileUploadWorker> {
	public static final int EOF = -1;

	private final RemoteConnection connection;

	private final File localFile;
	private final String remoteFilePath;

	public RemoteFileUploadWorker(final WorkerParentSimple parent, final RemoteConnection connection, final File localFile, final String remoteFilePath) throws Exception {
		super(parent);

		this.connection = connection;
		this.localFile = localFile;

		if (remoteFilePath != null && remoteFilePath.endsWith("/")) {
			this.remoteFilePath = remoteFilePath + localFile.getName();
		} else {
			this.remoteFilePath = remoteFilePath;
		}

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (localFile == null) {
			throw new Exception("No localFile");
		} else if (!localFile.exists()) {
			throw new Exception("Local file does not exist: \"" + localFile.getAbsolutePath() + "\"");
		} else if (!localFile.isFile()) {
			throw new Exception("Local file is not a file: \"" + localFile.getAbsolutePath() + "\"");
		} else if (Utilities.isBlank(this.remoteFilePath)) {
			throw new Exception("No remoteFilePath");
		} else if (!"/".equals(FileUtilities.getParentFromPath(this.remoteFilePath)) && connection.getFolderInfo(FileUtilities.getParentFromPath(this.remoteFilePath)) == null) {
			throw new Exception("Remote file parent folder does not exist: \"" + this.remoteFilePath + "\"");
		} else if (connection.getFileInfo(this.remoteFilePath) != null) {
			throw new Exception("Remote file already exists: \"" + this.remoteFilePath + "\"");
		}
	}

	@Override
	public RemoteFileUploadWorker work() throws Exception {
		signalUnlimitedProgress();

		itemsToDo = localFile.length();

		signalProgress();
		try (InputStream inputStream = new ProgressInputStream(new FileInputStream(localFile))) {
			connection.uploadFile(remoteFilePath, inputStream);
			signalProgress();
		}

		if (cancel && connection.getFileInfo(remoteFilePath) != null) {
			connection.deleteFile(remoteFilePath);
		}

		return this;
	}

	private class ProgressInputStream extends FilterInputStream {
		protected ProgressInputStream(final InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public int read() throws IOException {
			final int readByte = super.read();
			if (readByte != EOF) {
				itemsDone ++;
			}
			return readByte;
		}

		@Override
		public int read(final byte b[]) throws IOException {
			final int readBytes = super.read(b);
			if (readBytes != EOF) {
				itemsDone += readBytes;
			}
			return readBytes;
		}

		@Override
		public int read(final byte b[], final int off, final int len) throws IOException {
			final int readBytes = super.read(b, off, len);
			if (readBytes > 0) {
				itemsDone += readBytes;
			}
			return readBytes;
		}
	}
}
