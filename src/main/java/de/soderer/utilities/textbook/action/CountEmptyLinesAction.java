package de.soderer.utilities.textbook.action;

import java.util.List;

import de.soderer.utilities.TextUtilities.LineBreak;
import de.soderer.utilities.Utilities;

public class CountEmptyLinesAction implements TextLineAction {
	private long numberOfLines = 0;

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
			return true;
		} else {
			return false;
		}
	}

	public long getNumberOfLines() {
		return numberOfLines;
	}
}
