package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;

import de.soderer.utilities.crypto.DirectoryHashingWorker;
import de.soderer.utilities.crypto.FileHashingWorker;

public class FileUtilities {
	/**
	 * Delete all subfiles fitting namestart and extension
	 *
	 * @param directory
	 * @param nameStart
	 * @param extension
	 */
	public static void deleteSubfilesByNameAndExtension(final File directory, final String nameStart, final String extension) {
		for (final File file : getSubfilesByNameAndExtension(directory, nameStart, extension)) {
			FileUtils.deleteQuietly(file);
		}
	}

	/**
	 * Read a directory and return all files fitting namestart and extension
	 *
	 * @param directory
	 * @param nameStart
	 * @param extension
	 * @return
	 */
	public static File[] getSubfilesByNameAndExtension(final File directory, final String nameStart, final String extension) {
		final List<File> files = new LinkedList<>();
		for (final File file : directory.listFiles()) {
			if (file.getName().startsWith(nameStart) && file.getName().endsWith("." + extension)) {
				files.add(file);
			}
		}
		return files.toArray(new File[0]);
	}

	/**
	 * Read a directory and return all files fitting to a wildcard pattern
	 *
	 * @param startDirectory
	 * @param fileFilterWithWildcards
	 * @param traverseCompletely
	 * @return
	 */
	public static List<File> getFilesWithWildcards(final File startDirectory, final String fileFilterWithWildcards, final boolean traverseCompletely) {
		final List<File> files = new ArrayList<>();
		if (startDirectory.isDirectory()) {
			final FileFilter fileFilter = new WildcardFileFilter(fileFilterWithWildcards);
			for (final File file : startDirectory.listFiles(fileFilter)) {
				if (file.isFile()) {
					files.add(file);
				}
			}

			if (traverseCompletely) {
				for (final File file : startDirectory.listFiles()) {
					if (file.isDirectory()) {
						files.addAll(getFilesWithWildcards(file, fileFilterWithWildcards, traverseCompletely));
					}
				}
			}
		}
		return files;
	}

	/**
	 * Search textfile for string
	 *
	 * @param searchFile
	 * @param searchText
	 * @param searchCaseSensitive
	 * @param searchTextIsRegularExpression
	 * @param defaultEncoding
	 * @return
	 * @throws Exception
	 */
	public static boolean fileSearchText(final File searchFile, final String searchText, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding) throws Exception {
		final List<String> searchTexts = new ArrayList<>();
		searchTexts.add(searchText);
		return fileSearchText(searchFile, searchTexts, searchCaseSensitive, searchTextIsRegularExpression, defaultEncoding);
	}

	/**
	 * Search textfile for string
	 *
	 * @param searchFile
	 * @param searchTexts
	 * @param searchCaseSensitive
	 * @param searchTextIsRegularExpression
	 * @param defaultEncoding
	 * @return
	 * @throws Exception
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
					if (searchCaseSensitive) {
						searchPattern = Pattern.compile(searchTexts.get(i));
					} else {
						searchPattern = Pattern.compile(searchTexts.get(i), Pattern.CASE_INSENSITIVE);
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
	 * Replace string in textfile
	 *
	 * @param searchFile
	 * @param searchText
	 * @param replacementText
	 * @param searchCaseSensitive
	 * @param searchTextIsRegularExpression
	 * @param defaultEncoding
	 * @return
	 * @throws Exception
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
	 * Replace strings in textfile
	 *
	 * @param searchFile
	 * @param searchTexts
	 * @param replacementTexts
	 * @param searchCaseSensitive
	 * @param searchTextIsRegularExpression
	 * @param defaultEncoding
	 * @return
	 * @throws Exception
	 */
	public static boolean fileReplaceText(final File searchFile, final List<String> searchTexts, final List<String> replacementTexts, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression, final Charset defaultEncoding) throws Exception {
		try (FileInputStream fileInputStream = new FileInputStream(searchFile)) {
			boolean foundAndReplaced = false;
			if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0 && replacementTexts != null && replacementTexts.size() > 0) {
				final byte[] encodingBuffer = readFirstFileData(searchFile, 100);
				Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
				if (encodingData == null) {
					encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding.name());
				}

				String dataString = Utilities.toString(fileInputStream, Charset.forName(encodingData.getFirst()));

				for (int i = 0; i < searchTexts.size(); i++) {
					Pattern searchPattern;
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
					}
					final Matcher matcher = searchPattern.matcher(dataString);
					if (matcher.find()) {
						dataString = matcher.replaceAll(replacementTexts.get(i));
						foundAndReplaced = true;
					}
				}

