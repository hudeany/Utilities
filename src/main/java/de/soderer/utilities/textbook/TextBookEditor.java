package de.soderer.utilities.textbook;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.soderer.utilities.BOM;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.LineReader;
import de.soderer.utilities.TextUtilities.LineBreak;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.textbook.action.CountBlankLinesAction;
import de.soderer.utilities.textbook.action.CountEmptyLinesAction;
import de.soderer.utilities.textbook.action.CountLinesContainingAction;
import de.soderer.utilities.textbook.action.CountLinesNotContainingAction;
import de.soderer.utilities.textbook.action.DropBlankLinesAction;
import de.soderer.utilities.textbook.action.DropEmptyLinesAction;
import de.soderer.utilities.textbook.action.DropLinesContainingAction;
import de.soderer.utilities.textbook.action.DropLinesNotContainingAction;
import de.soderer.utilities.textbook.action.KeepOnlyLinesContainingAction;
import de.soderer.utilities.textbook.action.KeepOnlyLinesNotContainingAction;
import de.soderer.utilities.textbook.action.TextLineAction;

public class TextBookEditor implements AutoCloseable {
	public static final long DEFAULT_MAXIMUM_PAGE_SIZE = 8192;
	public static final long DEFAULT_MAXIMUM_IN_MEMORY_PAGES = 512;

	private String interimStorageKey;

	private final File textFile;
	public long maximumPageSize;
	public long maximumInMemoryPages;
	private final Charset charset;
	private long size;
	private LocalDateTime modificationDate;
	private BOM bom = BOM.NONE;
	private LineBreak lineBreakType = LineBreak.Unknown;

	private List<Page> pages = null;

	boolean revalidateStatisticData = true;
	private long numberOfLines;
	private long numberOfPages;
	private long numberOfPagesWithNewContent = 0;
	private long numberOfPagesWithStoredNewContent = 0;

	public TextBookEditor(final File textFile, final Charset charset) throws Exception {
		this(textFile, DEFAULT_MAXIMUM_PAGE_SIZE, DEFAULT_MAXIMUM_IN_MEMORY_PAGES, charset);
	}

	public TextBookEditor(final File textFile, final long maximumPageSize, final long maximumInMemoryPages, final Charset charset) throws Exception {
		this.textFile = textFile;
		this.maximumPageSize = maximumPageSize;
		this.maximumInMemoryPages = maximumInMemoryPages;
		this.charset = charset;

		if (!textFile.exists()) {
			throw new FileNotFoundException(textFile.getCanonicalPath());
		} else if (!textFile.isFile()) {
			throw new FileNotFoundException(textFile.getCanonicalPath());
		} else if (charset == null) {
			throw new Exception("Mandatory charset parameter is missing");
		}
	}

	public TextBookEditor(final Charset charset) throws Exception {
		this(DEFAULT_MAXIMUM_PAGE_SIZE, DEFAULT_MAXIMUM_IN_MEMORY_PAGES, charset);
	}

	public TextBookEditor(final long maximumPageSize, final long maximumInMemoryPages, final Charset charset) throws Exception {
		textFile = null;
		this.maximumPageSize = maximumPageSize;
		this.maximumInMemoryPages = maximumInMemoryPages;
		this.charset = charset;
		size = -1;
		modificationDate = null;
		pages = new ArrayList<>();

		if (charset == null) {
			throw new Exception("Mandatory charset parameter is missing");
		}

		final Page startPage = new Page(this);
		startPage.setNewContentLines(new ArrayList<>());
	}

