package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.TextUtilities.LineBreak;
import de.soderer.utilities.Utilities;

/**
 * Removes the empty lines (without any character except the line break).
 */
public class DropEmptyLinesAction implements TextLineAction {
	private long numberOfDroppedLines = 0;

	/**
	 * Creates the action.
	 */
	public DropEmptyLinesAction() {
		// Nothing to initialize
	}

	@Override
	public boolean execute(final long overallLineIndex, final int pageLineIndex, final List<String> pageLines, final String lineWithLinebreak) throws Exception {
		String lineWithoutLinebreak;
		if (lineWithLinebreak.endsWith(LineBreak.Windows.toString())) {
			lineWithoutLinebreak = lineWithLinebreak.substring(0, lineWithLinebreak.length() - 2);
		} else if (lineWithLinebreak.endsWith(LineBreak.Mac.toString())) {
			lineWithoutLinebreak = lineWithLinebreak.substring(0, lineWithLinebreak.length() - 1);
		} else if (lineWithLinebreak.endsWith(LineBreak.Unix.toString())) {
			lineWithoutLinebreak = lineWithLinebreak.substring(0, lineWithLinebreak.length() - 1);
		} else {
			throw new Exception("Missing linebreak of text line: " + overallLineIndex);
		}

		if (Utilities.isEmpty(lineWithoutLinebreak)) {
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
