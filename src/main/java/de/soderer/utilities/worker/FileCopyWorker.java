package de.soderer.utilities.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import de.soderer.utilities.Utilities;

public class FileCopyWorker extends WorkerSimple<Boolean> {
	public static final int EOF = -1;

	private final String copySourceFileUrlString;
	private final File destinationFile;

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

		if (copySourceFileUrlString.toLowerCase().startsWith("file://")) {
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
