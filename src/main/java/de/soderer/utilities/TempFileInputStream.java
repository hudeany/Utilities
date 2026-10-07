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

	/**
	 * Creates a stream reading the given temporary file, which is deleted on close.
	 *
	 * @param tempFile
	 *            the temporary file to read and delete
	 * @throws FileNotFoundException
	 *             if the file cannot be opened for reading
	 */
	public TempFileInputStream(final File tempFile) throws FileNotFoundException {
		super(new FileInputStream(tempFile));
		this.tempFile = tempFile;
	}

	/**
	 * Creates a stream reading from the given stream, which deletes the given temporary file or
	 * directory (recursively) on close.
	 *
	 * @param inputStream
	 *            the stream to read from, for example a stream reading the temporary file
	 * @param tempFile
	 *            the temporary file or directory to delete on close, may be null
	 */
	public TempFileInputStream(final InputStream inputStream, final File tempFile) {
		super(inputStream);
		this.tempFile = tempFile;
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			// Delete the temporary data even if closing the underlying stream failed
			if (tempFile != null && tempFile.exists()) {
				FileUtilities.delete(tempFile);
			}
		}
	}
}
