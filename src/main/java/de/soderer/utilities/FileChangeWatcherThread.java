package de.soderer.utilities;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Daemon thread watching a file for changes of its modification date or size. The file is checked
 * once per second. The listener is notified once directly after the start (initial state) and on
 * each detected change. When the file does not exist anymore, the listener is notified and the
 * thread ends.
 */
public class FileChangeWatcherThread extends Thread {
	/**
	 * The watched file
	 */
	protected File file;
	/**
	 * The listener to notify
	 */
	protected NotificationListener listener;
	/**
	 * Modification date of the file at the last check, null before the first check
	 */
	protected LocalDateTime lastChangeDate = null;
	/**
	 * Size of the file at the last check
	 */
	protected long lastFileSize = 0L;
	private static final long delay = 1000L;
	/**
	 * False after the thread was stopped by {@link #kill()} or the file vanished
	 */
	protected volatile boolean running = true;

	/**
	 * Creates a new watcher thread, which must be started by {@link #start()}.
	 *
	 * @param file
	 *            the file to watch
	 * @param listener
	 *            the listener to notify about changes
	 */
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

	/**
	 * Handles an interruption of the thread. Unless the thread was stopped, watching continues.
	 */
	protected void handleInterrupt() {
		if (running) {
			run();
		}
	}

	/**
	 * Stops watching and ends the thread.
	 */
	public void kill() {
		running = false;
		interrupt();
	}

	/**
	 * Listener for file changes
	 */
	public interface NotificationListener {
		/**
		 * Called when the file was changed (and once for the initial state).
		 */
		void noticeFileChanged();

		/**
		 * Called when the file does not exist (anymore). The watcher thread ends afterwards.
		 */
		void noticeFileNotExists();
	}
}
