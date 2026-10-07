package de.soderer.utilities.textbook.action;

import java.util.List;

/**
 * Action executed by a {@link de.soderer.utilities.textbook.TextBookEditor} for each line of a range of lines,
 * from the last line of a page to its first.
 */
public interface TextLineAction {
	/**
	 * Executes the action for one line. The action may change or remove the line in pageLines.
	 *
	 * @param overallLineIndex 0-based index of the line in the whole text
	 * @param pageLineIndex 0-based index of the line in pageLines
	 * @param pageLines all lines of the current page, may be changed by the action
	 * @param lineWithLinebreak content of the line including its line break
	 * @return true if pageLines was changed, so the page gets the changed lines as new content
	 * @throws Exception if the line cannot be processed
	 */
	public boolean execute(long overallLineIndex, int pageLineIndex, List<String> pageLines, String lineWithLinebreak) throws Exception;
}
