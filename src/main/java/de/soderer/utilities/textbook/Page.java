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

public class Page {
	private final TextBookEditor parentTextBookEditor;
	private long startFileIndex;
	private long endFileIndex;

	private long amountOfLines = 0;

	private List<String> newContentLines = null;
	private File interimStorageFile = null;

	public Page(final TextBookEditor parentTextBookEditor) {
		this.parentTextBookEditor = parentTextBookEditor;
		parentTextBookEditor.getPages().add(this);
		parentTextBookEditor.revalidateStatisticData = true;
	}

	public TextBookEditor getParentTextBookEditor() {
		return parentTextBookEditor;
	}

	public long getStartFileIndex() {
		return startFileIndex;
	}

	public void setStartFileIndex(final long startFileIndex) {
		this.startFileIndex = startFileIndex;
	}

	public long getEndFileIndex() {
		return endFileIndex;
	}

	public void setEndFileIndex(final long endFileIndex) {
		this.endFileIndex = endFileIndex;
	}

	public long getAmountOfLines() {
		return newContentLines == null ? amountOfLines : newContentLines.size();
	}

	public void setAmountOfLines(final long amountOfLines) {
		if (newContentLines != null) {
			throw new IllegalArgumentException("New content was already defined");
		}
		this.amountOfLines = amountOfLines;
	}

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

	@SuppressWarnings("resource")
	public void setNewContentLines(final List<String> newContentLines) throws Exception {
		if (interimStorageFile != null) {
			interimStorageFile.delete();
			interimStorageFile = null;
			getParentTextBookEditor().setNumberOfPagesWithStoredNewContent(getParentTextBookEditor().getNumberOfPagesWithStoredNewContent() - 1);
		}
		if (this.newContentLines == null) {
			// fill newContentLines early, because number change may cause interim file storage
			this.newContentLines = newContentLines;
			amountOfLines = newContentLines.size();
			getParentTextBookEditor().setNumberOfPagesWithNewContent(getParentTextBookEditor().getNumberOfPagesWithNewContent() + 1);
		} else {
			this.newContentLines = newContentLines;
		}
		getParentTextBookEditor().revalidateStatisticData = true;
	}

	@SuppressWarnings("resource")
	public void storeInterimData() throws Exception {
		if (newContentLines != null) {
			interimStorageFile = File.createTempFile(getParentTextBookEditor().getInterimStorageKey() + "_", ".tmp.zip");
			try (ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(interimStorageFile)))) {
				outputStream.putNextEntry(new ZipEntry("InterimStorageFile.tmp"));
				outputStream.write(Utilities.join(newContentLines, null).getBytes(StandardCharsets.UTF_8));
			}
			newContentLines = null;
			getParentTextBookEditor().setNumberOfPagesWithNewContent(getParentTextBookEditor().getNumberOfPagesWithNewContent() - 1);
			getParentTextBookEditor().setNumberOfPagesWithStoredNewContent(getParentTextBookEditor().getNumberOfPagesWithStoredNewContent() + 1);
		}
	}

	public void clear() {
		if (interimStorageFile != null) {
			interimStorageFile.delete();
			interimStorageFile = null;
		}
		if (newContentLines != null) {
			newContentLines = null;
		}
	}

	public boolean hasNewContentData() {
		return interimStorageFile != null || newContentLines != null;
	}
}