				if (foundAndReplaced) {
					FileUtilities.writeStringToFile(searchFile, dataString, Charset.forName(encodingData.getFirst()));
				}
			}
			return foundAndReplaced;
		}
	}

	/**
	 * Read first n bytes of binary file
	 *
	 * @param dataFile
	 * @param maxAmountToRead
	 * @return
	 * @throws Exception
	 */
	public static byte[] readFirstFileData(final File dataFile, final int maxAmountToRead) throws Exception {
		try (FileInputStream input = new FileInputStream(dataFile)) {
			final byte[] encodingBufferInterim = new byte[maxAmountToRead];
			final int bytesRead = input.read(encodingBufferInterim);
			final byte[] encodingBuffer = ArrayUtils.subarray(encodingBufferInterim, 0, bytesRead);
			return encodingBuffer;
		} catch (final Exception e) {
			throw e;
		}
	}

	/**
	 * Read a directory and return all files starting with a basename form a list of basenames
	 *
	 * @param basenameList
	 * @return
	 */
	public static ArrayList<File> getArrayOfFilesByStammname(final String basenameList) {
		return getArrayOfFilesByStammname(Arrays.asList(basenameList.split(";")));
	}

	/**
	 * Read a directory and return all files starting with a basename form a list of basenames
	 *
	 * @param stammNamenListe
	 * @return
	 */
	public static ArrayList<File> getArrayOfFilesByStammname(final List<String> stammNamenListe) {
		final ArrayList<File> ergebnisListe = new ArrayList<>();

		for (final String stammName : stammNamenListe) {
			// Suchverzeichnis und Dateistammname ermitteln
			// Achtung: stammName kann auch wildcards wie '*' enthalten daher
			// NICHT mit File-Class arbeiten
			final int unixLastIndexOfSeparator = stammName.lastIndexOf("/");
			final int dosLastIndexOfSeparator = stammName.lastIndexOf("\\");
			final int lastIndexOfSeparator = Math.max(unixLastIndexOfSeparator, dosLastIndexOfSeparator);
			final String suchVerzeichnis = stammName.substring(0, lastIndexOfSeparator + 1);
			final String stammFileName = stammName.substring(lastIndexOfSeparator + 1);

			// Dateiliste erstellen
			final File searchPath = new File(suchVerzeichnis);
			if (searchPath.isDirectory()) {
				final File[] partList = searchPath.listFiles();
				for (int i = 0; i < partList.length; i++) {
					if (partList[i].isFile() && (stammFileName.endsWith("*") && partList[i].getName().startsWith(stammFileName.substring(0, stammFileName.length() - 1))
							|| !stammFileName.endsWith("*") && partList[i].getName().equals(stammFileName))) {
						// Datei gefunden: partList[i].getName());
						if (!ergebnisListe.contains(partList[i])) {
							ergebnisListe.add(partList[i]);
						}
					}
				}
			}
		}

		return ergebnisListe;
	}

	/**
	 * Read a directory and return all files fitting to a regex pattern
	 *
	 * @param startDirectory
	 * @param patternString
	 * @param traverseCompletely
	 * @return
	 */
	public static List<File> getFilesByPattern(final File startDirectory, final String patternString, final boolean traverseCompletely) {
		return getFilesByPattern(startDirectory, Pattern.compile(patternString), traverseCompletely);
	}

	/**
	 * Read a directory and return all files fitting to a regex pattern
	 *
	 * @param startDirectory
	 * @param pattern
	 * @param traverseCompletely
	 * @return
	 */
	public static List<File> getFilesByPattern(final File startDirectory, final Pattern pattern, final boolean traverseCompletely) {
		final List<File> files = new ArrayList<>();
		if (startDirectory.isDirectory()) {
			for (final File file : startDirectory.listFiles()) {
				if (file.isDirectory() && traverseCompletely) {
					if (traverseCompletely) {
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
	 * Get all recursive subfiles of a directory
	 *
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static List<File> getAllSubfiles(final File directory) throws Exception {
		try {
			final List<File> files = new LinkedList<>();
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						files.addAll(getAllSubfiles(file));
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
	 * Get number of all recursive subfiles of a directory
	 *
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static int getAllSubfilesNumber(final File directory) throws Exception {
		try {
			int fileCount = 0;
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						fileCount += getAllSubfilesNumber(file);
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
	 * Get size in bytes of all files and all descending subdirectories of a directory
	 *
	 * @param directory
	 * @return
	 * @throws Exception
	 */
	public static long getAllSubfilesSize(final File directory) throws Exception {
		try {
			long fileSizeSum = 0;
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						fileSizeSum += getAllSubfilesSize(file);
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
	 * Get number of lines of a textfile
	 *
	 * @param file
	 * @return
	 * @throws IOException
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

	public static void write(final File file, final byte[] data) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(data);
		}
	}

	public static void writeStringToFile(final File file, final String dataString, final Charset encoding) throws Exception {
		write(file, dataString.getBytes(encoding));
	}

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

	public static String readFileToString(final File dataFile, final Charset encoding) throws Exception {
		return new String(readFileToByteArray(dataFile), encoding);
	}

	public static List<String> readLinesFromFile(final File textFile, final boolean keepLinebreakInLines, final Charset encoding) throws Exception {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(textFile))) {
			return readLinesFromStream(inputStream, keepLinebreakInLines, encoding);
		}
	}

	public static List<String> readLinesFromStream(final InputStream inputStream, final boolean keepLinebreakInLines, final Charset encoding) throws Exception {
		final List<String> dataLines = new ArrayList<>();
		final ByteArrayOutputStream readData = new ByteArrayOutputStream();
		int nextByte;
		int lastByte = 0;
		while ((nextByte = inputStream.read()) != -1) {
			boolean isNewLine = false;
			if (nextByte == '\r') {
				// new linebreak (Mac style, possibly Windows style)
				if (keepLinebreakInLines) {
					readData.write(nextByte);
				}
				isNewLine = true;
			} else if (nextByte == '\n') {
				if (lastByte == '\r') {
					// second part of linebreak (Windows style)
					// ignore but extend latest page end index
					if (keepLinebreakInLines) {
						dataLines.set(dataLines.size() - 1, dataLines.get(dataLines.size() - 1) + '\r');
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

	public static void append(final File fileToAppend, final String textToAppend, final Charset encoding) throws Exception {
		try (OutputStream os = new FileOutputStream(fileToAppend, true)) {
			os.write(textToAppend.getBytes(encoding));
		}
	}

	public static void append(final File file, final byte[] data) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file, true)) {
			out.write(data);
		}
	}

	public static String trimLeadingFileSeparator(final String path) {
		if (path != null && path.startsWith("/")) {
			return path.substring(1);
		} else {
			return path;
		}
	}

	public static String trimTrailingFileSeparator(final String path) {
		if (path != null && path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		} else {
			return path;
		}
	}

	public static String getFilenameFromPath(final String path) {
		if (path != null && path.contains("/")) {
			return path.substring(path.lastIndexOf("/") + 1);
		} else {
			return path;
		}
	}

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
	 * Delete directories and all of their files in a recursive way
	 * @throws IOException
	 */
	public static void delete(final File file) throws IOException {
		if (file.isDirectory()) {
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

	public static boolean directoriesAreEqual(final File file1, final File file2) throws Exception {
		if (file1 == null) {
			throw new Exception("First directory is missing");
		} else if (!file1.exists()) {
			throw new Exception("First directory does not exists: " + file1.getAbsolutePath());
		} else if (!file1.isDirectory()) {
			throw new Exception("First directory is not a directory: " + file1.getAbsolutePath());
		} else if (file2 == null) {
			throw new Exception("Second directory is missing");
		} else if (!file2.exists()) {
			throw new Exception("Second directory does not exists: " + file2.getAbsolutePath());
		} else if (!file2.isDirectory()) {
			throw new Exception("Second directory is not a directory: " + file2.getAbsolutePath());
		}

		final HashItem hash1 = new DirectoryHashingWorker(null, file1, HashAlgorithm.SHA_512).work();
		final HashItem hash2 = new DirectoryHashingWorker(null, file2, HashAlgorithm.SHA_512).work();

		return Arrays.equals(hash1.getHash(), hash2.getHash());
	}

	public static boolean filesAreEqual(final File file1, final File file2) throws Exception {
		if (file1 == null) {
			throw new Exception("First file is missing");
		} else if (!file1.exists()) {
			throw new Exception("First file does not exists: " + file1.getAbsolutePath());
		} else if (!file1.isFile()) {
			throw new Exception("First file is not a file: " + file1.getAbsolutePath());
		} else if (file2 == null) {
			throw new Exception("Second file is missing");
		} else if (!file2.exists()) {
			throw new Exception("Second file does not exists: " + file2.getAbsolutePath());
		} else if (!file2.isFile()) {
			throw new Exception("Second file is not a file: " + file2.getAbsolutePath());
		}

		final byte[] hash1 = new FileHashingWorker(null, file1, HashAlgorithm.SHA_512).work();
		final byte[] hash2 = new FileHashingWorker(null, file2, HashAlgorithm.SHA_512).work();

		return Arrays.equals(hash1, hash2);
	}
}
