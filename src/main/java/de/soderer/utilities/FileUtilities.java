package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

/**
 * Utilities for files and directories: searching, reading, writing, comparing and deleting.
 */
public class FileUtilities {
	/**
	 * Value returned by read operations at the end of data.
	 */
	public static final int EOF = -1;

	/**
	 * Utility class
	 */
	private FileUtilities() {
		// Utility class
	}

	/**
	 * Returns the files of a directory (not recursive), whose names start with the given text and end
	 * with the given extension.
	 *
	 * @param directory
	 *            the directory to search in
	 * @param nameStart
	 *            the required start of the file names
	 * @param extension
	 *            the required file extension without leading dot
	 * @return the matching files, an empty array if the directory cannot be read
	 */
	public static File[] getSubfilesByNameAndExtension(final File directory, final String nameStart, final String extension) {
		final List<File> files = new LinkedList<>();
		final File[] directoryFiles = directory.listFiles();
		if (directoryFiles != null) {
			for (final File file : directoryFiles) {
				if (file.getName().startsWith(nameStart) && file.getName().endsWith("." + extension)) {
					files.add(file);
				}
			}
		}
		return files.toArray(new File[0]);
	}

	/**
	 * Returns the files of a directory, whose names match a wildcard pattern (see
	 * {@link WildcardFilenameFilter}).
	 *
	 * @param startDirectory
	 *            the directory to search in
	 * @param fileFilterWithWildcards
	 *            the wildcard pattern for the file names, like "*.txt"
	 * @param traverseCompletely
	 *            true to also search all sub directories recursively
	 * @return the matching files, an empty list if the start directory is no directory
	 */
	public static List<File> getFilesWithWildcards(final File startDirectory, final String fileFilterWithWildcards, final boolean traverseCompletely) {
		final List<File> files = new ArrayList<>();
		if (startDirectory.isDirectory()) {
			final FileFilter fileFilter = new WildcardFilenameFilter(fileFilterWithWildcards);
			final File[] matchingFiles = startDirectory.listFiles(fileFilter);
			if (matchingFiles != null) {
				for (final File file : matchingFiles) {
					if (file.isFile()) {
						files.add(file);
					}
				}
			}

			if (traverseCompletely) {
				final File[] subFiles = startDirectory.listFiles();
				if (subFiles != null) {
					for (final File file : subFiles) {
						if (isRealDirectory(file)) {
							files.addAll(getFilesWithWildcards(file, fileFilterWithWildcards, traverseCompletely));
						}
					}
				}
			}
		}
		return files;
	}

	/**
	 * Checks whether a text file contains a text. The encoding of the file is detected by its BOM, the
	 * default encoding is used for files without BOM.
	 *
	 * @param searchFile
	 *            the file to search in
	 * @param searchText
	 *            the text or regular expression to search for
	 * @param searchCaseSensitive
	 *            true to search case sensitive
	 * @param searchTextIsRegularExpression
	 *            true if the search text is a regular expression, false for a literal text
	 * @param defaultEncoding
	 *            the encoding used for files without BOM
	 * @return true, if the text was found
	 * @throws Exception
	 *             if the file cannot be read or the regular expression is invalid
	 */
	public static boolean fileSearchText(final File searchFile, final String searchText, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding) throws Exception {
		final List<String> searchTexts = new ArrayList<>();
		searchTexts.add(searchText);
		return fileSearchText(searchFile, searchTexts, searchCaseSensitive, searchTextIsRegularExpression, defaultEncoding);
	}

	/**
	 * Checks whether a text file contains any of some texts. The encoding of the file is detected by its
	 * BOM, the default encoding is used for files without BOM.
	 *
	 * @param searchFile
	 *            the file to search in
	 * @param searchTexts
	 *            the texts or regular expressions to search for
	 * @param searchCaseSensitive
	 *            true to search case sensitive
	 * @param searchTextIsRegularExpression
	 *            true if the search texts are regular expressions (in MULTILINE mode, so "^" and "$" match at line
	 *            starts and ends), false for literal texts
	 * @param defaultEncoding
	 *            the encoding used for files without BOM
	 * @return true, if any of the texts was found, false also for an empty file or no search texts
	 * @throws Exception
	 *             if the file cannot be read or a regular expression is invalid
	 */
	public static boolean fileSearchText(final File searchFile, final List<String> searchTexts, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding) throws Exception {
		if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0) {
			final byte[] encodingBuffer = readFirstFileData(searchFile, 100);
			Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
			if (encodingData == null) {
				encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding.name());
			}

