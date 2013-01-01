package de.soderer.utilities;

import java.io.File;
import java.util.Date;

public class FileChangeWatcherThread extends Thread {
	private File file;
	private Listener listener;
	private Date lastChangeDate = null;
	private long lastFileSize = 0L;
	private static final long delay = 1000L;
	private boolean running = true;

	public FileChangeWatcherThread(File file, Listener listener) {
		this.file = file;
		this.listener = listener;
		setDaemon(true);
	}

	public void run() {
		try {
			while (running) {
				if (!file.exists()) {
					running = false;
				} else {
					if (lastChangeDate == null || lastChangeDate.before(new Date(file.lastModified())) || lastFileSize != file.length()) {
						lastChangeDate = new Date(file.lastModified());
						lastFileSize = file.length();
						listener.noticeFileChanged();
					}

					Thread.sleep(delay);
				}
			}
		} catch (InterruptedException IE) {
			handleInterrupt();
		}
	}

	private void handleInterrupt() {
		if (running) {
			run();
		}
	}

	public void kill() {
		running = false;
		Thread.currentThread().interrupt();
	}
	
	public interface Listener {
		public void noticeFileChanged();
	}
}
