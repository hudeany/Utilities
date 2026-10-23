package de.soderer.utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Simple table of texts with one or two header lines, which can be rendered as aligned plain text,
 * CSV or HTML.
 * <p>
 * Usage: define the columns, then call {@link #startNewLine()} for each line and add its values in
 * column order by {@link #addValueToCurrentLine(String)}.
 * </p>
 */
public class TextTable {
	/**
	 * Line marker of header lines in the CSV format with metadata
	 */
	protected static final String METADATA_HEADER = "HEADER";
	/**
	 * Line marker of the column colors line in the CSV format with metadata
	 */
	protected static final String METADATA_COLOR = "COLOR";
	/**
	 * Line marker of the column alignment line in the CSV format with metadata
	 */
	protected static final String METADATA_RIGHTALIGNMENT = "RIGHTALIGN";

	/**
	 * Header lines, each containing one name per column
	 */
	protected List<List<String>> columnNames;
	/**
	 * Data lines, each containing one value per column
	 */
	protected List<List<String>> content;
	/**
	 * Color of each column, null for no color
	 */
	protected List<String> columnColors;
	/**
	 * Color of each data line, null for no color
	 */
	protected List<String> lineColors;
	/**
	 * Right alignment flag of each column
	 */
	protected List<Boolean> columnRightAligned;
	/**
	 * Index of the data line currently filled, -1 if there is none
	 */
	protected int currentLineIndex = -1;
	/**
	 * Index of the column which receives the next value of the current line
	 */
	protected int currentColumnIndex = -1;

	/**
	 * Creates an empty table without columns.
	 */
	public TextTable() {
		columnNames = new ArrayList<>();
		columnColors = new ArrayList<>();
		content = new ArrayList<>();
		columnRightAligned = new ArrayList<>();
		lineColors = new ArrayList<>();

		// First line contains headers
		columnNames.add(new ArrayList<>());
	}

	/**
	 * Creates a table with the given left aligned columns.
	 *
	 * @param columnNames
	 *            the column names
	 */
	public TextTable(final String... columnNames) {
		this();

		for (final String columnName : columnNames) {
			addColumn(columnName);
		}
	}

	/**
	 * Returns whether at least one data line was started.
	 *
	 * @return true, if the table contains data lines
	 */
	public boolean hasData() {
		return currentLineIndex >= 0;
	}

	/**
	 * Adds a left aligned column without color and without second header line.
	 *
	 * @param columnName
	 *            the column name
	 */
	public void addColumn(final String columnName) {
		addColumn(columnName, null, false);
	}

	/**
	 * Adds a column without color.
	 *
	 * @param columnName
	 *            the column name
	 * @param columnNameLine2
	 *            the second header line of the column, or null for none
	 * @param alignRight
	 *            true to align the values of the column to the right
	 */
	public void addColumn(final String columnName, final String columnNameLine2, final boolean alignRight) {
		addColumn(columnName, columnNameLine2, alignRight, null);
	}

	/**
	 * Adds a column. Existing data lines get an empty value for it.
	 *
	 * @param columnName
	 *            the column name
	 * @param columnNameLine2
	 *            the second header line of the column, or null for none
	 * @param alignRight
	 *            true to align the values of the column to the right
	 * @param farbe
	 *            the color of the column (xls color name, see {@link #convertXlsColor2HtmlColor(String)}),
	 *            or null for none
	 */
	public void addColumn(final String columnName, final String columnNameLine2, final boolean alignRight, final String farbe) {
		// Null is stored as empty name, otherwise rendering the table fails
		columnNames.get(0).add(columnName == null ? "" : columnName);
		columnColors.add(farbe);

		// Insert second line
		if (columnNameLine2 != null && columnNames.size() == 1) {
			columnNames.add(new ArrayList<>());
		}

		if (columnNames.size() > 1) {
			while (columnNames.get(1).size() < columnNames.get(0).size() - 1) {
				columnNames.get(1).add("");
			}
		}

		if (columnNameLine2 != null) {
			columnNames.get(1).add(columnNameLine2);
		} else if (columnNames.size() > 1) {
			columnNames.get(1).add("");
		}

		columnRightAligned.add(alignRight);

		// Adjust existing columns of lines to header length
		for (final List<String> line : content) {
			while (line.size() < columnNames.get(0).size()) {
				line.add("");
			}
		}
	}

	/**
	 * Removes a column with all its values.
	 *
	 * @param index
	 *            the zero based index of the column
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public void dropColumn(final int index) {
		columnNames.get(0).remove(index);
		columnColors.remove(index);

		if (columnNames.size() > 1) {
			columnNames.get(1).remove(index);
		}

		columnRightAligned.remove(index);

		// Adjust existing columns of lines to header lengths
		for (final List<String> line : content) {
			line.remove(index);
		}

		// The next value of the current line must still go to the same column as before
		if (index < currentColumnIndex) {
			currentColumnIndex--;
		}
	}

	/**
	 * Sets the alignment of a column.
	 *
	 * @param columnIndex
	 *            the zero based index of the column
	 * @param alignRight
	 *            true to align the values of the column to the right
	 * @throws Exception
	 *             if the column index is invalid
	 */
	public void setAlignmentForColumn(final int columnIndex, final boolean alignRight) throws Exception {
		if (columnIndex < 0 || columnIndex >= columnNames.get(0).size()) {
			throw new Exception("Invalid column index: " + columnIndex);
		}

		columnRightAligned.set(columnIndex, alignRight);
	}

	/**
	 * Starts a new data line without color. All its values are empty initially.
	 */
	public void startNewLine() {
		startNewLine(null);
	}

	/**
	 * Starts a new data line. All its values are empty initially.
	 *
	 * @param farbe
	 *            the color of the line (HTML color), or null for none
	 */
	public void startNewLine(final String farbe) {
		content.add(new ArrayList<>());
		lineColors.add(farbe);
		currentLineIndex++;
		currentColumnIndex = 0;

		// Adjust columns of new line to header lengths
		while (content.get(currentLineIndex).size() < columnNames.get(0).size()) {
			content.get(currentLineIndex).add("");
		}
	}

	/**
	 * Sets the value of the next column of the current line.
	 *
	 * @param value
	 *            the value
	 * @throws Exception
	 *             if no line was started or all columns of the current line are filled already
	 */
	public void addValueToCurrentLine(final int value) throws Exception {
		addValueToCurrentLine(Integer.toString(value));
	}

	/**
	 * Sets the value of the next column of the current line.
	 *
	 * @param value
	 *            the value, null is stored as empty value
	 * @throws Exception
	 *             if no line was started or all columns of the current line are filled already
	 */
	public void addValueToCurrentLine(String value) throws Exception {
		if (currentLineIndex == -1) {
			throw new Exception("No line created for extension");
		} else if (content.get(currentLineIndex).size() <= currentColumnIndex) {
			throw new Exception("Tried to write to a non existent line");
		}

		if (value == null) {
			value = "";
		}
		content.get(currentLineIndex).set(currentColumnIndex, value);
		currentColumnIndex++;
	}

	/**
	 * Aligns all columns to the right, which only contain integer values or blank values, and all
	 * other columns to the left.
	 */
	public void setNumericColumnsRightALigned() {
		for (int columnIndex = 0; columnIndex < columnRightAligned.size(); columnIndex++) {
			boolean isNumeric = true;
			for (final List<String> line : content) {
				if (StringUtilities.isNotBlank(line.get(columnIndex)) && !NumberUtilities.isInteger(line.get(columnIndex))) {
					isNumeric = false;
					break;
				}
			}
			columnRightAligned.set(columnIndex, isNumeric);
		}
	}

	/**
	 * Renders the table as plain text with aligned columns, separated by a blank. The header lines are
	 * followed by a line of dashes.
	 *
	 * @return the table text
	 */
	@Override
	public String toString() {
		final List<StringBuilder> columnNamesLines = new ArrayList<>();
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			columnNamesLines.add(new StringBuilder());
		}

		final List<StringBuilder> contentLines = new ArrayList<>();
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			contentLines.add(new StringBuilder());
		}

		// Iterate over all columns
		for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
			final int size = getMaxSizeOfColumn(columnIndex);

			for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
				if (columnIndex > 0) {
					columnNamesLines.get(lineIndex).append(" ");
				}

				columnNamesLines.get(lineIndex).append(TextUtilities.trimStringToLengthAlignedLeft(columnNames.get(lineIndex).get(columnIndex), size));
			}

			// Iterate over all lies
			for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
				if (columnIndex > 0) {
					contentLines.get(lineIndex).append(" ");
				}

				if (columnRightAligned.get(columnIndex)) {
					contentLines.get(lineIndex).append(TextUtilities.trimStringToLengthAlignedRight(content.get(lineIndex).get(columnIndex), size));
				} else {
					contentLines.get(lineIndex).append(TextUtilities.trimStringToLengthAlignedLeft(content.get(lineIndex).get(columnIndex), size));
				}
			}
		}

		final StringBuilder text = new StringBuilder();
		for (int lineIndex = 0; lineIndex < columnNamesLines.size(); lineIndex++) {
			text.append(columnNamesLines.get(lineIndex) + "\n");
		}
		// Remove headerline
		text.append(StringUtilities.repeat("-", columnNamesLines.get(0).length()) + "\n");
		for (int lineIndex = 0; lineIndex < contentLines.size(); lineIndex++) {
			text.append(contentLines.get(lineIndex) + "\n");
		}

		return text.toString();
	}

	/**
	 * Renders the header and data lines as CSV text with semicolon separator and quoted values.
	 *
	 * @return the CSV text
	 */
	public String toCsvString() {
		final StringBuilder csvText = new StringBuilder();

		// Write columnheader first
		// Iterate over all header lines
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			// Iterate over all columns
			for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
				csvText.append(csvQuote(columnNames.get(lineIndex).get(columnIndex)) + ";");
			}
			csvText.append("\n");
		}

		// Write data lines second
		// Iterate over all lines
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			// Iterate over all columns
			for (int columnIndex = 0; columnIndex < content.get(0).size(); columnIndex++) {
				csvText.append(csvQuote(content.get(lineIndex).get(columnIndex)) + ";");
			}
			csvText.append("\n");
		}

		return csvText.toString();
	}

	/**
	 * Renders the table as CSV text with metadata (column colors, alignments and line colors), which
	 * can be loaded again by {@link #loadCsvWithMetadataFile(byte[])}. A leading apostrophe of a value
	 * (Excel text marker) is removed, unless the value also ends with an apostrophe.
	 *
	 * @return the CSV text with metadata
	 */
	public String toCsvStringWithMetaData() {
		final StringBuilder csvText = new StringBuilder();

		// Write columnheader first
		// Iterate over all header lines
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			csvText.append("\"" + METADATA_HEADER + "\";");

			// Iterate over all columns
			for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
				csvText.append(csvQuote(columnNames.get(lineIndex).get(columnIndex)) + ";");
			}
			csvText.append("\n");
		}

		csvText.append("\"" + METADATA_COLOR + "\";");
		for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
			csvText.append(csvQuote(String.valueOf(columnColors.get(columnIndex))) + ";");
		}
		csvText.append("\n");

		csvText.append("\"" + METADATA_RIGHTALIGNMENT + "\";");
		for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
			csvText.append(csvQuote(String.valueOf(columnRightAligned.get(columnIndex))) + ";");
		}
		csvText.append("\n");

		// Write data lines second
		// Iterate over all lines
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			csvText.append(csvQuote(String.valueOf(lineColors.get(lineIndex))) + ";");

			// Iterate over all columns
			for (int columnIndex = 0; columnIndex < content.get(0).size(); columnIndex++) {
				// Remove leading apostrophes
				String cellContentString = content.get(lineIndex).get(columnIndex);
				if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'")) {
					cellContentString = cellContentString.substring(1);
				}
				csvText.append(csvQuote(cellContentString) + ";");
			}
			csvText.append("\n");
		}

		return csvText.toString();
	}

	/**
	 * Returns the start of an HTML document including the current date and time.
	 *
	 * @param title
	 *            the title of the document
	 * @return the HTML text up to the start of the body content
	 */
	public static String getHtmlHeader(final String title) {
		return getHtmlHeader(title, true, null);
	}

	/**
	 * Returns the start of an HTML document.
	 *
	 * @param title
	 *            the title of the document
	 * @param mitZeit
	 *            true to include the current date and time
	 * @return the HTML text up to the start of the body content
	 */
	public static String getHtmlHeader(final String title, final boolean mitZeit) {
		return getHtmlHeader(title, mitZeit, null);
	}

	/**
	 * Returns the start of an HTML document. The title is HTML encoded.
	 *
	 * @param title
	 *            the title of the document
	 * @param mitZeit
	 *            true to include the current date and time
	 * @param inlineCssStyleSheet
	 *            complete style element to include in the HTML head, or null for none
	 * @return the HTML text up to the start of the body content
	 */
	public static String getHtmlHeader(final String title, final boolean mitZeit, final String inlineCssStyleSheet) {
		return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" + "<html>\n" + "<head>\n<title>" + htmlEncodeString(title) + "</title>\n"
				+ (StringUtilities.isEmpty(inlineCssStyleSheet) ? "" : inlineCssStyleSheet) + "</head>\n" + "<body>\n" + "<h2>" + htmlEncodeString(title) + "</h2>\n"
				+ (mitZeit ? "<h4>Stand: " + DateUtilities.formatDate(DateUtilities.DD_MM_YYYY_HH_MM_SS, new GregorianCalendar().getTime()) + "</h4>\n" : "");
	}

	/**
	 * Returns the end of an HTML document started by {@link #getHtmlHeader(String)}.
	 *
	 * @return the closing body and html tags
	 */
	public static String getHtmlFooter() {
		return "</body>\n" + "</html>\n";
	}

	/**
	 * Renders the table as HTML table with default font size.
	 *
	 * @param tableTitle
	 *            the title shown above the table
	 * @return the HTML text of the table
	 */
	public String toHtmlString(final String tableTitle) {
		return toHtmlString(tableTitle, 0);
	}

	/**
	 * Renders the table as HTML table. All values are HTML encoded.
	 *
	 * @param tableTitle
	 *            the title shown above the table
	 * @param fontsizeInPt
	 *            the font size of the table cells in points, 0 or less for the default size
	 * @return the HTML text of the table
	 */
	public String toHtmlString(final String tableTitle, final int fontsizeInPt) {
		final StringBuilder htmlTableStringBuffer = new StringBuilder();
		htmlTableStringBuffer.append(htmlEncodeString(tableTitle) + "<br><br>\n");
		if (fontsizeInPt > 0) {
			htmlTableStringBuffer.append("<STYLE TYPE=\"text/css\"><!--TD{font-family: Arial; font-size: " + fontsizeInPt + "pt;}---></STYLE>");
		}
		htmlTableStringBuffer.append("<table border='1' cellspacing='1' cellpadding='1'>\n");
		// Alternatively for full width: "<table border='1' width='100%' cellspacing='1' cellpadding='1'>\n");

		int rowcount = 0;

		htmlTableStringBuffer.append("<tr align='center'>");
		// Iterate over all columns
		for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
			if (columnColors.get(columnIndex) == null) {
				htmlTableStringBuffer.append("<td>");
			} else {
				htmlTableStringBuffer.append("<td bgcolor='" + convertXlsColor2HtmlColor(columnColors.get(columnIndex)) + "'>");
			}

			htmlTableStringBuffer.append(htmlEncodeString(columnNames.get(0).get(columnIndex)).replace(" ", "&nbsp;").replace("-", "&#8209;"));
			if (columnNames.size() > 1) {
				htmlTableStringBuffer.append("<br/>" + htmlEncodeString(columnNames.get(1).get(columnIndex)).replace(" ", "&nbsp;").replace("-", "&#8209;"));
			}
			htmlTableStringBuffer.append("</td>");
		}
		htmlTableStringBuffer.append("</tr>\n");

		// Iterate over all lines
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			if (lineColors.get(lineIndex) != null) {
				htmlTableStringBuffer.append("<tr align='right' bgcolor='" + htmlEncodeString(lineColors.get(lineIndex)) + "'>");
			} else if (rowcount % 2 == 0) {
				htmlTableStringBuffer.append("<tr align='right' bgcolor='cornflowerblue'>");
			} else {
				htmlTableStringBuffer.append("<tr align='right' bgcolor='lightblue'>");
			}

			// Iterate over all columns
			for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
				// Remove leading apostrophes
				String cellContentString = content.get(lineIndex).get(columnIndex);
				if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'")) {
					cellContentString = cellContentString.substring(1);
				}
				htmlTableStringBuffer.append("<td>" + htmlEncodeString(cellContentString).replace(" ", "&nbsp;").replace("-", "&#8209;") + "</td>");
			}
			htmlTableStringBuffer.append("</tr>\n");

			rowcount++;
		}

		htmlTableStringBuffer.append("</table>\n");
		htmlTableStringBuffer.append("<br>\n");
		htmlTableStringBuffer.append("<br>\n");

		return htmlTableStringBuffer.toString();
	}

	/**
	 * Returns the maximum length of all header and data values of a column.
	 *
	 * @param index
	 *            the zero based index of the column
	 * @return the maximum number of characters
	 */
	protected int getMaxSizeOfColumn(final int index) {
		int maxSize = 0;
		for (final List<String> line : columnNames) {
			maxSize = Math.max(line.get(index).length(), maxSize);
		}
		for (final List<String> line : content) {
			maxSize = Math.max(line.get(index).length(), maxSize);
		}
		return maxSize;
	}

	/**
	 * Converts an Excel color name to a similar HTML color name.
	 *
	 * @param xlsColor
	 *            the Excel color name, like "LIGHT_GREEN"
	 * @return the HTML color name, "white" for unknown colors
	 */
	public static String convertXlsColor2HtmlColor(final String xlsColor) {
		if ("VERY_LIGHT_YELLOW".equalsIgnoreCase(xlsColor)) {
			return "yellow";
		} else if ("LIGHT_GREEN".equalsIgnoreCase(xlsColor)) {
			return "lime";
		} else if ("LAVENDER".equalsIgnoreCase(xlsColor)) {
			return "fuchsia";
		} else if ("TAN".equalsIgnoreCase(xlsColor)) {
			return "fuchsia";
		} else if ("TURQUOISE".equalsIgnoreCase(xlsColor)) {
			return "aqua";
		} else if ("YELLOW".equalsIgnoreCase(xlsColor)) {
			return "yellow";
		} else if ("LIGHT_TURQUOISE".equalsIgnoreCase(xlsColor)) {
			return "aqua";
		} else if ("LIGHT_ORANGE".equalsIgnoreCase(xlsColor)) {
			return "red";
		} else if ("ROSE".equalsIgnoreCase(xlsColor)) {
			return "fuchsia";
		} else if ("WOCHENENDE".equalsIgnoreCase(xlsColor)) {
			// Special color adaptions for Html statistics
			return "coral";
		} else {
			return "white";
		}
	}

	/**
	 * Replaces the whole table content by the data of a CSV text with metadata as written by
	 * {@link #toCsvStringWithMetaData()}.
	 *
	 * @param csvWithMetadata
	 *            the UTF-8 encoded CSV data
	 * @throws IOException
	 *             if reading the data fails
	 */
	public void loadCsvWithMetadataFile(final byte[] csvWithMetadata) throws IOException {
		if (csvWithMetadata == null) {
			throw new IOException("Missing CSV data");
		}

		columnNames.clear();
		content.clear();
		columnColors.clear();
		lineColors.clear();
		columnRightAligned.clear();
		currentLineIndex = -1;
		currentColumnIndex = 0;

		// Split into records by a quote aware parser, because quoted values may contain line breaks
		for (final String[] lineParts : splitCsvRecords(new String(csvWithMetadata, StandardCharsets.UTF_8))) {
			if (lineParts.length == 0) {
				// Empty line
				continue;
			} else if (METADATA_HEADER.equals(lineParts[0])) {
				final List<String> headerLine = new ArrayList<>();
				columnNames.add(headerLine);
				for (int i = 1; i < lineParts.length; i++) {
					headerLine.add(lineParts[i]);
				}
			} else if (METADATA_COLOR.equals(lineParts[0])) {
				for (int i = 1; i < lineParts.length; i++) {
					if ("null".equalsIgnoreCase(lineParts[i])) {
						columnColors.add(null);
					} else {
						columnColors.add(lineParts[i]);
					}
				}
			} else if (METADATA_RIGHTALIGNMENT.equals(lineParts[0])) {
				for (int i = 1; i < lineParts.length; i++) {
					columnRightAligned.add("true".equalsIgnoreCase(lineParts[i]));
				}
			} else {
				if ("null".equalsIgnoreCase(lineParts[0])) {
					lineColors.add(null);
				} else {
					lineColors.add(lineParts[0]);
				}
				final List<String> contentLine = new ArrayList<>();
				content.add(contentLine);
				for (int i = 1; i < lineParts.length; i++) {
					contentLine.add(lineParts[i]);
				}
			}
		}

		normalizeLoadedData();

		// New lines are appended after the loaded lines
		currentLineIndex = content.size() - 1;
		// The loaded lines are complete, so values must not be added without starting a new line first
		currentColumnIndex = getColumnCount();
	}

	/**
	 * Makes loaded data consistent, so data with missing metadata lines or values can be used like a
	 * table created by the API: at least one header line, at most two, and all header lines, column
	 * colors, alignments and data lines with one entry per column.
	 */
	private void normalizeLoadedData() {
		if (columnNames.isEmpty()) {
			columnNames.add(new ArrayList<>());
		}
		while (columnNames.size() > 2) {
			columnNames.remove(columnNames.size() - 1);
		}
		int columnCount = 0;
		for (final List<String> headerLine : columnNames) {
			columnCount = Math.max(columnCount, headerLine.size());
		}
		for (final List<String> line : content) {
			columnCount = Math.max(columnCount, line.size());
		}
		for (final List<String> headerLine : columnNames) {
			padList(headerLine, columnCount, "");
		}
		for (final List<String> line : content) {
			padList(line, columnCount, "");
		}
		padList(columnColors, columnCount, null);
		padList(columnRightAligned, columnCount, false);
		while (columnColors.size() > columnCount) {
			columnColors.remove(columnColors.size() - 1);
		}
		while (columnRightAligned.size() > columnCount) {
			columnRightAligned.remove(columnRightAligned.size() - 1);
		}
	}

	private static <T> void padList(final List<T> list, final int size, final T fillValue) {
		while (list.size() < size) {
			list.add(fillValue);
		}
	}

	/**
	 * Quotes a value for the CSV output. Contained quotes are doubled.
	 *
	 * @param value
	 *            the value, null is written as empty value
	 * @return the quoted value
	 */
	private static String csvQuote(final String value) {
		return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
	}

	/**
	 * Splits the CSV format written by {@link #toCsvStringWithMetaData()} into records and their
	 * unquoted values. Separators and line breaks within quotes are part of the value, doubled
	 * quotes within quotes represent a single quote. A trailing separator does not start another
	 * value. Records are separated by LF, CRLF or CR.
	 *
	 * @param text
	 *            the CSV text
	 * @return the values of each record, an empty array for an empty line
	 */
	private static List<String[]> splitCsvRecords(final String text) {
		final List<String[]> records = new ArrayList<>();
		final List<String> values = new ArrayList<>();
		final StringBuilder value = new StringBuilder();
		boolean withinQuotes = false;
		// A leading byte order mark is no part of the data
		final int start = !text.isEmpty() && text.charAt(0) == '\uFEFF' ? 1 : 0;
		for (int i = start; i < text.length(); i++) {
			final char nextChar = text.charAt(i);
			if (withinQuotes) {
				if (nextChar == '"') {
					if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
						value.append('"');
						i++;
					} else {
						withinQuotes = false;
					}
				} else {
					value.append(nextChar);
				}
			} else if (nextChar == '"') {
				withinQuotes = true;
			} else if (nextChar == ';') {
				values.add(value.toString());
				value.setLength(0);
			} else if (nextChar == '\n' || nextChar == '\r') {
				if (nextChar == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				addCsvRecord(records, values, value);
			} else {
				value.append(nextChar);
			}
		}
		if (!values.isEmpty() || value.length() > 0) {
			addCsvRecord(records, values, value);
		}
		return records;
	}

	private static void addCsvRecord(final List<String[]> records, final List<String> values, final StringBuilder value) {
		if (value.length() > 0) {
			values.add(value.toString());
		}
		records.add(values.toArray(new String[0]));
		values.clear();
		value.setLength(0);
	}

	/**
	 * Removes data lines from the start, so that at most the given number of lines remains.
	 *
	 * @param numberOfLastRows
	 *            the maximum number of data lines to keep
	 */
	public void keepOnlyLastRows(final int numberOfLastRows) {
		while (content.size() > Math.max(0, numberOfLastRows)) {
			content.remove(0);
			// Line colors must stay aligned with their lines
			lineColors.remove(0);
		}
		currentLineIndex = content.size() - 1;
	}

	/**
	 * Removes data lines from the end, so that at most the given number of lines remains.
	 *
	 * @param numberOfFirstRows
	 *            the maximum number of data lines to keep
	 */
	public void keepOnlyFirstRows(final int numberOfFirstRows) {
		while (content.size() > Math.max(0, numberOfFirstRows)) {
			content.remove(content.size() - 1);
			// Line colors must stay aligned with their lines
			lineColors.remove(lineColors.size() - 1);
		}
		currentLineIndex = content.size() - 1;
	}

	/**
	 * Returns the number of header lines.
	 *
	 * @return the number of header lines (1 or 2)
	 */
	public int getHeaderLineCount() {
		return columnNames.size();
	}

	/**
	 * Returns the number of columns.
	 *
	 * @return the number of columns
	 */
	public int getColumnCount() {
		return columnNames.get(0).size();
	}

	/**
	 * Returns the number of data lines.
	 *
	 * @return the number of data lines
	 */
	public int getDataLineCount() {
		return content.size();
	}

	/**
	 * Returns a header value.
	 *
	 * @param headerLineIndex
	 *            the zero based index of the header line
	 * @param columnIndex
	 *            the zero based index of the column
	 * @return the header value
	 * @throws Exception
	 *             if an index is invalid
	 */
	public String getHeader(final int headerLineIndex, final int columnIndex) throws Exception {
		if (columnNames.size() <= headerLineIndex || columnNames.get(headerLineIndex).size() <= columnIndex) {
			throw new Exception("Invalid data request at columnheaders line index " + headerLineIndex + " column index " + columnIndex);
		}
		return columnNames.get(headerLineIndex).get(columnIndex);
	}

	/**
	 * Returns the color of a column.
	 *
	 * @param columnIndex
	 *            the zero based index of the column
	 * @return the column color, or null for none
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public String getHeaderColor(final int columnIndex) {
		return columnColors.get(columnIndex);
	}

	/**
	 * Returns the color of a data line.
	 *
	 * @param lineIndex
	 *            the zero based index of the data line
	 * @return the line color, or null for none
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public String getLineColor(final int lineIndex) {
		return lineColors.get(lineIndex);
	}

	/**
	 * Returns a data value.
	 *
	 * @param lineIndex
	 *            the zero based index of the data line
	 * @param columnIndex
	 *            the zero based index of the column
	 * @return the data value
	 * @throws Exception
	 *             if an index is invalid
	 */
	public String getData(final int lineIndex, final int columnIndex) throws Exception {
		if (content.size() <= lineIndex || content.get(lineIndex).size() <= columnIndex) {
			throw new Exception("Invalid data request at line index " + lineIndex + " column index " + columnIndex);
		}
		return content.get(lineIndex).get(columnIndex);
	}

	/**
	 * Encodes a text for HTML element content and attribute values. Besides the HTML special characters
	 * the German umlauts are encoded as named entities.
	 *
	 * @param value
	 *            the text to encode, may be null
	 * @return the encoded text, an empty text for null
	 */
	public static String htmlEncodeString(final String value) {
		if (value == null) {
			return "";
		} else {
			return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
					.replace("Ä", "&Auml;").replace("ä", "&auml;").replace("Ö", "&Ouml;").replace("ö", "&ouml;").replace("Ü", "&Uuml;")
					.replace("ü", "&uuml;").replace("ß", "&szlig;");
		}
	}
}
