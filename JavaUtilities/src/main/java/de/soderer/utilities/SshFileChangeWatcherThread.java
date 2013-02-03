package de.soderer.utilities;

import java.util.Date;

public class SshFileChangeWatcherThread extends FileChangeWatcherThread {
	private ServerAccessData serverData;
	private String filePath;
	private static final long sshdelay = 1000L;

	public SshFileChangeWatcherThread(ServerAccessData serverData, String filePath, Listener listener) {
		super(null, listener);
		this.serverData = serverData;
		this.filePath = filePath;
	}

	public void run() {
		try {
			while (running) {
				try {
					JschFileData fileData = JschWrapper.getFileData(serverData, filePath);
					long newFileLength = fileData.getSize();
					Date newChangeDate = fileData.getChangeDate();
					if (lastChangeDate == null || lastChangeDate.before(newChangeDate) || lastFileSize != newFileLength) {
						lastChangeDate = newChangeDate;
						lastFileSize = newFileLength;
						listener.noticeFileChanged();
					}

					Thread.sleep(sshdelay);
				} catch (InterruptedException ie) {
					throw ie;
				} catch (Exception e) {
					e.printStackTrace();
					running = false;
				}
			}
		} catch (InterruptedException ie) {
			handleInterrupt();
		}
	}
}
