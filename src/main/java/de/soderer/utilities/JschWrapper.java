package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

public class JschWrapper {
	private static final transient Logger logger = LoggerFactory.getLogger(JschWrapper.class);

	public static Session openSession(ServerAccessData serverEntry) throws Exception {
		Session session = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(serverEntry.getUsername(), serverEntry.getHostname(), serverEntry.getPort());

			if (Utilities.isNotBlank(serverEntry.getKeyFile())) {
				jsch.addIdentity(serverEntry.getUsername(), FileUtils.readFileToByteArray(new File(serverEntry.getKeyFile())), null,
						serverEntry.getPassword() == null ? new byte[0] : serverEntry.getPassword().getBytes("UTF-8"));
			} else {
				session.setPassword(serverEntry.getPassword());
			}

			if (serverEntry.getCheckHostKey()) {
				setKnownHostKeys(jsch);
			} else {
				session.setConfig("StrictHostKeyChecking", "no");
			}

			session.connect();

			return session;
		} catch (JSchException jse) {
			if (jse.getCause() != null && jse.getCause() instanceof UnknownHostException) {
				throw new Exception(LangResources.get("noConnectionTohost", serverEntry.getHostname()));
			} else {
				throw jse;
			}
		} catch (UnknownHostException uhe) {
			throw new Exception(LangResources.get("noConnectionTohost", serverEntry.getHostname()));
		} catch (Exception e) {
			closeQuietly(session);
			throw new Exception("Error in opening ssh session: " + ExceptionUtilities.toString(e), e);
		}
	}

	public static boolean exists(Session session, String filePath) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		InputStream inErrorStream = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls " + filePath);

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			Utilities.toByteArray(inStream);

			byte[] errorData = Utilities.toByteArray(inErrorStream);

			return errorData == null || errorData.length == 0;
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(inErrorStream);
		}
	}

	public static boolean isFile(Session session, String filePath) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		InputStream inErrorStream = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("test -f " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			byte[] successData = Utilities.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");

			byte[] errorData = Utilities.toByteArray(inErrorStream);

			if (errorData != null && errorData.length > 0) {
				return false;
			} else {
				return Utilities.interpretAsBool(successString);
			}
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(inErrorStream);
		}
	}

	public static boolean isFolder(Session session, String filePath) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		InputStream inErrorStream = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("test -d " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			byte[] successData = Utilities.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");

			byte[] errorData = Utilities.toByteArray(inErrorStream);

			if (errorData != null && errorData.length > 0) {
				return false;
			} else {
				return Utilities.interpretAsBool(successString);
			}
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(inErrorStream);
		}
	}

	public static Tuple<List<String>, List<String>> getDirectory(Session session, String filePath) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		InputStream inErrorStream = null;
		try {
			// Read folders
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -1ad " + filePath + (filePath.endsWith("/") ? "" : "/") + "*/");

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			List<String> folderListRaw = Utilities.readLines(inStream, "UTF-8");

			byte[] errorData = Utilities.toByteArray(inErrorStream);
			if (errorData != null && errorData.length > 0) {
				// there is no subdirectory
				// throw new Exception(new String(errorData, "UTF-8"));
			}

			channel.disconnect();
			channel = null;

			// Read file entries
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -1a " + filePath);

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			List<String> fileList = Utilities.readLines(inStream, "UTF-8");

			errorData = Utilities.toByteArray(inErrorStream);
			if (errorData != null && errorData.length > 0) {
				throw new Exception(new String(errorData, "UTF-8"));
			}

			// Calculate return values
			List<String> folderList = new ArrayList<String>();

			fileList.remove(".");
			fileList.remove("..");
			for (String folder : folderListRaw) {
				if (folder.endsWith("/")) {
					folder = folder.substring(0, folder.length() - 1);
				}
				if (folder.contains("/")) {
					folder = folder.substring(folder.lastIndexOf("/") + 1);
				}
				folderList.add(folder);
				fileList.remove(folder);
			}

			return new Tuple<List<String>, List<String>>(folderList, fileList);
		} catch (Exception e) {
			throw new Exception("Error in getDirectory", e);
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(inErrorStream);
		}
	}

	public static void createFolder(Session session, String selectedFilePath, String folderName) throws Exception {
		executeCommand(session, "mkdir " + selectedFilePath + "/" + folderName);
	}

	public static void uploadFileData(Session session, String filePath, byte[] fileData) throws Exception {
		Channel channel = null;
		OutputStream outStream = null;
		InputStream inStream = null;
		try {
			String command = "scp -p -t " + filePath;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			outStream = channel.getOutputStream();
			inStream = channel.getInputStream();

			channel.connect();

			checkAcknowledge(inStream);

			// send command "C0664 filesize filename"
			String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			command = "C0664 " + fileData.length + " " + fileName + "\n";
			outStream.write(command.getBytes("UTF-8"));
			outStream.flush();

			checkAcknowledge(inStream);

			// send content of file
			outStream.write(fileData, 0, fileData.length);

			// send '\0' as EOF
			sendZeroByte(outStream);

			checkAcknowledge(inStream);
		} catch (Exception e) {
			throw new Exception("Error in uploadFileData", e);
		} finally {
			Utilities.closeQuietly(outStream);
			Utilities.closeQuietly(inStream);
			closeQuietly(channel);
		}
	}

	public static JschFileData getFileData(Session session, String filePath) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		InputStream inErrorStream = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ls -l " + filePath);

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();

			channel.connect();

			byte[] successData = Utilities.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");

			byte[] errorData = Utilities.toByteArray(inErrorStream);

			if (errorData != null && errorData.length > 0) {
				return null;
			} else {
				return new JschFileData(successString);
			}
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(inErrorStream);
		}
	}

	public static InputStream getFileInputStream(Session session, String filePath) throws Exception {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("scp -f " + filePath);

			OutputStream outStream = channel.getOutputStream();
			InputStream inStream = channel.getInputStream();
			// InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			// read rights key like '0644 ' (5 bytes)
			byte[] rightsData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String rights = null;
			if (rightsData != null && rightsData.length > 0) {
				rights = new String(rightsData, "UTF-8");
			}

			// read filesize (if empty filesize read until no more data)
			byte[] filesizeData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			long filesize = -1;
			if (filesizeData != null && filesizeData.length > 0) {
				filesize = Long.parseLong(new String(filesizeData));
			}

			// read filename (even if it is not used on single download)
			byte[] filenameData = readStreamUntilBlankOrEndOrLinefeed(inStream);
			@SuppressWarnings("unused")
			String filename = null;
			if (filenameData != null && filenameData.length > 0) {
				filename = new String(filenameData, "UTF-8");
			}

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			return inStream;
		} catch (Exception e) {
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
	public static void deleteFile(Session session, String remoteHostPathAndFileName) throws Exception {
		Channel channel = null;
		InputStream inStream = null;
		try {
			// exec 'rm /full/path/to/file' remotely
			String command = "rm -f " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O stream for remote scp
			inStream = channel.getInputStream();

			channel.connect();

			checkAcknowledge(inStream);

			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Delete: " + e.getMessage());
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
		}
	}

	/**
	 * Upload von Dateien via SCP
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
	public static void uploadFile(Session session, String remoteHostPathAndFileName, String localPathAndFileName) throws Exception {
		if (localPathAndFileName.contains("*") || localPathAndFileName.contains("?")) {
			throw new Exception("Fehler bei SCP-Upload: Wildcards in localPathAndFileName sind bei SCP-Upload nicht erlaubt");
		}

		if (!new File(localPathAndFileName).exists()) {
			throw new Exception("Fehler bei SCP-Upload: Quelldatei '" + localPathAndFileName + "' existiert nicht");
		}

		String remoteHostPath = getPathOfPathAndFileName(remoteHostPathAndFileName);
		String remoteHostFileName = getFileNameOfPathAndFileName(remoteHostPathAndFileName);
		if (Utilities.isEmpty(remoteHostFileName)) {
			remoteHostFileName = getFileNameOfPathAndFileName(localPathAndFileName);
		}

		List<String> directory = listFiles(session, remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Fehler bei SCP-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		}
		if (directory.contains(remoteHostFileName)) {
			throw new Exception("Fehler bei SCP-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");
		}

		Channel channel = null;
		OutputStream out = null;
		InputStream in = null;
		BufferedInputStream bufferedFileInputStream = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			in = channel.getInputStream();

			channel.connect();

			checkAcknowledge(in);

			// send "C0664 filesize filename", where filename should not include '/'
			long filesize = new File(localPathAndFileName).length();
			command = "C0664 " + filesize + " " + remoteHostFileName + "\n";
			out.write(command.getBytes("UTF-8"));
			out.flush();
			checkAcknowledge(in);

			// send a content of lfile
			bufferedFileInputStream = new BufferedInputStream(new FileInputStream(localPathAndFileName));
			byte[] buf = new byte[4096];
			while (true) {
				int len = bufferedFileInputStream.read(buf);
				if (len <= 0) {
					break;
				}
				out.write(buf, 0, len); // out.flush();
			}
			bufferedFileInputStream.close();
			bufferedFileInputStream = null;
			// send '\0'
			sendZeroByte(out);

			checkAcknowledge(in);

			out.close();

			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Upload: " + e.getMessage());
		} finally {
			Utilities.closeQuietly(bufferedFileInputStream);
			closeQuietly(channel);
			Utilities.closeQuietly(in);
			Utilities.closeQuietly(out);
		}
	}

	public static void uploadFileData(Session session, String remoteHostPath, String remoteHostFileName, byte[] fileData) throws Exception {
		// Directory auslesen um zu Prüfen, ob das Verzeichnis existiert aber die Datei noch nicht
		List<String> directory = listFiles(session, remoteHostPath);
		if (!directory.contains(".")) {
			throw new Exception("Fehler bei SCP-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		}
		if (directory.contains(remoteHostFileName)) {
			throw new Exception("Fehler bei SCP-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");
		}

		Channel channel = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPath + "/" + remoteHostFileName;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			in = channel.getInputStream();

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
			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Upload: " + e.getMessage());
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(in);
			Utilities.closeQuietly(out);
		}
	}

	public static List<String> listFiles(Session session, String remoteHostPath) throws Exception {
		Channel channel = null;
		InputStream in = null;
		try {
			List<String> returnList = new ArrayList<String>();

			// exec 'ls -1 pathn' remotely
			String command = "ls -1a " + remoteHostPath;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			// OutputStream out = channel.getOutputStream();
			in = channel.getInputStream();

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

			session.disconnect();

			return returnList;
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Download\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(in);
		}
	}

	/**
	 * Hinzufügen von public Hostkeys zu der KnownHosts-Datei
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 */
	public static void addHostKeyToKnownHostKeys(String hostname, String username, String password, byte[] privateKey) {
		Session session = null;
		try {
			JSch jsch = new JSch();
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

			HostKey hk = session.getHostKey();
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

			session.disconnect();
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			if (session != null) {
				try {
					session.disconnect();
					session = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static byte[] downloadFileData(Session session, String filePath) throws Exception {
		Channel channel = null;
		OutputStream outStream = null;
		InputStream inStream = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("scp -f " + filePath);

			outStream = channel.getOutputStream();
			inStream = channel.getInputStream();
			// InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			while (true) {
				if (checkAcknowledge(inStream) != 'C') {
					break;
				}

				// read rights key like '0644 ' (5 bytes)
				byte[] rightsData = readStreamUntilBlankOrEndOrLinefeed(inStream);
				@SuppressWarnings("unused")
				String rights = null;
				if (rightsData != null && rightsData.length > 0) {
					rights = new String(rightsData, "UTF-8");
				}

				// read filesize (if empty filesize read until no more data)
				byte[] filesizeData = readStreamUntilBlankOrEndOrLinefeed(inStream);
				long filesize = -1;
				if (filesizeData != null && filesizeData.length > 0) {
					filesize = Long.parseLong(new String(filesizeData));
				}

				// read filename (even if it is not used on single download)
				byte[] filenameData = readStreamUntilBlankOrEndOrLinefeed(inStream);
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
					byte[] buffer = new byte[4096];
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
		} catch (Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			Utilities.closeQuietly(bos);
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(outStream);
		}
	}

	public static void tailviewFileData(Session session, String filePath, int numberOfLastLinesToShow, LineListener lineListener) throws Exception {
		Channel channel = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -f -n " + numberOfLastLinesToShow + " " + filePath);

			OutputStream outStream = channel.getOutputStream();
			InputStream inStream = channel.getInputStream();

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
				byte[] nextLineData = Utilities.readStreamUntilEndOrLinefeed(inStream);
				lineListener.addLineData(nextLineData);
			}
		} catch (java.io.InterruptedIOException e) {
			// do nothing
		} catch (Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			Utilities.closeQuietly(bos);
			closeQuietly(channel);
		}
	}

	public static byte[] readTailFileData(Session session, String filePath, int numberOfLastLinesToShow) throws Exception {
		Channel channel = null;
		OutputStream outStream = null;
		InputStream inStream = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("tail -n " + numberOfLastLinesToShow + " " + filePath);

			outStream = channel.getOutputStream();
			inStream = channel.getInputStream();

			channel.connect();

			// send '\0' to start data transfer
			sendZeroByte(outStream);

			return Utilities.toByteArray(inStream);
		} catch (Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			Utilities.closeQuietly(bos);
			closeQuietly(channel);
			Utilities.closeQuietly(inStream);
			Utilities.closeQuietly(outStream);
		}
	}

	public static String executeCommand(Session session, String command) throws Exception {
		Channel channel = null;
		OutputStream out = null;
		InputStream in = null;
		InputStream inError = null;
		try {
			String returnText = "";

			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			in = channel.getInputStream();
			inError = channel.getExtInputStream();

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

			session.disconnect();

			if (returnError.length() > 0) {
				throw new Exception(returnError);
			} else {
				return returnText;
			}
		} catch (Exception e) {
			throw new Exception("Fehler bei SSH-Command\n" + ExceptionUtilities.toString(e), e);
		} finally {
			closeQuietly(channel);
			Utilities.closeQuietly(in);
			Utilities.closeQuietly(out);
			Utilities.closeQuietly(inError);
		}
	}

	private static void setKnownHostKeys(JSch jsch) throws Exception {
		InputStream knownHostsKeysInputStream = null;
		try {
			// Nicht mit der URI arbeiten, einen Pfad zu einer Resource in einem JAR-File gibts nicht
			// Wann immer möglich im JavaEE-Umfeld mit Streams arbeiten!
			// So nicht: InputStream knownHostsKeysInputStream = Utilities.getResource("KnownHostsKeysForSCP");
			knownHostsKeysInputStream = Utilities.getResourceAsStream("KnownHostsKeysForSCP");
			logger.debug("KnownHostsKeysForSCP " + (knownHostsKeysInputStream == null ? "NOT " : "") + "found");
			if (knownHostsKeysInputStream != null) {
				jsch.setKnownHosts(knownHostsKeysInputStream);
			} else {
				throw new Exception("KnownHostsKeysForSCP für SCP-Verbindung nicht gefunden");
			}

		} catch (JSchException e) {
			e.printStackTrace();
		} finally {
			Utilities.closeQuietly(knownHostsKeysInputStream);
		}
	}

	private static void printKnownHostKeys(JSch jsch) {
		HostKeyRepository hkr = jsch.getHostKeyRepository();
		HostKey[] hks = hkr.getHostKey();
		if (hks != null) {
			System.out.println("Host keys in " + hkr.getKnownHostsRepositoryID());
			for (HostKey hk : hks) {
				System.out.println(hk.getHost() + " " + hk.getType() + " " + hk.getFingerPrint(jsch));
			}
			System.out.println("");
		}
	}

	private static int checkAcknowledge(InputStream in) throws Exception {
		int returnCode = in.read();
		// returnCode may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (returnCode == 1 || returnCode == 2) {
			StringBuffer returnTextBuffer = new StringBuffer();
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

	private static String getFileNameOfPathAndFileName(String pathAndFileName) {
		if (Utilities.isEmpty(pathAndFileName)) {
			return null;
		} else {
			int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(lastIndexOfPathSeparator + 1);
		}
	}

	private static String getPathOfPathAndFileName(String pathAndFileName) {
		if (Utilities.isEmpty(pathAndFileName)) {
			return null;
		} else {
			int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(0, lastIndexOfPathSeparator + 1);
		}
	}

	private static void sendZeroByte(OutputStream outputStream) throws IOException {
		outputStream.write(new byte[] { 0 });
		outputStream.flush();
	}

	private static byte[] readStreamUntilBlankOrEndOrLinefeed(InputStream inStream) throws IOException {
		ByteArrayOutputStream returnData = new ByteArrayOutputStream();
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

	public static void closeQuietly(Channel channel) {
		try {
			if (channel != null) {
				channel.disconnect();
			}
		} catch (Exception ioe) {
			// ignore
		}
	}

	public static void closeQuietly(Session session) {
		try {
			if (session != null) {
				session.disconnect();
			}
		} catch (Exception ioe) {
			// ignore
		}
	}
}
