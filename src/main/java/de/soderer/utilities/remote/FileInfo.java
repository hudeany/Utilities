package de.soderer.utilities.remote;

import java.time.ZonedDateTime;

import de.soderer.utilities.DateUtilities;

public class FileInfo {
	private final String fileName;
	private final long fileSize;
	private final ZonedDateTime modificationDate;

	public FileInfo(final String fileName, final long fileSize, final ZonedDateTime modificationDate) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.modificationDate = modificationDate;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public ZonedDateTime getModificationDate() {
		return modificationDate;
	}

	@Override
	public String toString() {
		return fileName + " [" + fileSize + " Byte, " + DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, modificationDate.toLocalDateTime()) + "]";
	}
}
