package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.Utilities;

public class DropBlankLinesAction implements TextLineAction {
	private long numberOfDroppedLines = 0;
	
	@Override
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception {
		if (Utilities.isBlank(lineWithLinebreak)) {
			pageLines.remove(pageLineIndex);
			numberOfDroppedLines++;
			return true;
		} else {
			return false;
		}
	}

	public long getNumberOfDroppedLines() {
		return numberOfDroppedLines;
	}
}
