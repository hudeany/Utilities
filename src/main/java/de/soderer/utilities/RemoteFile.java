package de.soderer.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RemoteFile {
	private final String filePath;
	private final long fileSize;
	private final Date modificationDate;

	public RemoteFile(final String filePath, final long fileSize, final Date modificationDate) {
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.modificationDate = modificationDate;
	}

	public String getFilePath() {
		return filePath;
	}

	public long getFileSize() {
		return fileSize;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	@Override
	public String toString() {
		return filePath + " [" + fileSize + " Byte, " + new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HHMMSS).format(modificationDate) + "]";
	}
}
