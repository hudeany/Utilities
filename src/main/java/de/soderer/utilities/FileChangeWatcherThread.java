package de.soderer.utilities;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Daemon thread watching a file for changes of its modification date or size. The file is checked
 * once per second. The listener is notified once directly after the start (initial state) and on
 * each detected change. When the file does not exist anymore, the listener is notified and the
 * thread ends.
 * <p>
 * Any change of the modification date is detected, also to an older date (e.g. when a backup is
 * restored). A file vanishing only for a moment, like when an editor saves via a temporary file
 * and a rename, is not reported as missing. Exceptions thrown by the listener are printed to
 * standard error and do not stop the watching.
 * </p>
 */
public class FileChangeWatcherThread extends Thread {
	/** Delay between two checks of the file in milliseconds */
	private static final long DELAY = 1000L;

	/** Delay before a missing file is checked again, to ignore a short replacement by rename */
	private static final long MISSING_FILE_RECHECK_DELAY = 200L;

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
	/**
	 * Modification time of the file at the last check in milliseconds. Compared instead of
	 * {@link #lastChangeDate}, because a local date time is ambiguous when the clock is turned
	 * back at the end of daylight saving time.
	 */
	private long lastModifiedMillis = 0L;
	/**
	 * False after the thread was stopped by {@link #kill()} or the file vanished
	 */
	protected volatile boolean running = true;

	/**
	 * Creates a new watcher thread, which must be started by {@link #start()}.
	 *
	 * @param file
	 *            the file to watch, must not be null
	 * @param listener
	 *            the listener to notify about changes, must not be null
	 * @throws IllegalArgumentException
	 *             if the file or the listener is null
	 */
	public FileChangeWatcherThread(final File file, final NotificationListener listener) {
		super(getThreadName(file, listener));
		this.file = file;
		this.listener = listener;
		setDaemon(true);
	}

	/**
	 * Checks the constructor parameters and returns the thread name, which is passed to the super constructor.
	 *
	 * @param file
	 *            the file to watch
	 * @param listener
	 *            the listener to notify about changes
	 * @return the thread name
	 * @throws IllegalArgumentException
	 *             if the file or the listener is null
	 */
	private static String getThreadName(final File file, final NotificationListener listener) {
		if (file == null) {
			throw new IllegalArgumentException("Invalid empty file to watch");
		} else if (listener == null) {
			throw new IllegalArgumentException("Invalid empty listener");
		}
		return "FileChangeWatcher " + file.getName();
	}

	@Override
	public void run() {
		// Loop instead of recursion, so any number of interrupts does not grow the stack
		while (running) {
			try {
				checkFile();
				if (running) {
					Thread.sleep(DELAY);
				}
			} catch (@SuppressWarnings("unused") final InterruptedException ie) {
				handleInterrupt();
			}
		}
	}

	/**
	 * Checks the file once and notifies the listener about a change or a missing file.
	 *
	 * @throws InterruptedException
	 *             if the thread is interrupted while waiting to check a missing file again
	 */
	private void checkFile() throws InterruptedException {
		// Each value is read once, so the stored state is exactly the compared state
		long lastModified = file.lastModified();
		if (lastModified == 0L && !file.exists()) {
			// Editors saving via a temporary file and a rename may remove the file for a moment
			Thread.sleep(MISSING_FILE_RECHECK_DELAY);
			lastModified = file.lastModified();
			if (lastModified == 0L && !file.exists()) {
				running = false;
				notifyListener(true);
				return;
			}
		}

		final long fileSize = file.length();
		if (lastChangeDate == null || lastModified != lastModifiedMillis || fileSize != lastFileSize) {
			lastModifiedMillis = lastModified;
			lastChangeDate = DateUtilities.getLocalDateTime(lastModified);
			lastFileSize = fileSize;
			notifyListener(false);
		}
	}

	/**
	 * Notifies the listener. Runtime exceptions of the listener are printed and do not end the watching.
	 *
	 * @param fileNotExists
	 *            true to notify about the missing file, false to notify about a change
	 */
	private void notifyListener(final boolean fileNotExists) {
		try {
			if (fileNotExists) {
				listener.noticeFileNotExists();
			} else {
				listener.noticeFileChanged();
			}
		} catch (final RuntimeException e) {
			System.err.println("Error in listener of " + getName() + ": " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Handles an interruption of the thread. Unless the thread was stopped by {@link #kill()}, watching continues
	 * after this method returns. The default implementation does nothing.
	 */
	protected void handleInterrupt() {
		// Watching continues in the loop of run(), if the thread is still running
	}

	/**
	 * Stops watching and ends the thread. The thread is interrupted to end its waiting time, so a listener running at
	 * that moment may also see the interruption (e.g. as ClosedByInterruptException on NIO channels).
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
