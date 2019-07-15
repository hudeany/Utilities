package de.soderer.utilities.swt.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.RemoteConnection;
import de.soderer.utilities.remote.RemoteFileInfo;
import de.soderer.utilities.remote.RemoteFolderInfo;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

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

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (Utilities.isBlank(remoteDirectoryPath)) {
			throw new Exception("No remoteDirectoryPath");
		} else if (Utilities.isBlank(localParentDirectoryPath)) {
			throw new Exception("No localDestination");
		}

		this.connection = connection;
		this.remoteDirectoryPath = FileUtilities.trimTrailingFileSeparator(remoteDirectoryPath);
		this.localParentDirectoryPath = FileUtilities.trimTrailingFileSeparator(localParentDirectoryPath);

		if (!this.connection.folderExists(this.remoteDirectoryPath)) {
			throw new Exception("Remote directory not found or is no directory: \"" + this.remoteDirectoryPath + "\"");
		} else if (!new File(this.localParentDirectoryPath).exists() || !new File(this.localParentDirectoryPath).isDirectory()) {
			throw new Exception("Destination not found or is no directory: \"" + this.remoteDirectoryPath + "\"");
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
