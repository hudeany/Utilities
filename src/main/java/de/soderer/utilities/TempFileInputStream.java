package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * When this InputStream is closed, the temporary file or directory is deleted.
 * The extra InputStream is needed because it may be some ZipInputStream etc.
 */
public class TempFileInputStream extends FilterInputStream {
	private final File tempFile;

	public TempFileInputStream(final File tempFile) throws FileNotFoundException {
		super(new FileInputStream(tempFile));
		this.tempFile = tempFile;
	}

	public TempFileInputStream(final InputStream inputStream, final File tempFile) {
		super(inputStream);
		this.tempFile = tempFile;
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (tempFile != null && tempFile.exists()) {
			FileUtilities.delete(tempFile);
		}
	}
}
