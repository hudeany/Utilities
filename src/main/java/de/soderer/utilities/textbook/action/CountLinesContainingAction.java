package de.soderer.utilities.textbook.action;

import java.util.List;
import java.util.regex.Pattern;

public class CountLinesContainingAction implements TextLineAction {
	private final Pattern pattern;
	private final String searchText;
	private final boolean caseSensitive;
	
	private long numberOfLines = 0;
	
	public CountLinesContainingAction(final String searchText, boolean caseSensitive, boolean regularExpression) {
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
		if (pattern != null && pattern.matcher(lineWithLinebreak).matches()) {
			numberOfLines++;
		} else if (pattern == null) {
			if (caseSensitive && lineWithLinebreak.contains(searchText)) {
				numberOfLines++;
			} else if (!caseSensitive && lineWithLinebreak.toLowerCase().contains(searchText.toLowerCase())) {
				numberOfLines++;
			}
		}
		return false;
	}

	public long getNumberOfLines() {
		return numberOfLines;
	}
}
