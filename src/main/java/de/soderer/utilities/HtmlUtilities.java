package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple helpers for extracting data from HTML text without a full HTML parser.
 */
public class HtmlUtilities {
	/** HTML comment, which may contain markup that must not be read */
	private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

	/**
	 * Table row element, with optional attributes. The end tag is optional in HTML, so a row also ends at the next row
	 * or at the end of its table section.
	 */
	private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("<tr(?:\\s[^>]*)?>(.*?)(?=</tr\\s*>|<tr[\\s>/]|</table\\s*>|</tbody\\s*>|</thead\\s*>|</tfoot\\s*>|<tbody[\\s>]|<thead[\\s>]|<tfoot[\\s>]|\\z)",
			Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	/**
	 * Table cell element (data or header cell), with optional attributes. The end tag is optional in HTML, so a cell
	 * also ends at the next cell or at the end of its row.
	 */
	private static final Pattern TABLE_CELL_PATTERN = Pattern.compile("<(t[dh])(?:\\s[^>]*)?>(.*?)(?=</t[dh]\\s*>|<t[dh][\\s>/]|\\z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	/** Line break element, which separates words like a blank */
	private static final Pattern LINEBREAK_TAG_PATTERN = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

	/** Any other tag */
	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

	/** Sequence of whitespace characters, displayed as a single blank in HTML */
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private HtmlUtilities() {
		// Utility class
	}

	/**
	 * Extract the cell texts of all table rows ({@code <tr>}) of an HTML text.
	 * Only data cells ({@code <td>}) are read, header cells ({@code <th>}) are ignored, so a row with header cells only
	 * results in an empty list. The optional end tags of rows and cells may be omitted, as allowed by HTML. Content of
	 * HTML comments is ignored.
	 * HTML tags within a cell are removed ({@code <br>} is replaced by a blank), whitespace sequences are reduced to a
	 * single blank as displayed by browsers, and the cell text is trimmed. HTML entities are not decoded.
	 * Nested tables are not supported.
	 *
	 * @param htmlData
	 *            HTML text, must not be null
	 * @return one list of cell texts per table row, in document order
	 */
	public static List<List<String>> parseHtmlTable(final String htmlData) {
		final List<List<String>> entries = new ArrayList<>();

		final String htmlDataWithoutComments = COMMENT_PATTERN.matcher(htmlData).replaceAll("");
		final Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(htmlDataWithoutComments);

		while (rowMatcher.find()) {
			final String row = rowMatcher.group(1);

			final List<String> tableRowData = new ArrayList<>();
			final Matcher cellMatcher = TABLE_CELL_PATTERN.matcher(row);
			while (cellMatcher.find()) {
				// Header cells are matched too, so they end the preceding data cell, but they are not part of the result
				if ("td".equalsIgnoreCase(cellMatcher.group(1))) {
					tableRowData.add(getCellText(cellMatcher.group(2)));
				}
			}

			entries.add(tableRowData);
		}

		return entries;
	}

	/**
	 * Returns the displayed text of a cell content.
	 *
	 * @param cellHtml
	 *            the HTML content of the cell
	 * @return the text without tags, with reduced whitespace, trimmed
	 */
	private static String getCellText(final String cellHtml) {
		final String withoutLinebreakTags = LINEBREAK_TAG_PATTERN.matcher(cellHtml).replaceAll(" ");
		final String withoutTags = TAG_PATTERN.matcher(withoutLinebreakTags).replaceAll("");
		return WHITESPACE_PATTERN.matcher(withoutTags).replaceAll(" ").trim();
	}
}
