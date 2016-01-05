package de.soderer.utilities;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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

public class ExcelTextTable extends TextTable {

	public ExcelTextTable() {
		columnNames = new ArrayList<List<String>>();
		columnColors = new ArrayList<String>();
		content = new ArrayList<List<String>>();
		columnRightAligned = new ArrayList<Boolean>();
		lineColors = new ArrayList<String>();

		// Erste Zeile enthält die Header
		columnNames.add(new ArrayList<String>());
	}

	public ExcelTextTable(String... columnNames) {
		this();

		for (String columnName : columnNames) {
			addColumn(columnName);
		}
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
			if (sheetTitle.length() > 31) {
				sheetTitle = sheetTitle.substring(0, 31);
			}
			WritableSheet sheet = excelWorkbook.createSheet(sheetTitle, 1);

			// Spaltennamen füllen
			for (int spaltenIndex = 0; spaltenIndex < columnNames.get(0).size(); spaltenIndex++) {
				Colour columnHeaderColor = getColourByString(columnColors.get(spaltenIndex));

				WritableCellFormat headerFormatLine1 = new WritableCellFormat();
				headerFormatLine1.setBorder(Border.TOP, BorderLineStyle.MEDIUM);
				if (columnNames.size() > 1) {
					headerFormatLine1.setBorder(Border.BOTTOM, BorderLineStyle.NONE);
				} else {
					headerFormatLine1.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
				}
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
				} else {
					// Gruppenwechsel
					if (contentChanged == false) {
						sheet.mergeCells(startColumnIndex, 0, columnIndex - 1, 0);
						if (latestContent.trim().equals("") && columnNames.size() > 1) {
							sheet.mergeCells(startColumnIndex, 1, columnIndex - 1, 1);
						}
					}

					latestContent = sheet.getCell(columnIndex, 0).getContents();
					contentChanged = true;
					startColumnIndex = columnIndex;
				}
			}

