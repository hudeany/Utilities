package de.soderer.utilities.textbook;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.Utilities;

/**
 * Part of the lines of a {@link TextBookEditor}. An unedited page is described by its byte range in the text file,
 * an edited page holds its lines in memory or in a compressed temporary file.
 */
public class Page {
	private final TextBookEditor parentTextBookEditor;
	private long startFileIndex;
	private long endFileIndex;

	private long amountOfLines = 0;

	private List<String> newContentLines = null;
	private File interimStorageFile = null;

	/**
	 * Creates a new page and appends it to the pages of the editor.
	 *
	 * @param parentTextBookEditor editor the page belongs to
	 */
	public Page(final TextBookEditor parentTextBookEditor) {
		this.parentTextBookEditor = parentTextBookEditor;
		parentTextBookEditor.getPages().add(this);
		parentTextBookEditor.revalidateStatisticData = true;
	}

	/**
	 * Returns the editor the page belongs to.
	 *
	 * @return editor of this page
	 */
	public TextBookEditor getParentTextBookEditor() {
		return parentTextBookEditor;
	}

	/**
	 * Returns the byte offset of the page's first line in the text file (after the BOM).
	 *
	 * @return start byte offset
	 */
	public long getStartFileIndex() {
		return startFileIndex;
	}

	/**
	 * Sets the byte offset of the page's first line in the text file (after the BOM).
	 *
	 * @param startFileIndex start byte offset
	 */
	public void setStartFileIndex(final long startFileIndex) {
		this.startFileIndex = startFileIndex;
	}

	/**
	 * Returns the byte offset right after the page's last line in the text file (after the BOM).
	 *
	 * @return end byte offset (exclusive)
	 */
	public long getEndFileIndex() {
		return endFileIndex;
	}

	/**
	 * Sets the byte offset right after the page's last line in the text file (after the BOM).
	 *
	 * @param endFileIndex end byte offset (exclusive)
	 */
	public void setEndFileIndex(final long endFileIndex) {
		this.endFileIndex = endFileIndex;
	}

	/**
	 * Returns the number of lines of this page.
	 *
	 * @return number of lines
	 */
	public long getAmountOfLines() {
		return newContentLines == null ? amountOfLines : newContentLines.size();
	}

	/**
	 * Sets the number of lines of an unedited page.
	 *
	 * @param amountOfLines number of lines
	 * @throws IllegalArgumentException if the page has new content already
	 */
	public void setAmountOfLines(final long amountOfLines) {
		if (newContentLines != null) {
			throw new IllegalArgumentException("New content was already defined");
		}
		this.amountOfLines = amountOfLines;
	}

	/**
	 * Returns the changed lines of this page.
	 *
	 * @return changed lines including their line breaks, or null if the page is unedited
	 * @throws Exception if the temporary file with the changed lines cannot be read
	 */
	public List<String> getNewContentLines() throws Exception {
		if (interimStorageFile != null) {
			try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(interimStorageFile)))) {
				inputStream.getNextEntry();
				return FileUtilities.readLinesFromStream(inputStream, true, StandardCharsets.UTF_8);
			}
		} else {
			return newContentLines;
		}
	}

	/**
	 * Sets the changed lines of this page, which replace its content in the text file.
	 *
	 * @param newContentLines changed lines including their line breaks
	 * @throws Exception if moving pages to temporary files fails
	 */
	public void setNewContentLines(final List<String> newContentLines) throws Exception {
		if (interimStorageFile != null) {
			interimStorageFile.delete();
			interimStorageFile = null;
			getParentTextBookEditor().setNumberOfPagesWithStoredNewContent(getParentTextBookEditor().getNumberOfPagesWithStoredNewContent() - 1);
		}
		amountOfLines = newContentLines.size();
		if (this.newContentLines == null) {
			this.newContentLines = newContentLines;
			getParentTextBookEditor().setNumberOfPagesWithNewContent(getParentTextBookEditor().getNumberOfPagesWithNewContent() + 1);
		} else {
			this.newContentLines = newContentLines;
		}
		getParentTextBookEditor().revalidateStatisticData = true;
	}

	/**
	 * Moves the changed lines of this page from memory into a compressed temporary file.
	 *
	 * @throws Exception if the temporary file cannot be written
	 */
	public void storeInterimData() throws Exception {
		if (newContentLines != null) {
			interimStorageFile = File.createTempFile(getParentTextBookEditor().getInterimStorageKey() + "_", ".tmp.zip");
			try (ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(interimStorageFile)))) {
				outputStream.putNextEntry(new ZipEntry("InterimStorageFile.tmp"));
				outputStream.write(Utilities.join(newContentLines, null).getBytes(StandardCharsets.UTF_8));
			}
			// Keep the line count, it is used for all line index calculations while the content is stored
			amountOfLines = newContentLines.size();
			newContentLines = null;
			getParentTextBookEditor().setNumberOfPagesWithNewContent(getParentTextBookEditor().getNumberOfPagesWithNewContent() - 1);
			getParentTextBookEditor().setNumberOfPagesWithStoredNewContent(getParentTextBookEditor().getNumberOfPagesWithStoredNewContent() + 1);
		}
	}

	/**
	 * Discards the changed lines of this page, including its temporary file.
	 *
	 * @throws Exception if the editor's counters cannot be updated
	 */
	public void clear() throws Exception {
		if (interimStorageFile != null) {
			interimStorageFile.delete();
			interimStorageFile = null;
			getParentTextBookEditor().setNumberOfPagesWithStoredNewContent(getParentTextBookEditor().getNumberOfPagesWithStoredNewContent() - 1);
		}
		if (newContentLines != null) {
			amountOfLines = newContentLines.size();
			newContentLines = null;
			getParentTextBookEditor().setNumberOfPagesWithNewContent(getParentTextBookEditor().getNumberOfPagesWithNewContent() - 1);
		}
	}

	/**
	 * Checks whether this page has changed lines in memory or in a temporary file.
	 *
	 * @return true if the page has changed lines
	 */
	public boolean hasNewContentData() {
		return interimStorageFile != null || newContentLines != null;
	}
}
