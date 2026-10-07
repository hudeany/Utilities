package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple helpers for extracting data from HTML text without a full HTML parser.
 */
public class HtmlUtilities {
	/** Table row element, with optional attributes */
	private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("<tr(?:\\s[^>]*)?>(.*?)</tr\\s*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	/** Table data cell element, with optional attributes */
	private static final Pattern TABLE_CELL_PATTERN = Pattern.compile("<td(?:\\s[^>]*)?>(.*?)</td\\s*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	private HtmlUtilities() {
		// Utility class
	}

	/**
	 * Extract the cell texts of all table rows ({@code <tr>}) of an HTML text.
	 * Only data cells ({@code <td>}) are read, header cells ({@code <th>}) are ignored.
	 * HTML tags within a cell are removed and the cell text is trimmed. HTML entities are not decoded.
	 * Nested tables are not supported.
	 *
	 * @param htmlData
	 *            HTML text, must not be null
	 * @return one list of cell texts per table row, in document order
	 */
	public static List<List<String>> parseHtmlTable(final String htmlData) {
		final List<List<String>> entries = new ArrayList<>();

		final Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(htmlData);

		while (rowMatcher.find()) {
			final String row = rowMatcher.group(1);

			final List<String> tableRowData = new ArrayList<>();
			final Matcher cellMatcher = TABLE_CELL_PATTERN.matcher(row);
			while (cellMatcher.find()) {
				tableRowData.add(stripTags(cellMatcher.group(1)).trim());
			}

			entries.add(tableRowData);
		}

		return entries;
	}

	private static String stripTags(final String html) {
		return html.replaceAll("<[^>]+>", "");
	}
}
