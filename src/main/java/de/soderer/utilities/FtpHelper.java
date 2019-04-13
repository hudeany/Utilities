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
 * The Class FtpHelper.
 * It uses FTP passive mode by default
 */
public class FtpHelper implements Closeable {

	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(FtpHelper.class);

	/** The host. */
	private String host = null;

	/** The user. */
	private String user = null;

	/** The password. */
	private String password = null;

	/** The port. */
	private int port = 21; // Default port for FTP is 21

	/**
	 * usePassiveMode
	 */
	private final boolean usePassiveMode = true;

	/**
	 * Currently connected client
	 */
	private FTPClient ftpClient = null;

	/**
	 * Instantiates a new ftp helper.
	 * Default port for FTP is 21.
	 *
	 * @param host the host
	 * @param user the user
	 * @param password the password
	 */
	public FtpHelper(final String host, final String user, final String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	/**
	 * Instantiates a new ftp helper.
	 *
	 * @param host the host
	 * @param user the user
	 * @param password the password
	 * @param port the port
	 */
	public FtpHelper(final String host, final String user, final String password, final int port) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.port = port;
	}

	/**
	 * Instantiates a new ftp helper.
	 * Default port for FTP is 21
	 *
	 * @param fileServerAndAuthConfigString like "ftp://[username[:password]@]server[:port]"
	 */
	public FtpHelper(String fileServerAndAuthConfigString) throws Exception {
		if (fileServerAndAuthConfigString.toLowerCase().startsWith("sftp://")) {
			throw new Exception("Invalid protocol for FtpHelper");
		} else if (fileServerAndAuthConfigString.toLowerCase().startsWith("ftp://")) {
			fileServerAndAuthConfigString = fileServerAndAuthConfigString.substring(6);
		}

		if (fileServerAndAuthConfigString.contains("@")) {
			final int hostSeparatorIndex = fileServerAndAuthConfigString.lastIndexOf("@");
			final String authPart = fileServerAndAuthConfigString.substring(0, hostSeparatorIndex);
			final String serverPart = fileServerAndAuthConfigString.substring(hostSeparatorIndex + 1);

			if (authPart.contains(":")) {
				user = authPart.substring(0, authPart.indexOf(":"));
				password = authPart.substring(authPart.indexOf(":") + 1);
			} else {
				user = authPart;
			}

			if (serverPart.contains(":")) {
				host = serverPart.substring(0, serverPart.indexOf(":"));
				port = Integer.parseInt(serverPart.substring(serverPart.indexOf(":") + 1));
			} else {
				host = serverPart;
			}
		} else {
			host = fileServerAndAuthConfigString;
		}
	}

	/**
	 * Connect.
	 *
	 * @throws Exception the exception
	 */
	public void connect() throws Exception {
		try {
			ftpClient = new FTPClient();
			ftpClient.connect(host, port);
			if (usePassiveMode) {
				ftpClient.enterLocalPassiveMode();
			}
			if (!ftpClient.login(user, password)) {
				ftpClient.logout();
				throw new Exception("Authentication failed");
			}
			ftpClient.setListHiddenFiles(true);
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

	class FtpInputStream extends FilterInputStream {
		FtpHelper ftpHelper;
		boolean isClosed = false;

		protected FtpInputStream(final InputStream in, final FtpHelper ftpHelper) {
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

	public static FtpHelper openConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"ftp".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for FTP connection. Only 'ftp' is allowed");
		}
		FtpHelper ftpHelper = null;
		try {
			ftpHelper = new FtpHelper(serverEntry.getHostname(), serverEntry.getUsername(), new String(serverEntry.getPassword()), serverEntry.getPort());
			ftpHelper.connect();
			return ftpHelper;
		} catch (final Exception e) {
			closeQuietly(ftpHelper);
			throw new Exception("Error in opening ftp connection: " + ExceptionUtilities.toString(e), e);
		}
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

	public static void closeQuietly(final FtpHelper ftpHelper) {
		try {
			if (ftpHelper != null) {
				ftpHelper.close();
			}
		} catch (final Exception ioe) {
			// ignore
		}
	}
}
