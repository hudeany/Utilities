package de.soderer.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;

public class FileUtilities {
	/**
	 * Delete all subfiles fitting namestart and extension
	 * 
	 * @param directory
	 * @param nameStart
	 * @param extension
	 */
	public static void deleteSubfilesByNameAndExtension(File directory, String nameStart, String extension) {
		for (File file : getSubfilesByNameAndExtension(directory, nameStart, extension)) {
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
	public static File[] getSubfilesByNameAndExtension(File directory, String nameStart, String extension) {
		List<File> files = new LinkedList<File>();
		for (File file : directory.listFiles()) {
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
	public static List<File> getFilesWithWildcards(File startDirectory, String fileFilterWithWildcards, boolean traverseCompletely) {
		List<File> files = new ArrayList<File>();
		if (startDirectory.isDirectory()) {
			FileFilter fileFilter = new WildcardFileFilter(fileFilterWithWildcards);
			for (File file : startDirectory.listFiles(fileFilter)) {
				if (file.isFile()) {
					files.add(file);
				}
			}

			if (traverseCompletely) {
				for (File file : startDirectory.listFiles()) {
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
	public static boolean fileSearchText(File searchFile, String searchText, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		List<String> searchTexts = new ArrayList<String>();
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
	public static boolean fileSearchText(File searchFile, List<String> searchTexts, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		FileInputStream fileInputStream = null;
		try {
			if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0) {
				byte[] encodingBuffer = readFirstFileData(searchFile, 100);
				Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
				if (encodingData == null) {
					encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding);
				}

				fileInputStream = new FileInputStream(searchFile);
				String dataString = Utilities.toString(fileInputStream, encodingData.getFirst());

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
					Matcher matcher = searchPattern.matcher(dataString);
					if (matcher.find()) {
						return true;
					}
				}
			}
			return false;
		} finally {
			Utilities.closeQuietly(fileInputStream);
		}
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
	public static boolean fileReplaceText(File searchFile, String searchText, String replacementText, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		List<String> searchTexts = new ArrayList<String>();
		searchTexts.add(searchText);
		List<String> replacementTexts = new ArrayList<String>();
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
	public static boolean fileReplaceText(File searchFile, List<String> searchTexts, List<String> replacementTexts, boolean searchCaseSensitive, boolean searchTextIsRegularExpression, String defaultEncoding) throws Exception {
		FileInputStream fileInputStream = null;
		try {
			boolean foundAndReplaced = false;
			if (searchFile.length() > 0 && searchTexts != null && searchTexts.size() > 0 && replacementTexts != null && replacementTexts.size() > 0) {
				byte[] encodingBuffer = readFirstFileData(searchFile, 100);
				Tuple<String, Boolean> encodingData = TextUtilities.detectEncoding(encodingBuffer);
				if (encodingData == null) {
					encodingData = TextFilePropertiesReader.getEncodingData(defaultEncoding);
				}

				fileInputStream = new FileInputStream(searchFile);
				String dataString = Utilities.toString(fileInputStream, encodingData.getFirst());

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
					Matcher matcher = searchPattern.matcher(dataString);
					if (matcher.find()) {
						dataString = matcher.replaceAll(replacementTexts.get(i));
						foundAndReplaced = true;
					}
				}

				if (foundAndReplaced) {
					FileUtils.writeStringToFile(searchFile, dataString, encodingData.getFirst());
				}
			}
			return foundAndReplaced;
		} finally {
			Utilities.closeQuietly(fileInputStream);
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
	public static byte[] readFirstFileData(File dataFile, int maxAmountToRead) throws Exception {
		FileInputStream input = null;
		try {
			input = new FileInputStream(dataFile);
			byte[] encodingBufferInterim = new byte[maxAmountToRead];
			int bytesRead = input.read(encodingBufferInterim);
			byte[] encodingBuffer = ArrayUtils.subarray(encodingBufferInterim, 0, bytesRead);
			return encodingBuffer;
		} catch (Exception e) {
			throw e;
		} finally {
			Utilities.closeQuietly(input);
		}
	}
}
