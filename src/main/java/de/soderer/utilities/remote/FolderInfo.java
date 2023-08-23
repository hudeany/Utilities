package de.soderer.utilities.remote;

import java.time.ZonedDateTime;

import de.soderer.utilities.DateUtilities;

public class FolderInfo {
	private final String folderName;
	private final ZonedDateTime modificationDate;

	public FolderInfo(final String folderName, final ZonedDateTime modificationDate) {
		this.folderName = folderName;
		this.modificationDate = modificationDate;
	}

	public String getFolderName() {
		return folderName;
	}

	public ZonedDateTime getModificationDate() {
		return modificationDate;
	}

	@Override
	public String toString() {
		return folderName + " [" + (getModificationDate() != null ? DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, getModificationDate().toLocalDateTime()) : "Unknown modification date") + "]";
	}
}
