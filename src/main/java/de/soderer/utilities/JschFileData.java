package de.soderer.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JschFileData {
	public enum JschFileType {
		RegularFile,
		Directory,
		CharacterDeviceFile,
		BlockDeviceFile,
		LocalSocketFile,
		NamedPipe,
		SymbolicLink
	}

	private JschFileType fileType;
	private String filePath;
	private String fileName;
	private long fileSize;
	private Date changeDate;

	public JschFileData(final String dataString) throws Exception {
		if (Utilities.isBlank(dataString)) {
			throw new Exception("Invalid file data string: " + dataString);
		}
		// -rw-r--r-- 1 root root 23 Feb 2 23:37 test.txt
		// -rw-rw-r-- 1 user group 8 Feb 2 23:22 test.txt
		// drwx------ 2 root root 4096 Feb 2 21:19 dirname
		// lrwxrwxrwx 1 root root 33 Jun 29 06:23 //initrd.img -> boot/initrd.img-4.4.0-154-generic


		final String[] dataStringParts = dataString.split("[ ]+");

		if (dataStringParts == null || dataStringParts.length < 9) {
			throw new Exception("Invalid file data string: " + dataString);
		}

		try {
			// Filetype
			final char fileTypeChar = dataStringParts[0].charAt(0);
			switch (fileTypeChar) {
				case '-':
					fileType = JschFileType.RegularFile;
					break;

				case 'd':
					fileType = JschFileType.Directory;
					break;

				case 'c':
					fileType = JschFileType.CharacterDeviceFile;
					break;

				case 'b':
					fileType = JschFileType.BlockDeviceFile;
					break;

				case 's':
					fileType = JschFileType.LocalSocketFile;
					break;

				case 'p':
					fileType = JschFileType.NamedPipe;
					break;

				case 'l':
					fileType = JschFileType.SymbolicLink;
					break;

				default:
					throw new Exception("Invalid file fileTypeChar: " + fileTypeChar);
			}

			// Rights
			@SuppressWarnings("unused")
			final
			String rightString = dataStringParts[0].substring(1);

			// inode links count

			// owner

			// group

			// size
			if (fileType == JschFileType.RegularFile) {
				try {
					fileSize = Long.parseLong(dataStringParts[4]);
				} catch (final Exception e2) {
					throw new Exception("Invalid file fileSize");
				}
			}

			if (fileType == JschFileType.CharacterDeviceFile
					|| fileType == JschFileType.BlockDeviceFile) {
				filePath = "";
				fileName = dataStringParts[9];
			} else {
				// change date
				final String changeDateString = dataStringParts[5] + " " + dataStringParts[6] + " " + dataStringParts[7];
				try {
					changeDate = new SimpleDateFormat("MMM d HH:mm", Locale.ENGLISH).parse(changeDateString);
				} catch (final Exception e) {
					try {
						changeDate = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH).parse(changeDateString);
					} catch (final Exception e1) {
						throw new Exception("Invalid changeDate");
					}
				}

				// name
				final String filePathAndName = dataStringParts[8];
				if (filePathAndName.contains("/")) {
					filePath = filePathAndName.substring(0, filePathAndName.lastIndexOf("/"));
					fileName = filePathAndName.substring(filePathAndName.lastIndexOf("/") + 1);
				} else {
					filePath = "";
					fileName = filePathAndName;
				}
			}
		} catch (final Exception e) {
			throw new Exception("Invalid file data string: " + dataString);
		}
	}

	public JschFileType getFileType() {
		return fileType;
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
