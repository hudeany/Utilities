package de.soderer.utilities.swt.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.FileInfo;
import de.soderer.utilities.remote.FolderInfo;
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
		} else if (!"/".equals(remoteParentDirectoryPath) && !connection.folderExists(remoteParentDirectoryPath)) {
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
		if (connection.folderExists(remotePath) || connection.fileExists(remotePath)) {
			throw new Exception("Remote directory already exists: \"" + remotePath + "\"");
		}
	}

	@Override
	public DirectoryUploadWorker work() throws Exception {
		showUnlimitedProgress();
		final FolderInfo localFolderInfo = FileUtilities.getFolderInfo(localDirectory.getAbsolutePath());
		itemsToDo = localFolderInfo.getRecursiveFolderDataSize();
		showProgress(true);
		connection.createFolder(remoteParentDirectoryPath, localFolderInfo.getFolderName());
		recursiveUpload(localDirectory, localFolderInfo, remoteParentDirectoryPath + "/" + localFolderInfo.getFolderName());
		return this;
	}

	private void recursiveUpload(final File localUploadDirectory, final FolderInfo localFolderInfo, final String destinationDirectory) throws Exception {
		if (!cancel) {
			for (final FolderInfo subFolderInfo : localFolderInfo.getSubFolders()) {
				if (!cancel) {
					connection.createFolder(destinationDirectory, subFolderInfo.getFolderName());
					recursiveUpload(new File(localUploadDirectory, subFolderInfo.getFolderName()), subFolderInfo, destinationDirectory + "/" + subFolderInfo.getFolderName());
				}
			}
		}
		if (!cancel) {
			for (final FileInfo fileInfo : localFolderInfo.getFiles()) {
				if (!cancel) {
					final File nextLocalFile = new File(localUploadDirectory, fileInfo.getFileName());
					final String nextRemoteFile = destinationDirectory + "/" + fileInfo.getFileName();
					subItemsDone = 0;
					subItemsToDo = fileInfo.getFileSize();
					showItemStart(fileInfo.getFileName());
					showProgress();
					try (InputStream inputStream = new ProgressInputStream(new FileInputStream(nextLocalFile))) {
						connection.uploadFile(nextRemoteFile, inputStream);
						itemsDone += subItemsDone;
						showProgress();
					}

					if (cancel && connection.fileExists(nextRemoteFile)) {
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
