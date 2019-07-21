package de.soderer.utilities;

import java.io.ByteArrayInputStream;
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
		// -rw-rw-r-- 1 user group 8 Feb 2 23:22 test with blank.txt
		// drwx------ 2 root root 4096 Feb 2 21:19 dirname
		// lrwxrwxrwx 1 root root 33 Jun 29 06:23 //initrd.img -> boot/initrd.img-4.4.0-154-generic

		try (BasicReader reader = new BasicReader(new ByteArrayInputStream(dataString.getBytes("UTF-8")))) {
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
				} catch (final Exception e2) {
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
				final String changeDateString = dateString1 + " " + dateString2 + " " + dateString3;
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
				reader.readNextNonWhitespace();
				reader.reuseCurrentChar();
				String filePathAndName = "";
				Character nextChar;
				while ((nextChar = reader.readNextCharacter()) != null) {
					filePathAndName += nextChar;
				}
				if (filePathAndName.contains("/")) {
					filePath = filePathAndName.substring(0, filePathAndName.lastIndexOf("/"));
					fileName = filePathAndName.substring(filePathAndName.lastIndexOf("/") + 1);
				} else {
					filePath = "";
					fileName = filePathAndName;
				}

				if (fileType == JschFileType.SymbolicLink) {
					fileName = fileName.substring(0, fileName.indexOf(" ->"));
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
