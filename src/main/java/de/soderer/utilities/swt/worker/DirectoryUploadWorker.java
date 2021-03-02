package de.soderer.utilities.swt.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.RemoteConnection;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DirectoryUploadWorker extends WorkerDual<DirectoryUploadWorker> {
	private final RemoteConnection connection;
	private final File localDirectory;
	private String remoteParentDirectoryPath;
	private final String remoteDirectoryName;

	public DirectoryUploadWorker(final WorkerParentDual parent, final RemoteConnection connection, final File localDirectory, final String remoteParentDirectoryPath) throws Exception {
		this(parent, connection, localDirectory, remoteParentDirectoryPath, null);
	}

	/**
	 *Local directory "localParentDirectoryPath" will be copied to remote "remoteDirectoryPath/remoteDirectoryName"
	 */
	public DirectoryUploadWorker(final WorkerParentDual parent, final RemoteConnection connection, final File localDirectory, final String remoteParentDirectoryPath, final String remoteDirectoryName) throws Exception {
		super(parent);

		this.connection = connection;
		this.localDirectory = localDirectory;
		this.remoteParentDirectoryPath = remoteParentDirectoryPath;

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (localDirectory == null) {
			throw new Exception("No localDestination");
		} else if (!localDirectory.exists()) {
			throw new Exception("Source directory not found: \"" + localDirectory.getAbsolutePath() + "\"");
		} else if (!localDirectory.isDirectory()) {
			throw new Exception("Source directory is no directory: \"" + localDirectory.getAbsolutePath() + "\"");
		} else if (Utilities.isBlank(remoteParentDirectoryPath)) {
			throw new Exception("No remoteParentDirectoryPath");
		} else if (!"/".equals(remoteParentDirectoryPath) && connection.getFolderInfo(remoteParentDirectoryPath) == null) {
			throw new Exception("Remote directory not found or is no directory: \"" + remoteParentDirectoryPath + "\"");
		}

		if (this.remoteParentDirectoryPath.endsWith("/")) {
			this.remoteParentDirectoryPath = this.remoteParentDirectoryPath.substring(0, this.remoteParentDirectoryPath.length() - 1);
		}

		if (Utilities.isBlank(remoteDirectoryName)) {
			this.remoteDirectoryName = this.localDirectory.getName();
		} else {
			this.remoteDirectoryName = remoteDirectoryName;
		}

		String remotePath;
		if ("/".equals(remoteParentDirectoryPath)) {
			remotePath = "/" + this.remoteDirectoryName;
		} else {
			remotePath = remoteParentDirectoryPath + "/" + this.remoteDirectoryName;
		}
		if (connection.getFolderInfo(remotePath) != null || connection.getFileInfo(remotePath) != null) {
			throw new Exception("Remote directory already exists: \"" + remotePath + "\"");
		}
	}

	@Override
	public DirectoryUploadWorker work() throws Exception {
		showUnlimitedProgress();
		itemsToDo = FileUtilities.getAllSubfilesSize(localDirectory);
		showProgress(true);
		connection.createFolder(remoteParentDirectoryPath, localDirectory.getName());
		recursiveUpload(localDirectory, remoteParentDirectoryPath + "/" + localDirectory.getName());
		return this;
	}

	private void recursiveUpload(final File localUploadDirectory, final String destinationDirectory) throws Exception {
		final File[] subFiles = localUploadDirectory.listFiles();
		if (subFiles != null) {
			for (final File file : subFiles) {
				if (cancel) {
					break;
				} else if (file.isDirectory()) {
					if (!cancel) {
						connection.createFolder(destinationDirectory, file.getName());
						recursiveUpload(new File(localUploadDirectory, file.getName()), destinationDirectory + "/" + file.getName());
					}
				} else {
					final File nextLocalFile = new File(localUploadDirectory, file.getName());
					final String nextRemoteFile = destinationDirectory + "/" + file.getName();
					subItemsDone = 0;
					subItemsToDo = file.length();
					showItemStart(file.getName());
					showProgress();
					try (InputStream inputStream = new ProgressInputStream(new FileInputStream(nextLocalFile))) {
						connection.uploadFile(nextRemoteFile, inputStream);
						itemsDone += subItemsDone;
						showProgress();
					}

					if (cancel && connection.getFileInfo(nextRemoteFile) != null) {
						connection.deleteFile(nextRemoteFile);
					}
				}
			}
		}
	}

	private class ProgressInputStream extends FilterInputStream {
		protected ProgressInputStream(final InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public int read() throws IOException {
			final int readByte = super.read();
			if (readByte != -1) {
				subItemsDone ++;
			}
			return readByte;
		}

		@Override
		public int read(final byte b[]) throws IOException {
			final int readBytes = super.read(b);
			if (readBytes > 0) {
				subItemsDone += readBytes;
			}
			return readBytes;
		}

		@Override
		public int read(final byte b[], final int off, final int len) throws IOException {
			final int readBytes = super.read(b, off, len);
			if (readBytes > 0) {
				subItemsDone += readBytes;
			}
			return readBytes;
		}
	}
}
