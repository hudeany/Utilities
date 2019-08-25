package de.soderer.utilities.textbook.action;

import java.util.List;
import java.util.regex.Pattern;

public class KeepOnlyLinesNotContainingAction implements TextLineAction {
	private final Pattern pattern;
	private final String searchText;
	private final boolean caseSensitive;
	
	private long numberOfDroppedLines = 0;
	
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
		if (pattern != null && !pattern.matcher(lineWithLinebreak).matches()) {
			return false;
		} else if (pattern == null) {
			if (caseSensitive && !lineWithLinebreak.contains(searchText)) {
				return false;
			} else if (!caseSensitive && !lineWithLinebreak.toLowerCase().contains(searchText.toLowerCase())) {
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

	public long getNumberOfDroppedLines() {
		return numberOfDroppedLines;
	}
}
