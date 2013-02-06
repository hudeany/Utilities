package de.soderer.utilities;

import com.jcraft.jsch.Session;

public class SshFileChangeWatcherThread extends Thread {
	private ServerAccessData serverData;
	private Session session;
	private String filePath;
	protected LineListener listener;
	protected boolean running = true;

	public SshFileChangeWatcherThread(ServerAccessData serverData, String filePath, LineListener listener) {
		setDaemon(true);
		this.serverData = serverData;
		this.filePath = filePath;
		this.listener = listener;
	}

	public void run() {
		try {
			session = JschWrapper.openSession(serverData);
			try {
				JschWrapper.tailviewFileData(session, filePath, listener);
			} catch (InterruptedException ie) {
				throw ie;
			} catch (Exception e) {
				e.printStackTrace();
				running = false;
			}
		} catch (InterruptedException ie) {
			handleInterrupt();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JschWrapper.closeQuietly(session);
		}
	}

	protected void handleInterrupt() {
		if (running) {
			run();
		}
	}

	public void kill() {
		running = false;
		Thread.currentThread().interrupt();
	}
	
	public interface LineListener {
		public void addLineData(byte[] textLineData);
	}
}
