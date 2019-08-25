package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.Utilities;

public class CountBlankLinesAction implements TextLineAction {
	private long numberOfLines = 0;
	
	@Override
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception {
		if (Utilities.isBlank(lineWithLinebreak)) {
			numberOfLines++;
			return true;
		} else {
			return false;
		}
	}

	public long getNumberOfLines() {
		return numberOfLines;
	}
}
