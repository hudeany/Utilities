package de.soderer.utilities;

import java.util.Date;

public class JschFileData {
	private boolean isFile;
	private String filePath;
	private String fileName;
	private long fileSize;
	private Date changeDate;

	public JschFileData(String dataString) throws Exception {
		if (Utilities.isBlank(dataString)) {
			throw new Exception("Invalid file data string: " + dataString);
		}
		// -rw-r--r-- 1 root root 23 Feb 2 23:37 test.txt
		// -rw-rw-r-- 1 user group 8 Feb 2 23:22 test.txt
		// drwx------ 2 root root 4096 Feb 2 21:19 dirname

		String[] dataStringParts = dataString.split("[ ]+");

		if (dataStringParts == null || dataStringParts.length != 9) {
			throw new Exception("Invalid file data string: " + dataString);
		}

		try {
			// Filetype
			char fileTypeChar = dataStringParts[0].charAt(0);
			if (fileTypeChar == 'd') {
				isFile = false;
			} else if (fileTypeChar == '-') {
				isFile = true;
			} else {
				throw new Exception("Invalid file fileTypeChar");
			}

			// Rights
			@SuppressWarnings("unused")
			String rightString = dataStringParts[0].substring(1);

			// inode links count

			// owner

			// group

			// size
			try {
				fileSize = Long.parseLong(dataStringParts[4]);
			} catch (Exception e2) {
				throw new Exception("Invalid file fileSize");
			}

			// change date
			String changeDateString = dataStringParts[5] + " " + dataStringParts[6] + " " + dataStringParts[7];
			try {
				changeDate = DateUtilities.MMM_D_HHMM.parse(changeDateString);
			} catch (Exception e) {
				try {
					changeDate = DateUtilities.MMM_D_YYYY.parse(changeDateString);
				} catch (Exception e1) {
					throw new Exception("Invalid changeDate");
				}
			}

			// name
			String filePathAndName = dataStringParts[8];
			if (filePathAndName.contains("/")) {
				filePath = filePathAndName.substring(0, filePathAndName.lastIndexOf("/"));
				fileName = filePathAndName.substring(filePathAndName.lastIndexOf("/") + 1);
			} else {
				filePath = "";
				fileName = filePathAndName;
			}
		} catch (Exception e) {
			throw new Exception("Invalid file data string: " + dataString);
		}
	}

	public boolean getIsFile() {
		return isFile;
	}

	public long getSize() {
		return fileSize;
	}

	public Date getChangeDate() {
		return changeDate;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
	}
}
