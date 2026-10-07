package de.soderer.utilities.textbook.action;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Keeps only the lines not containing a text or a match of a regular expression and removes all others.
 * The text is searched including the line break, a regular expression may match anywhere in the line.
 */
public class KeepOnlyLinesNotContainingAction implements TextLineAction {
	private final Pattern pattern;
	private final String searchText;
	private final boolean caseSensitive;
	
	private long numberOfDroppedLines = 0;
	
	/**
	 * Creates the action.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 */
	public KeepOnlyLinesNotContainingAction(final String searchText, boolean caseSensitive, boolean regularExpression) {
		this.searchText = searchText;
		this.caseSensitive = caseSensitive;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		} else {
			pattern = null;
		}
	}
	
	@Override
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception {
		if (pattern != null && !pattern.matcher(lineWithLinebreak).find()) {
			return false;
		} else if (pattern == null) {
			if (caseSensitive && !lineWithLinebreak.contains(searchText)) {
				return false;
			} else if (!caseSensitive && !lineWithLinebreak.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) {
				return false;
			} else {
				pageLines.remove(pageLineIndex);
				numberOfDroppedLines++;
				return true;
			}
		} else {
			pageLines.remove(pageLineIndex);
			numberOfDroppedLines++;
			return true;
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
