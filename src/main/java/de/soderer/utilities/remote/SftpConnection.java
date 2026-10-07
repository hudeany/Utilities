package de.soderer.utilities.remote;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;

import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.JschFileData;
import de.soderer.utilities.JschFileData.JschFileType;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.SshFileChangeWatcherThread.LineListener;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.UnknownHostKeyException;
import de.soderer.utilities.Utilities;

/**
 * Remote connection via SSH (scp and shell commands), based on JSch. Authentication by key file and/or password.
 * The server's host key is checked against a known hosts file or an expected fingerprint (see {@link #DISABLE_HOST_KEY_CHECK}).
 * Paths are passed to the remote shell in single quotes.
 */
public class SftpConnection implements RemoteConnection {
	/**
	 * Return value of InputStream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The keyFileData. */
	private byte[] keyFileData = null;

	/** The password. */
	private char[] password = null;

	private ProxyConfiguration proxyConfiguration = null;

	/**
	 * The port.<br />
	 * Default port for SFTP is 22
	 */
	private int port = 22;

	/**
	 * Sentinel value for {@link #hostKey}: pass this value explicitly to disable host key checking entirely.
	 * This is insecure (no protection against man-in-the-middle attacks) and should only ever be used
	 * deliberately, e.g. for throwaway test environments. Never rely on this being the default.
	 */
	public static final String DISABLE_HOST_KEY_CHECK = "<<INSECURE_DISABLE_HOST_KEY_CHECK>>";

	/**
	 * The hostKey for ssh and sftp
	 * if null or empty: check against local known host keys (~/.ssh/known_hosts) (secure default)<br />
	 * if filepath: check against specific known host keys file<br />
	 * if filled with hash value: check against this fingerprint<br />
	 * if equal to {@link #DISABLE_HOST_KEY_CHECK}: do not check at all (insecure, explicit opt-in only)<br />
	 */
	private String hostKey = null;

	private Session session = null;

	private OutputStream newKnownHosts;

	/**
	 * Creates a connection from access data.
	 *
	 * @param serverEntry access data with protocol "ssh" or "sftp"; its host key is a known hosts file path, a fingerprint or {@link #DISABLE_HOST_KEY_CHECK}
	 * @throws Exception if the protocol is not supported or the connection fails
	 */
	public SftpConnection(final ServerAccessData serverEntry) throws Exception {
		this(serverEntry, null);
	}

	/**
	 * Creates a connection from access data and writes the host key, if it was verified by fingerprint, in known hosts format.
	 *
	 * @param serverEntry access data with protocol "ssh" or "sftp"; its host key is a known hosts file path, a fingerprint or {@link #DISABLE_HOST_KEY_CHECK}
	 * @param newKnownHosts stream receiving the verified host key, may be null
	 * @throws Exception if the protocol is not supported or the connection fails
	 */
	public SftpConnection(final ServerAccessData serverEntry, final OutputStream newKnownHosts) throws Exception {
		if (!"ssh".equalsIgnoreCase(serverEntry.getProtocol()) && !"sftp".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for SFTP connection. Only 'ssh' and 'sftp' are allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			// Password only access data have no key file
			keyFileData = Utilities.isBlank(serverEntry.getKeyFile()) ? null : FileUtilities.readFileToByteArray(new File(Utilities.replaceUsersHome(serverEntry.getKeyFile())));
			password = serverEntry.getPassword();
			port = serverEntry.getPort();
			hostKey = serverEntry.getHostKey();

			this.newKnownHosts = newKnownHosts;

			connect();
		}
	}

	/**
	 * Creates a connection.
	 *
	 * @param hostname host name of the server
	 * @param username user name
	 * @param keyFileData content of the private key file, or null for password authentication
	 * @param password password, or passphrase of the key file
	 * @param port port of the server
	 * @param hostKey known hosts file path (null for "~/.ssh/known_hosts"), expected fingerprint ("SHA256:..." or MD5 hex) or {@link #DISABLE_HOST_KEY_CHECK}
	 * @throws Exception if the connection fails
	 */
	public SftpConnection(final String hostname, final String username, final byte[] keyFileData, final char[] password, final int port, final String hostKey) throws Exception {
		this(hostname, username, keyFileData, password, port, hostKey, null);
	}

	/**
	 * Creates a connection and writes the host key, if it was verified by fingerprint, in known hosts format.
	 *
	 * @param hostname host name of the server
	 * @param username user name
	 * @param keyFileData content of the private key file, or null for password authentication
	 * @param password password, or passphrase of the key file
	 * @param port port of the server
	 * @param hostKey known hosts file path (null for "~/.ssh/known_hosts"), expected fingerprint ("SHA256:..." or MD5 hex) or {@link #DISABLE_HOST_KEY_CHECK}
	 * @param newKnownHosts stream receiving the verified host key, may be null
	 * @throws Exception if the connection fails
	 */
	public SftpConnection(final String hostname, final String username, final byte[] keyFileData, final char[] password, final int port, final String hostKey, final OutputStream newKnownHosts) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.keyFileData = keyFileData;
		this.password = password;
		this.port = port;
		this.hostKey = hostKey;

