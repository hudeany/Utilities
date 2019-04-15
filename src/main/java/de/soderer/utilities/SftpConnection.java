package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.soderer.utilities.SshFileChangeWatcherThread.LineListener;

public class SftpConnection implements Closeable {
	private static final transient Logger logger = LoggerFactory.getLogger(SftpConnection.class);

	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The keyFileData. */
	private byte[] keyFileData = null;

	/** The password. */
	private String password = null;

	/**
	 * The port.<br />
	 * Default port for SFTP is 22
	 */
	private int port = 22;

	/**
	 * The hostKey for ssh and sftp
	 * if null: do not check<br />
	 * if empty: check against local known host keys (~/.ssh/known_hosts)<br />
	 * if filepath: check against specific known host keys file<br />
	 * if filled with hash value: check against this fingerprint<br />
	 */
	private String hostKey = null;

	private Session session = null;

	public SftpConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"ssh".equalsIgnoreCase(serverEntry.getProtocol()) && !"sftp".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for SFTP connection. Only 'ssh' and 'ftp' are allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			keyFileData = FileUtils.readFileToByteArray(new File(serverEntry.getKeyFile()));
			password = new String(serverEntry.getPassword());
			port = serverEntry.getPort();
			hostKey = serverEntry.getHostKey();

			connect();
		}
	}

	private SftpConnection connect() throws Exception {
		try {
			final JSch jsch = new JSch();
			session = jsch.getSession(username, hostname, port);

			if (keyFileData != null) {
				jsch.addIdentity(username, keyFileData, null, password == null ? new byte[0] : new String(password).getBytes("UTF-8"));
			} else {
				session.setPassword(new String(password));
			}

			boolean checkHostKeyFingerprint = false;
			if (hostKey == null) {
				session.setConfig("StrictHostKeyChecking", "no");
			} else if ("".equals(hostKey)) {
				final File knownHostsFile = new File(Utilities.replaceUsersHome("~/.ssh/known_hosts"));
				if (knownHostsFile.isFile()) {
					try (FileInputStream fileInputStream = new FileInputStream(knownHostsFile)) {
						jsch.setKnownHosts(fileInputStream);
					}
				} else {
					setKnownHostKeys(jsch);
				}
			} else {
				final File knownHostsFile = new File(Utilities.replaceUsersHome(hostKey));
				if (knownHostsFile.isFile()) {
					try (FileInputStream fileInputStream = new FileInputStream(knownHostsFile)) {
						jsch.setKnownHosts(fileInputStream);
					}
				} else {
					// Special fingerprint check afterwards
					session.setConfig("StrictHostKeyChecking", "no");
					checkHostKeyFingerprint = true;
				}
			}

			session.connect();

			if (checkHostKeyFingerprint) {
				final String actualHostKeyFingerprint = session.getHostKey().getFingerPrint(jsch);
				if (!hostKey.replace(":", "").trim().equalsIgnoreCase(actualHostKeyFingerprint.replace(":", "").trim())) {
					throw new UnknownHostException("Unexpected fingerprint of hostkey. \nExpected: " + hostKey + " \nHost: " + actualHostKeyFingerprint);
				}
			}

			return this;
		} catch (final JSchException jse) {
			close();
			if (jse.getCause() != null && jse.getCause() instanceof UnknownHostException) {
				throw new Exception("Cannot connect to host: " + hostname);
			} else {
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

	public boolean exists(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls " + filePath);

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				Utilities.toByteArray(inStream);

				final byte[] errorData = Utilities.toByteArray(inErrorStream);

				return errorData == null || errorData.length == 0;
			}
		} finally {
			closeQuietly(channel);
		}
	}

	public boolean isFile(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("test -f " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				final byte[] successData = Utilities.toByteArray(inStream);
				final String successString = new String(successData, "UTF-8");

				final byte[] errorData = Utilities.toByteArray(inErrorStream);

				if (errorData != null && errorData.length > 0) {
					return false;
				} else {
					return Utilities.interpretAsBool(successString);
				}
			}
		} finally {
			closeQuietly(channel);
		}
	}

	public boolean isFolder(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("test -d " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				final byte[] successData = Utilities.toByteArray(inStream);
				final String successString = new String(successData, "UTF-8");

				final byte[] errorData = Utilities.toByteArray(inErrorStream);

				if (errorData != null && errorData.length > 0) {
					return false;
				} else {
					return Utilities.interpretAsBool(successString);
				}
			}
		} finally {
			closeQuietly(channel);
		}
	}

	public List<String> getFolders(final String filePath) throws Exception {
		final List<String> folderList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read folders
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + filePath + (filePath.endsWith("/") ? "" : "/") + " -mindepth 1 -maxdepth 1 -type d");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, "UTF-8")) {
					folderList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = Utilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, "UTF-8"));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}
		folderList.addAll(getFolderLinks(filePath));
		Collections.sort(folderList);
		return folderList;
	}

	public List<String> getFiles(final String directoryPath) throws Exception {
		final List<String> fileList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read files
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + directoryPath + (directoryPath.endsWith("/") ? "" : "/") + " -mindepth 1 -maxdepth 1 -type f");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, "UTF-8")) {
					fileList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = Utilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, "UTF-8"));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}
		fileList.addAll(getFileLinks(directoryPath));
		Collections.sort(fileList);
		return fileList;
	}

	public List<String> getFolderLinks(final String directoryPath) throws Exception {
		final List<String> folderList = new ArrayList<>();
		final List<String> linkList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read links
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + directoryPath + (directoryPath.endsWith("/") ? "" : "/") + " -mindepth 1 -maxdepth 1 -type l");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {

				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, "UTF-8")) {
					linkList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = Utilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, "UTF-8"));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}

		for (final String link : linkList) {
			if (isFolder(directoryPath + "/" + link)) {
				folderList.add(link);
			}
		}
		Collections.sort(folderList);
		return folderList;
	}

	public List<String> getFileLinks(final String filePath) throws Exception {
		final List<String> fileList = new ArrayList<>();
		final List<String> linkList = new ArrayList<>();
		Channel channel = null;
		try {
			// Read links
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("find " + filePath + (filePath.endsWith("/") ? "" : "/") + " -mindepth 1 -maxdepth 1 -type l");

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {

				channel.connect();

				for (final String folderLine : Utilities.readLines(inStream, "UTF-8")) {
					linkList.add(folderLine.substring(folderLine.lastIndexOf("/") + 1));
				}

				final byte[] errorData = Utilities.toByteArray(inErrorStream);
				if (errorData != null && errorData.length > 0) {
					throw new Exception(new String(errorData, "UTF-8"));
				}
			}
		} finally {
			closeQuietly(channel);
			channel = null;
		}

		for (final String link : linkList) {
			if (isFile( filePath + "/" + link)) {
				fileList.add(link);
			}
		}
		Collections.sort(fileList);
		return fileList;
	}

	public void createFolder(final String selectedFilePath, final String folderName) throws Exception {
		executeCommand("mkdir " + selectedFilePath + "/" + folderName);
	}

	public void uploadFileData(final String filePath, final byte[] fileData) throws Exception {
		Channel channel = null;
		try {
			String command = "scp -p -t " + filePath;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				checkAcknowledge(inStream);
	
				// send command "C0664 filesize filename"
				final String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
				command = "C0664 " + fileData.length + " " + fileName + "\n";
				outStream.write(command.getBytes("UTF-8"));
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

	public JschFileData getFileInfo(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -l " + filePath);

			// Streams must be fetched before channel connect
			try (InputStream inStream = channel.getInputStream();
					InputStream inErrorStream = channel.getExtInputStream()) {
				channel.connect();
	
				final byte[] successData = Utilities.toByteArray(inStream);
				final String successString = new String(successData, "UTF-8");
	
				final byte[] errorData = Utilities.toByteArray(inErrorStream);
	
				if (errorData != null && errorData.length > 0) {
					return null;
				} else {
					return new JschFileData(successString);
			}
			}
		} finally {
			closeQuietly(channel);
		}
	}

	public InputStream getFileInputStream(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("scp -f " + filePath);

			final OutputStream outStream = channel.getOutputStream();
			final InputStream inStream = channel.getInputStream();
			// InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			// read rights key like '0644 ' (5 bytes)
			final byte[] rightsData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String rights = null;
			if (rightsData != null && rightsData.length > 0) {
				rights = new String(rightsData, "UTF-8");
			}

			// read filesize (if empty filesize read until no more data)
			final byte[] filesizeData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			long filesize = -1;
			if (filesizeData != null && filesizeData.length > 0) {
				filesize = Long.parseLong(new String(filesizeData));
			}

			// read filename (even if it is not used on single download)
			final byte[] filenameData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String filename = null;
			if (filenameData != null && filenameData.length > 0) {
				filename = new String(filenameData, "UTF-8");
			}

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			return inStream;
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		}
	}

	/**
	 * Löschen von Dateien via SSH
	 *
	 * @param remoteHostname
	 * @param checkHostKey
	 * @param remoteHostPathAndFileName
	 *            auch Wildcards(*?) sind erlaubt
	 * @param username
	 * @param privateKey
	 * @param password
	 *            des PrivateKey falls notwendig
	 * @throws Exception
	 */
	public void deleteFile(final String remoteHostPathAndFileName) throws Exception {
		Channel channel = null;
		try {
			// exec 'rm /full/path/to/file' remotely
			final String command = "rm -f " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O stream for remote scp
			try (InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				checkAcknowledge(inStream);
			}
		} catch (final Exception e) {
			throw new Exception("Fehler bei SSH-Delete: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Delete a folder via SSH
	 *
	 * @param session
	 * @param remoteHostPathAndFileName
	 * @throws Exception
	 */
	public void deleteFolder(final String remoteHostPathAndFolderName) throws Exception {
		Channel channel = null;
		try {
			final String command = "rm -rf " + remoteHostPathAndFolderName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O stream for remote scp
			try (InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				checkAcknowledge(inStream);
			}
		} catch (final Exception e) {
			throw new Exception("Fehler bei SSH-Delete: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Upload von Dateien via SSH
	 *
	 * @param remoteHostname
	 * @param checkHostKey
	 * @param remoteHostPathAndFileName
	 *            Verzeichnis ist als Ziel erlaubt, muß aber bereits existieren
	 * @param localPathAndFileName
	 *            KEINE Wildcards(*?) erlaubt
	 * @param username
	 * @param privateKey
	 * @param password
	 *            des PrivateKey falls notwendig
	 * @throws Exception
	 */
	public void uploadFile(final String remoteHostPathAndFileName, final String localPathAndFileName) throws Exception {
		if (localPathAndFileName.contains("*") || localPathAndFileName.contains("?")) {
			throw new Exception("Fehler bei SSH-Upload: Wildcards in localPathAndFileName sind bei SSH-Upload nicht erlaubt");
		}

		if (!new File(localPathAndFileName).exists()) {
			throw new Exception("Fehler bei SSH-Upload: Quelldatei '" + localPathAndFileName + "' existiert nicht");
		}

		final String remoteHostPath = getPathOfPathAndFileName(remoteHostPathAndFileName);
		String remoteHostFileName = getFileNameOfPathAndFileName(remoteHostPathAndFileName);
		if (Utilities.isEmpty(remoteHostFileName)) {
			remoteHostFileName = getFileNameOfPathAndFileName(localPathAndFileName);
		}

		final List<String> directory = listFiles(remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Fehler bei SSH-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		}
		if (directory.contains(remoteHostFileName)) {
			throw new Exception("Fehler bei SSH-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");
		}

		Channel channel = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPathAndFileName;
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
				out.write(command.getBytes("UTF-8"));
				out.flush();
				checkAcknowledge(in);
	
				// send a content of lfile
				try (InputStream bufferedFileInputStream = new BufferedInputStream(new FileInputStream(localPathAndFileName))) {
					final byte[] buf = new byte[4096];
					while (true) {
						final int len = bufferedFileInputStream.read(buf);
						if (len <= 0) {
							break;
						}
						out.write(buf, 0, len); // out.flush();
					}
					// send '\0'
					sendZeroByte(out);
		
					checkAcknowledge(in);
		
					out.close();
		
					channel.disconnect();
				}
			}
		} catch (final Exception e) {
			throw new Exception("Fehler bei SSH-Upload: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	public void uploadFileData(final String remoteHostPath, final String remoteHostFileName, final byte[] fileData) throws Exception {
		// Directory auslesen um zu Prüfen, ob das Verzeichnis existiert aber die Datei noch nicht
		final List<String> directory = listFiles( remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Fehler bei SSH-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		}
		if (directory.contains(remoteHostFileName)) {
			throw new Exception("Fehler bei SSH-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");
		}

		Channel channel = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPath + "/" + remoteHostFileName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			try (OutputStream out = channel.getOutputStream();
					InputStream in = channel.getInputStream()) {
				channel.connect();
	
				checkAcknowledge(in);
	
				// send command "C0664 filesize filename"
				command = "C0664 " + fileData.length + " " + remoteHostFileName + "\n";
				out.write(command.getBytes("UTF-8"));
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
			throw new Exception("Fehler bei SSH-Upload: " + e.getMessage());
		} finally {
			closeQuietly(channel);
		}
	}

	public List<String> listFiles(final String remoteHostPath) throws Exception {
		Channel channel = null;
		try {
			final List<String> returnList = new ArrayList<>();

			// exec 'ls -1 pathn' remotely
			final String command = "ls -1a " + remoteHostPath;
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
	
				while (charValue != -1) { // EOF
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
			throw new Exception("Fehler bei SSH-Download\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Hinzufügen von public Hostkeys zu der KnownHosts-Datei
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 */
	public void addHostKeyToKnownHostKeys(final String hostname, final String username, final String password, final byte[] privateKey) {
		Session session = null;
		try {
			final JSch jsch = new JSch();
			setKnownHostKeys(jsch);

			System.out.println("KnownHosts (before adding):");
			printKnownHostKeys(jsch);

			session = jsch.getSession(username, hostname, 22);
			if (privateKey != null) {
				// privateKey wird durch die Benutzung geleert
				jsch.addIdentity(username, privateKey.clone(), null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			session.setConfig("StrictHostKeyChecking", "no"); // automatische
			// Aufnahme
			// neuer
			// HostPublicKeys
			// session.setConfig("HashKnownHosts", "yes"); // In adding to
			// known_hosts file, host names will be hashed
			session.connect();

			final HostKey hk = session.getHostKey();
			System.out.println("Adding HostKey: " + hk.getHost() + " " + hk.getType() + " " + hk.getFingerPrint(jsch) + "\n");

			// Channel channel = session.openChannel("shell");
			//
			// channel.setInputStream(System.in);
			// channel.setOutputStream(System.out);
			//
			// channel.connect();

			System.out.println("KnownHosts (after adding):");
			printKnownHostKeys(jsch);
			System.out.println("");
		} catch (final Exception e) {
			System.out.println(e);
		}
	}

	public byte[] downloadFileData(final String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("scp -f " + filePath);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				// send '\0' to start data transfer
				sendZeroByte(outStream);
				
				while (true) {
					if (checkAcknowledge(inStream) != 'C') {
						break;
					}
	
					// read rights key like '0644 ' (5 bytes)
					final byte[] rightsData = readStreamUntilBlankOrEndOrLinefeed(inStream);
					@SuppressWarnings("unused")
					String rights = null;
					if (rightsData != null && rightsData.length > 0) {
						rights = new String(rightsData, "UTF-8");
					}
	
					// read filesize (if empty filesize read until no more data)
					final byte[] filesizeData = readStreamUntilBlankOrEndOrLinefeed(inStream);
					long filesize = -1;
					if (filesizeData != null && filesizeData.length > 0) {
						filesize = Long.parseLong(new String(filesizeData));
					}
	
					// read filename (even if it is not used on single download)
					final byte[] filenameData = readStreamUntilBlankOrEndOrLinefeed(inStream);
					@SuppressWarnings("unused")
					String filename = null;
					if (filenameData != null && filenameData.length > 0) {
						filename = new String(filenameData, "UTF-8");
					}
	
					// send '\0' to start data transfer
					sendZeroByte(outStream);
	
					// read file content
					if (filesize > 0) {
						int nextReadLength;
						final byte[] buffer = new byte[4096];
						while (true) {
							if (buffer.length < filesize) {
								nextReadLength = buffer.length;
							} else {
								nextReadLength = (int) filesize;
							}
							nextReadLength = inStream.read(buffer, 0, nextReadLength);
							if (nextReadLength < 0) {
								// error
								break;
							} else {
								bos.write(buffer, 0, nextReadLength);
								filesize -= nextReadLength;
								if (filesize <= 0) {
									break;
								}
							}
						}
					}
	
					checkAcknowledge(inStream);
	
					// send '\0' to start data transfer of next file (if this is a multi download)
					sendZeroByte(outStream);
				}
	
				return bos.toByteArray();
			}
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	public void tailviewFileData(final String filePath, final int numberOfLastLinesToShow, final LineListener lineListener) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -f -n " + numberOfLastLinesToShow + " " + filePath);

			try (OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				// send '\0' to start data transfer
				sendZeroByte(outStream);
	
				while (true) {
					// reads all now available data at once
					byte[] nextData = Utilities.toByteArray(inStream);
					while (nextData.length > 0) {
						lineListener.addLineData(nextData);
						nextData = Utilities.toByteArray(inStream);
					}
	
					// waits if no more data is available right now
					final byte[] nextLineData = Utilities.readStreamUntilEndOrLinefeed(inStream);
					lineListener.addLineData(nextLineData);
				}
			}
		} catch (final java.io.InterruptedIOException e) {
			// do nothing
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	public byte[] readTailFileData(final String filePath, final int numberOfLastLinesToShow) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -n " + numberOfLastLinesToShow + " " + filePath);

			try(OutputStream outStream = channel.getOutputStream();
					InputStream inStream = channel.getInputStream()) {
				channel.connect();
	
				// send '\0' to start data transfer
				sendZeroByte(outStream);
	
				return Utilities.toByteArray(inStream);
			}
		} catch (final Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			closeQuietly(channel);
		}
	}

	public String executeCommand(final String command) throws Exception {
		Channel channel = null;
		try {
			String returnText = "";

			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			try (OutputStream out = channel.getOutputStream();
					InputStream in = channel.getInputStream();
					InputStream inError = channel.getExtInputStream()) {
				channel.connect();
	
				// send '\0'
				sendZeroByte(out);
	
				int charValue = checkAcknowledge(in);
				StringBuilder inputString = new StringBuilder();
	
				while (charValue != -1) { // EOF
					if (charValue == '\n') { // EOL
						returnText += inputString + "\n";
						inputString = new StringBuilder();
					} else {
						inputString.append((char) charValue);
					}
					charValue = checkAcknowledge(in);
				}
	
				String returnError = "";
				int charValueErrorStream = checkAcknowledge(inError);
				StringBuilder inputStringErrorStream = new StringBuilder();
	
				while (charValueErrorStream != -1) { // EOF
					if (charValueErrorStream == '\n') { // EOL
						returnError += inputStringErrorStream + "\n";
						inputStringErrorStream = new StringBuilder();
					} else {
						inputStringErrorStream.append((char) charValueErrorStream);
					}
					charValueErrorStream = checkAcknowledge(inError);
				}
	
				if (returnError.length() > 0) {
					throw new Exception(returnError);
				} else {
					return returnText;
				}
			}
		} catch (final Exception e) {
			throw new Exception("Fehler bei SSH-Command\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
		}
	}

	/**
	 * Close connection
	 */
	@Override
	public void close() {
		if (session != null) {
			try {
				session.disconnect();
			} catch (final Exception f) {
				// do nothing
			}
			session = null;
		}
	}

	private static void setKnownHostKeys(final JSch jsch) throws Exception {
		try (InputStream knownHostsKeysInputStream = Utilities.getResourceAsStream("KnownHostsKeysForSSH")) {
			logger.debug("KnownHostsKeysForSSH " + (knownHostsKeysInputStream == null ? "NOT " : "") + "found");
			if (knownHostsKeysInputStream != null) {
				jsch.setKnownHosts(knownHostsKeysInputStream);
			} else {
				throw new Exception("KnownHostsKeysForSSH für SSH-Verbindung nicht gefunden");
			}
		} catch (final JSchException e) {
			e.printStackTrace();
		}
	}

	private static int checkAcknowledge(final InputStream in) throws Exception {
		final int returnCode = in.read();
		// returnCode may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (returnCode == 1 || returnCode == 2) {
			final StringBuffer returnTextBuffer = new StringBuffer();
			int charReadBuffer;
			do {
				charReadBuffer = in.read();
				returnTextBuffer.append((char) charReadBuffer);
			} while (charReadBuffer != '\n');

			if (returnCode == 1) { // error
				throw new Exception("Error on SSH conection: " + returnTextBuffer.toString());
			} else { // fatal error
				throw new Exception("Fatal error on SSH conection: " + returnTextBuffer.toString());
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

	private static void closeQuietly(final Channel channel) {
		try {
			if (channel != null) {
				channel.disconnect();
			}
		} catch (final Exception ioe) {
			// ignore
		}
	}
}
