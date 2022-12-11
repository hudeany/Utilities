package de.soderer.utilities;

import java.io.File;
import java.time.LocalDateTime;

public class FileChangeWatcherThread extends Thread {
	protected File file;
	protected NotificationListener listener;
	protected LocalDateTime lastChangeDate = null;
	protected long lastFileSize = 0L;
	private static final long delay = 1000L;
	protected boolean running = true;

	public FileChangeWatcherThread(final File file, final NotificationListener listener) {
		this.file = file;
		this.listener = listener;
		setDaemon(true);
	}

	@Override
	public void run() {
		try {
			while (running) {
				if (!file.exists()) {
					running = false;
					listener.noticeFileNotExists();
				} else {
					if (lastChangeDate == null || lastChangeDate.isBefore(DateUtilities.getLocalDateTime(file.lastModified())) || lastFileSize != file.length()) {
						lastChangeDate = DateUtilities.getLocalDateTime(file.lastModified());
						lastFileSize = file.length();
						listener.noticeFileChanged();
					}

					Thread.sleep(delay);
				}
			}
		} catch (@SuppressWarnings("unused") final InterruptedException ie) {
			handleInterrupt();
		}
	}

	protected void handleInterrupt() {
		if (running) {
			run();
		}
	}

	public void kill() {
		running = false;
		interrupt();
	}

	public interface NotificationListener {
		void noticeFileChanged();

		void noticeFileNotExists();
	}
}