			final String dataString;
			try (FileInputStream fileInputStream = new FileInputStream(searchFile)) {
				dataString = Utilities.toString(fileInputStream, Charset.forName(encodingData.getFirst()));
			}

			for (int i = 0; i < searchTexts.size(); i++) {
				Pattern searchPattern;
				if (searchTextIsRegularExpression) {
					// MULTILINE like in fileReplaceText, so "^" and "$" match at line starts and ends in both methods
					if (searchCaseSensitive) {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE);
					} else {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
					}
				} else {
					if (searchCaseSensitive) {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.LITERAL);
					} else {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
					}
				}
				final Matcher matcher = searchPattern.matcher(dataString);
				if (matcher.find()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Replaces all occurrences of a text in a text file. The file is only rewritten if the text was
	 * found, its encoding (detected by its BOM, or the default encoding) is kept.
	 *
	 * @param searchFile
	 *            the file to change
	 * @param searchText
	 *            the text or regular expression to search for
	 * @param replacementText
	 *            the replacement text. For regular expressions it may contain group references like
	 *            "$1", for literal search texts it is used literally.
	 * @param searchCaseSensitive
	 *            true to search case sensitive
	 * @param searchTextIsRegularExpression
	 *            true if the search text is a regular expression, false for a literal text
	 * @param defaultEncoding
	 *            the encoding used for files without BOM
	 * @return true, if the text was found and replaced
	 * @throws Exception
	 *             if the file cannot be read or written or the regular expression is invalid
	 */
	public static boolean fileReplaceText(final File searchFile, final String searchText, final String replacementText, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding)
			throws Exception {
		final List<String> searchTexts = new ArrayList<>();
		searchTexts.add(searchText);
		final List<String> replacementTexts = new ArrayList<>();
		replacementTexts.add(replacementText);
		return fileReplaceText(searchFile, searchTexts, replacementTexts, searchCaseSensitive, searchTextIsRegularExpression, defaultEncoding);
	}

	/**
	 * Replaces all occurrences of some texts in a text file. The replacements are done one after
	 * another in list order. The file is only rewritten if any text was found, its encoding (detected
	 * by its BOM, or the default encoding) is kept.
	 *
	 * @param searchFile
	 *            the file to change
	 * @param searchTexts
	 *            the texts or regular expressions to search for
	 * @param replacementTexts
	 *            the replacement texts at the same list positions as their search texts. For regular
	 *            expressions they may contain group references like "$1", for literal search texts they
	 *            are used literally.
	 * @param searchCaseSensitive
	 *            true to search case sensitive
	 * @param searchTextIsRegularExpression
	 *            true if the search texts are regular expressions, false for literal texts
	 * @param defaultEncoding
	 *            the encoding used for files without BOM
	 * @return true, if any text was found and replaced
	 * @throws Exception
	 *             if the file does not exist, cannot be read or written or a regular expression is
	 *             invalid
	 */
	public static boolean fileReplaceText(final File searchFile, final List<String> searchTexts, final List<String> replacementTexts, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding) throws Exception {
		if (!searchFile.exists()) {
			throw new FileNotFoundException("File not found: " + searchFile.getAbsolutePath());
		} else if (searchTexts != null && replacementTexts != null && searchTexts.size() != replacementTexts.size()) {
			throw new IllegalArgumentException("Number of search texts (" + searchTexts.size() + ") and replacement texts (" + replacementTexts.size() + ") differ");
		}
		boolean foundAndReplaced = false;
		if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0 && replacementTexts != null && replacementTexts.size() > 0) {
			final byte[] encodingBuffer = readFirstFileData(searchFile, 100);
			Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
			if (encodingData == null) {
				encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding.name());
			}

			String dataString;
			// The file must be closed before it is overwritten
			try (FileInputStream fileInputStream = new FileInputStream(searchFile)) {
				dataString = Utilities.toString(fileInputStream, Charset.forName(encodingData.getFirst()));
			}

			for (int i = 0; i < searchTexts.size(); i++) {
				Pattern searchPattern;
				String replacementText = replacementTexts.get(i);
				if (searchTextIsRegularExpression) {
					if (searchCaseSensitive) {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE);
					} else {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
					}
				} else {
					if (searchCaseSensitive) {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.LITERAL);
					} else {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
					}
					// A literal replacement must not interpret "$" (group reference) and "\\"
					replacementText = Matcher.quoteReplacement(replacementText);
				}
				final Matcher matcher = searchPattern.matcher(dataString);
				if (matcher.find()) {
					dataString = matcher.replaceAll(replacementText);
					foundAndReplaced = true;
				}
			}

			if (foundAndReplaced) {
				FileUtilities.writeStringToFile(searchFile, dataString, Charset.forName(encodingData.getFirst()));
			}
		}
		return foundAndReplaced;
	}

	/**
	 * Reads the first bytes of a file.
	 *
	 * @param dataFile
	 *            the file to read
	 * @param maxAmountToRead
	 *            the maximum number of bytes to read
	 * @return the bytes read, fewer than requested if the file is smaller
	 * @throws Exception
	 *             if the file cannot be read
	 */
	public static byte[] readFirstFileData(final File dataFile, final int maxAmountToRead) throws Exception {
		try (FileInputStream input = new FileInputStream(dataFile)) {
			// A single read may return fewer bytes than available
			return input.readNBytes(maxAmountToRead);
		}
	}

	/**
	 * Returns the files matching a semicolon separated list of basenames, see
	 * {@link #getArrayOfFilesByBasename(List)}.
	 *
	 * @param basenameList
	 *            semicolon separated list of file paths, each may end with "*"
	 * @return the matching files without duplicates
	 */
	public static ArrayList<File> getArrayOfFilesByStammname(final String basenameList) {
		return getArrayOfFilesByBasename(Arrays.asList(basenameList.split(";")));
	}

	/**
	 * Returns the files matching a list of basenames. A basename is a file path, whose file name part
	 * may end with "*" to match all files starting with the text before it. A basename without directory
	 * refers to the current working directory.
	 *
	 * @param basenameList
	 *            the file paths, each may end with "*"
	 * @return the matching files without duplicates
	 */
	public static ArrayList<File> getArrayOfFilesByBasename(final List<String> basenameList) {
		final ArrayList<File> resultList = new ArrayList<>();

		for (final String basename : basenameList) {
			final int unixLastIndexOfSeparator = basename.lastIndexOf("/");
			final int dosLastIndexOfSeparator = basename.lastIndexOf("\\");
			final int lastIndexOfSeparator = Math.max(unixLastIndexOfSeparator, dosLastIndexOfSeparator);
			final String suchVerzeichnis = basename.substring(0, lastIndexOfSeparator + 1);
			final String stammFileName = basename.substring(lastIndexOfSeparator + 1);

			// A basename without directory refers to the current directory, new File("") is no directory
			final File searchPath = new File(suchVerzeichnis.isEmpty() ? "." : suchVerzeichnis);
			final File[] partList = searchPath.isDirectory() ? searchPath.listFiles() : null;
			if (partList != null) {
				for (int i = 0; i < partList.length; i++) {
					if (partList[i].isFile() && (stammFileName.endsWith("*") && partList[i].getName().startsWith(stammFileName.substring(0, stammFileName.length() - 1))
							|| !stammFileName.endsWith("*") && partList[i].getName().equals(stammFileName))) {
						if (!resultList.contains(partList[i])) {
							resultList.add(partList[i]);
						}
					}
				}
			}
		}

		return resultList;
	}

	/**
	 * Returns the files of a directory, whose names completely match a regular expression.
	 *
	 * @param startDirectory
	 *            the directory to search in
	 * @param patternString
	 *            the regular expression for the file names
	 * @param traverseCompletely
	 *            true to also search all sub directories recursively
	 * @return the matching files, an empty list if the start directory is no directory
	 */
	public static List<File> getFilesByPattern(final File startDirectory, final String patternString, final boolean traverseCompletely) {
		return getFilesByPattern(startDirectory, Pattern.compile(patternString), traverseCompletely);
	}

	/**
	 * Returns the files of a directory, whose names completely match a regular expression.
	 *
	 * @param startDirectory
	 *            the directory to search in
	 * @param pattern
	 *            the regular expression for the file names
	 * @param traverseCompletely
	 *            true to also search all sub directories recursively
	 * @return the matching files, an empty list if the start directory is no directory
	 */
	public static List<File> getFilesByPattern(final File startDirectory, final Pattern pattern, final boolean traverseCompletely) {
		final List<File> files = new ArrayList<>();
		final File[] subFiles = startDirectory.isDirectory() ? startDirectory.listFiles() : null;
		if (subFiles != null) {
			for (final File file : subFiles) {
				if (file.isDirectory()) {
					if (traverseCompletely && isRealDirectory(file)) {
						files.addAll(getFilesByPattern(file, pattern, traverseCompletely));
					}
				} else if (file.isFile() && pattern.matcher(file.getName()).matches()) {
					files.add(file);
				}
			}
		}
		return files;
	}

	/**
	 * Returns all files (not directories) within a directory tree. Symbolic links to directories are not followed.
	 *
	 * @param directory
	 *            the root directory
	 * @return the files of the directory tree, empty if the directory cannot be read
	 * @throws Exception
	 *             if reading the directory tree fails
	 */
	public static List<File> getAllSubfiles(final File directory) throws Exception {
		try {
			final List<File> files = new LinkedList<>();
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						if (isRealDirectory(file)) {
							files.addAll(getAllSubfiles(file));
						}
					} else {
						files.add(file);
					}
				}
			}
			return files;
		} catch (final Exception e) {
			throw new Exception("Error reading subfiles of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Returns the number of all files (not directories) within a directory tree. Symbolic links to directories are not followed.
	 *
	 * @param directory
	 *            the root directory
	 * @return the number of files of the directory tree
	 * @throws Exception
	 *             if reading the directory tree fails
	 */
	public static int getAllSubfilesNumber(final File directory) throws Exception {
		try {
			int fileCount = 0;
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						if (isRealDirectory(file)) {
							fileCount += getAllSubfilesNumber(file);
						}
					} else {
						fileCount++;
					}
				}
			}
			return fileCount;
		} catch (final Exception e) {
			throw new Exception("Error reading subfilenumber of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Checks whether a file is a directory and no symbolic link to one. The recursive methods of this class do not
	 * descend into symbolic links (like "find" by default), because a link to a parent directory would cause an endless
	 * recursion, and files reachable via several paths would be counted repeatedly.
	 */
	private static boolean isRealDirectory(final File file) {
		return file.isDirectory() && !Files.isSymbolicLink(file.toPath());
	}

	/**
	 * Returns the size of all files within a directory tree. Symbolic links to directories are not followed.
	 *
	 * @param directory
	 *            the root directory
	 * @return the sum of all file sizes in bytes
	 * @throws Exception
	 *             if reading the directory tree fails
	 */
	public static long getAllSubfilesSize(final File directory) throws Exception {
		try {
			long fileSizeSum = 0;
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						if (isRealDirectory(file)) {
							fileSizeSum += getAllSubfilesSize(file);
						}
					} else {
						fileSizeSum += file.length();
					}
				}
			}
			return fileSizeSum;
		} catch (final Exception e) {
			throw new Exception("Error reading subfilesize of: " + directory.getAbsolutePath(), e);
		}
	}

	/**
	 * Returns the number of lines of a text file. For a file with extension ".zip" the lines of the first
	 * zip entry are counted. The data is decoded with the platform default charset.
	 *
	 * @param file
	 *            the text or zip file
	 * @return the number of lines
	 * @throws IOException
	 *             if reading fails
	 */
	public static int getLineCount(final File file) throws IOException {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			if (Utilities.endsWithIgnoreCase(file.getName(), ".zip")) {
				inputStream = new ZipInputStream(inputStream);
				((ZipInputStream) inputStream).getNextEntry();
			}

			try (LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(inputStream))) {
				while (lineNumberReader.readLine() != null) {
					// do nothing
				}

				return lineNumberReader.getLineNumber();
			}
		} finally {
			Utilities.closeQuietly(inputStream);
		}
	}

	/**
	 * Writes data to a file, replacing its previous content.
	 *
	 * @param file
	 *            the file to write
	 * @param data
	 *            the data to write
	 * @throws Exception
	 *             if writing fails
	 */
	public static void write(final File file, final byte[] data) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(data);
		}
	}

	/**
	 * Writes a text to a file, replacing its previous content.
	 *
	 * @param file
	 *            the file to write
	 * @param dataString
	 *            the text to write
	 * @param encoding
	 *            the encoding for the text
	 * @throws Exception
	 *             if writing fails
	 */
	public static void writeStringToFile(final File file, final String dataString, final Charset encoding) throws Exception {
		write(file, dataString.getBytes(encoding));
	}

	/**
	 * Reads the complete content of a file.
	 *
	 * @param dataFile
	 *            the file to read
	 * @return the file content
	 * @throws Exception
	 *             if reading fails
	 */
	public static byte[] readFileToByteArray(final File dataFile) throws Exception {
		ByteArrayOutputStream output = null;
		try (FileInputStream input = new FileInputStream(dataFile)) {
			output = new ByteArrayOutputStream();
			IoUtilities.copy(input, output);
			return output.toByteArray();
		} catch (final Exception e) {
			throw e;
		}
	}

	/**
	 * Reads the complete content of a text file.
	 *
	 * @param dataFile
	 *            the file to read
	 * @param encoding
	 *            the encoding of the file
	 * @return the decoded file content
	 * @throws Exception
	 *             if reading fails
	 */
	public static String readFileToString(final File dataFile, final Charset encoding) throws Exception {
		return new String(readFileToByteArray(dataFile), encoding);
	}

	/**
	 * Reads the lines of a text file, see {@link #readLinesFromStream(InputStream, boolean, Charset)}.
	 *
	 * @param textFile
	 *            the file to read
	 * @param keepLinebreakInLines
	 *            true to keep the line breaks at the end of the lines
	 * @param encoding
	 *            the encoding of the file, which must be ASCII compatible (no UTF-16 or UTF-32)
	 * @return the lines of the file
	 * @throws Exception
	 *             if reading fails
	 */
	public static List<String> readLinesFromFile(final File textFile, final boolean keepLinebreakInLines, final Charset encoding) throws Exception {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(textFile))) {
			return readLinesFromStream(inputStream, keepLinebreakInLines, encoding);
		}
	}

	/**
	 * Reads the lines of a text stream. Line breaks may be LF, CRLF or CR. A trailing line break does not
	 * start another line.
	 *
	 * @param inputStream
	 *            the stream to read, it is not closed
	 * @param keepLinebreakInLines
	 *            true to keep the line breaks at the end of the lines
	 * @param encoding
	 *            the encoding of the data, which must be ASCII compatible (no UTF-16 or UTF-32), because
	 *            the lines are split on the byte values of CR and LF
	 * @return the lines of the data
	 * @throws Exception
	 *             if reading fails
	 */
	public static List<String> readLinesFromStream(final InputStream inputStream, final boolean keepLinebreakInLines, final Charset encoding) throws Exception {
		final List<String> dataLines = new ArrayList<>();
		final ByteArrayOutputStream readData = new ByteArrayOutputStream();
		int nextByte;
		int lastByte = 0;
		while ((nextByte = inputStream.read()) != EOF) {
			boolean isNewLine = false;
			if (nextByte == '\r') {
				// new linebreak (Mac style, possibly Windows style)
				if (keepLinebreakInLines) {
					readData.write(nextByte);
				}
				isNewLine = true;
			} else if (nextByte == '\n') {
				if (lastByte == '\r') {
					// second part of linebreak (Windows style), the line was already added with its "\r"
					if (keepLinebreakInLines) {
						dataLines.set(dataLines.size() - 1, dataLines.get(dataLines.size() - 1) + '\n');
					}
				} else {
					// new linebreak (Unix style)
					if (keepLinebreakInLines) {
						readData.write(nextByte);
					}
					isNewLine = true;
				}
			} else {
				readData.write(nextByte);
			}

			if (isNewLine) {
				dataLines.add(new String(readData.toByteArray(), encoding));
				readData.reset();
			}

			lastByte = nextByte;
		}

		if (readData.size() > 0) {
			dataLines.add(new String(readData.toByteArray(), encoding));
			readData.reset();
		}

		return dataLines;
	}

	/**
	 * Appends a text to a file. The file is created if it does not exist.
	 *
	 * @param fileToAppend
	 *            the file to append to
	 * @param textToAppend
	 *            the text to append
	 * @param encoding
	 *            the encoding for the text
	 * @throws Exception
	 *             if writing fails
	 */
	public static void append(final File fileToAppend, final String textToAppend, final Charset encoding) throws Exception {
		try (OutputStream os = new FileOutputStream(fileToAppend, true)) {
			os.write(textToAppend.getBytes(encoding));
		}
	}

	/**
	 * Appends data to a file. The file is created if it does not exist.
	 *
	 * @param file
	 *            the file to append to
	 * @param data
	 *            the data to append
	 * @throws Exception
	 *             if writing fails
	 */
	public static void append(final File file, final byte[] data) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file, true)) {
			out.write(data);
		}
	}

	/**
	 * Removes a single leading "/" of a path.
	 *
	 * @param path
	 *            the path, may be null
	 * @return the path without leading "/"
	 */
	public static String trimLeadingFileSeparator(final String path) {
		if (path != null && path.startsWith("/")) {
			return path.substring(1);
		} else {
			return path;
		}
	}

	/**
	 * Removes a single trailing "/" of a path.
	 *
	 * @param path
	 *            the path, may be null
	 * @return the path without trailing "/"
	 */
	public static String trimTrailingFileSeparator(final String path) {
		if (path != null && path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		} else {
			return path;
		}
	}

	/**
	 * Returns the last part of a "/" separated path.
	 *
	 * @param path
	 *            the path, may be null
	 * @return the part after the last "/", the path itself if it contains no "/"
	 */
	public static String getFilenameFromPath(final String path) {
		if (path != null && path.contains("/")) {
			return path.substring(path.lastIndexOf("/") + 1);
		} else {
			return path;
		}
	}

	/**
	 * Returns the parent of a "/" separated path.
	 *
	 * @param path
	 *            the path, may be null
	 * @return the part before the last "/", "/" for a path directly within the root directory, or an
	 *         empty text if the path contains no "/"
	 */
	public static String getParentFromPath(final String path) {
		if (path != null && path.contains("/")) {
			final String returnPath = path.substring(0, path.lastIndexOf("/"));
			if (Utilities.isEmpty(returnPath) && path.startsWith("/")) {
				return "/";
			} else {
				return returnPath;
			}
		} else {
			if (path != null && path.startsWith("/")) {
				return "/";
			} else {
				return "";
			}
		}
	}

	/**
	 * Deletes a file, or a directory and all of its content recursively. Symbolic links are deleted
	 * themselves, the link targets are never followed.
	 *
	 * @param file
	 *            the file or directory to delete
	 * @throws IOException
	 *             if any file or directory cannot be deleted
	 */
	public static void delete(final File file) throws IOException {
		// Symbolic links are deleted themselves, but never followed. Otherwise the content of the
		// link target outside of the deleted directory tree would be deleted, too.
		if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
			final File[] entries = file.listFiles();
			if (entries != null) {
				for (final File entry : entries) {
					delete(entry);
				}
			}
		}
		if (!file.delete()) {
			throw new IOException("Failed to delete " + file);
		}
	}

	/**
	 * Removes the content of all lines starting with "#". The empty lines remain.
	 *
	 * @param propertiesTextData
	 *            the properties text
	 * @return the text without comment lines
	 */
	public static String removePropertiesCommentLines(final String propertiesTextData) {
		return propertiesTextData.replaceAll("(?m)^#.*", "");
	}

	/**
	 * Checks whether a text is a syntactically valid file path for the current file system. The file
	 * does not need to exist.
	 *
	 * @param path
	 *            the path to check, may be null
	 * @return true, if the path is valid, false for null or blank paths
	 */
	public static boolean isValidFilePath(final String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		try {
			Paths.get(path);
		} catch (@SuppressWarnings("unused") InvalidPathException | NullPointerException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Detects the data format and compression of a file by its file extensions, like "data.json.zip".
	 *
	 * @param filePath
	 *            the file path or name
	 * @return the file type, whose data format or compression is null if unknown
	 */
	public static FileType getFileTypeByExtension(final String filePath) {
		final FileType fileType = new FileType();

		String filePathWithoutCompressionPart;

		if (Utilities.endsWithIgnoreCase(filePath, ".zip")) {
			fileType.setFileCompressionType(FileCompressionType.ZIP);
			filePathWithoutCompressionPart = filePath.substring(0, filePath.length() - 4);
		} else if (Utilities.endsWithIgnoreCase(filePath, ".tar.gz")) {
			fileType.setFileCompressionType(FileCompressionType.TARGZ);
			filePathWithoutCompressionPart = filePath.substring(0, filePath.length() - 7);
		} else if (Utilities.endsWithIgnoreCase(filePath, ".tgz")) {
			fileType.setFileCompressionType(FileCompressionType.TGZ);
			filePathWithoutCompressionPart = filePath.substring(0, filePath.length() - 4);
		} else if (Utilities.endsWithIgnoreCase(filePath, ".gz")) {
			fileType.setFileCompressionType(FileCompressionType.GZ);
			filePathWithoutCompressionPart = filePath.substring(0, filePath.length() - 3);
		} else {
			fileType.setFileCompressionType(null);
			filePathWithoutCompressionPart = filePath;
		}

		if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".csv")
			|| Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".del")) {
			fileType.setFileDataType(FileDataType.CSV);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".xls") || Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".xlsx")) {
			fileType.setFileDataType(FileDataType.EXCEL);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".jsn") || Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".json")) {
			fileType.setFileDataType(FileDataType.JSON);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".yml") || Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".yaml")) {
			fileType.setFileDataType(FileDataType.YAML);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".kdbx")) {
			fileType.setFileDataType(FileDataType.KDBX);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".ods")) {
			fileType.setFileDataType(FileDataType.ODS);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".sql")) {
			fileType.setFileDataType(FileDataType.SQL);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".vcf")) {
			fileType.setFileDataType(FileDataType.VCF);
		} else if (Utilities.endsWithIgnoreCase(filePathWithoutCompressionPart, ".xml")) {
			fileType.setFileDataType(FileDataType.XML);
		} else {
			fileType.setFileDataType(null);
		}

		return fileType;
	}

	/**
	 * Detects the charset of a text file. Files starting with a UTF-8, UTF-16 or UTF-32 BOM get the
	 * matching charset. Otherwise the first 4 KB are searched for German umlauts encoded with each of the given
	 * charsets in order.
	 *
	 * @param dataFile
	 *            the file to check
	 * @param charsets
	 *            the candidate charsets in order of preference
	 * @return the detected charset, or null if the file is empty, has another BOM or none of the
	 *         charsets was detected
	 * @throws FileNotFoundException
	 *             if the file does not exist
	 * @throws IOException
	 *             if reading fails
	 */
	public static Charset detectFileCharset(final File dataFile, final Charset... charsets) throws FileNotFoundException, IOException {
		try (BOMInputStream fileInputStream = new BOMInputStream(new FileInputStream(dataFile))) {
			final byte[] firstBytes = fileInputStream.readNBytes(4096);
			final int readBytesLength = firstBytes.length;
			if (readBytesLength <= 0) {
				return null;
			} else if (fileInputStream.getBOM() != null && fileInputStream.getBOM() != BOM.NONE) {
				if (fileInputStream.getBOM() == BOM.UTF_8) {
					return StandardCharsets.UTF_8;
				} else if (fileInputStream.getBOM() == BOM.UTF_16_BE) {
					return StandardCharsets.UTF_16BE;
				} else if (fileInputStream.getBOM() == BOM.UTF_16_LE) {
					return StandardCharsets.UTF_16LE;
				} else if (fileInputStream.getBOM() == BOM.UTF_32_BE) {
					return Charset.forName("UTF-32BE");
				} else if (fileInputStream.getBOM() == BOM.UTF_32_LE) {
					return Charset.forName("UTF-32LE");
				} else {
					return null;
				}
			} else {
				for (final Charset charset : charsets) {
					// String.getBytes returns exactly the encoded bytes, while the backing array of
					// Charset.encode() may contain additional unused bytes (e.g. "C3 A4 00" for UTF-8 "ä")
					if (Utilities.arrayContains(firstBytes, "ä".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "ö".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "ü".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "Ä".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "Ö".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "Ü".getBytes(charset))
							|| Utilities.arrayContains(firstBytes, "ß".getBytes(charset))) {
						return charset;
					}
				}
				return null;
			}
		}
	}

	/**
	 * Returns a part of a byte array. Indexes out of range are limited to the array bounds.
	 *
	 * @param array
	 *            the array, may be null
	 * @param startIndexInclusive
	 *            the start index (inclusive)
	 * @param endIndexExclusive
	 *            the end index (exclusive)
	 * @return the part of the array, an empty array for an empty range, or null if the array is null
	 */
	public static byte[] subarray(final byte[] array, int startIndexInclusive, int endIndexExclusive) {
		if (array == null) {
			return null;
		} else {
			if (startIndexInclusive < 0) {
				startIndexInclusive = 0;
			}
			if (endIndexExclusive > array.length) {
				endIndexExclusive = array.length;
			}
			final int newSize = endIndexExclusive - startIndexInclusive;
			if (newSize <= 0) {
				return new byte[0];
			} else {
				final byte[] subarray = new byte[newSize];
				System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
				return subarray;
			}
		}
	}
}
