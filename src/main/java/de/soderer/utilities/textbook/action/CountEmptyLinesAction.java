package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.StringUtilities;
import de.soderer.utilities.TextUtilities.LineBreak;

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
			// The last line of a text may have no line break
			lineWithoutLinebreak = lineWithLinebreak;
		}

		if (StringUtilities.isEmpty(lineWithoutLinebreak)) {
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
