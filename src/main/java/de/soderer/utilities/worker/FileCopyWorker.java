package de.soderer.utilities.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import de.soderer.utilities.Utilities;

/**
 * Worker that copies a local file (given as path or "file://" URL) to a new file.
 * Its result is true on success and false if it was cancelled.
 */
public class FileCopyWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of InputStream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private final String copySourceFileUrlString;
	private final File destinationFile;

	/**
	 * Creates a copy worker.
	 *
	 * @param parent parent receiving the progress signals, may be null
	 * @param copySourceFileUrlString path or "file://" URL of the file to copy
	 * @param destinationFile file to create, must not exist yet
	 */
	public FileCopyWorker(final WorkerParentSimple parent, final String copySourceFileUrlString, final File destinationFile) {
		super(parent);

		this.destinationFile = destinationFile;
		this.copySourceFileUrlString = copySourceFileUrlString;
	}

	@Override
	public Boolean work() throws Exception {
		signalProgress();

		if (destinationFile.exists()) {
			throw new Exception("File already exists: " + destinationFile.getAbsolutePath());
		}

		signalUnlimitedProgress();

		if (copySourceFileUrlString.toLowerCase(Locale.ROOT).startsWith("file://")) {
			final URL sourceFileUrl = new URL(copySourceFileUrlString);
			signalProgress(true);
			try (InputStream inputStream = sourceFileUrl.openStream();
					FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
				itemsToDo = inputStream.available();
				final byte[] buffer = new byte[4096];
				int readLength;
				while ((readLength = inputStream.read(buffer)) != EOF) {
					if (cancel) {
						break;
					} else {
						fileOutputStream.write(buffer, 0, readLength);
						itemsDone += readLength;
						signalProgress();
					}
				}
				Utilities.clear(buffer);
			} catch (final Exception e) {
				// The destination did not exist before, so a partially written file can be removed
				destinationFile.delete();
				throw new Exception("Cannot copy file: " + e.getMessage(), e);
			}
		} else if (new File(copySourceFileUrlString).exists()) {
			itemsToDo = new File(copySourceFileUrlString).length();
			signalProgress(true);
			try (FileInputStream inputStream = new FileInputStream(copySourceFileUrlString);
					FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
				final byte[] buffer = new byte[4096];
				int readLength;
				while ((readLength = inputStream.read(buffer)) != EOF) {
					if (cancel) {
						break;
					} else {
						fileOutputStream.write(buffer, 0, readLength);
						itemsDone += readLength;
						signalProgress();
					}
				}
				Utilities.clear(buffer);
			} catch (final Exception e) {
				// The destination did not exist before, so a partially written file can be removed
				destinationFile.delete();
				throw new Exception("Cannot copy file: " + e.getMessage(), e);
			}
		} else {
			throw new Exception("No such file: '" + copySourceFileUrlString + "'");
		}

		if (cancel) {
			destinationFile.delete();
			return false;
		} else {
			signalProgress(true);
			return true;
		}
	}
}
