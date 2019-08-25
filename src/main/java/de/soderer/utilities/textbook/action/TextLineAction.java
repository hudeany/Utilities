package de.soderer.utilities.textbook.action;

import java.util.List;

public interface TextLineAction {
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception;
}
