package de.soderer.utilities;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

/**
 * FtpConnection
 */
public class FtpConnection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(FtpConnection.class);

	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The password. */
	private String password = null;

	/** The port. */
	private int port = 21; // Default port for FTP is 21

	/**
	 * usePassiveMode
	 */
	private final boolean usePassiveMode;

	/**
	 * Currently connected client
	 */
	private FTPClient ftpClient = null;

	/**
	 * Instantiates a new FtpConnection.
	 * Default port for FTP is 21.
	 * Uses FTP passive mode.
	 *
	 * @param host the host
	 * @param user the user
	 * @param password the password
	 * @throws Exception
	 */
	public FtpConnection(final String hostname, final String username, final String password) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		usePassiveMode = true;

		connect();
	}

	/**
	 * Instantiates a new FtpConnection.
	 * Uses FTP passive mode.
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 * @param port
	 * @throws Exception
	 */
	public FtpConnection(final String hostname, final String username, final String password, final int port) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.port = port;
		usePassiveMode = true;

		connect();
	}

	/**
	 * Instantiates a new FtpConnection.
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 * @param port
	 * @param usePassiveMode
	 * @throws Exception
	 */
	public FtpConnection(final String hostname, final String username, final String password, final int port, final boolean usePassiveMode) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.port = port;
		this.usePassiveMode = usePassiveMode;

		connect();
	}

	public FtpConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"ftp".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for FTP connection. Only 'ftp' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			password = new String(serverEntry.getPassword());
			port = serverEntry.getPort();
			usePassiveMode = true;

			connect();
		}
	}

	/**
	 * Connect.
	 */
	private void connect() throws Exception {
		try {
			ftpClient = new FTPClient();
			ftpClient.connect(hostname, port);
			if (usePassiveMode) {
				ftpClient.enterLocalPassiveMode();
			}
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.setAutodetectUTF8(true);
			if (!ftpClient.login(username, password)) {
				ftpClient.logout();
				throw new Exception("Authentication failed");
			}
			ftpClient.setListHiddenFiles(true);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed");
		}
	}

	@Override
	public List<String> getFiles(final String folderPath) throws Exception {
		checkForConnection();
		final FTPFile[] files = ftpClient.listFiles(folderPath);
		if (files != null) {
			final List<String> returnList = new ArrayList<>();
			for (final FTPFile file : files) {
				if (file.isFile()) {
					returnList.add(file.getName());
				}
			}
			return returnList;
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public List<String> getFolders(final String folderPath) throws Exception {
		checkForConnection();
		final FTPFile[] directories = ftpClient.listDirectories(folderPath);
		if (directories != null) {
			final List<String> returnList = new ArrayList<>();
			for (final FTPFile directory : directories) {
				final String dirctoryName = directory.getName();
				if (!".".equals(dirctoryName) && !"..".equals(dirctoryName) && directory.isDirectory()) {
					returnList.add(dirctoryName);
				}
			}
			return returnList;
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * Check if folder exists
	 */
	@Override
	public boolean folderExists(final String folderPath) throws Exception {
		checkForConnection();
		ftpClient.changeWorkingDirectory(folderPath);
		final int returnCode = ftpClient.getReplyCode();
		if (returnCode == 550) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check if file exists on sftp server
	 *
	 * @param filePath
	 * @return
	 * @throws Exception on invalid directory
	 */
	@Override
	public boolean fileExists(final String filePath) throws Exception {
		checkForConnection();
		return ftpClient.listFiles(filePath).length > 0;
	}

	/**
	 * Put a file on the server.
	 *
	 * @param in the in
	 * @param dst the dst
	 */
	@Override
	public void uploadFile(final String filePath, final InputStream inputStream) throws Exception {
		checkForConnection();
		ftpClient.storeFile(filePath, inputStream);
	}

	/**
	 * Gets the file.
	 *
	 * @param name the name
	 * @return the input stream
	 */
	@Override
	public InputStream getFile(final String filePath) throws Exception {
		// TODO: Filestream is empty
		checkForConnection();
		ftpClient.cwd(filePath.substring(0, filePath.lastIndexOf("/")));
		final InputStream inputStream = ftpClient.retrieveFileStream(filePath.substring(filePath.lastIndexOf("/") + 1));
		final int returnCode = ftpClient.getReplyCode();
		if (inputStream != null && returnCode != 550) {
			return new FtpInputStream(inputStream, this);
		} else {
			throw new Exception("File not found");
		}
	}

	private void checkForConnection() throws Exception {
		if (ftpClient == null) {
			throw new Exception("FtpConnection is not connected");
		} else if (!ftpClient.isConnected()) {
			ftpClient = null;
			throw new Exception("FtpConnection is not connected anymore");
		}
	}

	/**
	 * Close connection
	 */
	@Override
	public void close() {
		if (ftpClient != null) {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (final Exception f) {
					// do nothing
				}
			}
			ftpClient = null;
		}
	}

	private boolean completePendingCommand() {
		if (ftpClient != null) {
			if (ftpClient.isConnected()) {
				try {
					return ftpClient.completePendingCommand();
				} catch (final Exception f) {
					// do nothing
				}
			}
		}
		return false;
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();
		ftpClient.deleteFile(filePath);
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkForConnection();
		final FTPFile[] files = ftpClient.listFiles(folderPath);
		if (files.length > 0) {
			for (final FTPFile ftpFile : files) {
				final String fileName = ftpFile.getName();
				if (!".".equals(fileName) && !"..".equals(fileName)) {
					if (ftpFile.isDirectory()) {
						deleteFolder(folderPath + "/" + fileName);
					} else {
						ftpClient.deleteFile(folderPath + "/" + fileName);
					}
				}
			}
		}
		ftpClient.removeDirectory(folderPath);
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkForConnection();
		ftpClient.makeDirectory(parentFolderPath + "/" + folderName);
	}

	@Override
	public RemoteFile getFileInfo(final String filePath) throws Exception {
		checkForConnection();

		final FTPFile file = ftpClient.mlistFile(filePath);
		return new RemoteFile(filePath, file.getSize(), file.getTimestamp().getTime());
	}

	class FtpInputStream extends FilterInputStream {
		FtpConnection ftpConnection;
		boolean isClosed = false;

		protected FtpInputStream(final InputStream in, final FtpConnection ftpConnection) {
			super(in);
			this.ftpConnection = ftpConnection;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (!isClosed) {
				ftpConnection.completePendingCommand();
			}

			isClosed = true;
		}
	}
}
