package de.soderer.utilities.swt.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.soderer.utilities.RemoteConnection;
import de.soderer.utilities.RemoteFileInfo;
import de.soderer.utilities.RemoteFolderInfo;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WorkerDual;
import de.soderer.utilities.WorkerParentDual;

public class DirectoryDownloadWorker extends WorkerDual<DirectoryDownloadWorker> {
	private RemoteConnection connection = null;

	private String remoteDirectoryPath = null;
	private String localParentDirectoryPath = null;

	/**
	 * Remote directory "remoteDirectoryPath" will be copied to local "localParentDirectoryPath/remoteDirectoryName"
	 *
	 * @param parent
	 * @param connection
	 * @param remoteDirectoryPath
	 * @param localParentDirectoryPath
	 * @throws Exception
	 */
	public DirectoryDownloadWorker(final WorkerParentDual parent, final RemoteConnection connection, final String remoteDirectoryPath, final String localParentDirectoryPath) throws Exception {
		super(parent);

		this.connection = connection;
		this.remoteDirectoryPath = remoteDirectoryPath;
		this.localParentDirectoryPath = localParentDirectoryPath;

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (Utilities.isBlank(remoteDirectoryPath)) {
			throw new Exception("No remoteDirectoryPath");
		} else if (!connection.folderExists(remoteDirectoryPath)) {
			throw new Exception("Remote directory not found or is no directory: \"" + remoteDirectoryPath + "\"");
		} else if (Utilities.isBlank(localParentDirectoryPath)) {
			throw new Exception("No localDestination");
		} else if (!new File(localParentDirectoryPath).exists() || !new File(localParentDirectoryPath).isDirectory()) {
			throw new Exception("Destination not found or is no directory: \"" + remoteDirectoryPath + "\"");
		}

		if (this.remoteDirectoryPath.endsWith("/")) {
			this.remoteDirectoryPath = this.remoteDirectoryPath.substring(0, this.remoteDirectoryPath.length() - 1);
		}

		if (this.localParentDirectoryPath.endsWith("/")) {
			this.localParentDirectoryPath = this.localParentDirectoryPath.substring(0, this.localParentDirectoryPath.length() - 1);
		}
	}

	@Override
	public DirectoryDownloadWorker work() throws Exception {
		showUnlimitedProgress();
		final RemoteFolderInfo remoteFolderInfo = connection.getFolderInfo(remoteDirectoryPath);
		itemsToDo = remoteFolderInfo.getRecursiveFolderDataSize();
		showProgress(true);
		final File destinationParentDirectoryPath = new File(localParentDirectoryPath + "/" + remoteFolderInfo.getFolderName());
		destinationParentDirectoryPath.mkdir();
		recursiveDownload(remoteDirectoryPath, remoteFolderInfo, destinationParentDirectoryPath);
		return this;
	}

	private void recursiveDownload(final String remoteFolderPath, final RemoteFolderInfo remoteFolderInfo, final File destinationParentDirectory) throws Exception {
		if (!cancel) {
			for (final RemoteFolderInfo subFolderInfo : remoteFolderInfo.getSubFolders()) {
				if (!cancel) {
					final File nextFolderDestination = new File(destinationParentDirectory, subFolderInfo.getFolderName());
					nextFolderDestination.mkdir();
					recursiveDownload(remoteFolderPath + "/" + subFolderInfo.getFolderName(), subFolderInfo, nextFolderDestination);
				}
			}
		}
		if (!cancel) {
			for (final RemoteFileInfo fileInfo : remoteFolderInfo.getFiles()) {
				final String nextRemoteFile = remoteFolderPath + "/" + fileInfo.getFileName();
				final File nextLocalFile = new File(destinationParentDirectory, fileInfo.getFileName());
				subItemsDone = 0;
				final String nextRemoteFileRelativePath = nextRemoteFile.substring(remoteDirectoryPath.length() + 1);
				showItemStart(nextRemoteFileRelativePath);
				showProgress();

				if (!cancel) {
					subItemsToDo = fileInfo.getFileSize();
					showItemStart(nextRemoteFileRelativePath);
					try (InputStream inputStream = connection.getFile(nextRemoteFile);
							OutputStream outputStream = new FileOutputStream(nextLocalFile)) {

						final byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = inputStream.read(buffer)) >= 0) {
							if (cancel) {
								break;
							} else {
								outputStream.write(buffer, 0, bytesRead);
								subItemsDone += bytesRead;
								showItemProgress();
							}
						}
						itemsDone += subItemsDone;
						showProgress();
					}

					if (cancel && nextLocalFile.exists()) {
						nextLocalFile.delete();
					}
				}
			}
		}
	}
}
