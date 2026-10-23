package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.StringUtilities;

/**
 * Removes the blank lines (containing only whitespace).
 */
public class DropBlankLinesAction implements TextLineAction {
	private long numberOfDroppedLines = 0;

	/**
	 * Creates the action.
	 */
	public DropBlankLinesAction() {
		// Nothing to initialize
	}

	@Override
	public boolean execute(final long overallLineIndex, final int pageLineIndex, final List<String> pageLines, final String lineWithLinebreak) throws Exception {
		if (StringUtilities.isBlank(lineWithLinebreak)) {
			pageLines.remove(pageLineIndex);
			numberOfDroppedLines++;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the number of lines removed so far.
	 *
	 * @return number of removed lines
	 */
	public long getNumberOfDroppedLines() {
		return numberOfDroppedLines;
	}
}
