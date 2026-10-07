package de.soderer.utilities.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Tuple;

/**
 * Remote connection via FTP, based on Apache Commons Net. Transfers use binary mode.
 */
public class FtpConnection implements RemoteConnection {
	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The password. */
	private char[] password = null;

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

	private ProxyConfiguration proxyConfiguration = null;

	/**
	 * Instantiates a new FtpConnection.
	 * Default port for FTP is 21.
	 * Uses FTP passive mode.
	 *
	 * @param hostname host name of the server
	 * @param username user name
	 * @param password password
	 * @throws Exception if the connection or login fails
	 */
	public FtpConnection(final String hostname, final String username, final char[] password) throws Exception {
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
	 * @param hostname host name of the server
	 * @param username user name
	 * @param password password
	 * @param port port of the server
	 * @throws Exception if the connection or login fails
	 */
	public FtpConnection(final String hostname, final String username, final char[] password, final int port) throws Exception {
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
	 * @param hostname host name of the server
	 * @param username user name
	 * @param password password
	 * @param port port of the server
	 * @param usePassiveMode true for passive mode (usually needed behind firewalls and NAT)
	 * @throws Exception if the connection or login fails
	 */
	public FtpConnection(final String hostname, final String username, final char[] password, final int port, final boolean usePassiveMode) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.port = port;
		this.usePassiveMode = usePassiveMode;

		connect();
	}

	/**
	 * Instantiates a new FtpConnection in passive mode.
	 *
	 * @param serverEntry access data with protocol "ftp"
	 * @throws Exception if the protocol is not "ftp" or the connection or login fails
	 */
	public FtpConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"ftp".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for FTP connection. Only 'ftp' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			password = serverEntry.getPassword();
			port = serverEntry.getPort();
			usePassiveMode = true;

			connect();
		}
	}

	/**
	 * Sets the proxy configuration. As the connection is established by the constructor already, it is established
	 * again with the proxy.
	 *
	 * @param proxyConfiguration proxy configuration, null for a direct connection
	 * @return this connection for chaining
	 * @throws IllegalStateException if the new connection fails
	 */
	public FtpConnection setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
		// The constructors connect immediately, so the proxy only takes effect with a new connection
		if (ftpClient != null) {
			close();
			try {
				connect();
			} catch (final Exception e) {
				throw new IllegalStateException("Cannot reconnect with proxy configuration: " + e.getMessage(), e);
			}
		}
		return this;
	}

	/**
	 * Connect.
	 */
	private void connect() throws Exception {
		try {
			ftpClient = new FTPClient();

			if (proxyConfiguration != null) {
				final Proxy proxy = proxyConfiguration.getProxy(hostname);
				if (proxy != null && !Proxy.NO_PROXY.equals(proxy)) {
					ftpClient.setProxy(proxy);
				}
			}

			ftpClient.setAutodetectUTF8(true);
			ftpClient.connect(hostname, port);
			if (usePassiveMode) {
				ftpClient.enterLocalPassiveMode();
			}
			if (!ftpClient.login(username, password == null ? "" : new String(password))) {
				ftpClient.logout();
				throw new Exception("Authentication failed");
			}
			// Binary mode must be set after login, many servers reject the TYPE command before
			if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
				throw new Exception("Cannot set binary transfer mode: " + ftpClient.getReplyString());
			}
			ftpClient.setListHiddenFiles(true);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void uploadFile(final String filePath, final InputStream inputStream) throws Exception {
		checkForConnection();
		if (!ftpClient.storeFile(filePath, inputStream)) {
			throw new Exception("Cannot upload file '" + filePath + "': " + ftpClient.getReplyString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream downloadFile(final String filePath) throws Exception {
		checkForConnection();
		final InputStream inputStream = ftpClient.retrieveFileStream(filePath);
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
				} catch (@SuppressWarnings("unused") final Exception e) {
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
				} catch (@SuppressWarnings("unused") final Exception e) {
					// do nothing
				}
			}
		}
		return false;
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkForConnection();
		if (!ftpClient.deleteFile(filePath)) {
			throw new Exception("Cannot delete file '" + filePath + "': " + ftpClient.getReplyString());
		}
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkForConnection();
		// Replacing blanks with escaped blanks is a workaround for FtpClient bug, which returns an empty list on foldernames with blanks
		final FTPFile[] files = ftpClient.listFiles(folderPath.replace(" ", "\\ "));
		if (files.length > 0) {
			for (final FTPFile ftpFile : files) {
				final String fileName = ftpFile.getName();
				if (!".".equals(fileName) && !"..".equals(fileName)) {
					if (ftpFile.isDirectory()) {
						deleteFolder(folderPath + "/" + fileName);
					} else if (!ftpClient.deleteFile(folderPath + "/" + fileName)) {
						throw new Exception("Cannot delete file '" + folderPath + "/" + fileName + "': " + ftpClient.getReplyString());
					}
				}
			}
		}
		if (!ftpClient.removeDirectory(folderPath)) {
			throw new Exception("Cannot delete folder '" + folderPath + "': " + ftpClient.getReplyString());
		}
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkForConnection();
		if (!ftpClient.makeDirectory(parentFolderPath + "/" + folderName)) {
			throw new Exception("Cannot create folder '" + parentFolderPath + "/" + folderName + "': " + ftpClient.getReplyString());
		}
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		checkForConnection();

		final FTPFile file = ftpClient.mlistFile(filePath);
		if (file == null || file.isDirectory()) {
			return null;
		} else {
			final String fileName = FileUtilities.getFilenameFromPath(file.getName());
			return new FileInfo(fileName, file.getSize(), getChangeDate(file));
		}
	}

	@Override
	public FolderInfo getFolderInfo(final String folderPath) throws Exception {
		checkForConnection();

		final FTPFile file = ftpClient.mlistFile(folderPath);
		if (file == null || file.isFile()) {
			return null;
		} else {
			final String folderName = FileUtilities.getFilenameFromPath(file.getName());
			return new FolderInfo(folderName, getChangeDate(file));
		}
	}

	private static ZonedDateTime getChangeDate(final FTPFile file) {
		if (file.getTimestamp() == null) {
			// Not every server reports a timestamp
			return null;
		} else {
			return DateUtilities.getLocalDateTimeForDate(file.getTimestamp().getTime()).atZone(ZoneId.systemDefault());
		}
	}

	class FtpInputStream extends FilterInputStream {
		private final FtpConnection ftpConnection;
		private boolean isClosed = false;

		protected FtpInputStream(final InputStream inputStream, final FtpConnection ftpConnection) {
			super(inputStream);
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

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String folderPath) throws Exception {
		checkForConnection();

		folderPath = folderPath.replace("\\", "/");
		String folderName;
		if (folderPath.contains("/")) {
			folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
		} else {
			folderName = folderPath;
		}
		if ("".equals(folderName)) {
			folderName = "/";
		}

		final List<FolderInfo> folderInfoList = new ArrayList<>();
		// Replacing blanks with escaped blanks is a workaround for FtpClient bug, which returns an empty list on foldernames with blanks
		final FTPFile[] directories = ftpClient.listDirectories(folderPath.replace(" ", "\\ "));
		if (directories != null) {
			for (final FTPFile directory : directories) {
				final String directoryName = directory.getName();
				if (!".".equals(directoryName) && !"..".equals(directoryName) && directory.isDirectory()) {
					folderInfoList.add(getFolderInfo(folderPath + "/" + directoryName));
				}
			}
		}

		final List<FileInfo> fileInfoList = new ArrayList<>();
		// Replacing blanks with escaped blanks is a workaround for FtpClient bug, which returns an empty list on foldernames with blanks
		final FTPFile[] files = ftpClient.listFiles(folderPath.replace(" ", "\\ "));
		if (files != null) {
			for (final FTPFile file : files) {
				if (file.isFile()) {
					fileInfoList.add(new FileInfo(file.getName(), file.getSize(), getChangeDate(file)));
				}
			}
		}

		return new Tuple<>(folderInfoList, fileInfoList);
	}
}
