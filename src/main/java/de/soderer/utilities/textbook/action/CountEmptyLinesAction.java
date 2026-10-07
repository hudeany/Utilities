package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.TextUtilities.LineBreak;
import de.soderer.utilities.Utilities;

/**
 * Counts the empty lines (without any character except the line break). Lines are not changed.
 */
public class CountEmptyLinesAction implements TextLineAction {
	private long numberOfLines = 0;

	/**
	 * Creates the action.
	 */
	public CountEmptyLinesAction() {
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
			numberOfLines++;
		}
		// Counting never changes the page
		return false;
	}

	/**
	 * Returns the number of empty lines found so far.
	 *
	 * @return number of empty lines
	 */
	public long getNumberOfLines() {
		return numberOfLines;
	}
}
