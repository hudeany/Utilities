package de.soderer.utilities;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

/**
 * FtpConnection
 */
public class FtpConnection implements Closeable {
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
	 * Instantiates a new ftp helper.
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
	 * Instantiates a new ftp helper.
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
	 * Instantiates a new ftp helper.
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
	 * Instantiates a new ftp helper.
	 * Default port for FTP is 21
	 *
	 * @param fileServerAndAuthConfigString like "ftp://[username[:password]@]server[:port]"
	 */
	public FtpConnection(String fileServerAndAuthConfigString) throws Exception {
		if (fileServerAndAuthConfigString == null) {
			throw new Exception("Invalid empty connection string");
		} else if (fileServerAndAuthConfigString.matches("^[A-Za-z]+://.*")) {
			final String protocol = fileServerAndAuthConfigString.substring(0, fileServerAndAuthConfigString.indexOf("://"));
			if (!"ftp".equalsIgnoreCase(protocol)) {
				throw new Exception("Invalid protocol '" + protocol + "' for FTP connection. Only 'ftp' is allowed");
			} else {
				fileServerAndAuthConfigString = fileServerAndAuthConfigString.substring(fileServerAndAuthConfigString.indexOf("://") + 3);
			}
		}

		if (fileServerAndAuthConfigString.contains("@")) {
			final int hostSeparatorIndex = fileServerAndAuthConfigString.lastIndexOf("@");
			final String authPart = fileServerAndAuthConfigString.substring(0, hostSeparatorIndex);
			final String serverPart = fileServerAndAuthConfigString.substring(hostSeparatorIndex + 1);

			if (authPart.contains(":")) {
				username = authPart.substring(0, authPart.indexOf(":"));
				password = authPart.substring(authPart.indexOf(":") + 1);
			} else {
				username = authPart;
			}

			if (serverPart.contains(":")) {
				hostname = serverPart.substring(0, serverPart.indexOf(":"));
				port = Integer.parseInt(serverPart.substring(serverPart.indexOf(":") + 1));
			} else {
				hostname = serverPart;
			}
		} else {
			hostname = fileServerAndAuthConfigString;
		}
		usePassiveMode = true;

		connect();
	}

	/**
	 * Connect.
	 */
	private FtpConnection connect() throws Exception {
		try {
			ftpClient = new FTPClient();
			ftpClient.connect(hostname, port);
			if (usePassiveMode) {
				ftpClient.enterLocalPassiveMode();
			}
			if (!ftpClient.login(username, password)) {
				ftpClient.logout();
				throw new Exception("Authentication failed");
			}
			ftpClient.setListHiddenFiles(true);
			return this;
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed");
		}
	}

	/**
	 * Cd.
	 *
	 * @param path the path
	 */
	public void cd(final String path) throws Exception {
		checkForConnection();
		ftpClient.changeWorkingDirectory(path);
	}

	public List<String> getFiles(final String path) throws Exception {
		checkForConnection();
		final FTPFile[] files = ftpClient.listFiles(path);
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

	public List<String> getFolders(final String path) throws Exception {
		checkForConnection();
		final FTPFile[] directories = ftpClient.listDirectories(path);
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
	 * Check if directory exists on sftp server
	 *
	 * @param directoryPath
	 * @return
	 * @throws Exception
	 */
	public boolean directoryExists(final String directoryPath) throws Exception {
		checkForConnection();
		ftpClient.changeWorkingDirectory(directoryPath);
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
	public void put(final InputStream inputStream, final String destination) throws Exception {
		checkForConnection();
		ftpClient.storeFile(destination, inputStream);
	}

	/**
	 * Gets the file.
	 *
	 * @param name the name
	 * @return the input stream
	 */
	public InputStream get(final String name) throws Exception {
		checkForConnection();
		final InputStream inputStream = ftpClient.retrieveFileStream(name);
		final int returnCode = ftpClient.getReplyCode();
		if (inputStream != null && returnCode != 550) {
			return new FtpInputStream(inputStream, this);
		} else {
			throw new Exception("File not found");
		}
	}

	private void checkForConnection() throws Exception {
		if (ftpClient == null) {
			throw new Exception("FtpHelper is not connected");
		} else if (!ftpClient.isConnected()) {
			ftpClient = null;
			throw new Exception("FtpHelper is not connected anymore");
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

	public boolean completePendingCommand() {
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

	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();
		ftpClient.deleteFile(filePath);
	}

	public void deleteFolder(final String directoryPath) throws Exception {
		checkForConnection();

		final FTPFile[] files = ftpClient.listFiles(directoryPath);
		if (files.length > 0) {
			for (final FTPFile ftpFile : files) {
				final String fileName = ftpFile.getName();
				if (!".".equals(fileName) && !"..".equals(fileName)) {
					if (ftpFile.isDirectory()) {
						deleteFolder(directoryPath + "/" + fileName);
					} else {
						ftpClient.deleteFile(directoryPath + "/" + fileName);
					}
				}
			}
		}
		ftpClient.removeDirectory(directoryPath);
	}

	public void createFolder(final String parentDirectoryPath, final String folderName) throws Exception {
		checkForConnection();
		ftpClient.makeDirectory(parentDirectoryPath + "/" + folderName);
	}

	class FtpInputStream extends FilterInputStream {
		FtpConnection ftpHelper;
		boolean isClosed = false;

		protected FtpInputStream(final InputStream in, final FtpConnection ftpHelper) {
			super(in);
			this.ftpHelper = ftpHelper;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (!isClosed) {
				ftpHelper.completePendingCommand();
			}

			isClosed = true;
		}
	}

	public static void closeQuietly(final FtpConnection ftpHelper) {
		try {
			if (ftpHelper != null) {
				ftpHelper.close();
			}
		} catch (final Exception ioe) {
			// ignore
		}
	}
}
