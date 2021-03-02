package de.soderer.utilities.remote;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.soderer.utilities.DateUtilities;

public class FolderInfo {
	private final String folderName;
	private final Date modificationDate;

	public FolderInfo(final String folderName, final Date modificationDate) {
		this.folderName = folderName;
		this.modificationDate = modificationDate;
	}

	public String getFolderName() {
		return folderName;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	@Override
	public String toString() {
		return folderName + " [" + (getModificationDate() != null ? new SimpleDateFormat(DateUtilities.YYYY_MM_DD_HHMMSS).format(getModificationDate()) : "Unknown modification date")  + "]";
	}
}
