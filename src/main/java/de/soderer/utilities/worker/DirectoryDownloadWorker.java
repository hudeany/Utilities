package de.soderer.utilities.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.FileInfo;
import de.soderer.utilities.remote.FolderInfo;
import de.soderer.utilities.remote.RemoteConnection;

public class DirectoryDownloadWorker extends WorkerDual<DirectoryDownloadWorker> {
	private final RemoteConnection connection;
	private final String remoteDirectoryPath;
	private final File localParentDirectory;
	private final String localDirectoryName;

	public DirectoryDownloadWorker(final WorkerParentDual parent, final RemoteConnection connection, final String remoteDirectoryPath, final File localParentDirectory) throws Exception {
		this(parent, connection, remoteDirectoryPath, localParentDirectory, null);
	}

	/**
	 * Remote directory "remoteDirectoryPath" will be copied to local "localParentDirectoryPath/remoteDirectoryName"
	 *
	 * @param parent
	 * @param connection
	 * @param remoteDirectoryPath
	 * @param localParentDirectory
	 * @throws Exception
	 */
	public DirectoryDownloadWorker(final WorkerParentDual parent, final RemoteConnection connection, final String remoteDirectoryPath, final File localParentDirectory, final String localDirectoryName) throws Exception {
		super(parent);

		if (connection == null) {
			throw new Exception("No RemoteConnection");
		} else if (Utilities.isBlank(remoteDirectoryPath)) {
			throw new Exception("No remoteDirectoryPath");
		} else if (localParentDirectory == null) {
			throw new Exception("No localDestination");
		}

		this.connection = connection;
		this.remoteDirectoryPath = FileUtilities.trimTrailingFileSeparator(remoteDirectoryPath);
		this.localParentDirectory = localParentDirectory;
		if (Utilities.isBlank(localDirectoryName)) {
			this.localDirectoryName = FileUtilities.getFilenameFromPath(remoteDirectoryPath);
		} else {
			this.localDirectoryName = localDirectoryName;
		}

		if (this.connection.getFolderInfo(this.remoteDirectoryPath) == null) {
			throw new Exception("Remote directory not found or is no directory: \"" + this.remoteDirectoryPath + "\"");
		} else if (!this.localParentDirectory.exists()) {
			throw new Exception("Destination not found: \"" + this.localParentDirectory.getAbsolutePath() + "\"");
		} else if (!this.localParentDirectory.isDirectory()) {
			throw new Exception("Destination is no directory: \"" + this.localParentDirectory.getAbsolutePath() + "\"");
		}

		if (new File(this.localParentDirectory, this.localDirectoryName).exists()) {
			throw new Exception("Local directory already exists: \"" + new File(this.localParentDirectory, this.localDirectoryName).getAbsolutePath() + "\"");
		}
	}

	@Override
	public DirectoryDownloadWorker work() throws Exception {
		showUnlimitedProgress();
		itemsToDo = getRemoteDirectorySize(remoteDirectoryPath);
		showProgress(true);
		final File destinationParentDirectoryPath = new File(localParentDirectory, localDirectoryName);
		destinationParentDirectoryPath.mkdir();
		recursiveDownload(remoteDirectoryPath, destinationParentDirectoryPath);
		return this;
	}

	private long getRemoteDirectorySize(final String subDirectoryPath) throws Exception {
		final Tuple<List<FolderInfo>, List<FileInfo>> directory = connection.getDirectory(subDirectoryPath);
		long fileSizeSum = 0;

		for (final FileInfo fileInfo : directory.getSecond()) {
			fileSizeSum += fileInfo.getFileSize();
		}

		for (final FolderInfo folderInfo : directory.getFirst()) {
			fileSizeSum += getRemoteDirectorySize(subDirectoryPath + "/" + folderInfo.getFolderName());
		}

		return fileSizeSum;
	}

	private void recursiveDownload(final String remoteFolderPath, final File destinationParentDirectory) throws Exception {
		final Tuple<List<FolderInfo>, List<FileInfo>> directory = connection.getDirectory(remoteFolderPath);

		for (final FileInfo fileInfo : directory.getSecond()) {
			if (!cancel) {
				final String nextRemoteFile = remoteFolderPath + "/" + fileInfo.getFileName();
				final File nextLocalFile = new File(destinationParentDirectory, fileInfo.getFileName());
				subItemsDone = 0;
				subItemsToDo = fileInfo.getFileSize();
				showItemStart(nextRemoteFile.substring(remoteDirectoryPath.length() + 1));
				showProgress();
				try (InputStream inputStream = connection.downloadFile(nextRemoteFile);
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
					Utilities.clear(buffer);
					itemsDone += subItemsDone;
					showProgress();
				}

				if (cancel && nextLocalFile.exists()) {
					nextLocalFile.delete();
				}
			}
		}

		for (final FolderInfo subFolderInfo : directory.getFirst()) {
			if (!cancel) {
				final File nextFolderDestination = new File(destinationParentDirectory, subFolderInfo.getFolderName());
				nextFolderDestination.mkdir();
				recursiveDownload(remoteFolderPath + "/" + subFolderInfo.getFolderName(), nextFolderDestination);
			}
		}
	}
}
