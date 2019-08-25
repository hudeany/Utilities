package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.Utilities;

public class DropEmptyLinesAction implements TextLineAction {
	private long numberOfDroppedLines = 0;
	
	@Override
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception {
		String lineWithoutLinebreak;
		if (lineWithLinebreak.endsWith(TextUtilities.LinebreakWindows)) {
			lineWithoutLinebreak = lineWithLinebreak.substring(0, lineWithLinebreak.length() - 2);
		} else if (lineWithLinebreak.endsWith(TextUtilities.LinebreakMac)) {
			lineWithoutLinebreak = lineWithLinebreak.substring(0, lineWithLinebreak.length() - 1);
		} else if (lineWithLinebreak.endsWith(TextUtilities.LinebreakUnix)) {
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

	public long getNumberOfDroppedLines() {
		return numberOfDroppedLines;
	}
}