	public void read() throws Exception {
		if (pages != null) {
			throw new Exception("Read was already called before");
		}

		size = textFile.length();
		modificationDate = DateUtilities.getLocalDateTime(textFile.lastModified());

		pages = new ArrayList<>();

		Page currentPage = null;

		long index = 0;
		try (LineReader lineReader = getTextFileLineReader(textFile, charset)) {
			String nextLine;
			while ((nextLine = lineReader.readLine()) != null) {
				if (currentPage == null) {
					currentPage = new Page(this);
					currentPage.setStartFileIndex(0);
				} else if (currentPage.getAmountOfLines() == maximumPageSize) {
					currentPage.setEndFileIndex(index);
					currentPage = new Page(this);
					currentPage.setStartFileIndex(index);
				}

				currentPage.setAmountOfLines(currentPage.getAmountOfLines() + 1);

				index = index + nextLine.length();
				if (lineReader.getLatestLineBreakType() == LineBreak.Unix || lineReader.getLatestLineBreakType() == LineBreak.Mac) {
					index = index + 1;
				} else if (lineReader.getLatestLineBreakType() == LineBreak.Windows) {
					index = index + 2;
				}
			}
			if (currentPage != null) {
				currentPage.setEndFileIndex(index);
			}
			bom = lineReader.getBom();
			lineBreakType = lineReader.getGlobalLineBreakType();
		} catch (final Exception e) {
			size = 0;
			modificationDate = null;
			pages = null;
			throw e;
		}
	}

