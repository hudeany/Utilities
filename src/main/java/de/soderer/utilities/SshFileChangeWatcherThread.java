package de.soderer.utilities;

import de.soderer.utilities.remote.SftpConnection;

public class SshFileChangeWatcherThread extends Thread {
	private final ServerAccessData serverData;
	private final String filePath;
	private final int numberOfLastLinesToShow;
	protected LineListener listener;
	protected boolean running = true;

	public SshFileChangeWatcherThread(final ServerAccessData serverData, final String filePath, final int numberOfLastLinesToShow, final LineListener listener) {
		setDaemon(true);
		this.serverData = serverData;
		this.filePath = filePath;
		this.numberOfLastLinesToShow = numberOfLastLinesToShow;
		this.listener = listener;
	}

	@Override
	public void run() {
		try (SftpConnection sftpConnection = new SftpConnection(serverData)) {
			try {
				sftpConnection.tailviewFileData(filePath, numberOfLastLinesToShow, listener);
			} catch (final InterruptedException ie) {
				throw ie;
			} catch (final Exception e) {
				e.printStackTrace();
				running = false;
			}
		} catch (final InterruptedException ie) {
			handleInterrupt();
		} catch (final Exception e) {
			e.printStackTrace();
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

	public interface LineListener {
		void addLineData(byte[] textLineData);
	}
}
