package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * File information parsed from a line of an "ls -l" listing (as received via an SSH exec channel).
 */
public class JschFileData {
	/**
	 * Type of a file, as given by the first character of an "ls -l" line
	 */
	public enum JschFileType {
		/** Regular file ('-') */
		RegularFile,
		/** Directory ('d') */
		Directory,
		/** Character device file ('c') */
		CharacterDeviceFile,
		/** Block device file ('b') */
		BlockDeviceFile,
		/** Local socket file ('s') */
		LocalSocketFile,
		/** Named pipe ('p') */
		NamedPipe,
		/** Symbolic link ('l') */
		SymbolicLink
	}

	private JschFileType fileType;
	private String filePath;
	private String fileName;
	private long fileSize;
	private LocalDateTime changeDate;

	/**
	 * Parses a line of an "ls -l" listing, e.g. "-rw-r--r-- 1 user group 23 Feb 2 23:37 /home/user/test.txt".
	 * Dates without year (files changed within the last months) are assigned to the current year, or to the previous year
	 * if that date would be in the future. For symbolic links the link target ("name -&gt; target") is not part of the name.
	 *
	 * @param dataString line of an "ls -l" listing
	 * @throws Exception if the line cannot be parsed
	 */
	public JschFileData(final String dataString) throws Exception {
		if (Utilities.isBlank(dataString)) {
			throw new Exception("Invalid file data string: " + dataString);
		}
		// -rw-r--r-- 1 root root 23 Feb 2 23:37 test.txt
		// -rw-rw-r-- 1 user group 8 Feb 2 23:22 test.txt
		// -rw-rw-r-- 1 user group 8 Feb 2 23:22 test with blank.txt
		// drwx------ 2 root root 4096 Feb 2 21:19 dirname
		// lrwxrwxrwx 1 root root 33 Jun 29 06:23 //initrd.img -> boot/initrd.img-4.4.0-154-generic
		// -rw-------. 1 user@local.com namex 1176 Jan 23 15:03 /home/user/.ssh/authorized_keys
		// -rw-r--r-- 1 user@local.com namex 1182 Jun 14  2024 /home/user/.ssh/authorized_keys


		try (BasicReader reader = new BasicReader(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)))) {
			reader.readNextNonWhitespace();
			final String fileTypeAndRights = reader.readUpToNext(false, null, ' ');

			if (Utilities.isBlank(fileTypeAndRights)) {
				throw new Exception("Invalid file data string: " + dataString);
			}

			// Filetype
			final char fileTypeChar = fileTypeAndRights.charAt(0);
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

			// inode links count
			reader.readNextNonWhitespace();
			@SuppressWarnings("unused")
			final
			String inodeLinks = reader.readUpToNext(false, null, ' ');

			// owner
			reader.readNextNonWhitespace();
			@SuppressWarnings("unused")
			final
			String owner = reader.readUpToNext(false, null, ' ');

			// group
			reader.readNextNonWhitespace();
			@SuppressWarnings("unused")
			final
			String group = reader.readUpToNext(false, null, ' ');

			// size
			reader.readNextNonWhitespace();
			final String sizeString = reader.readUpToNext(false, null, ' ');
			if (fileType == JschFileType.RegularFile) {
				try {
					fileSize = Long.parseLong(sizeString);
				} catch (@SuppressWarnings("unused") final Exception e2) {
					throw new Exception("Invalid file fileSize");
				}
			}

			if (fileType == JschFileType.CharacterDeviceFile
					|| fileType == JschFileType.BlockDeviceFile) {
				reader.readNextNonWhitespace();
				@SuppressWarnings("unused")
				final String someString1 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				@SuppressWarnings("unused")
				final String someString2 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				@SuppressWarnings("unused")
				final String someString3 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				@SuppressWarnings("unused")
				final String someString4 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				reader.reuseCurrentChar();
				filePath = "";
				fileName = "";
				Character nextChar;
				while ((nextChar = reader.readNextCharacter()) != null) {
					fileName += nextChar;
				}
			} else {
				// change date
				reader.readNextNonWhitespace();
				final String dateString1 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				final String dateString2 = reader.readUpToNext(false, null, ' ');

				reader.readNextNonWhitespace();
				final String dateString3 = reader.readUpToNext(false, null, ' ');
				try {
					final String changeDateString = DateUtilities.parseThreeLetterMonth(dateString1) + " " + dateString2 + " " + dateString3;
					try {
						changeDate = DateUtilities.parseLocalDateTime("M d HH:mm yyyy", changeDateString + " " + new GregorianCalendar().get(Calendar.YEAR));
						// "ls" omits the year for dates within the last six months, which may be in the previous year
						if (changeDate.isAfter(LocalDateTime.now().plusDays(1))) {
							changeDate = changeDate.minusYears(1);
						}
					} catch (@SuppressWarnings("unused") final Exception e) {
						try {
							changeDate = DateUtilities.parseLocalDate("M d yyyy", changeDateString).atStartOfDay();
						} catch (@SuppressWarnings("unused") final Exception e1) {
							throw new Exception("Invalid changeDate");
						}
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					changeDate = null;
				}

				// name
				reader.readNextNonWhitespace();
				reader.reuseCurrentChar();
				String filePathAndName = "";
				Character nextChar;
				while ((nextChar = reader.readNextCharacter()) != null) {
					filePathAndName += nextChar;
				}
				if (fileType == JschFileType.SymbolicLink && filePathAndName.contains(" -> ")) {
					// Remove the link target before splitting path and name, because the target may contain '/' too
					filePathAndName = filePathAndName.substring(0, filePathAndName.indexOf(" -> "));
				}
				if (filePathAndName.contains("/")) {
					filePath = filePathAndName.substring(0, filePathAndName.lastIndexOf("/"));
					fileName = filePathAndName.substring(filePathAndName.lastIndexOf("/") + 1);
				} else {
					filePath = "";
					fileName = filePathAndName;
				}

			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			throw new Exception("Invalid file data string: " + dataString);
		}
	}

	/**
	 * Type of the file.
	 *
	 * @return file type
	 */
	public JschFileType getFileType() {
		return fileType;
	}

	/**
	 * Size of a regular file.
	 *
	 * @return size in bytes, 0 for all other file types
	 */
	public long getSize() {
		return fileSize;
	}

	/**
	 * Last change date (minute precision, or day precision for older files).
	 *
	 * @return change date, or null for device files or if it could not be parsed
	 */
	public LocalDateTime getChangeDate() {
		return changeDate;
	}

	/**
	 * Directory path of the file as given in the listing (without trailing '/').
	 *
	 * @return file path, empty if the listing contained no path
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Name of the file without path (and without link target for symbolic links).
	 *
	 * @return file name
	 */
	public String getFileName() {
		return fileName;
	}
}
