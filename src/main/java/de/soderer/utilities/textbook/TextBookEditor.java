package de.soderer.utilities.textbook;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * Editor for large text files, which are held in pages of lines, so only the edited pages need to be kept in memory.
 * <p>
 * Unedited pages are read from the file on demand. Edited pages are kept in memory, and when there are more than
 * {@link #maximumInMemoryPages} of them, they are moved to compressed temporary files. Changes are written to the
 * file by {@link #write()}. Files with the extension ".zip" are read from and written to their single zip entry.
 * <p>
 * Lines are returned and stored including their line break. Line numbers are 1-based, except for
 * {@link #getLine(long)} and {@link #getLines(long, long)}, which use 0-based line indexes.
 */
public class TextBookEditor implements AutoCloseable {
	/**
	 * Return value of InputStream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	/**
	 * Default maximum number of lines per page.
	 */
	public static final long DEFAULT_MAXIMUM_PAGE_SIZE = 8192;
	/**
	 * Default maximum number of edited pages kept in memory before they are moved to temporary files.
	 */
	public static final long DEFAULT_MAXIMUM_IN_MEMORY_PAGES = 512;

	private String interimStorageKey;

	private final File textFile;
	/**
	 * Maximum number of lines per page when reading the file.
	 */
	public long maximumPageSize;
	/**
	 * Maximum number of edited pages kept in memory before they are moved to temporary files.
	 */
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

	/**
	 * Small bounded cache of parsed line lists for unedited pages (pages whose content still
	 * lives only on disk, i.e. page.getNewContentLines() == null). Without this, every single
	 * read access to a line - e.g. every getLine() call SWT's StyledText makes while painting or
	 * scrolling - would re-read and re-parse the entire page from disk from scratch, only to
	 * extract one line and throw the rest away again. That turned scrolling into a multi-second
	 * operation for large files. A handful of cached pages is enough to cover normal scrolling
	 * (including back-and-forth across a page boundary) without meaningfully increasing memory
	 * use. Edited pages are never put in here - their content already lives in memory via
	 * page.getNewContentLines() and doesn't need this cache.
	 */
	private static final int READ_PAGE_CACHE_SIZE = 4;
	private final Map<Page, List<String>> readPageCache = new LinkedHashMap<>(READ_PAGE_CACHE_SIZE + 1, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(final Map.Entry<Page, List<String>> eldest) {
			return size() > READ_PAGE_CACHE_SIZE;
		}
	};

	/**
	 * Returns the lines of a page for read access, using the cache above for unedited pages
	 * instead of re-reading them from disk on every call. Edited pages (getNewContentLines() !=
	 * null) always return their in-memory content directly, bypassing the cache entirely.
	 */
	private List<String> getPageLinesForRead(final Page page) throws Exception {
		final List<String> newContentLines = page.getNewContentLines();
		if (newContentLines != null) {
			return newContentLines;
		}
		List<String> cachedLines = readPageCache.get(page);
		if (cachedLines == null) {
			cachedLines = readTextFileLines(page.getStartFileIndex(), page.getEndFileIndex());
			readPageCache.put(page, cachedLines);
		}
		return cachedLines;
	}

	/**
	 * Creates an editor for an existing text file with default page settings. Call {@link #read()} before accessing the content.
	 *
	 * @param textFile text file (or zip file containing it) to edit
	 * @param charset charset of the text file
	 * @throws Exception if the file does not exist or the charset is missing
	 */
	public TextBookEditor(final File textFile, final Charset charset) throws Exception {
		this(textFile, DEFAULT_MAXIMUM_PAGE_SIZE, DEFAULT_MAXIMUM_IN_MEMORY_PAGES, charset);
	}

	/**
	 * Creates an editor for an existing text file. Call {@link #read()} before accessing the content.
	 *
	 * @param textFile text file (or zip file containing it) to edit
	 * @param maximumPageSize maximum number of lines per page
	 * @param maximumInMemoryPages maximum number of edited pages kept in memory
	 * @param charset charset of the text file
	 * @throws Exception if the file does not exist or the charset is missing
	 */
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

	/**
	 * Creates an editor for new text without a file, with default page settings. The text must be written with
	 * {@link #write(File)}.
	 *
	 * @param charset charset for writing the text
	 * @throws Exception if the charset is missing
	 */
	public TextBookEditor(final Charset charset) throws Exception {
		this(DEFAULT_MAXIMUM_PAGE_SIZE, DEFAULT_MAXIMUM_IN_MEMORY_PAGES, charset);
	}

	/**
	 * Creates an editor for new text without a file. The text must be written with {@link #write(File)}.
	 *
	 * @param maximumPageSize maximum number of lines per page
	 * @param maximumInMemoryPages maximum number of edited pages kept in memory
	 * @param charset charset for writing the text
	 * @throws Exception if the charset is missing
	 */
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

	/**
	 * Scans the text file and builds the page index. Must be called once before accessing the content of a file.
	 *
	 * @throws Exception if read was called before or the file cannot be read
	 */
	public void read() throws Exception {
		if (pages != null) {
			throw new Exception("Read was already called before");
		}

		scanTextFile();
	}

	/**
	 * Re-scans textFile from scratch, discarding any pages currently held in memory or
	 * interim storage. Used when the file is known to have changed externally (e.g. a growing
	 * log file in tail-view mode, or a FileChangeWatcherThread notification) and the in-memory
	 * page index needs to be brought back in sync.
	 * <p>
	 * Any unsaved in-memory edits are discarded by this call - it is not a merge.
	 *
	 * @throws Exception if read was never called before or the file cannot be read
	 */
	public void reload() throws Exception {
		if (pages == null) {
			throw new Exception("Read was never called before");
		}
		for (final Page page : pages) {
			page.clear();
		}
		scanTextFile();
	}

	/**
	 * (Re-)scans the textFile and rebuilds the page index, size and modification date.
	 * Used by read() for the initial read, and again after an in-place write(),
	 * because writing replaces the file on disk (different byte layout), which would
	 * otherwise leave "size"/"modificationDate" and all page file-offsets stale.
	 */
	private void scanTextFile() throws Exception {
		size = textFile.length();
		modificationDate = DateUtilities.getLocalDateTime(textFile.lastModified());

		// The old Page objects (and any cached line lists keyed by them) become invalid once
		// pages is rebuilt from scratch below - drop them so stale entries can't linger.
		readPageCache.clear();

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

				index = index + nextLine.getBytes(charset).length;
				index = index + getLineBreakByteLength(lineReader.getLatestLineBreakType());
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

		numberOfPagesWithNewContent = 0;
		numberOfPagesWithStoredNewContent = 0;
		revalidateStatisticData = true;
	}

	/**
	 * Byte length of a line break in the editor's charset (e.g. 1 byte for "\n" in UTF-8,
	 * but 2 bytes for "\n" in UTF-16). Unknown/Mixed contributes 0, matching a line with no
	 * detected line break (e.g. the very last line of a file without a trailing line break).
	 */
	private int getLineBreakByteLength(final LineBreak lineBreakTypeOfLine) {
		if (lineBreakTypeOfLine == LineBreak.Unix || lineBreakTypeOfLine == LineBreak.Mac || lineBreakTypeOfLine == LineBreak.Windows) {
			return lineBreakTypeOfLine.toString().getBytes(charset).length;
		} else {
			return 0;
		}
	}

	private static LineReader getTextFileLineReader(final File textFile, final Charset charset) throws Exception {
		if (Utilities.endsWithIgnoreCase(textFile.getName(), ".zip")) {
			final ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(textFile)), charset);
			inputStream.getNextEntry();
			return new LineReader(inputStream, charset);
		} else {
			return new LineReader(new FileInputStream(textFile), charset);
		}
	}

	/**
	 * Opens the textFile (or its single zip entry) as a raw byte stream, positioned at byte 0
	 * (including any BOM). Used by write() to copy unedited page content byte-for-byte, without
	 * going through character decoding/encoding at all.
	 */
	private static InputStream getTextFileRawInputStream(final File textFile) throws Exception {
		if (Utilities.endsWithIgnoreCase(textFile.getName(), ".zip")) {
			final ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(textFile)));
			inputStream.getNextEntry();
			return inputStream;
		} else {
			return new BufferedInputStream(new FileInputStream(textFile));
		}
	}

	/**
	 * Skips exactly bytesToSkip bytes from inputStream. InputStream#skip may skip fewer bytes than
	 * requested (this is explicitly allowed by its contract, and ZipInputStream in particular often
	 * does), so this loops and falls back to reading single bytes when skip() stalls.
	 */
	private static void skipBytes(final InputStream inputStream, final long bytesToSkip) throws Exception {
		long remaining = bytesToSkip;
		while (remaining > 0) {
			final long actuallySkipped = inputStream.skip(remaining);
			if (actuallySkipped > 0) {
				remaining -= actuallySkipped;
			} else if (inputStream.read() == EOF) {
				throw new Exception("Unexpected end of textfile while skipping data");
			} else {
				remaining--;
			}
		}
	}

	/**
	 * Opens an output stream writing to outputFile. Whether the data is zipped (and the name of the zip entry)
	 * depends on targetFile, which is the file the data ends up in, possibly after moving outputFile there.
	 */
	private static OutputStream getTextFileOutputStream(final File outputFile, final File targetFile) throws Exception {
		if (Utilities.endsWithIgnoreCase(targetFile.getName(), ".zip")) {
			final ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
			outputStream.putNextEntry(new ZipEntry(targetFile.getName().substring(0, targetFile.getName().length() - 4)));
			return outputStream;
		} else {
			return new BufferedOutputStream(new FileOutputStream(outputFile));
		}
	}

	/**
	 * Returns the pages of the text.
	 *
	 * @return pages in text order, null before {@link #read()} for a file
	 */
	public List<Page> getPages() {
		return pages;
	}

	/**
	 * Checks whether any page contains unsaved changes.
	 *
	 * @return true if there are unsaved changes
	 */
	public boolean hasNewContentData() {
		for (final Page page : pages) {
			if (page.hasNewContentData()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces a line. A missing line break is added.
	 *
	 * @param lineNumber 1-based number of the line
	 * @param lineContent new content of the line, or null to remove the line
	 * @throws Exception if the line does not exist or a page cannot be read
	 */
	public void replaceLine(final long lineNumber, final String lineContent) throws Exception {
		if (lineContent != null) {
			replaceLines(lineNumber, lineNumber, Arrays.asList(new String[] { lineContent }));
		} else {
			replaceLines(lineNumber, lineNumber, null);
		}
	}

	/**
	 * Replaces a range of lines by new lines. Missing line breaks of the new lines are added.
	 *
	 * @param startLineNumber 1-based number of the first line to replace
	 * @param endLineNumber 1-based number of the last line to replace (inclusive)
	 * @param newContentLines new lines, or null to only remove the lines; the list is not changed
	 * @throws Exception if the lines do not exist or a page cannot be read
	 */
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
				if (lineIndex + page.getAmountOfLines() >= endLineNumber) {
					endPageLineIndex = lineIndex;
					endPage = page;
					break;
				} else {
					pagesToRemove.add(page);
				}
			}
			lineIndex += page.getAmountOfLines();
		}

		if (startPage == null) {
			throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Cannot find start in data");
		} else if (startPageLineIndex + startPage.getAmountOfLines() >= endLineNumber) {
			if (endPage != null) {
				throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Mistake in finding end data");
			}

			final List<String> startPageLines = new ArrayList<>(getPageLinesForRead(startPage));
			for (int i = startPageLines.size() - 1; i >= 0; i--) {
				if (startLineNumber <= startPageLineIndex + i + 1 && startPageLineIndex + i + 1 <= endLineNumber) {
					startPageLines.remove(i);
				}
			}
			if (newContentLines != null) {
				startPageLines.addAll((int) (startLineNumber - 1 - startPageLineIndex), addMissingLineBreaks(newContentLines));
			}
			startPage.setNewContentLines(startPageLines);
			readPageCache.remove(startPage);

			revalidateStatisticData = true;
		} else {
			if (endPage == null) {
				throw new Exception("Error while replacing lines " + startLineNumber + " to " + endLineNumber + ": Mistake in finding end data page");
			}

			final List<String> startPageLines = new ArrayList<>(getPageLinesForRead(startPage));
			for (int i = startPageLines.size() - 1; i >= 0; i--) {
				if (startLineNumber <= startPageLineIndex + i + 1) {
					startPageLines.remove(i);
				}
			}
			if (newContentLines != null) {
				startPageLines.addAll(addMissingLineBreaks(newContentLines));
			}
			startPage.setNewContentLines(startPageLines);
			readPageCache.remove(startPage);

			final List<String> endPageLines = new ArrayList<>(getPageLinesForRead(endPage));
			for (int i = endPageLines.size() - 1; i >= 0; i--) {
				if (endPageLineIndex + i + 1 <= endLineNumber) {
					endPageLines.remove(i);
				}
			}
			readPageCache.remove(endPage);
			if (endPageLines.isEmpty()) {
				// endLineNumber landed exactly on this page's last line (e.g. deleting/replacing
				// through the very last line of the file) - nothing of this page survives, so
				// drop it entirely instead of leaving an empty Page behind.
				endPage.clear();
				removePage(endPage);
			} else {
				endPage.setNewContentLines(endPageLines);
			}

			for (final Page pageToRemove : pagesToRemove) {
				readPageCache.remove(pageToRemove);
				pageToRemove.clear();
				removePage(pageToRemove);
			}

			revalidateStatisticData = true;
		}
	}

	/**
	 * Inserts a new line. A missing line break is added.
	 *
	 * @param insertAfterLineNumber 1-based number of the line to insert after, 0 to insert before the first line
	 * @param lineContent content of the new line, nothing is inserted for null
	 * @throws Exception if the line does not exist or a page cannot be read
	 */
	public void insertLine(final long insertAfterLineNumber, final String lineContent) throws Exception {
		if (lineContent != null) {
			insertLines(insertAfterLineNumber, Arrays.asList(new String[] { lineContent }));
		} else {
			return;
		}
	}

	/**
	 * Inserts new lines. Missing line breaks of the new lines are added.
	 *
	 * @param insertAfterLineNumber 1-based number of the line to insert after, 0 to insert before the first line
	 * @param contentLines lines to insert, may be null or empty; the list is not changed
	 * @throws Exception if the line does not exist or a page cannot be read
	 */
	public void insertLines(final long insertAfterLineNumber, final List<String> contentLines) throws Exception {
		long lineIndex = 0;
		for (final Page page : pages) {
			if (lineIndex + page.getAmountOfLines() >= insertAfterLineNumber) {
				if (contentLines != null && contentLines.size() > 0) {
					final List<String> pageLines = new ArrayList<>(getPageLinesForRead(page));
					pageLines.addAll((int) (insertAfterLineNumber - lineIndex), addMissingLineBreaks(contentLines));
					page.setNewContentLines(pageLines);
					readPageCache.remove(page);
				}
				return;
			}
			lineIndex += page.getAmountOfLines();
		}
		throw new Exception("Error while inserting lines " + insertAfterLineNumber + ": Cannot find start in data");
	}

	private List<String> addMissingLineBreaks(final List<String> linesToAdd) {
		// Work on a copy, the caller's list must not be changed
		final List<String> lines = new ArrayList<>(linesToAdd);
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

	/**
	 * Returns a line including its line break.
	 *
	 * @param lineNumber 0-based index of the line
	 * @return content of the line
	 * @throws Exception if the line does not exist or its page cannot be read
	 */
	public String getLine(final long lineNumber) throws Exception {
		return getLines(lineNumber, lineNumber).get(0);
	}

	/**
	 * Returns a range of lines including their line breaks.
	 *
	 * @param startLineIndex 0-based index of the first line
	 * @param endLineIndex 0-based index of the last line (inclusive)
	 * @return contents of the lines
	 * @throws Exception if the lines do not exist or a page cannot be read
	 */
	public List<String> getLines(final long startLineIndex, final long endLineIndex) throws Exception {
		long startPageLineIndex = -1;
		Page startPage = null;
		long endPageLineIndex = -1;
		Page endPage = null;
		final List<Page> interPages = new ArrayList<>();
		final List<String> returnList = new ArrayList<>();

		long lineIndex = 0;
		for (final Page page : pages) {
			// Line indexes are 0-based, so a page covers the indexes lineIndex to lineIndex + amountOfLines - 1
			if (startPage == null && lineIndex + page.getAmountOfLines() > startLineIndex) {
				startPageLineIndex = lineIndex;
				startPage = page;
				if (lineIndex + page.getAmountOfLines() > endLineIndex) {
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
		} else if (startPageLineIndex + startPage.getAmountOfLines() > endLineIndex) {
			if (endPage != null) {
				throw new Exception("Error while searching lines " + startLineIndex + " to " + endLineIndex + ": Mistake in finding end data");
			}

			final List<String> startPageLines = getPageLinesForRead(startPage);
			for (int i = 0; i < startPageLines.size(); i++) {
				if (startLineIndex <= startPageLineIndex + i && startPageLineIndex + i <= endLineIndex) {
					returnList.add(startPageLines.get(i));
				}
			}
		} else {
			if (endPage == null) {
				throw new Exception("Error while searching lines " + startLineIndex + " to " + endLineIndex + ": Mistake in finding end data page");
			}

			final List<String> startPageLines = getPageLinesForRead(startPage);
			for (int i = 0; i < startPageLines.size(); i++) {
				if (startLineIndex <= startPageLineIndex + i) {
					returnList.add(startPageLines.get(i));
				}
			}

			for (final Page page : interPages) {
				returnList.addAll(getPageLinesForRead(page));
			}

			final List<String> endPageLines = getPageLinesForRead(endPage);
			for (int i = 0; i < endPageLines.size(); i++) {
				if (endPageLineIndex + i <= endLineIndex) {
					returnList.add(endPageLines.get(i));
				}
			}
		}

		return returnList;
	}

	/**
	 * Searches the first occurrence of a text, ignoring the case.
	 *
	 * @param searchText text to search
	 * @return position of the first occurrence (1-based line, 0-based column), or null if not found
	 * @throws Exception if a page cannot be read
	 */
	public TextPosition search(final String searchText) throws Exception {
		return search(searchText, false, false, null, null);
	}

	/**
	 * Searches the first occurrence of a text or regular expression within one line.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param start position to start searching at (1-based line, 0-based column), or null for the beginning
	 * @param end position to stop searching at (1-based line, 0-based exclusive column), or null for the end
	 * @return position of the first occurrence (1-based line, 0-based column in the whole line), or null if not found
	 * @throws Exception if a page cannot be read
	 */
	public TextPosition search(final String searchText, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1) {
				if (end == null || lineIndex <= end.getLine() - 1) {
					final List<String> pageLines = getPageLinesForRead(page);
					for (int pageLineIndex = 0; pageLineIndex < pageLines.size() && (end == null || pageLineIndex + lineIndex <= end.getLine() - 1); pageLineIndex++) {
						final String line = pageLines.get(pageLineIndex);
						final int partStartColumn = getPartStartColumn(line, pageLineIndex + lineIndex, start);
						final String linePart = line.substring(partStartColumn, getPartEndColumn(line, pageLineIndex + lineIndex, start, end));

						if (pattern != null) {
							final Matcher matcher = pattern.matcher(linePart);
							if (matcher.find()) {
								return new TextPosition(pageLineIndex + lineIndex + 1, partStartColumn + matcher.start());
							}
						} else {
							if (caseSensitive && linePart.contains(searchText)) {
								return new TextPosition(pageLineIndex + lineIndex + 1, partStartColumn + linePart.indexOf(searchText));
							} else if (!caseSensitive && linePart.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) {
								return new TextPosition(pageLineIndex + lineIndex + 1, partStartColumn + linePart.toLowerCase(Locale.ROOT).indexOf(searchText.toLowerCase(Locale.ROOT)));
							}
						}
					}
				}
			}
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return null;
			}
			lineIndex += previousAmountOfLines;
		}
		return null;
	}

	/**
	 * Counts the occurrences of a text, ignoring the case.
	 *
	 * @param searchText text to count
	 * @return number of occurrences
	 * @throws Exception if a page cannot be read
	 */
	public long searchCount(final String searchText) throws Exception {
		return searchCount(searchText, false, false, null, null);
	}

	/**
	 * Counts the non overlapping occurrences of a text or regular expression within lines.
	 *
	 * @param searchText text or regular expression to count
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param start position to start counting at (1-based line, 0-based column), or null for the beginning
	 * @param end position to stop counting at (1-based line, 0-based exclusive column), or null for the end
	 * @return number of occurrences
	 * @throws Exception if a page cannot be read
	 */
	public long searchCount(String searchText, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		long numberOfFindings = 0;

		// Regular expressions must not be lowercased, that would change their meaning (e.g. "\D" to "\d")
		if (!caseSensitive && !regularExpression) {
			searchText = searchText.toLowerCase(Locale.ROOT);
		}

		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1) {
				if (end == null || lineIndex <= end.getLine() - 1) {
					final List<String> pageLines = getPageLinesForRead(page);
					for (int pageLineIndex = 0; pageLineIndex < pageLines.size() && (end == null || pageLineIndex + lineIndex <= end.getLine() - 1); pageLineIndex++) {
						final String line = pageLines.get(pageLineIndex);
						final String linePart = line.substring(getPartStartColumn(line, pageLineIndex + lineIndex, start), getPartEndColumn(line, pageLineIndex + lineIndex, start, end));

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
							final String linePartLower = linePart.toLowerCase(Locale.ROOT);
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
			}
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return numberOfFindings;
			}
			lineIndex += previousAmountOfLines;
		}
		return numberOfFindings;
	}

	/**
	 * Replaces all occurrences of a text, ignoring the case.
	 * Multiline replacements are NOT available
	 *
	 * @param searchText text to replace
	 * @param replaceText replacement text
	 * @return number of replacements
	 * @throws Exception if a page cannot be read
	 */
	public long searchAndReplace(final String searchText, final String replaceText) throws Exception {
		return searchAndReplace(searchText, replaceText, false, false, false, null, null);
	}

	/**
	 * Replaces occurrences of a text or regular expression within lines.
	 * Multiline replacements are NOT available. The replacement text is inserted literally, group references like "$1" are not resolved.
	 *
	 * @param searchText text or regular expression to replace
	 * @param replaceText replacement text
	 * @param onlyReplaceFirstOccurrence true to replace only the first occurrence in the whole text
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param start position to start replacing at (1-based line, 0-based column), or null for the beginning
	 * @param end position to stop replacing at (1-based line, 0-based exclusive column), or null for the end
	 * @return number of replacements
	 * @throws Exception if a page cannot be read
	 */
	public long searchAndReplace(String searchText, final String replaceText, final boolean onlyReplaceFirstOccurrence, final boolean caseSensitive, final boolean regularExpression, final TextPosition start, final TextPosition end) throws Exception {
		long numberOfReplacements = 0;

		// Regular expressions must not be lowercased, that would change their meaning (e.g. "\D" to "\d")
		if (!caseSensitive && !regularExpression) {
			searchText = searchText.toLowerCase(Locale.ROOT);
		}

		Pattern pattern = null;
		if (regularExpression) {
			pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		}

		long lineIndex = 0;
		for (final Page page : pages) {
			// Only the first occurrence of the whole text is replaced, so stop after the first changed page
			if (numberOfReplacements > 0 && onlyReplaceFirstOccurrence) {
				break;
			}
			boolean changed = false;

			final long previousAmountOfLines = page.getAmountOfLines();
			if ((end == null || lineIndex <= end.getLine() - 1) && (start == null || lineIndex + previousAmountOfLines >= start.getLine() - 1)) {
				final boolean pageWasAlreadyEdited = page.getNewContentLines() != null;
				final List<String> pageLines = pageWasAlreadyEdited ? getPageLinesForRead(page) : new ArrayList<>(getPageLinesForRead(page));
				for (int pageLineIndex = 0; (!changed || !onlyReplaceFirstOccurrence) && pageLineIndex < pageLines.size() && (end == null || pageLineIndex + lineIndex <= end.getLine() - 1); pageLineIndex++) {
					boolean changedLine = false;
					final String line = pageLines.get(pageLineIndex);
					final int partStartColumn = getPartStartColumn(line, pageLineIndex + lineIndex, start);
					final int partEndColumn = getPartEndColumn(line, pageLineIndex + lineIndex, start, end);
					final String linePart = line.substring(partStartColumn, partEndColumn);

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
						final String linePartLower = linePart.toLowerCase(Locale.ROOT);
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
						pageLines.set(pageLineIndex, line.substring(0, partStartColumn) + resultLinePart.toString() + line.substring(partEndColumn));
						changed = true;
					}
				}

				if (changed) {
					page.setNewContentLines(pageLines);
					readPageCache.remove(page);
				}
			}
			if (end != null && lineIndex + previousAmountOfLines > end.getLine() - 1) {
				return numberOfReplacements;
			}
			lineIndex += previousAmountOfLines;
		}
		return numberOfReplacements;
	}

	/**
	 * Start column of the searched part of a line: the start position's column in the start line, 0 otherwise.
	 */
	private static int getPartStartColumn(final String line, final long lineIndex, final TextPosition start) {
		if (start != null && lineIndex == start.getLine() - 1) {
			return Math.max(0, Math.min(start.getColumn(), line.length()));
		} else {
			return 0;
		}
	}

	/**
	 * End column (exclusive) of the searched part of a line: the end position's column in the end line, the line
	 * length otherwise. Never before the start column, also if start and end are in the same line.
	 */
	private static int getPartEndColumn(final String line, final long lineIndex, final TextPosition start, final TextPosition end) {
		final int partStartColumn = getPartStartColumn(line, lineIndex, start);
		if (end != null && lineIndex == end.getLine() - 1) {
			return Math.max(partStartColumn, Math.min(end.getColumn(), line.length()));
		} else {
			return line.length();
		}
	}

	/**
	 * Writes the changes into the text file. The file is replaced only after the new content was written completely.
	 * Does nothing if there are no changes.
	 *
	 * @throws Exception if the file was changed by someone else after reading, or cannot be written
	 */
	public void write() throws Exception {
		write(null);
	}

	/**
	 * Writes the text into a new file, or into the text file itself if destinationFile is null.
	 * When writing into another file, the changes stay unsaved in relation to the editor's own text file.
	 *
	 * @param destinationFile new file to write to (".zip" for a zipped text), or null to write into the text file itself
	 * @throws Exception if the destination file exists already, the text file was changed by someone else after reading, or the data cannot be written
	 */
	public void write(final File destinationFile) throws Exception {
		if (pages == null) {
			throw new Exception("No data was available to write");
		} else if (textFile != null && (size != textFile.length() || !modificationDate.equals(DateUtilities.getLocalDateTime(textFile.lastModified())))) {
			throw new Exception("The sourcefile has been tampered meanwile after read");
		} else if (!hasNewContentData() && textFile != null && (destinationFile == null || textFile.getCanonicalPath().equals(destinationFile.getCanonicalPath()))) {
			return;
		} else if (destinationFile != null && destinationFile.exists()) {
			throw new Exception("Destination file '" + destinationFile.getCanonicalPath() + "' already exists");
		} else if (destinationFile == null && textFile == null) {
			throw new Exception("Destination file is missing for text data without source file");
		}

		final boolean writeInPlace = destinationFile == null;
		final File targetFile;
		final File outputFile;
		if (destinationFile == null) {
			targetFile = textFile;
			// Interim file in the same directory, so it can replace the source file by a simple move
			outputFile = File.createTempFile("InterimTextBookFile", ".tmp", textFile.getAbsoluteFile().getParentFile());
		} else {
			targetFile = destinationFile;
			outputFile = destinationFile;
		}

		try {
			if (textFile != null) {
				try (InputStream inputStream = getTextFileRawInputStream(textFile);
						OutputStream outputStream = getTextFileOutputStream(outputFile, targetFile)) {
					outputStream.write(bom.getBytes());
					skipBytes(inputStream, bom.getBytes().length);
					long currentReadIndex = 0;
					final byte[] copyBuffer = new byte[4096];
					for (final Page page : pages) {
						final List<String> newContentLines = page.getNewContentLines();
						if (newContentLines != null) {
							final String newDataString = Utilities.join(newContentLines, null);
							outputStream.write(newDataString.getBytes(charset));
							skipBytes(inputStream, page.getEndFileIndex() - currentReadIndex);
							currentReadIndex = page.getEndFileIndex();
						} else {
							skipBytes(inputStream, page.getStartFileIndex() - currentReadIndex);
							currentReadIndex = page.getStartFileIndex();
							long bytesToCopy = page.getEndFileIndex() - currentReadIndex;
							while (bytesToCopy > 0) {
								final int bytesRead = inputStream.read(copyBuffer, 0, (int) Math.min(copyBuffer.length, bytesToCopy));
								if (bytesRead == EOF) {
									throw new Exception("Unexpected end of textfile while copying: " + textFile.getCanonicalPath());
								}
								outputStream.write(copyBuffer, 0, bytesRead);
								bytesToCopy -= bytesRead;
								currentReadIndex += bytesRead;
							}
						}
					}
				}
			} else {
				try (OutputStream outputStream = getTextFileOutputStream(outputFile, targetFile)) {
					for (final Page page : pages) {
						final List<String> newContentLines = page.getNewContentLines();
						if (newContentLines != null) {
							final String newDataString = Utilities.join(newContentLines, null);
							outputStream.write(newDataString.getBytes(charset));
						} else {
							throw new Exception("Invalid missing page data to write");
						}
					}
				}
			}

			if (writeInPlace) {
				// Replace the source file only after the new content was written completely
				Files.move(outputFile.toPath(), textFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (final Exception e) {
			// The output file is either the interim file or a destination file, which did not exist before
			if (outputFile.exists()) {
				outputFile.delete();
			}
			throw e;
		}

		if (writeInPlace) {
			// The edits are now part of the source file. When writing to another destination file, the edits stay
			// unsaved in relation to the source file of this editor.
			for (final Page page : pages) {
				page.clear();
			}
			scanTextFile();
		}
	}

	private List<String> readTextFileLines(final long startFileIndex, final long endFileIndex) throws Exception {
		if (size != textFile.length() || !modificationDate.equals(DateUtilities.getLocalDateTime(textFile.lastModified()))) {
			throw new Exception("The sourcefile has been tampered meanwile after read");
		}

		try (LineReader lineReader = openTextFileLineReaderAtByteOffset(startFileIndex)) {
			long index = startFileIndex;
			final List<String> dataLines = new ArrayList<>();
			String nextLine;
			while (index < endFileIndex && (nextLine = lineReader.readLine()) != null) {
				index = index + nextLine.getBytes(charset).length;
				index = index + getLineBreakByteLength(lineReader.getLatestLineBreakType());
				if (lineReader.getLatestLineBreakType() == LineBreak.Unix) {
					nextLine += LineBreak.Unix.toString();
				} else if (lineReader.getLatestLineBreakType() == LineBreak.Mac) {
					nextLine += LineBreak.Mac.toString();
				} else if (lineReader.getLatestLineBreakType() == LineBreak.Windows) {
					nextLine += LineBreak.Windows.toString();
				}

				dataLines.add(nextLine);
			}

			return dataLines;
		}
	}

	/**
	 * Opens a LineReader positioned exactly at the given byte offset (measured right after any BOM,
	 * matching how startFileIndex/endFileIndex are tracked in scanTextFile()).
	 *
	 * For plain (non-zip) files this seeks directly via RandomAccessFile/FileChannel instead of
	 * re-scanning the file from the beginning - turning random page access from O(fileSize) into
	 * effectively O(1) per access, which matters a lot once a file has many pages.
	 *
	 * Zip entries are deflate-compressed and cannot be seeked into directly, so those still fall
	 * back to scanning (skipping) from the start of the decompressed stream.
	 */
	private LineReader openTextFileLineReaderAtByteOffset(final long byteOffset) throws Exception {
		if (Utilities.endsWithIgnoreCase(textFile.getName(), ".zip")) {
			final LineReader lineReader = getTextFileLineReader(textFile, charset);
			try {
				lineReader.skipChars(byteOffset);
				return lineReader;
			} catch (final Exception e) {
				lineReader.close();
				throw new Exception("Cannot skip data while reading textfile: " + textFile.getCanonicalPath(), e);
			}
		} else {
			@SuppressWarnings("resource")
			final RandomAccessFile randomAccessFile = new RandomAccessFile(textFile, "r");
			try {
				randomAccessFile.seek(bom.getBytes().length + byteOffset);
				final InputStream inputStream = Channels.newInputStream(randomAccessFile.getChannel());
				return new LineReader(new InputStreamReader(inputStream, charset));
			} catch (final Exception e) {
				randomAccessFile.close();
				throw new Exception("Cannot seek to position in textfile: " + textFile.getCanonicalPath(), e);
			}
		}
	}

	/**
	 * Returns all lines of a page including their line breaks.
	 *
	 * @param pageIndex 0-based index of the page
	 * @return lines of the page, must not be changed by the caller
	 * @throws Exception if the page cannot be read
	 */
	public List<String> getLinesByPage(final int pageIndex) throws Exception {
		return getPageLinesForRead(pages.get(pageIndex));
	}

	/**
	 * Builds the full current content (including any unsaved in-memory edits) as a byte array,
	 * WITHOUT writing anything to disk and WITHOUT mutating page state.
	 * <p>
	 * It includes the BOM of the original file and is meant for use cases like Save-As or an export/preview
	 * feature, which need the current bytes without changing the editor's state.
	 *
	 * @return complete content in the editor's charset
	 * @throws Exception if no data is available or a page cannot be read
	 */
	public byte[] getFileData() throws Exception {
		if (pages == null) {
			throw new Exception("No data was available to build file data from");
		}

		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			byteArrayOutputStream.write(bom.getBytes());
			for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
				for (final String line : getLinesByPage(pageIndex)) {
					byteArrayOutputStream.write(line.getBytes(charset));
				}
			}
			return byteArrayOutputStream.toByteArray();
		}
	}

	/**
	 * Returns the current number of lines.
	 *
	 * @return number of lines
	 */
	public long getNumberOfLines() {
		if (revalidateStatisticData) {
			calculateStatisticData();
		}
		return numberOfLines;
	}

	/**
	 * Returns the current number of pages.
	 *
	 * @return number of pages
	 */
	public long getNumberOfPages() {
		if (revalidateStatisticData) {
			calculateStatisticData();
			revalidateStatisticData = false;
		}
		return numberOfPages;
	}

	/**
	 * Sets the number of pages with changes held in memory. If it exceeds {@link #maximumInMemoryPages}, edited pages
	 * are moved to temporary files. Used by {@link Page}.
	 *
	 * @param numberOfPagesWithNewContent number of pages with changes held in memory
	 * @throws Exception if a page cannot be moved to a temporary file
	 */
	public void setNumberOfPagesWithNewContent(final long numberOfPagesWithNewContent) throws Exception {
		this.numberOfPagesWithNewContent = numberOfPagesWithNewContent;
		if (numberOfPagesWithNewContent > maximumInMemoryPages) {
			for (final Page page : pages) {
				if (this.numberOfPagesWithNewContent <= maximumInMemoryPages) {
					break;
				}
				if (page.hasNewContentData()) {
					page.storeInterimData();
				}
			}
		}
	}

	/**
	 * Returns the number of pages with changes held in memory.
	 *
	 * @return number of pages with changes held in memory
	 */
	public long getNumberOfPagesWithNewContent() {
		return numberOfPagesWithNewContent;
	}

	/**
	 * Sets the number of pages with changes stored in temporary files. Used by {@link Page}.
	 *
	 * @param numberOfPagesWithStoredNewContent number of pages with changes stored in temporary files
	 */
	public void setNumberOfPagesWithStoredNewContent(final long numberOfPagesWithStoredNewContent) {
		this.numberOfPagesWithStoredNewContent = numberOfPagesWithStoredNewContent;
	}

	/**
	 * Returns the number of pages with changes stored in temporary files.
	 *
	 * @return number of pages with changes stored in temporary files
	 */
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

	/**
	 * Counts the lines containing a text, respecting the case.
	 *
	 * @param searchText text to search
	 * @return number of lines containing the text
	 * @throws Exception if a page cannot be read
	 */
	public long countLinesContaining(final String searchText) throws Exception {
		return countLinesContaining(searchText, true, false, 0, Long.MAX_VALUE);
	}

	/**
	 * Counts the lines containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of matching lines
	 * @throws Exception if a page cannot be read
	 */
	public long countLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new CountLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfLines();
	}

	/**
	 * Counts the lines not containing a text, respecting the case.
	 *
	 * @param searchText text to search
	 * @return number of lines not containing the text
	 * @throws Exception if a page cannot be read
	 */
	public long countLinesNotContaining(final String searchText) throws Exception {
		return countLinesNotContaining(searchText, true, false, 0, Long.MAX_VALUE);
	}

	/**
	 * Counts the lines not containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of not matching lines
	 * @throws Exception if a page cannot be read
	 */
	public long countLinesNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new CountLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfLines();
	}

	/**
	 * Removes all lines not containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long keepOnlyLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((KeepOnlyLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new KeepOnlyLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	/**
	 * Removes all lines containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long keepOnlyNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((KeepOnlyLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new KeepOnlyLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	/**
	 * Removes all lines containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long dropLinesContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropLinesContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new DropLinesContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	/**
	 * Removes all lines not containing a text or a match of a regular expression.
	 *
	 * @param searchText text or regular expression to search
	 * @param caseSensitive true to respect the case
	 * @param regularExpression true if searchText is a regular expression
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long dropLinesNotContaining(final String searchText, final boolean caseSensitive, final boolean regularExpression, final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropLinesNotContainingAction) executeTextLineAction(startLineNumber, endLineNumber, new DropLinesNotContainingAction(searchText, caseSensitive, regularExpression))).getNumberOfDroppedLines();
	}

	/**
	 * Counts the empty lines (without any character except the line break).
	 *
	 * @return number of empty lines
	 * @throws Exception if a page cannot be read or a line has no line break
	 */
	public long countEmptyLines() throws Exception {
		return countEmptyLines(0, Long.MAX_VALUE);
	}

	/**
	 * Counts the empty lines (without any character except the line break) in a range of lines.
	 *
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of empty lines
	 * @throws Exception if a page cannot be read or a line has no line break
	 */
	public long countEmptyLines(final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountEmptyLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new CountEmptyLinesAction())).getNumberOfLines();
	}

	/**
	 * Counts the blank lines (containing only whitespace).
	 *
	 * @return number of blank lines
	 * @throws Exception if a page cannot be read
	 */
	public long countBlankLines() throws Exception {
		return countBlankLines(0, Long.MAX_VALUE);
	}

	/**
	 * Counts the blank lines (containing only whitespace) in a range of lines.
	 *
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of blank lines
	 * @throws Exception if a page cannot be read
	 */
	public long countBlankLines(final long startLineNumber, final long endLineNumber) throws Exception {
		return ((CountBlankLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new CountBlankLinesAction())).getNumberOfLines();
	}

	/**
	 * Removes all empty lines (without any character except the line break).
	 *
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read or a line has no line break
	 */
	public long dropEmptyLines() throws Exception {
		return dropEmptyLines(0, Long.MAX_VALUE);
	}

	/**
	 * Removes the empty lines (without any character except the line break) in a range of lines.
	 *
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read or a line has no line break
	 */
	public long dropEmptyLines(final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropEmptyLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new DropEmptyLinesAction())).getNumberOfDroppedLines();
	}

	/**
	 * Removes all blank lines (containing only whitespace).
	 *
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long dropBlankLines() throws Exception {
		return dropBlankLines(0, Long.MAX_VALUE);
	}

	/**
	 * Removes the blank lines (containing only whitespace) in a range of lines.
	 *
	 * @param startLineNumber 1-based number of the first line to check
	 * @param endLineNumber 1-based number of the last line to check (inclusive)
	 * @return number of removed lines
	 * @throws Exception if a page cannot be read
	 */
	public long dropBlankLines(final long startLineNumber, final long endLineNumber) throws Exception {
		revalidateStatisticData = true;
		return ((DropBlankLinesAction) executeTextLineAction(startLineNumber, endLineNumber, new DropBlankLinesAction())).getNumberOfDroppedLines();
	}

	/**
	 * Returns the key of this editor, which is part of the names of its temporary files.
	 *
	 * @return unique key of this editor
	 */
	public String getInterimStorageKey() {
		if (interimStorageKey == null) {
			interimStorageKey = Utilities.generateUUID();
		}
		return interimStorageKey;
	}

	/**
	 * Returns the type of line break used in the text.
	 *
	 * @return line break type, which may also be unknown or mixed
	 */
	public LineBreak getLineBreakType() {
		return lineBreakType;
	}

	/**
	 * Returns the byte order mark of the text file.
	 *
	 * @return byte order mark, {@link BOM#NONE} if there is none
	 */
	public BOM getBom() {
		return bom;
	}

	/**
	 * Removes a page from the text. Used when a page became empty.
	 *
	 * @param page page to remove
	 * @return true if the page was part of the text
	 */
	public boolean removePage(final Page page) {
		revalidateStatisticData = true;
		return pages.remove(page);
	}

	private TextLineAction executeTextLineAction(final long startLineNumber, final long endLineNumber, final TextLineAction action) throws Exception {
		long lineIndex = 0;
		for (final Page page : pages) {
			final long previousAmountOfLines = page.getAmountOfLines();
			if (lineIndex <= endLineNumber - 1 && lineIndex + previousAmountOfLines >= startLineNumber - 1) {
				final boolean pageWasAlreadyEdited = page.getNewContentLines() != null;
				final List<String> pageLines = pageWasAlreadyEdited ? getPageLinesForRead(page) : new ArrayList<>(getPageLinesForRead(page));
				boolean changed = false;
				for (int pageLineIndex = pageLines.size() - 1; pageLineIndex >= 0; pageLineIndex--) {
					if (startLineNumber - 1 <= lineIndex + pageLineIndex && lineIndex + pageLineIndex <= endLineNumber - 1) {
						final boolean lineChanged = action.execute(lineIndex + pageLineIndex, pageLineIndex, pageLines, pageLines.get(pageLineIndex));
						changed = changed || lineChanged;
					}
				}
				if (changed) {
					page.setNewContentLines(pageLines);
					readPageCache.remove(page);
				}
			}
			if (lineIndex + previousAmountOfLines > endLineNumber - 1) {
				return action;
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
		readPageCache.clear();
	}
}