package de.soderer.utilities.remote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.soderer.utilities.DateUtilities;

public class FolderInfo {
	private final String folderName;
	private final List<FolderInfo> subFolders;
	private final List<FileInfo> files;

	public FolderInfo(final String folderName, final List<FolderInfo> subFolders, final List<FileInfo> files) {
		this.folderName = folderName;
		this.subFolders = subFolders;
		this.files = files;
	}

	public String getFolderName() {
		return folderName;
	}

	public List<FolderInfo> getSubFolders() {
		return subFolders;
	}

	public List<FileInfo> getFiles() {
		return files;
	}

	public long getFolderDataSize() {
		long folderSize = 0;
		for (final FileInfo file : files) {
			folderSize += file.getFileSize();
		}
		return folderSize;
	}

	public long getRecursiveFolderDataSize() {
		long folderSize = 0;
		for (final FolderInfo subFolder : subFolders) {
			folderSize += subFolder.getRecursiveFolderDataSize();
		}
		for (final FileInfo file : files) {
			folderSize += file.getFileSize();
		}
		return folderSize;
	}

	public int getFileCount() {
		return files.size();
	}

	public int getRecursiveFileCount() {
		int recursiveFileCount = 0;
		for (final FolderInfo subFolder : subFolders) {
			recursiveFileCount += subFolder.getRecursiveFileCount();
		}
		recursiveFileCount += files.size();
		return recursiveFileCount;
	}

	public int getFolderCount() {
		return subFolders.size();
	}

	public int getRecursiveFolderCount() {
		int recursiveFolderCount = 0;
		for (final FolderInfo subFolder : subFolders) {
			recursiveFolderCount += subFolder.getRecursiveFolderCount();
			recursiveFolderCount++;
		}
		return recursiveFolderCount;
	}

	public Date getMinimumModificationDate() {
		Date minimumModificationDate = null;
		for (final FileInfo file : files) {
			if (minimumModificationDate == null || minimumModificationDate.before(file.getModificationDate())) {
				minimumModificationDate = file.getModificationDate();
			}
		}
		return minimumModificationDate;
	}

	public Date getRecursiveMinimumModificationDate() {
		Date minimumModificationDate = null;
		for (final FolderInfo subFolder : subFolders) {
			final Date minimumModificationDateSubFolder = subFolder.getRecursiveMinimumModificationDate();
			if (minimumModificationDate == null || minimumModificationDate.before(minimumModificationDateSubFolder)) {
				minimumModificationDate = minimumModificationDateSubFolder;
			}
		}
		for (final FileInfo file : files) {
			if (minimumModificationDate == null || minimumModificationDate.before(file.getModificationDate())) {
				minimumModificationDate = file.getModificationDate();
			}
		}
		return minimumModificationDate;
	}

	public Date getMaximumModificationDate() {
		Date maximumModificationDate = null;
		for (final FileInfo file : files) {
			if (maximumModificationDate == null || maximumModificationDate.before(file.getModificationDate())) {
				maximumModificationDate = file.getModificationDate();
			}
		}
		return maximumModificationDate;
	}

	public Date getRecursiveMaximumModificationDate() {
		Date maximumModificationDate = null;
		for (final FolderInfo subFolder : subFolders) {
			final Date maximumModificationDateSubFolder = subFolder.getRecursiveMaximumModificationDate();
			if (maximumModificationDate == null || maximumModificationDate.before(maximumModificationDateSubFolder)) {
				maximumModificationDate = maximumModificationDateSubFolder;
			}
		}
		for (final FileInfo file : files) {
			if (maximumModificationDate == null || maximumModificationDate.before(file.getModificationDate())) {
				maximumModificationDate = file.getModificationDate();
			}
		}
		return maximumModificationDate;
	}

	@Override
	public String toString() {
		return folderName + " [" + getFileCount() + " Files, " + getFolderCount() + " Folders, " + getFolderDataSize() + " Byte, " + new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HHMMSS).format(getMinimumModificationDate()) + " - " + new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HHMMSS).format(getMaximumModificationDate()) + "]";
	}
}
