package de.soderer.utilities;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.commons.lang3.StringUtils;

public class TextTable {
	private static final String METADATA_HEADER = "HEADER";
	private static final String METADATA_FARBE = "FARBE";
	private static final String METADATA_RIGHTALIGNMENT = "RIGHTALIGN";
	
	private List<List<String>> columnNames;
	private List<List<String>> content;
	private List<String> columnColors;
	private List<String> lineColors;
	private List<Boolean> columnRightAligned;
	private int currentLineIndex = -1;
	private int currentColumnIndex = -1;
	
	public TextTable() {
		columnNames = new ArrayList<List<String>>();
		columnColors = new ArrayList<String>();
		content = new ArrayList<List<String>>();
		columnRightAligned = new ArrayList<Boolean>();
		lineColors = new ArrayList<String>();
		
		//Erste Zeile enthält die Header
		columnNames.add(new ArrayList<String>());
	}
	
	public TextTable(String... columnNames) {
		this();
		
		for (String columnName : columnNames)
			addColumn(columnName);
	}
	
	public boolean hasData() {
		return currentLineIndex >= 0;
	}
	
	public void addColumn(String columnName) {
		addColumn(columnName, null, false);
	}
	
	public void addColumn(String columnName, String columnNameLine2, boolean alignRight) {
		addColumn(columnName, columnNameLine2, alignRight, null);
	}
	
	public void addColumn(String columnName, String columnNameLine2, boolean alignRight, String farbe) {
		columnNames.get(0).add(columnName);
		columnColors.add(farbe);
		
		// Zweite Zeile einfügen
		if (columnNameLine2 != null && columnNames.size() == 1)
			columnNames.add(new ArrayList<String>());
		
		if (columnNames.size() > 1) {
			while (columnNames.get(1).size() < columnNames.get(0).size() - 1)
				columnNames.get(1).add("");
		}
		
		if (columnNameLine2 != null)
			columnNames.get(1).add(columnNameLine2);
		else if (columnNames.size() > 1)
			columnNames.get(1).add("");
		
		columnRightAligned.add(alignRight);
		
		// Alle bestehenden Zeilen auf die Header-Länge bringen
		for (List<String> line : content) {
			while (line.size() < columnNames.get(0).size())
				line.add("");
		}
	}
	
	public void dropColumn(int index) {
		columnNames.get(0).remove(index);
		columnColors.remove(index);
		
		if (columnNames.size() > 1)
			columnNames.get(1).remove(index);
		
		columnRightAligned.remove(index);
		
		// Alle bestehenden Zeilen auf die Header-Länge bringen
		for (List<String> line : content)
			line.remove(index);
	}
	
	public void setAlignmentForColumn(int columnIndex, boolean alignRight) throws Exception {
		if (columnIndex < 0 || columnIndex >= columnNames.get(0).size())
			throw new Exception("Ungültige Spalten-Indexangabe: " + columnIndex);
		
		columnRightAligned.set(columnIndex, alignRight);
	}
	
	public void startNewLine() {
		startNewLine(null);
	}
	
	public void startNewLine(String farbe) {
		content.add(new ArrayList<String>());
		lineColors.add(farbe);
		currentLineIndex++;
		currentColumnIndex = 0;
		
		// Neue Zeile auf die Header-Länge bringen
		while (content.get(currentLineIndex).size() < columnNames.get(0).size())
			content.get(currentLineIndex).add("");
	}
	
	public void addValueToCurrentLine(int value) throws Exception {
		addValueToCurrentLine(Integer.toString(value));
	}
	
	public void addValueToCurrentLine(String value) throws Exception {
		if (currentLineIndex == -1)
			throw new Exception("Keine Zeile zum Erweitern angelegt");
		else if (content.get(currentLineIndex).size() <= currentColumnIndex)
			throw new Exception("Versuch in eine Spalte zu Schreiben die noch nicht angelegt wurde");
		
		if (value == null) value = "";
		content.get(currentLineIndex).set(currentColumnIndex, value);
		currentColumnIndex++;
	}
	