			if (freezeLine != 0) {
				sheet.getSettings().setVerticalFreeze(freezeLine);
			}
			if (freezeColumn != 0) {
				sheet.getSettings().setHorizontalFreeze(freezeColumn);
			}

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
					if (Utilities.isNumber(cellContentString)) {
						sheet.addCell(new Number(columnIndex, lineIndex + columnNames.size(), Integer.parseInt(cellContentString), lineFormat));
					} else {
						// Führendes Apostroph abschneiden
						if (cellContentString != null && cellContentString.startsWith("'") && !cellContentString.endsWith("'")) {
							cellContentString = cellContentString.substring(1);
						}
						sheet.addCell(new Label(columnIndex, lineIndex + columnNames.size(), cellContentString, lineFormat));
					}
				}
			}

			sheet.setColumnView(0, getMaxSizeOfColumn(0));

			excelWorkbook.write();
			excelWorkbook.close();
			excelWorkbook = null;

			return byteArrayOutputStream.toByteArray();
		} catch (Exception e) {
			throw new Exception("Fehler beim Erstellen der Excel-Datei: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		} finally {
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
		if (colourName == null || "null".equalsIgnoreCase(colourName)) {
			return Colour.WHITE;
		} else if ("AQUA".equalsIgnoreCase(colourName)) {
			return Colour.AQUA;
		} else if ("AUTOMATIC".equalsIgnoreCase(colourName)) {
			return Colour.AUTOMATIC;
		} else if ("BLACK".equalsIgnoreCase(colourName)) {
			return Colour.BLACK;
		} else if ("BLUE".equalsIgnoreCase(colourName)) {
			return Colour.BLUE;
		} else if ("BLUE2".equalsIgnoreCase(colourName)) {
			return Colour.BLUE2;
		} else if ("BLUE_GREY".equalsIgnoreCase(colourName)) {
			return Colour.BLUE_GREY;
		} else if ("BRIGHT_GREEN".equalsIgnoreCase(colourName)) {
			return Colour.BRIGHT_GREEN;
		} else if ("BROWN".equalsIgnoreCase(colourName)) {
			return Colour.BROWN;
		} else if ("CORAL".equalsIgnoreCase(colourName)) {
			return Colour.CORAL;
		} else if ("DARK_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.DARK_BLUE;
		} else if ("DARK_BLUE2".equalsIgnoreCase(colourName)) {
			return Colour.DARK_BLUE2;
		} else if ("DARK_GREEN".equalsIgnoreCase(colourName)) {
			return Colour.DARK_GREEN;
		} else if ("DARK_PURPLE".equalsIgnoreCase(colourName)) {
			return Colour.DARK_PURPLE;
		} else if ("DARK_RED".equalsIgnoreCase(colourName)) {
			return Colour.DARK_RED;
		} else if ("DARK_RED2".equalsIgnoreCase(colourName)) {
			return Colour.DARK_RED2;
		} else if ("DARK_TEAL".equalsIgnoreCase(colourName)) {
			return Colour.DARK_TEAL;
		} else if ("DARK_YELLOW".equalsIgnoreCase(colourName)) {
			return Colour.DARK_YELLOW;
		} else if ("DEFAULT_BACKGROUND".equalsIgnoreCase(colourName)) {
			return Colour.DEFAULT_BACKGROUND;
		} else if ("GOLD".equalsIgnoreCase(colourName)) {
			return Colour.GOLD;
		} else if ("GRAY_25".equalsIgnoreCase(colourName)) {
			return Colour.GRAY_25;
		} else if ("GRAY_50".equalsIgnoreCase(colourName)) {
			return Colour.GRAY_50;
		} else if ("GRAY_80".equalsIgnoreCase(colourName)) {
			return Colour.GRAY_80;
		} else if ("GREEN".equalsIgnoreCase(colourName)) {
			return Colour.GREEN;
		} else if ("GREY_25_PERCENT".equalsIgnoreCase(colourName)) {
			return Colour.GREY_25_PERCENT;
		} else if ("GREY_40_PERCENT".equalsIgnoreCase(colourName)) {
			return Colour.GREY_40_PERCENT;
		} else if ("GREY_50_PERCENT".equalsIgnoreCase(colourName)) {
			return Colour.GREY_50_PERCENT;
		} else if ("GREY_80_PERCENT".equalsIgnoreCase(colourName)) {
			return Colour.GREY_80_PERCENT;
		} else if ("ICE_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.ICE_BLUE;
		} else if ("INDIGO".equalsIgnoreCase(colourName)) {
			return Colour.INDIGO;
		} else if ("IVORY".equalsIgnoreCase(colourName)) {
			return Colour.IVORY;
		} else if ("LAVENDER".equalsIgnoreCase(colourName)) {
			return Colour.LAVENDER;
		} else if ("LIGHT_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.LIGHT_BLUE;
		} else if ("LIGHT_GREEN".equalsIgnoreCase(colourName)) {
			return Colour.LIGHT_GREEN;
		} else if ("LIGHT_ORANGE".equalsIgnoreCase(colourName)) {
			return Colour.LIGHT_ORANGE;
		} else if ("LIGHT_TURQUOISE".equalsIgnoreCase(colourName)) {
			return Colour.LIGHT_TURQUOISE;
		} else if ("LIGHT_TURQUOISE2".equalsIgnoreCase(colourName)) {
			return Colour.LIGHT_TURQUOISE2;
		} else if ("LIME".equalsIgnoreCase(colourName)) {
			return Colour.LIME;
		} else if ("OCEAN_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.OCEAN_BLUE;
		} else if ("OLIVE_GREEN".equalsIgnoreCase(colourName)) {
			return Colour.OLIVE_GREEN;
		} else if ("ORANGE".equalsIgnoreCase(colourName)) {
			return Colour.ORANGE;
		} else if ("PALE_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.PALE_BLUE;
		} else if ("PALETTE_BLACK".equalsIgnoreCase(colourName)) {
			return Colour.PALETTE_BLACK;
		} else if ("PERIWINKLE".equalsIgnoreCase(colourName)) {
			return Colour.PERIWINKLE;
		} else if ("PINK".equalsIgnoreCase(colourName)) {
			return Colour.PINK;
		} else if ("PINK2".equalsIgnoreCase(colourName)) {
			return Colour.PINK2;
		} else if ("PLUM".equalsIgnoreCase(colourName)) {
			return Colour.PLUM;
		} else if ("PLUM2".equalsIgnoreCase(colourName)) {
			return Colour.PLUM2;
		} else if ("RED".equalsIgnoreCase(colourName)) {
			return Colour.RED;
		} else if ("ROSE".equalsIgnoreCase(colourName)) {
			return Colour.ROSE;
		} else if ("SEA_GREEN".equalsIgnoreCase(colourName)) {
			return Colour.SEA_GREEN;
		} else if ("SKY_BLUE".equalsIgnoreCase(colourName)) {
			return Colour.SKY_BLUE;
		} else if ("TAN".equalsIgnoreCase(colourName)) {
			return Colour.TAN;
		} else if ("TEAL".equalsIgnoreCase(colourName)) {
			return Colour.TEAL;
		} else if ("TEAL2".equalsIgnoreCase(colourName)) {
			return Colour.TEAL2;
		} else if ("TURQOISE2".equalsIgnoreCase(colourName)) {
			return Colour.TURQOISE2;
		} else if ("TURQUOISE".equalsIgnoreCase(colourName)) {
			return Colour.TURQUOISE;
		} else if ("UNKNOWN".equalsIgnoreCase(colourName)) {
			return Colour.UNKNOWN;
		} else if ("VERY_LIGHT_YELLOW".equalsIgnoreCase(colourName)) {
			return Colour.VERY_LIGHT_YELLOW;
		} else if ("VIOLET".equalsIgnoreCase(colourName)) {
			return Colour.VIOLET;
		} else if ("VIOLET2".equalsIgnoreCase(colourName)) {
			return Colour.VIOLET2;
		} else if ("WHITE".equalsIgnoreCase(colourName)) {
			return Colour.WHITE;
		} else if ("YELLOW".equalsIgnoreCase(colourName)) {
			return Colour.YELLOW;
		} else if ("YELLOW2".equalsIgnoreCase(colourName)) {
			return Colour.YELLOW2;
		} else if ("WOCHENENDE".equalsIgnoreCase(colourName)) {
			// Spezielle Farbanpassung für ExcelVerdistatistik
			return Colour.LIGHT_TURQUOISE;
		} else {
			return Colour.WHITE;
		}
	}
}
