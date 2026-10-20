package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;

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

	/**
	 * Closes the underlying stream and deletes the temporary file or directory, even if closing the
	 * stream failed. Closing an already closed stream has no effect.
	 *
	 * @throws IOException
	 *             if closing the underlying stream failed or the temporary data could not be
	 *             deleted completely. An error of closing the stream takes precedence, a deletion
	 *             error is then added as suppressed exception.
	 */
	@Override
	public void close() throws IOException {
		IOException closeException = null;
		try {
			super.close();
		} catch (final IOException e) {
			closeException = e;
		}

		// Delete the temporary data even if closing the underlying stream failed.
		// NOFOLLOW_LINKS: a symbolic link with a missing target is deleted, too (File.exists() would return false for it).
		if (tempFile != null && Files.exists(tempFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			try {
				FileUtilities.delete(tempFile);
			} catch (final IOException deleteException) {
				// Before, a deletion error thrown in the finally block replaced the error of closing the stream
				if (closeException != null) {
					closeException.addSuppressed(deleteException);
				} else {
					closeException = deleteException;
				}
			}
		}

		if (closeException != null) {
			throw closeException;
		}
	}
}
