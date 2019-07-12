package de.soderer.utilities.remote;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.soderer.utilities.DateUtilities;

public class RemoteFileInfo {
	private final String fileName;
	private final long fileSize;
	private final Date modificationDate;

	public RemoteFileInfo(final String fileName, final long fileSize, final Date modificationDate) {
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

	public Date getModificationDate() {
		return modificationDate;
	}

	@Override
	public String toString() {
		return fileName + " [" + fileSize + " Byte, " + new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HHMMSS).format(modificationDate) + "]";
	}
}
