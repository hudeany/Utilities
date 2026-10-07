package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.Utilities;

/**
 * Counts the blank lines (containing only whitespace). Lines are not changed.
 */
public class CountBlankLinesAction implements TextLineAction {
	private long numberOfLines = 0;

	/**
	 * Creates the action.
	 */
	public CountBlankLinesAction() {
		// Nothing to initialize
	}
	
	@Override
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception {
		if (Utilities.isBlank(lineWithLinebreak)) {
			numberOfLines++;
		}
		// Counting never changes the page
		return false;
	}

	/**
	 * Returns the number of blank lines found so far.
	 *
	 * @return number of blank lines
	 */
	public long getNumberOfLines() {
		return numberOfLines;
	}
}