	@Override
	public String toString() {
		List<StringBuilder> columnNamesLines = new ArrayList<StringBuilder>();
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			columnNamesLines.add(new StringBuilder());
		}
		
		List<StringBuilder> contentLines = new ArrayList<StringBuilder>();
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			contentLines.add(new StringBuilder());
		}
		
		//Über alle Spalten iterieren
		for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
			int size = getMaxSizeOfColumn(columnIndex);
			
			for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
				if (columnIndex > 0)
					columnNamesLines.get(lineIndex).append(" ");
				
				if (columnRightAligned.get(columnIndex))
					columnNamesLines.get(lineIndex).append(Utilities.trimStringToLengthAlignedRight(columnNames.get(lineIndex).get(columnIndex), size));
				else
					columnNamesLines.get(lineIndex).append(Utilities.trimStringToLengthAlignedLeft(columnNames.get(lineIndex).get(columnIndex), size));
			}
			
			//Über alle Zeilen iterieren
			for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
				if (columnIndex > 0)
					contentLines.get(lineIndex).append(" ");
				
				if (columnRightAligned.get(columnIndex))
					contentLines.get(lineIndex).append(Utilities.trimStringToLengthAlignedRight(content.get(lineIndex).get(columnIndex), size));
				else
					contentLines.get(lineIndex).append(Utilities.trimStringToLengthAlignedLeft(content.get(lineIndex).get(columnIndex), size));
			}
		}
		
		StringBuilder text = new StringBuilder();
		for (int lineIndex = 0 ; lineIndex < columnNamesLines.size(); lineIndex++) {
			text.append(columnNamesLines.get(lineIndex) + "\n");
		}
		//Headerline abtrennen
		text.append(Utilities.replicateString("-", columnNamesLines.get(0).length()) + "\n");
		for (int lineIndex = 0 ; lineIndex < contentLines.size(); lineIndex++) {
			text.append(contentLines.get(lineIndex) + "\n");
		}
		
		return text.toString();
	}
	
	public String toCsvString() {
		StringBuilder csvText = new StringBuilder();
		
		//Zuerst die Columnheader wegschreiben
		//Über alle HeaderZeilen iterieren
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			//Über alle Spalten iterieren
			for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
				csvText.append("\"" + columnNames.get(lineIndex).get(columnIndex) + "\";");
			}
			csvText.append("\n");
		}
		
		//Dann den Datenzeileninhalt wegschreiben
		//Über alle Zeilen iterieren
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			//Über alle Spalten iterieren
			for (int columnIndex = 0 ; columnIndex < content.get(0).size(); columnIndex++) {
				csvText.append("\"" + content.get(lineIndex).get(columnIndex) + "\";");
			}
			csvText.append("\n");
		}
		
		return csvText.toString();
	}
	
	public String toCsvStringWithMetaData() {
		StringBuilder csvText = new StringBuilder();
		
		//Zuerst die Columnheader wegschreiben
		//Über alle HeaderZeilen iterieren
		for (int lineIndex = 0; lineIndex < columnNames.size(); lineIndex++) {
			csvText.append("\"" + METADATA_HEADER + "\";");
			
			//Über alle Spalten iterieren
			for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
				csvText.append("\"" + columnNames.get(lineIndex).get(columnIndex) + "\";");
			}
			csvText.append("\n");
		}
		
		csvText.append("\"" + METADATA_FARBE + "\";");
		for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
			csvText.append("\"" + columnColors.get(columnIndex) + "\";");
		}
		csvText.append("\n");
		
		csvText.append("\"" + METADATA_RIGHTALIGNMENT + "\";");
		for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
			csvText.append("\"" + columnRightAligned.get(columnIndex) + "\";");
		}
		csvText.append("\n");
		
		//Dann den Datenzeileninhalt wegschreiben
		//Über alle Zeilen iterieren
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			csvText.append("\"" + lineColors.get(lineIndex) + "\";");
			
			//Über alle Spalten iterieren
			for (int columnIndex = 0 ; columnIndex < content.get(0).size(); columnIndex++) {
				//Führendes Apostroph abschneiden
				String cellContentString = content.get(lineIndex).get(columnIndex);
				if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'"))
					cellContentString = cellContentString.substring(1);
				csvText.append("\"" + cellContentString + "\";");
			}
			csvText.append("\n");
		}
		
		return csvText.toString();
	}
	
	public static String getHtmlHeader(String title) {
		return getHtmlHeader(title, true, null);
	}
	
	public static String getHtmlHeader(String title, boolean mitZeit) {
		return getHtmlHeader(title, mitZeit, null);
	}
	
	public static String getHtmlHeader(String title, boolean mitZeit, String inlineCssStyleSheet) {
		return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
			+ "<html>\n"
			+ "<head>\n<title>" + title + "</title>\n"
			+ (StringUtils.isEmpty(inlineCssStyleSheet) ? "" : inlineCssStyleSheet)
			+ "</head>\n"
			+ "<body>\n"
			+ "<h2>" + title + "</h2>\n"
			+ (mitZeit ? "<h4>Stand: " + DateUtils.DD_MM_YYYY_HH_MM_SS.format(new GregorianCalendar().getTime()) + "</h4>\n" : "");
	}
	
	public static String getHtmlFooter() {
		return "<body>\n" 
			+ "<html>\n";
	}

	public String toHtmlString(String tableTitle) {
		return toHtmlString(tableTitle, 0);
	}
	
	public String toHtmlString(String tableTitle, int fontsizeInPt) {
		StringBuilder htmlTableStringBuffer = new StringBuilder();
		htmlTableStringBuffer.append(HtmlUtilities.htmlEncodeString(tableTitle) + "<br><br>\n");
		if (fontsizeInPt > 0)
			htmlTableStringBuffer.append("<STYLE TYPE=\"text/css\"><!--TD{font-family: Arial; font-size: " + fontsizeInPt + "pt;}---></STYLE>");
		htmlTableStringBuffer.append("<table border='1' cellspacing='1' cellpadding='1'>\n");
		// Alternative für volle Breite: "<table border='1' width='100%' cellspacing='1' cellpadding='1'>\n");
	
		int rowcount = 0;
		
		htmlTableStringBuffer.append("<tr align='center'>");
		//Über alle Spalten iterieren
		for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
			if (columnColors.get(columnIndex) == null)
				htmlTableStringBuffer.append("<td>");
			else
				htmlTableStringBuffer.append("<td bgcolor='" + convertXlsColor2HtmlColor(columnColors.get(columnIndex)) + "'>");
			
			htmlTableStringBuffer.append(HtmlUtilities.htmlEncodeString(columnNames.get(0).get(columnIndex)).replace(" ", "&nbsp;").replace("-", "&#8209;"));
			if (columnNames.size() > 1)
				htmlTableStringBuffer.append("<br/>" + HtmlUtilities.htmlEncodeString(columnNames.get(1).get(columnIndex)).replace(" ", "&nbsp;").replace("-", "&#8209;"));
			htmlTableStringBuffer.append("</td>");
		}
		htmlTableStringBuffer.append("</tr>\n");
		
		//Über alle Zeilen iterieren
		for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
			if (lineColors.get(lineIndex) != null)
				htmlTableStringBuffer.append("<tr align='right' bgcolor='" + lineColors.get(lineIndex) + "'>");
			else if (rowcount % 2 == 0)
				htmlTableStringBuffer.append("<tr align='right' bgcolor='cornflowerblue'>");
			else
				htmlTableStringBuffer.append("<tr align='right' bgcolor='lightblue'>");
			
			//Über alle Spalten iterieren
			for (int columnIndex = 0 ; columnIndex < columnNames.get(0).size(); columnIndex++) {
				//Führendes Apostroph abschneiden
				String cellContentString = content.get(lineIndex).get(columnIndex);
				if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'"))
					cellContentString = cellContentString.substring(1);
				htmlTableStringBuffer.append("<td>" + HtmlUtilities.htmlEncodeString(cellContentString).replace(" ", "&nbsp;").replace("-", "&#8209;") + "</td>");
			}
			htmlTableStringBuffer.append("</tr>\n");

			rowcount++;
		}

		htmlTableStringBuffer.append("</table>\n");
		htmlTableStringBuffer.append("<br>\n");
		htmlTableStringBuffer.append("<br>\n");
	
		return htmlTableStringBuffer.toString();
	}
	

	public byte[] toExcelData(String sheetTitle) throws Exception {
		return toExcelData(sheetTitle, 0, 0);
	}
	
	public byte[] toExcelData(String sheetTitle, int freezeColumn, int freezeLine) throws Exception {
		BufferedOutputStream outputStream = null;
		WritableWorkbook excelWorkbook = null;
		
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			outputStream = new BufferedOutputStream(byteArrayOutputStream);
			
			WorkbookSettings settings = new WorkbookSettings();
			settings.setLocale(Locale.GERMAN);
			excelWorkbook = Workbook.createWorkbook(outputStream, settings);
			
			sheetTitle = sheetTitle.replace("/", " ").replace(":", " ").replace("[", "").replace("]", "");
			if (sheetTitle.length() > 31) // ExcelSheet-Titel ist in der Länge auf max. 31 Stellen begrenzt
				sheetTitle = sheetTitle.substring(0, 31);
			WritableSheet sheet = excelWorkbook.createSheet(sheetTitle, 1);

			// Spaltennamen füllen
			for (int spaltenIndex = 0; spaltenIndex < columnNames.get(0).size(); spaltenIndex++) {
				Colour columnHeaderColor = getColourByString(columnColors.get(spaltenIndex));

				WritableCellFormat headerFormatLine1 = new WritableCellFormat();
				headerFormatLine1.setBorder(Border.TOP, BorderLineStyle.MEDIUM);
				if (columnNames.size() > 1)
					headerFormatLine1.setBorder(Border.BOTTOM, BorderLineStyle.NONE);
				else
					headerFormatLine1.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
				headerFormatLine1.setBorder(Border.LEFT, BorderLineStyle.THIN);
				headerFormatLine1.setBorder(Border.RIGHT, BorderLineStyle.THIN);
				headerFormatLine1.setFont(new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD));
				headerFormatLine1.setBackground(columnHeaderColor);
				headerFormatLine1.setAlignment(Alignment.CENTRE);
				headerFormatLine1.setVerticalAlignment(VerticalAlignment.BOTTOM);
				headerFormatLine1.setWrap(true);
				
				sheet.addCell(new Label(spaltenIndex, 0, columnNames.get(0).get(spaltenIndex).replaceAll("\\\\n", "\n"), headerFormatLine1));
				
				if (columnNames.size() > 1) {
					WritableCellFormat headerFormatLine2 = new WritableCellFormat();
					headerFormatLine2.setBorder(Border.TOP, BorderLineStyle.NONE);
					headerFormatLine2.setBorder(Border.LEFT, BorderLineStyle.THIN);
					headerFormatLine2.setBorder(Border.RIGHT, BorderLineStyle.THIN);
					headerFormatLine2.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
					headerFormatLine2.setFont(new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD));
					headerFormatLine2.setBackground(columnHeaderColor);
					headerFormatLine2.setAlignment(Alignment.CENTRE);
					headerFormatLine2.setVerticalAlignment(VerticalAlignment.BOTTOM);
					headerFormatLine2.setWrap(true);
					
					sheet.addCell(new Label(spaltenIndex, 1, columnNames.get(1).get(spaltenIndex), headerFormatLine2));
				}
			}
			
			// Zusammenfassen von Headerfeldern in der ersten Zeile
			boolean contentChanged = true;
			String latestContent = null;
			int startColumnIndex = 0;
			for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
				if (sheet.getCell(columnIndex, 0).getContents().equals(latestContent)) {
					contentChanged = false;
				}
				else {
					// Gruppenwechsel
					if (contentChanged == false) {
						sheet.mergeCells(startColumnIndex, 0, columnIndex - 1, 0);
						if (latestContent.trim().equals("") && columnNames.size() > 1)
							sheet.mergeCells(startColumnIndex, 1, columnIndex - 1, 1);
					}
					
					latestContent = sheet.getCell(columnIndex, 0).getContents();
					contentChanged = true;
					startColumnIndex = columnIndex;
				}
			}
			
			if (freezeLine != 0) 
				sheet.getSettings().setVerticalFreeze(freezeLine);
			if (freezeColumn != 0) 
				sheet.getSettings().setHorizontalFreeze(freezeColumn);
			
			// ZeilenWerte befüllen
			for (int lineIndex = 0; lineIndex < content.size(); lineIndex++) {
				WritableCellFormat lineFormat = new WritableCellFormat();
				lineFormat.setBorder(Border.TOP, BorderLineStyle.THIN);
				lineFormat.setBorder(Border.LEFT, BorderLineStyle.THIN);
				lineFormat.setBorder(Border.RIGHT, BorderLineStyle.THIN);
				lineFormat.setBorder(Border.BOTTOM, BorderLineStyle.THIN);
				lineFormat.setFont(new WritableFont(WritableFont.ARIAL, 10));
				lineFormat.setBackground(getColourByString(lineColors.get(lineIndex)));
				lineFormat.setAlignment(Alignment.RIGHT);
				lineFormat.setVerticalAlignment(VerticalAlignment.BOTTOM);
				lineFormat.setWrap(false);
				
				for (int columnIndex = 0; columnIndex < columnNames.get(0).size(); columnIndex++) {
					String cellContentString = content.get(lineIndex).get(columnIndex);
					if (Utilities.isNumber(cellContentString))
						sheet.addCell(new Number(columnIndex, lineIndex + columnNames.size(), Integer.parseInt(cellContentString), lineFormat));
					else {
						//Führendes Apostroph abschneiden
						if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'"))
							cellContentString = cellContentString.substring(1);
						sheet.addCell(new Label(columnIndex, lineIndex + columnNames.size(), cellContentString, lineFormat));
					}
				}
			}
			
			sheet.setColumnView(0, getMaxSizeOfColumn(0));
			
			excelWorkbook.write();
			excelWorkbook.close();
			excelWorkbook = null;
			
			return byteArrayOutputStream.toByteArray();
		}
		catch (Exception e) {
			throw new Exception("Fehler beim Erstellen der Excel-Datei: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		finally {
			if (excelWorkbook != null) {
				excelWorkbook.close();
				excelWorkbook = null;
			}
			if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			}
		}
	}
	
	public static Colour getColourByString(String colourName) {
		if (colourName == null || "null".equalsIgnoreCase(colourName))
			return Colour.WHITE;
		else if ("AQUA".equalsIgnoreCase(colourName))
			return Colour.AQUA;
		else if ("AUTOMATIC".equalsIgnoreCase(colourName))
			return Colour.AUTOMATIC;
		else if ("BLACK".equalsIgnoreCase(colourName))
			return Colour.BLACK;
		else if ("BLUE".equalsIgnoreCase(colourName))
			return Colour.BLUE;
		else if ("BLUE2".equalsIgnoreCase(colourName))
			return Colour.BLUE2;
		else if ("BLUE_GREY".equalsIgnoreCase(colourName))
			return Colour.BLUE_GREY;
		else if ("BRIGHT_GREEN".equalsIgnoreCase(colourName))
			return Colour.BRIGHT_GREEN;
		else if ("BROWN".equalsIgnoreCase(colourName))
			return Colour.BROWN;
		else if ("CORAL".equalsIgnoreCase(colourName))
			return Colour.CORAL;
		else if ("DARK_BLUE".equalsIgnoreCase(colourName))
			return Colour.DARK_BLUE;
		else if ("DARK_BLUE2".equalsIgnoreCase(colourName))
			return Colour.DARK_BLUE2;
		else if ("DARK_GREEN".equalsIgnoreCase(colourName))
			return Colour.DARK_GREEN;
		else if ("DARK_PURPLE".equalsIgnoreCase(colourName))
			return Colour.DARK_PURPLE;
		else if ("DARK_RED".equalsIgnoreCase(colourName))
			return Colour.DARK_RED;
		else if ("DARK_RED2".equalsIgnoreCase(colourName))
			return Colour.DARK_RED2;
		else if ("DARK_TEAL".equalsIgnoreCase(colourName))
			return Colour.DARK_TEAL;
		else if ("DARK_YELLOW".equalsIgnoreCase(colourName))
			return Colour.DARK_YELLOW;
		else if ("DEFAULT_BACKGROUND".equalsIgnoreCase(colourName))
			return Colour.DEFAULT_BACKGROUND;
		else if ("GOLD".equalsIgnoreCase(colourName))
			return Colour.GOLD;
		else if ("GRAY_25".equalsIgnoreCase(colourName))
			return Colour.GRAY_25;
		else if ("GRAY_50".equalsIgnoreCase(colourName))
			return Colour.GRAY_50;
		else if ("GRAY_80".equalsIgnoreCase(colourName))
			return Colour.GRAY_80;
		else if ("GREEN".equalsIgnoreCase(colourName))
			return Colour.GREEN;
		else if ("GREY_25_PERCENT".equalsIgnoreCase(colourName))
			return Colour.GREY_25_PERCENT;
		else if ("GREY_40_PERCENT".equalsIgnoreCase(colourName))
			return Colour.GREY_40_PERCENT;
		else if ("GREY_50_PERCENT".equalsIgnoreCase(colourName))
			return Colour.GREY_50_PERCENT;
		else if ("GREY_80_PERCENT".equalsIgnoreCase(colourName))
			return Colour.GREY_80_PERCENT;
		else if ("ICE_BLUE".equalsIgnoreCase(colourName))
			return Colour.ICE_BLUE;
		else if ("INDIGO".equalsIgnoreCase(colourName))
			return Colour.INDIGO;
		else if ("IVORY".equalsIgnoreCase(colourName))
			return Colour.IVORY;
		else if ("LAVENDER".equalsIgnoreCase(colourName))
			return Colour.LAVENDER;
		else if ("LIGHT_BLUE".equalsIgnoreCase(colourName))
			return Colour.LIGHT_BLUE;
		else if ("LIGHT_GREEN".equalsIgnoreCase(colourName))
			return Colour.LIGHT_GREEN;
		else if ("LIGHT_ORANGE".equalsIgnoreCase(colourName))
			return Colour.LIGHT_ORANGE;
		else if ("LIGHT_TURQUOISE".equalsIgnoreCase(colourName))
			return Colour.LIGHT_TURQUOISE;
		else if ("LIGHT_TURQUOISE2".equalsIgnoreCase(colourName))
			return Colour.LIGHT_TURQUOISE2;
		else if ("LIME".equalsIgnoreCase(colourName))
			return Colour.LIME;
		else if ("OCEAN_BLUE".equalsIgnoreCase(colourName))
			return Colour.OCEAN_BLUE;
		else if ("OLIVE_GREEN".equalsIgnoreCase(colourName))
			return Colour.OLIVE_GREEN;
		else if ("ORANGE".equalsIgnoreCase(colourName))
			return Colour.ORANGE;
		else if ("PALE_BLUE".equalsIgnoreCase(colourName))
			return Colour.PALE_BLUE;
		else if ("PALETTE_BLACK".equalsIgnoreCase(colourName))
			return Colour.PALETTE_BLACK;
		else if ("PERIWINKLE".equalsIgnoreCase(colourName))
			return Colour.PERIWINKLE;
		else if ("PINK".equalsIgnoreCase(colourName))
			return Colour.PINK;
		else if ("PINK2".equalsIgnoreCase(colourName))
			return Colour.PINK2;
		else if ("PLUM".equalsIgnoreCase(colourName))
			return Colour.PLUM;
		else if ("PLUM2".equalsIgnoreCase(colourName))
			return Colour.PLUM2;
		else if ("RED".equalsIgnoreCase(colourName))
			return Colour.RED;
		else if ("ROSE".equalsIgnoreCase(colourName))
			return Colour.ROSE;
		else if ("SEA_GREEN".equalsIgnoreCase(colourName))
			return Colour.SEA_GREEN;
		else if ("SKY_BLUE".equalsIgnoreCase(colourName))
			return Colour.SKY_BLUE;
		else if ("TAN".equalsIgnoreCase(colourName))
			return Colour.TAN;
		else if ("TEAL".equalsIgnoreCase(colourName))
			return Colour.TEAL;
		else if ("TEAL2".equalsIgnoreCase(colourName))
			return Colour.TEAL2;
		else if ("TURQOISE2".equalsIgnoreCase(colourName))
			return Colour.TURQOISE2;
		else if ("TURQUOISE".equalsIgnoreCase(colourName))
			return Colour.TURQUOISE;
		else if ("UNKNOWN".equalsIgnoreCase(colourName))
			return Colour.UNKNOWN;
		else if ("VERY_LIGHT_YELLOW".equalsIgnoreCase(colourName))
			return Colour.VERY_LIGHT_YELLOW;
		else if ("VIOLET".equalsIgnoreCase(colourName))
			return Colour.VIOLET;
		else if ("VIOLET2".equalsIgnoreCase(colourName))
			return Colour.VIOLET2;
		else if ("WHITE".equalsIgnoreCase(colourName))
			return Colour.WHITE;
		else if ("YELLOW".equalsIgnoreCase(colourName))
			return Colour.YELLOW;
		else if ("YELLOW2".equalsIgnoreCase(colourName))
			return Colour.YELLOW2;
		else if ("WOCHENENDE".equalsIgnoreCase(colourName))
			//Spezielle Farbanpassung für ExcelVerdistatistik
			return Colour.LIGHT_TURQUOISE;
		else
			return Colour.WHITE;
	}
	
	private int getMaxSizeOfColumn(int index) {
		int maxSize = 0;
		for (List<String> line : columnNames) {
			maxSize = Math.max(line.get(index).length(), maxSize);
		}
		for (List<String> line : content) {
			maxSize = Math.max(line.get(index).length(), maxSize);
		}
		return maxSize;
	}
	
	public static String convertXlsColor2HtmlColor(String xlsColor) {
		if ("VERY_LIGHT_YELLOW".equalsIgnoreCase(xlsColor))
			return "yellow";
		else if ("LIGHT_GREEN".equalsIgnoreCase(xlsColor))
			return "lime";
		else if ("LAVENDER".equalsIgnoreCase(xlsColor))
			return "fuchsia";
		else if ("TAN".equalsIgnoreCase(xlsColor))
			return "fuchsia";
		else if ("TURQUOISE".equalsIgnoreCase(xlsColor))
			return "aqua";
		else if ("YELLOW".equalsIgnoreCase(xlsColor))
			return "yellow";
		else if ("LIGHT_TURQUOISE".equalsIgnoreCase(xlsColor))
			return "aqua";
		else if ("LIGHT_ORANGE".equalsIgnoreCase(xlsColor))
			return "red";
		else if ("ROSE".equalsIgnoreCase(xlsColor))
			return "fuchsia";
		else if ("WOCHENENDE".equalsIgnoreCase(xlsColor))
			//Spezielle Farbanpassung für HtmlVerdistatistik
			return "coral";
		else
			return "white";
	}
	
	public void loadCsvWithMetadataFile(byte[] csvWithMetadata) throws IOException {
		columnNames.clear();
		content.clear();
		columnColors.clear();
		lineColors.clear();
		columnRightAligned.clear();
		currentLineIndex = -1;
		currentColumnIndex = 0;

		BufferedReader fileReader = null;
				
		try {
			fileReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvWithMetadata), "UTF-8"));
			String fileLine = null;
			while ((fileLine = fileReader.readLine()) != null) {
				String[] lineParts = fileLine.split(";");
				for (int i = 0; i < lineParts.length; i++) {
					if (lineParts[i].startsWith("\""))
						lineParts[i] = lineParts[i].substring(1);
					if (lineParts[i].endsWith("\""))
						lineParts[i] = lineParts[i].substring(0, lineParts[i].length() - 1);
				}
				if (METADATA_HEADER.equals(lineParts[0])) {
					List<String> headerLine = new ArrayList<String>();
					columnNames.add(headerLine);
					for (int i = 1; i < lineParts.length; i++) {
						headerLine.add(lineParts[i]);
					}
				}
				else if (METADATA_FARBE.equals(lineParts[0])) {
					for (int i = 1; i < lineParts.length; i++) {
						if (lineParts[i].equalsIgnoreCase("null"))
							columnColors.add(null);
						else
							columnColors.add(lineParts[i]);
					}
				}
				else if (METADATA_RIGHTALIGNMENT.equals(lineParts[0])) {
					for (int i = 1; i < lineParts.length; i++) {
						if (lineParts[i].equalsIgnoreCase("true"))
							columnRightAligned.add(true);
						else
							columnRightAligned.add(false);
					}
				}
				else {
					if (lineParts[0].equalsIgnoreCase("null"))
						lineColors.add(null);
					else
						lineColors.add(lineParts[0]);
					List<String> contentLine = new ArrayList<String>();
					content.add(contentLine);
					for (int i = 1; i < lineParts.length; i++) {
						contentLine.add(lineParts[i]);
					}
				}
			}
		}
		finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			fileReader = null;
		}
	}
	
	public void keepOnlyLastRows(int numberOfLastRows) {
		while(content.size() > numberOfLastRows) {
			content.remove(0);
		}
	}
	
	public void keepOnlyFirstRows(int numberOfFirstRows) {
		while(content.size() > numberOfFirstRows) {
			content.remove(content.size() - 1);
		}
	}
	
	public int getHeaderLineCount() {
		return columnNames.size();
	}
	
	public int getColumnCount() {
		return columnNames.get(0).size();
	}
	
	public int getDataLineCount() {
		return content.size();
	}
	
	public String getHeader(int headerLineIndex, int columnIndex) throws Exception {
		if (columnNames.size() <= headerLineIndex || columnNames.get(headerLineIndex).size() <= columnIndex)
			throw new Exception("Ungültige Datenabfrage an Spaltenüberschriften Zeilenindex " + headerLineIndex + " Spaltenindex " + columnIndex);
		return columnNames.get(headerLineIndex).get(columnIndex);
	}
	
	public String getHeaderColor(int columnIndex) {
		return columnColors.get(columnIndex);
	}
	
	public String getLineColor(int lineIndex) {
		return lineColors.get(lineIndex);
	}
	
	public String getData(int lineIndex, int columnIndex) throws Exception {
		if (content.size() <= lineIndex || content.get(lineIndex).size() <= columnIndex)
			throw new Exception("Ungültige Datenabfrage an Zeilenindex " + lineIndex + " Spaltenindex " + columnIndex);
		return content.get(lineIndex).get(columnIndex);
	}
}
