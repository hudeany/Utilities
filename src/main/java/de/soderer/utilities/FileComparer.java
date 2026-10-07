package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Utilities to compare files and texts and to create a line based diff of them.
 */
public class FileComparer {
	/**
	 * Value returned by read operations at the end of data.
	 */
	public static final int EOF = -1;

	/**
	 * Utility class
	 */
	private FileComparer() {
		// Utility class
	}

	/**
	 * Selects the lines contained in a generated diff.
	 */
	public enum OutputMode {
		/** Output all lines */
		All,
		/** Output only lines contained in one of the sources */
		DifferencesOnly,
		/** Output only lines contained in both sources */
		CommonalitiesOnly
	}

	/**
	 * Prefix of lines only contained in the first source
	 */
	public static final String SOURCE1_SIGN = "-";
	/**
	 * Prefix of lines only contained in the second source
	 */
	public static final String SOURCE2_SIGN = "+";

	/**
	 * Compares two files byte by byte.
	 *
	 * @param file1
	 *            the first file
	 * @param file2
	 *            the second file
	 * @return true, if both files have exactly the same content
	 * @throws IOException
	 *             if reading fails
	 */
	public static boolean compareExactly(final File file1, final File file2) throws IOException {
		if (file1.length() == file2.length()) {
			try (BufferedInputStream fileInputStream1 = new BufferedInputStream(new FileInputStream(file1));
					BufferedInputStream fileInputStream2 = new BufferedInputStream(new FileInputStream(file2))) {
				int buffer = 0;
				while ((buffer = fileInputStream1.read()) != EOF) {
					if (buffer != fileInputStream2.read()) {
						return false;
					}
				}
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * Creates a line based diff of two text files, containing all lines and comparing blocks of lines.
	 * The encodings of the files are detected automatically.
	 *
	 * @param file1
	 *            the first file
	 * @param file2
	 *            the second file
	 * @return the diff text
	 * @throws Exception
	 *             if a file does not exist or cannot be read
	 */
	public static String generateUnifiedDiff(final File file1, final File file2) throws Exception {
		return generateUnifiedDiff(file1, file2, false, true, OutputMode.All);
	}

	/**
	 * Creates a line based diff of two text files. The encodings of the files are detected
	 * automatically.
	 *
	 * @param file1
	 *            the first file
	 * @param file2
	 *            the second file
	 * @param ignoreLeadingAndTrailingWhitespaces
	 *            true to remove leading and trailing whitespace of each line before comparing
	 * @param blockSensitiveCompare
	 *            true to synchronize the texts at the largest common block of lines, false to
	 *            synchronize at the next equal line
	 * @param outputMode
	 *            the lines to output
	 * @return the diff text, starting with two header lines naming the files and their modification
	 *         dates
	 * @throws Exception
	 *             if a file does not exist or cannot be read
	 */
	public static String generateUnifiedDiff(final File file1, final File file2, final boolean ignoreLeadingAndTrailingWhitespaces, final boolean blockSensitiveCompare, final OutputMode outputMode) throws Exception {
		if (!file1.exists()) {
			throw new Exception("File \"" + file1.getAbsolutePath() + "\" does not exist");
		} else if (!file2.exists()) {
			throw new Exception("File \"" + file2.getAbsolutePath() + "\" does not exist");
		}

		final TextFilePropertiesReader fileReader1 = new TextFilePropertiesReader(file1);
		final String dataString1 = fileReader1.readFileToString();

		final TextFilePropertiesReader fileReader2 = new TextFilePropertiesReader(file2);
		final String dataString2 = fileReader2.readFileToString();

		final StringBuilder result = new StringBuilder();
		result.append(generateUnifiedDiff(file1.getAbsolutePath() + " " + DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, DateUtilities.getLocalDateTime(file1.lastModified())), dataString1,
				file2.getAbsolutePath() + " " + DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, DateUtilities.getLocalDateTime(file2.lastModified())), dataString2, ignoreLeadingAndTrailingWhitespaces, blockSensitiveCompare,
				outputMode));

		return result.toString();
	}

	/**
	 * Creates a line based diff of two texts. Lines contained in both texts are prefixed with a blank,
	 * lines only contained in the first text with {@value #SOURCE1_SIGN} and lines only contained in the
	 * second text with {@value #SOURCE2_SIGN}. The output is a simplified unified diff format without
	 * hunk headers.
	 *
	 * @param sourceName1
	 *            the name of the first text for the header line
	 * @param dataString1
	 *            the first text
	 * @param sourceName2
	 *            the name of the second text for the header line
	 * @param dataString2
	 *            the second text
	 * @param ignoreLeadingAndTrailingWhitespaces
	 *            true to remove leading and trailing whitespace of each line before comparing
	 * @param blockSensitiveCompare
	 *            true to synchronize the texts at the largest common block of lines, false to
	 *            synchronize at the next equal line
	 * @param outputMode
	 *            the lines to output
	 * @return the diff text, starting with two header lines naming the sources
	 * @throws Exception
	 *             if a text is null
	 */
	public static String generateUnifiedDiff(final String sourceName1, String dataString1, final String sourceName2, String dataString2, final boolean ignoreLeadingAndTrailingWhitespaces, final boolean blockSensitiveCompare,
			final OutputMode outputMode) throws Exception {
		if (dataString1 == null) {
			throw new Exception("Datastring 1 is invalid");
		} else if (dataString2 == null) {
			throw new Exception("Datastring 2 is invalid");
		}

		if (ignoreLeadingAndTrailingWhitespaces) {
			dataString1 = TextUtilities.removeLeadingAndTrailingWhitespaces(dataString1);
			dataString2 = TextUtilities.removeLeadingAndTrailingWhitespaces(dataString2);
		}

		final List<String> lines1 = TextUtilities.getLines(dataString1);
		int position1 = 0;
		final List<String> lines2 = TextUtilities.getLines(dataString2);
		int position2 = 0;

		final StringBuilder result = new StringBuilder();
		result.append(Utilities.repeat(SOURCE1_SIGN, 3) + " " + sourceName1 + "\n");
		result.append(Utilities.repeat(SOURCE2_SIGN, 3) + " " + sourceName2 + "\n");

		while (position1 < lines1.size() && position2 < lines2.size()) {
			if (lines1.get(position1).equals(lines2.get(position2))) {
				if (outputMode == OutputMode.All || outputMode == OutputMode.CommonalitiesOnly) {
					result.append(" ");
					result.append(lines1.get(position1));
					result.append("\n");
				}
				position1++;
				position2++;
			} else {
				Tuple<Integer, Integer> nextEquationPosition;
				if (blockSensitiveCompare) {
					nextEquationPosition = findNextEquationByBlock(position1, lines1, position2, lines2);
				} else {
					nextEquationPosition = findNextEquationStraigth(position1, lines1, position2, lines2);
				}
				if (nextEquationPosition == null) {
					// no more equations
					break;
				} else {
					// print differences up to next equation
					while (position1 < nextEquationPosition.getFirst()) {
						if (outputMode == OutputMode.All || outputMode == OutputMode.DifferencesOnly) {
							result.append(SOURCE1_SIGN);
							result.append(lines1.get(position1));
							result.append("\n");
						}
						position1++;
					}
					while (position2 < nextEquationPosition.getSecond()) {
						if (outputMode == OutputMode.All || outputMode == OutputMode.DifferencesOnly) {
							result.append(SOURCE2_SIGN);
							result.append(lines2.get(position2));
							result.append("\n");
						}
						position2++;
					}
				}
			}
		}

		// add file tails not contained in other file
		while (position1 < lines1.size()) {
			if (outputMode == OutputMode.All || outputMode == OutputMode.DifferencesOnly) {
				result.append(SOURCE1_SIGN);
				result.append(lines1.get(position1));
				result.append("\n");
			}
			position1++;
		}
		while (position2 < lines2.size()) {
			if (outputMode == OutputMode.All || outputMode == OutputMode.DifferencesOnly) {
				result.append(SOURCE2_SIGN);
				result.append(lines2.get(position2));
				result.append("\n");
			}
			position2++;
		}

		return result.toString();
	}

	/**
	 * Finds the nearest pair of equal items in two lists, starting at the given positions.
	 *
	 * @param <T>
	 *            the type of the list items
	 * @param startPosition1
	 *            the start index in the first list
	 * @param list1
	 *            the first list
	 * @param startPosition2
	 *            the start index in the second list
	 * @param list2
	 *            the second list
	 * @return the indexes of the equal items in the first and second list, or null if there are no
	 *         more equal items
	 */
	public static <T> Tuple<Integer, Integer> findNextEquationStraigth(final int startPosition1, final List<T> list1, final int startPosition2, final List<T> list2) {
		Tuple<Integer, Integer> nextEquationPosition1 = null;
		Tuple<Integer, Integer> nextEquationPosition2 = null;

		for (int position1 = startPosition1; position1 < list1.size() && nextEquationPosition1 == null; position1++) {
			for (int position2 = startPosition2; position2 < list2.size() && nextEquationPosition1 == null; position2++) {
				if (list1.get(position1).equals(list2.get(position2))) {
					nextEquationPosition1 = new Tuple<>(position1, position2);
				}
			}
		}

		for (int position2 = startPosition2; position2 < list2.size() && nextEquationPosition2 == null; position2++) {
			for (int position1 = startPosition1; position1 < list1.size() && nextEquationPosition2 == null; position1++) {
				if (list2.get(position2).equals(list1.get(position1))) {
					nextEquationPosition2 = new Tuple<>(position1, position2);
				}
			}
		}

		if (nextEquationPosition1 != null && nextEquationPosition2 != null) {
			if (nextEquationPosition1.getFirst() + nextEquationPosition1.getSecond() <= nextEquationPosition2.getFirst() + nextEquationPosition2.getSecond()) {
				return nextEquationPosition1;
			} else {
				return nextEquationPosition2;
			}
		} else if (nextEquationPosition1 != null) {
			return nextEquationPosition1;
		} else {
			return nextEquationPosition2;
		}
	}

	/**
	 * Finds the next pair of equal items in two lists, which starts the largest common block of
	 * consecutive items. Blocks before that block are searched recursively, so the result is the first
	 * item pair of a common block before any larger block.
	 *
	 * @param <T>
	 *            the type of the list items
	 * @param startPosition1
	 *            the start index in the first list
	 * @param list1
	 *            the first list
	 * @param startPosition2
	 *            the start index in the second list
	 * @param list2
	 *            the second list
	 * @return the indexes of the equal items in the first and second list, or null if there are no
	 *         more equal items
	 */
	public static <T> Tuple<Integer, Integer> findNextEquationByBlock(final int startPosition1, final List<T> list1, final int startPosition2, final List<T> list2) {
		int endPosition1 = list1.size() - 1;
		int endPosition2 = list2.size() - 1;

		while (true) {
			final Tuple<Integer, Integer> equationBlockPosition = findLargestBlockEquation(startPosition1, endPosition1, list1, startPosition2, endPosition2, list2);
			if (equationBlockPosition != null && (equationBlockPosition.getFirst() < endPosition1 || equationBlockPosition.getSecond() < endPosition2)) {
				endPosition1 = equationBlockPosition.getFirst();
				endPosition2 = equationBlockPosition.getSecond();
			} else {
				return equationBlockPosition;
			}
		}
	}

	/**
	 * Finds the start of the largest block of consecutive equal items in two list ranges. Of several
	 * blocks with the same size the first one found is returned.
	 *
	 * @param <T>
	 *            the type of the list items
	 * @param startPosition1
	 *            the start index in the first list
	 * @param endPosition1
	 *            the inclusive end index in the first list, a negative or too large value means the
	 *            last item
	 * @param list1
	 *            the first list
	 * @param startPosition2
	 *            the start index in the second list
	 * @param endPosition2
	 *            the inclusive end index in the second list, a negative or too large value means the
	 *            last item
	 * @param list2
	 *            the second list
	 * @return the indexes of the first items of the largest block in the first and second list, or
	 *         null if there are no equal items in the ranges
	 */
	public static <T> Tuple<Integer, Integer> findLargestBlockEquation(final int startPosition1, int endPosition1, final List<T> list1, final int startPosition2, int endPosition2, final List<T> list2) {
		if (endPosition1 < 0 || endPosition1 >= list1.size()) {
			endPosition1 = list1.size() - 1;
		}
		if (endPosition2 < 0 || endPosition2 >= list2.size()) {
			endPosition2 = list2.size() - 1;
		}
		Tuple<Integer, Integer> largestEquationPosition = null;
		int largestEquationLength = 0;

		for (int position1 = startPosition1; position1 <= endPosition1; position1++) {
			for (int position2 = startPosition2; position2 <= endPosition2; position2++) {
				if (list1.get(position1).equals(list2.get(position2))) {
					// found an equations start, so check for maxLength
					int equationLength = 1;
					while (position1 + equationLength <= endPosition1 && position2 + equationLength <= endPosition2) {
						if (list1.get(position1 + equationLength).equals(list2.get(position2 + equationLength))) {
							equationLength++;
						} else {
							break;
						}
					}

					if (largestEquationLength < equationLength) {
						largestEquationLength = equationLength;
						largestEquationPosition = new Tuple<>(position1, position2);
					}
				}
			}
		}

		return largestEquationPosition;
	}
}