	private static LineReader getTextFileLineReader(final File textFile, final Charset charset) throws Exception {
		if (textFile.getName().toLowerCase().endsWith(".zip")) {
			final ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(textFile)), charset);
			inputStream.getNextEntry();
			return new LineReader(inputStream, charset);
		} else {
			return new LineReader(new FileInputStream(textFile), charset);
		}
	}

	private static OutputStream getTextFileOutputStream(final File textFile) throws Exception {
		if (textFile.getName().toLowerCase().endsWith(".zip")) {
			final ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(textFile)));
			outputStream.putNextEntry(new ZipEntry(textFile.getName().substring(0, textFile.getName().length() - 4)));
			return outputStream;
		} else {
			return new BufferedOutputStream(new FileOutputStream(textFile));
		}
	}

	public List<Page> getPages() {
		return pages;
	}

	public boolean hasNewContentData() {
		for (final Page page : pages) {
			if (page.hasNewContentData()) {
				return true;
			}
		}
		return false;
	}

	public void replaceLine(final long lineNumber, final String lineContent) throws Exception {
		if (lineContent != null) {
			replaceLines(lineNumber, lineNumber, Arrays.asList(new String[] { lineContent }));
		} else {
			replaceLines(lineNumber, lineNumber, null);
		}
	}

	public void replaceLines(final long startLineNumber, final long endLineNumber, final List<String> newContentLines) throws Exception {
		long startPageLineIndex = -1;
		Page startPage = null;
		long endPageLineIndex = -1;
		Page endPage = null;
		final List<Page> pagesToRemove = new ArrayList<>();

		long lineIndex = 0;
		for (final Page page : pages) {
			if (startPage == null && lineIndex + page.getAmountOfLines() >= startLineNumber) {
				startPageLineIndex = lineIndex;
				startPage = page;
				if (lineIndex + page.getAmountOfLines() >= endLineNumber) {
					break;
				}
			} else if (startPage != null) {
				if (lineIndex + page.getAmountOfLines() <= endLineNumber) {
					pagesToRemove.add(page);
				} else if (lineIndex + page.getAmountOfLines() > endLineNumber) {
					endPageLineIndex = lineIndex;
					endPage = page;
					break;
				}
			}
			lineIndex += page.getAmountOfLines();
		}

		if (startPage == null) {
			throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Cannot find start in data");
		} else if (startPageLineIndex + startPage.getAmountOfLines() >= endLineNumber) {
			// End line lies within start page also
			if (endPage != null) {
				throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Mistake in finding end data");
			}

			// Process start page
			List<String> startPageLines = startPage.getNewContentLines();
			if (startPageLines == null) {
				startPageLines = readTextFileLines(startPage.getStartFileIndex(), startPage.getEndFileIndex());
			}
			for (int i = startPageLines.size() - 1; i >= 0; i--) {
				if (startLineNumber <= startPageLineIndex + i + 1 && startPageLineIndex + i + 1 <= endLineNumber) {
					startPageLines.remove(i);
				}
			}
			if (newContentLines != null) {
				startPageLines.addAll((int) (startLineNumber - 1 - startPageLineIndex), addMissingLineBreaks(newContentLines));
				startPage.setNewContentLines(startPageLines);
			}

			revalidateStatisticData = true;
		} else {
			// End line lies in end page
			if (endPage == null) {
				throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Mistake in finding end data page");
			}

			// Process start page
			List<String> startPageLines = startPage.getNewContentLines();
			if (startPageLines == null) {
				startPageLines = readTextFileLines(startPage.getStartFileIndex(), startPage.getEndFileIndex());
			}
			for (int i = startPageLines.size() - 1; i >= 0; i--) {
				if (startLineNumber <= startPageLineIndex + i + 1) {
					startPageLines.remove(i);
				}
			}
			if (newContentLines != null) {
				startPageLines.addAll(addMissingLineBreaks(newContentLines));
				startPage.setNewContentLines(startPageLines);
			}

			// Process end page
			List<String> endPageLines = endPage.getNewContentLines();
			if (endPageLines == null) {
				endPageLines = readTextFileLines(endPage.getStartFileIndex(), endPage.getEndFileIndex());
			}
			for (int i = endPageLines.size() - 1; i >= 0; i--) {
				if (endPageLineIndex + i + 1 <= endLineNumber) {
					endPageLines.remove(i);
				}
			}

			for (final Page pageToRemove : pagesToRemove) {
				pageToRemove.clear();
				removePage(pageToRemove);
			}

			revalidateStatisticData = true;
		}
	}

	public void insertLine(final long insertAfterLineNumber, final String lineContent) throws Exception {
		if (lineContent != null) {
			insertLines(insertAfterLineNumber, Arrays.asList(new String[] { lineContent }));
		} else {
			return;
		}
	}

	public void insertLines(final long insertAfterLineNumber, final List<String> contentLines) throws Exception {
		long lineIndex = 0;
		for (final Page page : pages) {
			if (lineIndex + page.getAmountOfLines() >= insertAfterLineNumber) {
				if (contentLines != null && contentLines.size() > 0) {
					List<String> pageLines = page.getNewContentLines();
					if (pageLines == null) {
						pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
					}
					pageLines.addAll((int) (insertAfterLineNumber - lineIndex), addMissingLineBreaks(contentLines));
					page.setNewContentLines(pageLines);
				}
				return;
			}
			lineIndex += page.getAmountOfLines();
		}
		throw new Exception("Error while inserting lines " + insertAfterLineNumber + ": Cannot find start in data");
	}

	private List<String> addMissingLineBreaks(final List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if (line.endsWith(LineBreak.Windows.toString())) {
				if (lineBreakType == LineBreak.Mac) {
					lines.set(i, line.substring(0, line.length() - 2) + LineBreak.Mac.toString());
				} else if (lineBreakType == LineBreak.Unix) {
					lines.set(i, line.substring(0, line.length() - 2) + LineBreak.Unix.toString());
				} else if (lineBreakType == LineBreak.Unknown) {
					lineBreakType = LineBreak.Windows;
				}
			} else if (line.endsWith(LineBreak.Mac.toString())) {
				if (lineBreakType == LineBreak.Windows) {
					lines.set(i, line.substring(0, line.length() - 1) + LineBreak.Windows.toString());
				} else if (lineBreakType == LineBreak.Unix) {
					lines.set(i, line.substring(0, line.length() - 1) + LineBreak.Unix.toString());
				} else if (lineBreakType == LineBreak.Unknown) {
					lineBreakType = LineBreak.Mac;
				}
			} else if (line.endsWith(LineBreak.Unix.toString())) {
				if (lineBreakType == LineBreak.Windows) {
					lines.set(i, line.substring(0, line.length() - 1) + LineBreak.Windows.toString());
				} else if (lineBreakType == LineBreak.Mac) {
					lines.set(i, line.substring(0, line.length() - 1) + LineBreak.Mac.toString());
				} else if (lineBreakType == LineBreak.Unknown) {
					lineBreakType = LineBreak.Unix;
				}
			} else {
				if (lineBreakType == LineBreak.Windows) {
					lines.set(i, line + LineBreak.Windows.toString());
				} else if (lineBreakType == LineBreak.Mac) {
					lines.set(i, line + LineBreak.Mac.toString());
				} else if (lineBreakType == LineBreak.Unix) {
					lines.set(i, line + LineBreak.Unix.toString());
				} else if (lineBreakType == LineBreak.Unknown) {
					lineBreakType = LineBreak.Unix;
					lines.set(i, line + LineBreak.Unix.toString());
				} else {
					lines.set(i, line + LineBreak.Unix.toString());
				}
			}
		}
		return lines;
	}

	public String getLine(final long lineNumber) throws Exception {
		return getLines(lineNumber, lineNumber).get(0);
	}

	public List<String> getLines(final long startLineIndex, final long endLineIndex) throws Exception {
		long startPageLineIndex = -1;
		Page startPage = null;
		long endPageLineIndex = -1;
		Page endPage = null;
		final List<Page> interPages = new ArrayList<>();
		final List<String> returnList = new ArrayList<>();

		long lineIndex = 0;
		for (final Page page : pages) {
			if (startPage == null && lineIndex + page.getAmountOfLines() >= startLineIndex) {
				startPageLineIndex = lineIndex;
				startPage = page;
				if (lineIndex + page.getAmountOfLines() >= endLineIndex) {
					break;
				}
			} else if (startPage != null) {
				if (lineIndex + page.getAmountOfLines() > endLineIndex) {
					endPageLineIndex = lineIndex;
					endPage = page;
					break;
				} else {
					interPages.add(page);
				}
			}
			lineIndex += page.getAmountOfLines();
		}

		if (startPage == null) {
			throw new Exception("Error while searching lines " + startLineIndex + " to " + endLineIndex + ": Cannot find start in data");
		} else if (startPageLineIndex + startPage.getAmountOfLines() >= endLineIndex) {
			// End line lies within start page also
			if (endPage != null) {
				throw new Exception("Error while searching lines " + startLineIndex + " to " + endLineIndex + ": Mistake in finding end data");
			}

			// Process start page
			List<String> startPageLines = startPage.getNewContentLines();
			if (startPageLines == null) {
				startPageLines = readTextFileLines(startPage.getStartFileIndex(), startPage.getEndFileIndex());
			}
			for (int i = 0; i < startPageLines.size(); i++) {
				if (startLineIndex <= startPageLineIndex + i && startPageLineIndex + i <= endLineIndex) {
					returnList.add(startPageLines.get(i));
				}
			}
		} else {
			// End line lies in end page
			if (endPage == null) {
				throw new Exception("Error while searching lines " + startLineIndex + " to " + endLineIndex + ": Mistake in finding end data page");
			}

			// Process start page
			List<String> startPageLines = startPage.getNewContentLines();
			if (startPageLines == null) {
				startPageLines = readTextFileLines(startPage.getStartFileIndex(), startPage.getEndFileIndex());
			}
			for (int i = 0; i < startPageLines.size(); i++) {
				if (startLineIndex <= startPageLineIndex + i) {
					returnList.add(startPageLines.get(i));
				}
			}

			for (final Page page : interPages) {
				List<String> pageLines = page.getNewContentLines();
				if (pageLines == null) {
					pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
				}
				returnList.addAll(pageLines);
			}

			// Process end page
			List<String> endPageLines = endPage.getNewContentLines();
			if (endPageLines == null) {
				endPageLines = readTextFileLines(endPage.getStartFileIndex(), endPage.getEndFileIndex());
			}
			for (int i = 0; i < endPageLines.size(); i++) {
				if (endPageLineIndex + i <= endLineIndex) {
					returnList.add(endPageLines.get(i));
				}
			}
		}

		return returnList;
	}

	public TextPosition search(final String searchText) throws Exception {
		return search(searchText, false, false, null, null);
	}

	public TextPosition search(final String searchText, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return null;
			} else if (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1) {
				List<String> pageLines = page.getNewContentLines();
				if (pageLines == null) {
					pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
				}
				for (int pageLineIndex = 0; pageLineIndex < pageLines.size(); pageLineIndex++) {
					String linePart;
					if (start != null && pageLineIndex + lineIndex == start.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), start.getColumn());
					} else if (end != null && pageLineIndex + lineIndex == end.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), 0, end.getColumn());
					} else {
						linePart = pageLines.get(pageLineIndex);
					}

					if (pattern != null) {
						final Matcher matcher = pattern.matcher(linePart);
						if (matcher.matches()) {
							return new TextPosition(pageLineIndex + lineIndex + 1, matcher.start());
						}
					} else {
						if (caseSensitive && linePart.contains(searchText)) {
							return new TextPosition(pageLineIndex + lineIndex + 1, linePart.indexOf(searchText));
						} else if (!caseSensitive && linePart.toLowerCase().contains(searchText.toLowerCase())) {
							return new TextPosition(pageLineIndex + lineIndex + 1, linePart.toLowerCase().indexOf(searchText.toLowerCase()));
						}
					}
				}
			}
			lineIndex += previousAmountOfLines;
		}
		return null;
	}

	public long searchCount(final String searchText) throws Exception {
		return searchCount(searchText, false, false, null, null);
	}

	public long searchCount(String searchText, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		long numberOfFindings = 0;

		if (!caseSensitive) {
			searchText = searchText.toLowerCase();
		}

		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return numberOfFindings;
			} else if (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1) {
				List<String> pageLines = page.getNewContentLines();
				if (pageLines == null) {
					pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
				}
				for (int pageLineIndex = 0; pageLineIndex < pageLines.size(); pageLineIndex++) {
					String linePart;
					if (start != null && pageLineIndex + lineIndex == start.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), start.getColumn());
					} else if (end != null && pageLineIndex + lineIndex == end.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), 0, end.getColumn());
					} else {
						linePart = pageLines.get(pageLineIndex);
					}

					if (pattern != null) {
						final Matcher matcher = pattern.matcher(linePart);
						while (matcher.find()) {
							numberOfFindings++;
						}
					} else if (caseSensitive) {
						int lastStart = linePart.indexOf(searchText);
						int lastEnd = 0;
						while (lastStart > -1) {
							lastEnd = lastStart + searchText.length();
							lastStart = linePart.indexOf(searchText, lastEnd);
							numberOfFindings++;
						}
					} else {
						searchText = searchText.toLowerCase();
						final String linePartLower = linePart.toLowerCase();
						int lastStart = linePartLower.indexOf(searchText);
						int lastEnd = 0;
						while (lastStart > -1) {
							lastEnd = lastStart + searchText.length();
							lastStart = linePartLower.indexOf(searchText, lastEnd);
							numberOfFindings++;
						}
					}
				}
			}
			lineIndex += previousAmountOfLines;
		}
		return numberOfFindings;
	}

	/**
	 * Multiline replacements are NOT available
	 */
	public long searchAndReplace(final String searchText, final String replaceText) throws Exception {
		return searchAndReplace(searchText, replaceText, false, false, false, null, null);
	}

	/**
	 * Multiline replacements are NOT available
	 */
	public long searchAndReplace(String searchText, final String replaceText, final boolean onlyReplaceFirstOccurrence, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		long numberOfReplacements = 0;

		if (!caseSensitive) {
			searchText = searchText.toLowerCase();
		}

		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			boolean changed = false;
			if (changed && onlyReplaceFirstOccurrence) {
				break;
			}

			final long previousAmountOfLines = page.getAmountOfLines();
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return numberOfReplacements;
			} else if (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1) {
				List<String> pageLines = page.getNewContentLines();
				if (pageLines == null) {
					pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
				}
				for (int pageLineIndex = 0; (!changed || !onlyReplaceFirstOccurrence) && pageLineIndex < pageLines.size(); pageLineIndex++) {
					boolean changedLine = false;
					String linePart;
					if (start != null && pageLineIndex + lineIndex == start.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), start.getColumn());
					} else if (end != null && pageLineIndex + lineIndex == end.getLine() - 1) {
						linePart = Utilities.substring(pageLines.get(pageLineIndex), 0, end.getColumn());
					} else {
						linePart = pageLines.get(pageLineIndex);
					}

					final StringBuilder resultLinePart = new StringBuilder();
					if (pattern != null) {
						int lastEnd = 0;
						final Matcher matcher = pattern.matcher(linePart);
						while ((!changedLine || !onlyReplaceFirstOccurrence) && matcher.find()) {
							resultLinePart.append(linePart.substring(lastEnd, matcher.start()));
							resultLinePart.append(replaceText);
							lastEnd = matcher.end();
							changedLine = true;
							numberOfReplacements++;
						}
						if (changedLine) {
							resultLinePart.append(linePart.substring(lastEnd));
						}
					} else if (caseSensitive) {
						int lastStart = linePart.indexOf(searchText);
						int lastEnd = 0;
						while ((!changedLine || !onlyReplaceFirstOccurrence) && lastStart > -1) {
							resultLinePart.append(linePart.substring(lastEnd, lastStart));
							lastEnd = lastStart + searchText.length();
							resultLinePart.append(replaceText);
							lastStart = linePart.indexOf(searchText, lastEnd);
							changedLine = true;
							numberOfReplacements++;
						}
						if (changedLine) {
							resultLinePart.append(linePart.substring(lastEnd));
						}
					} else {
						searchText = searchText.toLowerCase();
						final String linePartLower = linePart.toLowerCase();
						int lastStart = linePartLower.indexOf(searchText);
						int lastEnd = 0;
						while ((!changedLine || !onlyReplaceFirstOccurrence) && lastStart > -1) {
							resultLinePart.append(linePart.substring(lastEnd, lastStart));
							lastEnd = lastStart + searchText.length();
							resultLinePart.append(replaceText);
							lastStart = linePartLower.indexOf(searchText, lastEnd);
							changedLine = true;
							numberOfReplacements++;
						}
						if (changedLine) {
							resultLinePart.append(linePart.substring(lastEnd));
						}
					}

					if (changedLine) {
						if (start != null && pageLineIndex + lineIndex == start.getLine() - 1) {
							pageLines.set(pageLineIndex, Utilities.substring(pageLines.get(pageLineIndex), 0, start.getColumn()) + resultLinePart.toString());
						} else if (end != null && pageLineIndex + lineIndex == end.getLine() - 1) {
							pageLines.set(pageLineIndex, resultLinePart.toString() + Utilities.substring(pageLines.get(pageLineIndex), end.getColumn()));
						} else {
							pageLines.set(pageLineIndex, resultLinePart.toString());
						}
						changed = true;
					}
				}

				if (changed) {
					page.setNewContentLines(pageLines);
				}
			}
			lineIndex += previousAmountOfLines;
		}
		return numberOfReplacements;
	}

	public void write() throws Exception {
		write(null);
	}

	public void write(final File destinationFile) throws Exception {
		if (pages == null) {
			throw new Exception("No data was available to write");
		} else if (textFile != null && (size != textFile.length() || !modificationDate.equals(DateUtilities.getLocalDateTime(textFile.lastModified())))) {
			throw new Exception("The sourcefile has been tampered meanwile after read");
		} else if (!hasNewContentData() && textFile != null && textFile.getCanonicalPath().equals(destinationFile.getCanonicalPath())) {
			return;
		} else if (destinationFile != null && destinationFile.exists()) {
			throw new Exception("Destination file '" + destinationFile.getCanonicalPath() + "' already exists");
		}

		File interimTextBookFile = null;
		try {
			if (destinationFile == null) {
				interimTextBookFile = File.createTempFile("InterimTextBookFile", null);
			} else {
				interimTextBookFile = destinationFile;
			}

			if (textFile != null) {
				try (LineReader lineReader = getTextFileLineReader(textFile, charset);
						OutputStream outputStream = getTextFileOutputStream(interimTextBookFile)) {
					outputStream.write(bom.getBytes());
					long currentReadIndex = 0;
					for (final Page page : pages) {
						final List<String> newContentLines = page.getNewContentLines();
						if (newContentLines != null) {
							final String newDataString = Utilities.join(newContentLines, null);
							outputStream.write(newDataString.getBytes(charset));
							page.clear();
						} else {
							lineReader.skipChars(page.getStartFileIndex() - currentReadIndex);
							currentReadIndex += page.getStartFileIndex() - currentReadIndex;
							int nextByte;
							final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
							while (currentReadIndex <= page.getEndFileIndex() && (nextByte = lineReader.readChar()) != -1) {
								buffer.write(nextByte);
								if (buffer.size() == 4096) {
									outputStream.write(buffer.toByteArray());
									buffer.reset();
								}
								currentReadIndex++;
							}
							if (buffer.size() > 0) {
								outputStream.write(buffer.toByteArray());
								buffer.reset();
							}
						}
					}
				}
			} else {
				try (OutputStream outputStream = getTextFileOutputStream(interimTextBookFile)) {
					for (final Page page : pages) {
						final List<String> newContentLines = page.getNewContentLines();
						if (newContentLines != null) {
							final String newDataString = Utilities.join(newContentLines, null);
							outputStream.write(newDataString.getBytes(charset));
							page.clear();
						} else {
							throw new Exception("Invalid missing page data to write");
						}
					}
				}
			}

			if (destinationFile == null && !textFile.delete()) {
				throw new Exception("Cannot remove the source file: " + textFile.getCanonicalPath());
			}
		} catch (final Exception e) {
			if (interimTextBookFile != null && interimTextBookFile.exists()) {
				interimTextBookFile.delete();
			}
			throw e;
		}
		if (destinationFile == null) {
			interimTextBookFile.renameTo(textFile);
		}
	}

	private List<String> readTextFileLines(final long startFileIndex, final long endFileIndex) throws Exception {
		if (size != textFile.length() || !modificationDate.equals(DateUtilities.getLocalDateTime(textFile.lastModified()))) {
			throw new Exception("The sourcefile has been tampered meanwile after read");
		}

		try (LineReader lineReader = getTextFileLineReader(textFile, charset)) {
			long bytesSkipped = 0;
			while (bytesSkipped < startFileIndex) {
				try {
					lineReader.skipChars(startFileIndex - bytesSkipped);
				} catch(final Exception e) {
					throw new Exception("Cannot skip data while reading textfile: " + textFile.getCanonicalPath(), e);
				}
				bytesSkipped += startFileIndex - bytesSkipped;
			}
			long index = startFileIndex;
			final List<String> dataLines = new ArrayList<>();
			String nextLine;
			while (index < endFileIndex && (nextLine = lineReader.readLine()) != null) {
				index = index + nextLine.length();
				if (lineReader.getLatestLineBreakType() == LineBreak.Unix) {
					nextLine += LineBreak.Unix.toString();
					index = index + 1;
				} else if (lineReader.getLatestLineBreakType() == LineBreak.Mac) {
					nextLine += LineBreak.Mac.toString();
					index = index + 1;
				} else if (lineReader.getLatestLineBreakType() == LineBreak.Windows) {
					nextLine += LineBreak.Windows.toString();
					index = index + 2;
				}

				dataLines.add(nextLine);
			}

			return dataLines;
		}
	}

	public List<String> getLinesByPage(final int pageIndex) throws Exception {
		final Page page = pages.get(pageIndex);

		final List<String> newContentLines = page.getNewContentLines();
		if (newContentLines == null) {
			return readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
		} else {
			return newContentLines;
		}
	}

	public long getNumberOfLines() {
		if (revalidateStatisticData) {
			calculateStatisticData();
		}
		return numberOfLines;
	}

	public long getNumberOfPages() {
		if (revalidateStatisticData) {
			calculateStatisticData();
			revalidateStatisticData = false;
		}
		return numberOfPages;
	}

	public void setNumberOfPagesWithNewContent(final long numberOfPagesWithNewContent) throws Exception {
		this.numberOfPagesWithNewContent = numberOfPagesWithNewContent;
		if (numberOfPagesWithNewContent > maximumInMemoryPages) {
			for (final Page page : pages) {
				page.storeInterimData();
			}
		}
	}

	public long getNumberOfPagesWithNewContent() {
		return numberOfPagesWithNewContent;
	}

	public void setNumberOfPagesWithStoredNewContent(final long numberOfPagesWithStoredNewContent) {
		this.numberOfPagesWithStoredNewContent = numberOfPagesWithStoredNewContent;
	}

	public long getNumberOfPagesWithStoredNewContent() {
		return numberOfPagesWithStoredNewContent;
	}

	private void calculateStatisticData() {
		numberOfLines = 0;
		numberOfPages = 0;
		for (final Page page : pages) {
			numberOfLines += page.getAmountOfLines();
			numberOfPages++;
		}
	}

	public long countLinesContaining(final String searchText) throws Exception {
		return countLinesContaining(searchText, true, false, 0, Long.MAX_VALUE);
	}

	public long countLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new CountLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfLines();
	}

	public long countLinesNotContaining(final String searchText) throws Exception {
		return countLinesNotContaining(searchText, true, false, 0, Long.MAX_VALUE);
	}

	public long countLinesNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new CountLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfLines();
	}

	public long keepOnlyLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((KeepOnlyLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new KeepOnlyLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	public long keepOnlyNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((KeepOnlyLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new KeepOnlyLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	public long dropLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new DropLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	public long dropLinesNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new DropLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	public long countEmptyLines() throws Exception {
		return countEmptyLines(0, Long.MAX_VALUE);
	}

	public long countEmptyLines(final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountEmptyLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new CountEmptyLinesAction())).getNumberOfLines();
	}

	public long countBlankLines() throws Exception {
		return countBlankLines(0, Long.MAX_VALUE);
	}

	public long countBlankLines(final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountBlankLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new CountBlankLinesAction())).getNumberOfLines();
	}

	public long dropEmptyLines() throws Exception {
		return dropEmptyLines(0, Long.MAX_VALUE);
	}

	public long dropEmptyLines(final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropEmptyLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new DropEmptyLinesAction())).getNumberOfDroppedLines();
	}

	public long dropBlankLines() throws Exception {
		return dropBlankLines(0, Long.MAX_VALUE);
	}

	public long dropBlankLines(final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropBlankLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new DropBlankLinesAction())).getNumberOfDroppedLines();
	}

	public String getInterimStorageKey() {
		if (interimStorageKey == null) {
			interimStorageKey = Utilities.generateUUID();
		}
		return interimStorageKey;
	}

	public LineBreak getLineBreakType() {
		return lineBreakType;
	}

	public BOM getBom() {
		return bom;
	}

	public boolean removePage(final Page page) {
		revalidateStatisticData = true;
		return pages.remove(page);
	}

	private TextLineAction executeTextLineAction(final long startLineNumber, final long endLineNumber, final TextLineAction action) throws Exception {
		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (lineIndex + previousAmountOfLines > endLineNumber - 1) {
				return action;
			} else if (lineIndex + previousAmountOfLines >= startLineNumber - 1) {
				List<String> pageLines = page.getNewContentLines();
				if (pageLines == null) {
					pageLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
				}
				boolean changed = false;
				for (int pageLineIndex = pageLines.size() - 1; pageLineIndex >= 0; pageLineIndex--) {
					if (startLineNumber - 1 <= lineIndex + pageLineIndex && lineIndex + pageLineIndex <= endLineNumber - 1) {
						final boolean lineChanged = action.execute(lineIndex + pageLineIndex, pageLineIndex, pageLines, pageLines.get(pageLineIndex));
						changed = changed || lineChanged;
					}
				}
				if (changed) {
					page.setNewContentLines(pageLines);
				}
			}
			lineIndex += previousAmountOfLines;
		}
		return action;
	}

	/**
	 * Clear temporary storage data if the changed data was not persisted in a new file
	 */
	@Override
	public void close() throws Exception {
		for (final Page page : pages) {
			page.clear();
		}
	}
}