		this.newKnownHosts = newKnownHosts;

		connect();
	}

	/**
	 * Sets the proxy configuration (HTTP proxy). As the connection is established by the constructor already, it is
	 * established again with the proxy.
	 *
	 * @param proxyConfiguration proxy configuration, null for a direct connection
	 * @return this connection for chaining
	 * @throws IllegalStateException if the new connection fails
	 */
	public SftpConnection setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
		this.proxyConfiguration = proxyConfiguration;
		// The constructors connect immediately, so the proxy only takes effect with a new connection
		if (session != null) {
			close();
			try {
				connect();
			} catch (final Exception e) {
				throw new IllegalStateException("Cannot reconnect with proxy configuration: " + e.getMessage(), e);
			}
		}
		return this;
	}

	private void connect() throws Exception {
		JSch jsch = null;
		try {
			jsch = new JSch();
			session = jsch.getSession(username, hostname, port);

			if (proxyConfiguration != null) {
				final Proxy proxy = proxyConfiguration.getProxy(hostname);
				if (proxy != null && !Proxy.NO_PROXY.equals(proxy)) {
					final InetSocketAddress proxyAdress = (InetSocketAddress) proxy.address();
					session.setProxy(new ProxyHTTP(proxyAdress.getHostName(), proxyAdress.getPort()));
				}
			}

			if (keyFileData != null) {
				jsch.addIdentity(username, keyFileData, null, password == null ? new byte[0] : new String(password).getBytes(StandardCharsets.UTF_8));
			} else if (password != null) {
				session.setPassword(new String(password));
			} else {
				throw new Exception("At least one of keyFileData or password is expected");
			}

			boolean checkHostKeyFingerprint = false;
			if (DISABLE_HOST_KEY_CHECK.equals(hostKey)) {
				System.err.println("WARNING: SSH host key checking is explicitly disabled for host '" + hostname
						+ "'. This connection is not protected against man-in-the-middle attacks.");
				session.setConfig("StrictHostKeyChecking", "no");
			} else if (isHostKeyFingerprint(hostKey)) {
				// The fingerprint is checked during the key exchange, so no credentials are sent to a host with an unexpected key
				jsch.setHostKeyRepository(new FingerprintHostKeyRepository(hostKey));
				session.setConfig("StrictHostKeyChecking", "yes");
				checkHostKeyFingerprint = true;
			} else {
				// Secure default: null or empty falls back to the local known_hosts file
				File knownHostsFile;
				if (hostKey == null || "".equals(hostKey)) {
					knownHostsFile = new File(Utilities.replaceUsersHome("~/.ssh/known_hosts"));
				} else {
					knownHostsFile = new File(Utilities.replaceUsersHome(hostKey));
				}

				if (knownHostsFile.isFile()) {
					try (FileInputStream fileInputStream = new FileInputStream(knownHostsFile)) {
						jsch.setKnownHosts(fileInputStream);
					}
				} else {
					throw new Exception("Cannot find known hosts file: " + knownHostsFile.getAbsolutePath());
				}
			}

			// If new hostkeys are added, they are stored in hashed format
			session.setConfig("HashKnownHosts", "yes");

			// Do not prefer Kerberos authentication
			session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");

			// Workaround fix for "ecdsa-sha2-nistp256" knownhostkeys
			// Otherwise the command "ssh-keyscan -H -t rsa <hostname> >> ~/.ssh/known_hosts" may help
			//			final HostKeyRepository hkr = jsch.getHostKeyRepository();
			//			for (final HostKey hk : hkr.getHostKey()) {
			//				if (hk.getHost().equalsIgnoreCase(hostname)) {
			//					final String type = hk.getType();
			//					session.setConfig("server_host_key", type);
			//				}
			//			}

			session.connect();

			if (checkHostKeyFingerprint && newKnownHosts != null) {
				// The host key was verified by its fingerprint, so it can be stored as known host
				final HostKey actualHostKey = session.getHostKey();
				newKnownHosts.write((actualHostKey.getHost() + " " + actualHostKey.getType() + " " + actualHostKey.getKey() + "\n").getBytes(StandardCharsets.UTF_8));
			}
		} catch (final JSchException jse) {
			if (jse.getCause() != null && jse.getCause() instanceof UnknownHostException) {
				close();
				throw new Exception("Cannot connect to host: " + hostname);
			} else if (jse.getMessage() != null && (jse.getMessage().contains("UnknownHostKey") || jse.getMessage().contains("reject HostKey"))) {
				final HostKey actualHostKey = session.getHostKey();
				final String actualHostKeyFingerprint = actualHostKey == null ? null : actualHostKey.getFingerPrint(jsch);
				close();
				throw new UnknownHostKeyException("Cannot connect to host, because its hostkey is unknown or does not match the expected fingerprint", hostname, actualHostKeyFingerprint);
			} else {
				close();
				throw jse;
			}
		} catch (final UnknownHostException uhe) {
			close();
			throw uhe;
		} catch (final Exception e) {
			close();
			throw new Exception("Error in opening ssh session: " + ExceptionUtilities.toString(e), e);
		}
	}

	/**
	 * Returns the names of the symbolic links in a folder, which point to folders.
	 *
	 * @param directoryPath path of the folder
	 * @return sorted names of the links to folders
	 * @throws Exception if the folder cannot be listed
	 */
	public List<String> getFolderLinks(final String directoryPath) throws Exception {
		final List<String> folderList = new ArrayList<>();
		final List<String> linkList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read links
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + quoteForShell(directoryPath + (directoryPath.endsWith("/") ? "" : "/")) + " -mindepth 1 -maxdepth 1 -type l");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {

				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, StandardCharsets.UTF_8)) {
					linkList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = IoUtilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, StandardCharsets.UTF_8));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}

		for (final String link : linkList) {
			if (getFolderInfo(directoryPath + "/" + link) != null) {
				folderList.add(link);
			}
		}
		Collections.sort(folderList);
		return folderList;
	}

	/**
	 * Returns the names of the symbolic links in a folder, which point to files.
	 *
	 * @param filePath path of the folder
	 * @return sorted names of the links to files
	 * @throws Exception if the folder cannot be listed
	 */
	public List<String> getFileLinks(final String filePath) throws Exception {
		final List<String> fileList = new ArrayList<>();
		final List<String> linkList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read links
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + quoteForShell(filePath + (filePath.endsWith("/") ? "" : "/")) + " -mindepth 1 -maxdepth 1 -type l");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {

				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, StandardCharsets.UTF_8)) {
					linkList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = IoUtilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, StandardCharsets.UTF_8));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}

		for (final String link : linkList) {
			if (getFileInfo(filePath + "/" + link) != null) {
				fileList.add(link);
			}
		}
		Collections.sort(fileList);
		return fileList;
	}

	@Override
	public void createFolder(final String selectedFilePath, final String folderName) throws Exception {
		executeCommand("mkdir " + quoteForShell(selectedFilePath + "/" + folderName));
	}

	@Override
	public void uploadFile(final String filePath, final InputStream inputStream) throws Exception {
		ChannelSftp channel = null;
		try {
			channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();
			channel.put(inputStream, filePath);
		} catch (final Exception e) {
			throw new Exception("Error in uploadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Uploads data into a remote file via scp, reading the complete data into memory first.
	 *
	 * @param filePath path of the remote file
	 * @param fileInputStream data to upload, is not closed
	 * @throws Exception if the upload fails
	 */
	public void uploadFileAlternative(final String filePath, final InputStream fileInputStream) throws Exception {
		Channel channel = null;
		try {
			final byte[] fileData = IoUtilities.toByteArray(fileInputStream);
			String command = "scp -p -t " + quoteForShell(filePath);
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();

				checkAcknowledge(inStream);

				// send command "C0664 filesize filename"
				final String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
				command = "C0664 " + fileData.length + " " + fileName + "\n";
				outStream.write(command.getBytes(StandardCharsets.UTF_8));
				outStream.flush();

				checkAcknowledge(inStream);

				// send content of file
				outStream.write(fileData, 0, fileData.length);

				// send '\0' as EOF
				sendZeroByte(outStream);

				checkAcknowledge(inStream);
			}
		} catch (final Exception e) {
			throw new Exception("Error in uploadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		Channel channel = null;
		try {
			String fileName;
			if (filePath.contains("/")) {
				fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			} else {
				fileName = filePath;
			}

			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -ld " + quoteForShell(filePath));

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				final byte[] successData = IoUtilities.toByteArray(inStream);
				final String successString = new String(successData, StandardCharsets.UTF_8);

				final byte[] errorData = IoUtilities.toByteArray(inErrorStream);

				if (errorData != null && errorData.length > 0) {
					return null;
				} else {
					final JschFileData fileData = new JschFileData(successString);
					if (fileData.getFileType() == JschFileType.Directory) {
						// Directories are no files
						return null;
					}
					final ZonedDateTime changeDate = fileData.getChangeDate().atZone(ZoneId.systemDefault());
					return new FileInfo(fileName, fileData.getSize(), changeDate);
				}
			}
		} finally {
			closeQuietly(channel);
		}
	}

	@Override
	public InputStream downloadFile(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("scp -f " + quoteForShell(filePath));

			final OutputStream outStream = channel.getOutputStream();
			final InputStream inStream = channel.getInputStream();

			channel.connect();

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			// scp answers with "C<rights> <size> <name>" or with an error marker byte 1 or 2 followed by the message
			final int firstByte = inStream.read();
			if (firstByte == 1 || firstByte == 2 || firstByte < 0) {
				final byte[] errortextData = readStreamUntilEndOrLinefeed(inStream);
				final String errortext = new String(errortextData, StandardCharsets.UTF_8).trim();
				throw new Exception("Download of file '" + filePath + "' failed: " + errortext);
			}

			// read rights key like '0644 ' (5 bytes), the leading 'C' was read above
			final byte[] rightsData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String rights = null;
			if (rightsData != null && rightsData.length > 0) {
				rights = new String(rightsData, StandardCharsets.UTF_8);
			}

			// read filesize (if empty filesize read until no more data)
			final byte[] filesizeData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			long filesize = -1;
			if (filesizeData != null && filesizeData.length > 0) {
				filesize = Long.parseLong(new String(filesizeData));
			}

			// read filename (even if it is not used on single download)
			final byte[] filenameData = readStreamUntilEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String filename = null;
			if (filenameData != null && filenameData.length > 0) {
				filename = new String(filenameData, StandardCharsets.UTF_8);
			}

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			return new SftpInputStream(channel, outStream, inStream, filesize);
		} catch (final Exception e) {
			closeQuietly(channel);
			throw new Exception("Error in downloadFileData", e);
		}
	}

	@Override
	public void deleteFile(final String remoteHostPathAndFileName) throws Exception {
		try {
			// exec 'rm /full/path/to/file' remotely, a non zero exit code (e.g. missing permissions) results in an exception
			executeCommand("rm -f " + quoteForShell(remoteHostPathAndFileName));
		} catch (final Exception e) {
			throw new Exception("Error in SSH delete: " + e.getMessage(), e);
		}
	}

	@Override
	public void deleteFolder(final String remoteHostPathAndFolderName) throws Exception {
		try {
			// A non zero exit code (e.g. missing permissions) results in an exception
			executeCommand("rm -rf " + quoteForShell(remoteHostPathAndFolderName));
		} catch (final Exception e) {
			throw new Exception("Error in SSH delete: " + e.getMessage(), e);
		}
	}

	/**
	 * Uploads a local file via scp into an existing remote folder.
	 *
	 * @param remoteHostPathAndFileName remote file path, or remote folder path ending with "/" to keep the local file name
	 * @param localPathAndFileName path of the local file, without wildcards
	 * @throws Exception if the local file or the remote folder does not exist, the remote file exists already or the upload fails
	 */
	public void uploadFile(final String remoteHostPathAndFileName, final String localPathAndFileName) throws Exception {
		if (localPathAndFileName.contains("*") || localPathAndFileName.contains("?")) {
			throw new Exception("Error in SSH upload: Wildcards in localPathAndFileName are not allowed in SSH upload");
		}

		if (!new File(localPathAndFileName).exists()) {
			throw new Exception("Error in SSH upload: Source file '" + localPathAndFileName + "' does not exist");
		}

		final String remoteHostPath = getPathOfPathAndFileName(remoteHostPathAndFileName);
		String remoteHostFileName = getFileNameOfPathAndFileName(remoteHostPathAndFileName);
		if (Utilities.isEmpty(remoteHostFileName)) {
			remoteHostFileName = getFileNameOfPathAndFileName(localPathAndFileName);
		}

		final List<String> directory = listFiles(remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Error in SSH upload: Destination folder '" + remoteHostPath + "' does not exist on destination host");
		}
		if (directory.contains(remoteHostFileName)) {
			throw new Exception("Error in SSH upload: File '" + remoteHostFileName + "' already exists on destination host");
		}

		Channel channel = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + quoteForShell(remoteHostPathAndFileName);
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			try (OutputStream out = channel.getOutputStream();
					InputStream in = channel.getInputStream()) {
				channel.connect();

				checkAcknowledge(in);

				// send "C0664 filesize filename", where filename should not include '/'
				final long filesize = new File(localPathAndFileName).length();
				command = "C0664 " + filesize + " " + remoteHostFileName + "\n";
				out.write(command.getBytes(StandardCharsets.UTF_8));
				out.flush();
				checkAcknowledge(in);

				// send a content of lfile
				try (InputStream bufferedFileInputStream = new BufferedInputStream(new FileInputStream(localPathAndFileName))) {
					final byte[] buffer = new byte[4096];
					while (true) {
						final int len = bufferedFileInputStream.read(buffer);
						if (len <= 0) {
							break;
						}
						out.write(buffer, 0, len); // out.flush();
					}
					Utilities.clear(buffer);
					// send '\0'
					sendZeroByte(out);

					checkAcknowledge(in);

					out.close();

					channel.disconnect();
				}
			}
		} catch (final Exception e) {
			throw new Exception("Error in SSH upload: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Uploads data into a new remote file with permissions 664 via scp.
	 *
	 * @param remoteHostPath path of the existing remote folder
	 * @param remoteHostFileName name of the new remote file
	 * @param fileData data to upload
	 * @throws Exception if the remote folder does not exist, the remote file exists already or the upload fails
	 */
	public void uploadFileData(final String remoteHostPath, final String remoteHostFileName, final byte[] fileData) throws Exception {
		uploadFileData(remoteHostPath, remoteHostFileName, fileData, null, false);
	}

	/**
	 * Uploads data into a remote file via scp.
	 *
	 * @param remoteHostPath path of the existing remote folder
	 * @param remoteHostFileName name of the remote file
	 * @param fileData data to upload
	 * @param filePermissions permissions of the file, null for 664
	 * @param overwriteIfExists true to replace an existing file
	 * @throws Exception if the remote folder does not exist, the remote file exists and must not be replaced, or the upload fails
	 */
	public void uploadFileData(final String remoteHostPath, final String remoteHostFileName, final byte[] fileData, final FilePermissions filePermissions, final boolean overwriteIfExists) throws Exception {
		final List<String> directory = listFiles(remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Error in SSH upload: Destination folder '" + remoteHostPath + "' does not exist on destination host");
		}
		if (directory.contains(remoteHostFileName) && !overwriteIfExists) {
			throw new Exception("Error in SSH upload: File '" + remoteHostFileName + "' already exists on destination host");
		}

		Channel channel = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + quoteForShell(remoteHostPath + "/" + remoteHostFileName);
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			try (OutputStream out = channel.getOutputStream();
					InputStream in = channel.getInputStream()) {
				channel.connect();

				checkAcknowledge(in);

				String permissionsString = "664";
				if (filePermissions != null) {
					permissionsString = filePermissions.getPermissionMapString();
				}

				// send command "C0664 filesize filename"
				command = "C0" + permissionsString + " " + fileData.length + " " + remoteHostFileName + "\n";
				out.write(command.getBytes(StandardCharsets.UTF_8));
				out.flush();
				checkAcknowledge(in);

				// send content of file
				out.write(fileData, 0, fileData.length);

				// send '\0' as EOF
				sendZeroByte(out);

				checkAcknowledge(in);
				out.close();

				channel.disconnect();
			}
		} catch (final Exception e) {
			throw new Exception("Error in SSH upload: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Lists all entries of a remote folder (including "." and "..") via "ls -1a".
	 *
	 * @param remoteHostPath path of the folder
	 * @return names of all entries
	 * @throws Exception if the folder cannot be listed
	 */
	public List<String> listFiles(final String remoteHostPath) throws Exception {
		Channel channel = null;
		try {
			final List<String> returnList = new ArrayList<>();

			// exec 'ls -1a <path>' remotely
			final String command = "ls -1a " + quoteForShell(remoteHostPath);
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			// OutputStream out = channel.getOutputStream();
			try (InputStream in = channel.getInputStream()) {
				channel.connect();

				// byte[] buf = new byte[4096];

				// send '\0'
				// sendZeroByte(out);

				int charValue = checkAcknowledge(in);
				StringBuilder inputString = new StringBuilder();

				while (charValue != EOF) {
					if (charValue == '\n') { // EOL
						returnList.add(inputString.toString());
						inputString = new StringBuilder();
					} else {
						inputString.append((char) charValue);
					}
					charValue = checkAcknowledge(in);
				}

				return returnList;
			}
		} catch (final Exception e) {
			throw new Exception("Error in SSH download\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Shows the last lines of a remote file and then all lines appended to it ("tail -f"), until the thread is interrupted
	 * or the remote process ends.
	 *
	 * @param filePath path of the remote file
	 * @param numberOfLastLinesToShow number of existing lines to show first
	 * @param lineListener receiver of the data
	 * @throws Exception if the file cannot be read
	 */
	public void tailviewFileData(final String filePath, final int numberOfLastLinesToShow, final LineListener lineListener) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -f -n " + numberOfLastLinesToShow + " " + quoteForShell(filePath));

			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();

				// send '\0' to start data transfer
				sendZeroByte(outStream);

				while (true) {
					// reads all now available data at once
					byte[] nextData = IoUtilities.toByteArray(inStream);
					while (nextData.length > 0) {
						lineListener.addLineData(nextData);
						nextData = IoUtilities.toByteArray(inStream);
					}

					// waits if no more data is available right now
					final byte[] nextLineData = Utilities.readStreamUntilEndOrLinefeed(inStream);
					if (nextLineData.length == 0 && channel.isClosed()) {
						// The remote tail process has ended
						break;
					}
					lineListener.addLineData(nextLineData);
				}
			}
		} catch (@SuppressWarnings("unused") final java.io.InterruptedIOException e) {
			// do nothing
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Reads the last lines of a remote file ("tail -n").
	 *
	 * @param filePath path of the remote file
	 * @param numberOfLastLinesToShow number of lines to read
	 * @return data of the last lines
	 * @throws Exception if the file cannot be read
	 */
	public byte[] readTailFileData(final String filePath, final int numberOfLastLinesToShow) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -n " + numberOfLastLinesToShow + " " + quoteForShell(filePath));

			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();

				// send '\0' to start data transfer
				sendZeroByte(outStream);

				return IoUtilities.toByteArray(inStream);
			}
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Executes a remote command and returns stdout, stderr and the exit code separately.
	 * Unlike {@link #executeCommand(String)}, this does NOT throw merely because the exit code
	 * is non-zero or stderr received output - it only throws on actual connection/channel errors,
	 * leaving the interpretation of the exit code to the caller. Arguments taken from user or server data must be quoted
	 * for the remote shell by the caller.
	 *
	 * @param command shell command to execute
	 * @return stdout, stderr and exit code (output decoded as UTF-8)
	 * @throws Exception if the command cannot be executed
	 */
	public CommandResult executeCommandWithResult(final String command) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote command execution
			try (InputStream in = channel.getInputStream();
					InputStream inError = channel.getExtInputStream()) {
				channel.connect();

				// stderr is read in parallel, otherwise a remote process writing much to stderr blocks before closing stdout
				final ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
				final Thread errorReader = new Thread(() -> {
					try {
						inError.transferTo(errorBuffer);
					} catch (@SuppressWarnings("unused") final IOException e) {
						// Channel closed
					}
				}, "SftpCommandErrorReader");
				errorReader.setDaemon(true);
				errorReader.start();
				final String returnText = readCommandOutputStream(in);
				errorReader.join();
				final String returnError = errorBuffer.toString(StandardCharsets.UTF_8);

				// Wait until the remote command has actually finished, so getExitStatus() is reliable
				while (!channel.isClosed()) {
					Thread.sleep(50);
				}
				final int exitStatus = channel.getExitStatus();

				return new CommandResult(returnText, returnError, exitStatus);
			}
		} catch (final Exception e) {
			throw new Exception("Error in SSH command\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Convenience wrapper over {@link #executeCommandWithResult(String)} for callers that just want
	 * a single text result and are happy with exception-based error handling: throws if the
	 * command's exit code is non-zero, otherwise returns stdout if it has content, falling back to
	 * stderr if stdout is blank (some commands write their normal, non-error output to stderr, e.g.
	 * "java -version").
	 *
	 * @param command shell command to execute
	 * @return output of the command
	 * @throws Exception if the command cannot be executed or exits with a non-zero code
	 */
	public String executeCommand(final String command) throws Exception {
		final CommandResult result = executeCommandWithResult(command);
		if (!result.isSuccessful()) {
			throw new Exception("Command exited with code " + result.getExitCode() + (Utilities.isNotBlank(result.getStdErr()) ? ":\n" + result.getStdErr() : ""));
		} else if (Utilities.isNotBlank(result.getStdOut())) {
			return result.getStdOut();
		} else {
			// Some commands write to stderr although nothing went wrong
			return result.getStdErr();
		}
	}

	/**
	 * Reads a command's stdout or stderr stream fully as plain bytes until EOF.
	 * Unlike {@link #checkAcknowledge(InputStream)}, this does NOT interpret byte values 1/2 as
	 * SCP-protocol error markers, since this is ordinary command output, not an SCP ack byte stream.
	 */
	private static String readCommandOutputStream(final InputStream inputStream) throws IOException {
		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}

	@Override
	public FolderInfo getFolderInfo(String folderPath) throws Exception {
		folderPath = FileUtilities.trimTrailingFileSeparator(folderPath);

		String folderName;
		if (folderPath.contains("/")) {
			folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
		} else {
			folderName = folderPath;
		}

		ZonedDateTime modificationDate = null;

		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -al " + quoteForShell(Utilities.isEmpty(folderPath) ? "/" : folderPath));

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				final byte[] successData = IoUtilities.toByteArray(inStream);
				final String successString = new String(successData, StandardCharsets.UTF_8);

				final byte[] errorData = IoUtilities.toByteArray(inErrorStream);

				if (errorData != null && errorData.length > 0) {
					if (new String(errorData, StandardCharsets.UTF_8).toLowerCase().contains("permission denied")) {
						throw new PermissionDeniedException("Permission denied", folderPath);
					}
					return null;
				} else {
					try (BufferedReader reader = new BufferedReader(new StringReader(successString))) {
						String nextLine;
						boolean isFirstLine = true;
						while ((nextLine = reader.readLine()) != null) {
							if (!isFirstLine || !(nextLine.startsWith("total") || nextLine.startsWith("insgesamt"))) {
								final JschFileData fileData = new JschFileData(nextLine);
								if (".".equals(fileData.getFileName())) {
									modificationDate = fileData.getChangeDate().atZone(ZoneId.systemDefault());
									break;
								}
							}
							isFirstLine = false;
						}
					}


				}
			}
		} finally {
			closeQuietly(channel);
		}

		if (Utilities.isEmpty(folderName)) {
			folderName = "/";
		}
		return new FolderInfo(folderName, modificationDate);
	}

	/**
	 * Close connection
	 */
	@Override
	public void close() {
		if (session != null) {
			try {
				session.disconnect();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}
			session = null;
		}
	}

	/**
	 * Checks whether a hostKey setting is a fingerprint ("SHA256:base64" or MD5 as 16 hex bytes, optionally with
	 * ":" separators and "MD5:" prefix) instead of a known hosts file path.
	 */
	static boolean isHostKeyFingerprint(final String hostKeySetting) {
		if (hostKeySetting == null) {
			return false;
		}
		final String value = hostKeySetting.trim();
		return value.matches("(?i)SHA256:[A-Za-z0-9+/]+=*") || value.matches("(?i)(MD5:)?[0-9a-f]{2}(:?[0-9a-f]{2}){15}");
	}

	/**
	 * Checks whether a host key (public key blob) matches the expected fingerprint in SHA256 (base64) or MD5 (hex) format.
	 */
	static boolean matchesHostKeyFingerprint(final String expectedFingerprint, final byte[] hostKeyBlob) throws Exception {
		final String expected = expectedFingerprint.trim();
		if (expected.regionMatches(true, 0, "SHA256:", 0, 7)) {
			final String actualSha256 = java.util.Base64.getEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(hostKeyBlob));
			return actualSha256.equals(expected.substring(7).replaceAll("=+$", ""));
		} else {
			final String expectedHex = expected.replaceFirst("(?i)^MD5:", "").replace(":", "").toLowerCase(java.util.Locale.ROOT);
			final StringBuilder actualHex = new StringBuilder();
			for (final byte nextByte : java.security.MessageDigest.getInstance("MD5").digest(hostKeyBlob)) {
				actualHex.append(String.format("%02x", nextByte));
			}
			return actualHex.toString().equals(expectedHex);
		}
	}

	/**
	 * Host key repository accepting exactly the host key with the expected fingerprint.
	 */
	private static class FingerprintHostKeyRepository implements HostKeyRepository {
		private final String expectedFingerprint;

		FingerprintHostKeyRepository(final String expectedFingerprint) {
			this.expectedFingerprint = expectedFingerprint;
		}

		@Override
		public int check(final String host, final byte[] key) {
			try {
				return matchesHostKeyFingerprint(expectedFingerprint, key) ? OK : NOT_INCLUDED;
			} catch (@SuppressWarnings("unused") final Exception e) {
				return NOT_INCLUDED;
			}
		}

		@Override
		public void add(final HostKey hostkey, final com.jcraft.jsch.UserInfo userInfo) {
			// Keys are never added, only the expected fingerprint is accepted
		}

		@Override
		public void remove(final String host, final String type) {
			// Nothing stored
		}

		@Override
		public void remove(final String host, final String type, final byte[] key) {
			// Nothing stored
		}

		@Override
		public String getKnownHostsRepositoryID() {
			return "Fingerprint " + expectedFingerprint;
		}

		@Override
		public HostKey[] getHostKey() {
			return new HostKey[0];
		}

		@Override
		public HostKey[] getHostKey(final String host, final String type) {
			return new HostKey[0];
		}
	}

	/**
	 * Quotes a value for use as a single argument in a remote POSIX shell command. Single quotes are used, because
	 * within double quotes the shell still expands "$(...)", backticks and variables, which would allow command
	 * injection via file names (e.g. names received from the server itself).
	 */
	static String quoteForShell(final String value) {
		return "'" + value.replace("'", "'\"'\"'") + "'";
	}

	private static int checkAcknowledge(final InputStream in) throws IOException {
		final int returnCode = in.read();
		// returnCode may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1 EOF
		if (returnCode == 1 || returnCode == 2) {
			final StringBuffer returnTextBuffer = new StringBuffer();
			int charReadBuffer;
			do {
				charReadBuffer = in.read();
				returnTextBuffer.append((char) charReadBuffer);
			} while (charReadBuffer != '\n');

			if (returnCode == 1) { // error
				throw new IOException("Error on SSH conection: " + returnTextBuffer.toString());
			} else { // fatal error
				throw new IOException("Fatal error on SSH conection: " + returnTextBuffer.toString());
			}
		} else {
			return returnCode;
		}
	}

	private static String getFileNameOfPathAndFileName(final String pathAndFileName) {
		if (Utilities.isEmpty(pathAndFileName)) {
			return null;
		} else {
			final int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(lastIndexOfPathSeparator + 1);
		}
	}

	private static String getPathOfPathAndFileName(final String pathAndFileName) {
		if (Utilities.isEmpty(pathAndFileName)) {
			return null;
		} else {
			final int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(0, lastIndexOfPathSeparator + 1);
		}
	}

	private static void sendZeroByte(final OutputStream outputStream) throws IOException {
		outputStream.write(new byte[] { 0 });
		outputStream.flush();
	}

	private static byte[] readStreamUntilBlankOrEndOrLinefeed(final InputStream inStream) throws IOException {
		final ByteArrayOutputStream returnData = new ByteArrayOutputStream();
		int nextByte;
		while (true) {
			nextByte = inStream.read();
			if (nextByte < 0) {
				// error
				break;
			} else if (nextByte == ' ' || nextByte == '\n') {
				break;
			} else {
				returnData.write(nextByte);
			}
		}
		return returnData.toByteArray();
	}

	private static byte[] readStreamUntilEndOrLinefeed(final InputStream inStream) throws IOException {
		final ByteArrayOutputStream returnData = new ByteArrayOutputStream();
		int nextByte;
		while (true) {
			nextByte = inStream.read();
			if (nextByte < 0) {
				// error
				break;
			} else if (nextByte == '\n') {
				break;
			} else {
				returnData.write(nextByte);
			}
		}
		return returnData.toByteArray();
	}

	private static void closeQuietly(final Channel channel) {
		try {
			if (channel != null) {
				channel.disconnect();
			}
		} catch (@SuppressWarnings("unused") final Exception ioe) {
			// ignore
		}
	}

	private class SftpInputStream extends InputStream {
		private final Channel channel;
		private final OutputStream controllOutputStream;
		private final InputStream dataInputStream;
		private long available;
		private boolean isClosed = false;

		protected SftpInputStream(final Channel channel, final OutputStream controllOutputStream, final InputStream dataInputStream, final long available) {
			this.channel = channel;
			this.controllOutputStream = controllOutputStream;
			this.dataInputStream = dataInputStream;
			this.available = available;
		}

		@Override
		public void close() throws IOException {
			if (isClosed) {
				return;
			}
			isClosed = true;
			try {
				checkAcknowledge(dataInputStream);

				// send '\0' to confirm the end of the transfer
				sendZeroByte(controllOutputStream);
			} finally {
				// The channel was only opened for this download
				closeQuietly(channel);
			}
		}

		@Override
		public int read(final byte[] buffer, final int offset, final int length) throws IOException {
			if (isClosed) {
				throw new IOException("Reading from a closed SftpInputStream");
			} else if (length == 0) {
				return 0;
			} else if (available <= 0) {
				return EOF;
			} else {
				final int bytesRead = dataInputStream.read(buffer, offset, (int) Math.min(length, available));
				if (bytesRead > 0) {
					available -= bytesRead;
				}
				return bytesRead;
			}
		}

		@Override
		public int read() throws IOException {
			if (isClosed) {
				throw new IOException("Reading from a closed SftpInputStream");
			} else if (available > 0) {
				final int nextByte = dataInputStream.read();
				if (nextByte != EOF) {
					available--;
				}
				return nextByte;
			} else {
				return EOF;
			}
		}
	}

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String folderPath) throws Exception {
		final List<FileInfo> fileInfoList = new ArrayList<>();
		final List<String> subFolderNames = new ArrayList<>();

		folderPath = FileUtilities.trimTrailingFileSeparator(folderPath);

		String folderName;
		if (folderPath.contains("/")) {
			folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
		} else {
			folderName = folderPath;
		}

		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -al " + quoteForShell(Utilities.isEmpty(folderPath) ? "/" : folderPath));

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				final byte[] successData = IoUtilities.toByteArray(inStream);
				final String successString = new String(successData, StandardCharsets.UTF_8);

				final byte[] errorData = IoUtilities.toByteArray(inErrorStream);

				if (errorData != null && errorData.length > 0) {
					return null;
				} else {
					try (BufferedReader reader = new BufferedReader(new StringReader(successString))) {
						String nextLine;
						boolean isFirstLine = true;
						while ((nextLine = reader.readLine()) != null) {
							if (!isFirstLine || !(nextLine.startsWith("total") || nextLine.startsWith("insgesamt"))) {
								final JschFileData fileData = new JschFileData(nextLine);
								if (!".".equals(fileData.getFileName()) && !"..".equals(fileData.getFileName())) {
									if (fileData.getFileType() == JschFileType.RegularFile) {
										final ZonedDateTime changeDate = fileData.getChangeDate().atZone(ZoneId.systemDefault());
										fileInfoList.add(new FileInfo(fileData.getFileName(), fileData.getSize(), changeDate));
									} else if (fileData.getFileType() == JschFileType.SymbolicLink) {
										final ZonedDateTime changeDate = fileData.getChangeDate().atZone(ZoneId.systemDefault());
										fileInfoList.add(new FileInfo(fileData.getFileName(), fileData.getSize(), changeDate));
									} else if (fileData.getFileType() == JschFileType.Directory) {
										subFolderNames.add(fileData.getFileName());
									}
								}
							}
							isFirstLine = false;
						}
					}
				}
			}
		} finally {
			closeQuietly(channel);
		}

		final List<FolderInfo> folderInfoList = new ArrayList<>();

		for (final String subFolderName : subFolderNames) {
			FolderInfo folderInfo;
			try {
				folderInfo = getFolderInfo(folderPath + "/" + subFolderName);
				if (folderInfo == null) {
					folderInfo = new FolderInfo(subFolderName, null);
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				folderInfo = new FolderInfo(subFolderName, null);
			}
			folderInfoList.add(folderInfo);
		}

		if (Utilities.isEmpty(folderName)) {
			folderName = "/";
		}
		return new Tuple<>(folderInfoList, fileInfoList);
	}
}
