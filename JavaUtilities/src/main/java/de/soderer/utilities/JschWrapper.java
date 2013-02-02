package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class JschWrapper {
	private static final transient Logger logger = LoggerFactory.getLogger(JschWrapper.class);
	
	private static Session openSession(ServerAccessData serverEntry) throws Exception {
		Session session = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(serverEntry.getUsername(), serverEntry.getHostname(), serverEntry.getPort());
			
			if (StringUtils.isNotBlank(serverEntry.getKeyFile())) {
				jsch.addIdentity(serverEntry.getUsername(), FileUtils.readFileToByteArray(new File(serverEntry.getKeyFile())), null, serverEntry.getPassword() == null ? new byte[0] : serverEntry.getPassword().getBytes("UTF-8"));
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
		} catch (Exception e) {
			closeQuietly(session);
			throw new Exception("Error in opening ssh session: " + ExceptionUtils.toString(e), e);
		}
	}
	
	public static boolean exists(ServerAccessData serverEntry, String filePath) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("ls " + filePath);

			// Streams must be fetched before channel connect
			InputStream inStream = channel.getInputStream();
			InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();
			
			IOUtils.toByteArray(inStream);
			
			byte[] errorData = IOUtils.toByteArray(inErrorStream);
			
			return errorData == null || errorData.length == 0;
		} finally {
			closeQuietly(channel);
			closeQuietly(session);
		}
	}
	
	public static boolean isFile(ServerAccessData serverEntry, String filePath) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("test -f " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			InputStream inStream = channel.getInputStream();
			InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();
			
			byte[] successData = IOUtils.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");
			
			byte[] errorData = IOUtils.toByteArray(inErrorStream);
			
			if (errorData != null && errorData.length > 0) {
				return false;
			} else {
				return Utilities.interpretAsBool(successString);
			}
		} finally {
			closeQuietly(channel);
			closeQuietly(session);
		}
	}
	
	public static boolean isFolder(ServerAccessData serverEntry, String filePath) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("test -d " + filePath + " && echo true || echo false");

			// Streams must be fetched before channel connect
			InputStream inStream = channel.getInputStream();
			InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();
			
			byte[] successData = IOUtils.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");
			
			byte[] errorData = IOUtils.toByteArray(inErrorStream);
			
			if (errorData != null && errorData.length > 0) {
				return false;
			} else {
				return Utilities.interpretAsBool(successString);
			}
		} finally {
			closeQuietly(channel);
			closeQuietly(session);
		}
	}
	
	public static Tuple<List<String>, List<String>> getDirectory(ServerAccessData serverEntry, String filePath) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		try {
			// Read folders
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("ls -1ad " + filePath + (filePath.endsWith("/") ? "" : "/") + "*/");

			// Streams must be fetched before channel connect
			InputStream inStream = channel.getInputStream();
			InputStream inErrorStream = channel.getExtInputStream();
			
			channel.connect();
			
			List<String> folderListRaw = IOUtils.readLines(inStream);
			
			byte[] errorData = IOUtils.toByteArray(inErrorStream);
			if (errorData != null && errorData.length > 0) {
				throw new Exception(new String(errorData, "UTF-8"));
			}
			
			channel.disconnect();
			channel = null;
			
			// Read file entries
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("ls -1a " + filePath);

			// Streams must be fetched before channel connect
			inStream = channel.getInputStream();
			inErrorStream = channel.getExtInputStream();
			
			channel.connect();

			List<String> fileList = IOUtils.readLines(inStream);
			
			errorData = IOUtils.toByteArray(inErrorStream);
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
			closeQuietly(session);
		}
	}
	
	public static byte[] downloadFileData(ServerAccessData serverEntry, String filePath) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand("scp -f " + filePath);

			OutputStream out = channel.getOutputStream();
			InputStream inStream = channel.getInputStream();
			InputStream inErrorStream = channel.getExtInputStream();

			channel.connect();

			byte[] buf = new byte[1024];
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			int c1 = checkAck(inStream);
			int c2 = checkAck(inStream);
			int c3 = checkAck(inStream);
			int c4 = checkAck(inStream);

			byte[] successData = IOUtils.toByteArray(inStream);
			String successString = new String(successData, "UTF-8");

			byte[] errorData = IOUtils.toByteArray(inErrorStream);
			String errorString = new String(errorData, "UTF-8");

			return bos.toByteArray();
		} catch (Exception e) {
			throw new Exception("Error in downloadFileData", e);
		} finally {
			IOUtils.closeQuietly(bos);
			closeQuietly(channel);
			closeQuietly(session);
		}
	} 
	
	static int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if(b==0) return b;
		if(b==-1) return b;
		 
		if(b==1 || b==2){
		StringBuffer sb=new StringBuffer();
		int c;
		do {
		c=in.read();
		sb.append((char)c);
		}
		while(c!='\n');
		if(b==1){ // error
		System.out.print(sb.toString());
		}
		if(b==2){ // fatal error
		System.out.print(sb.toString());
		}
		}
		return b;
	}

	/**
	 * Download von Dateien via SCP
	 * 
	 * @param remoteHostname
	 * @param checkHostKey
	 * @param remoteHostPathAndFileName
	 *            auch Wildcards(*?) sind erlaubt
	 * @param localPathAndFileName
	 *            auch ein Verzeichnisse ist erlaubt
	 * @param username
	 * @param privateKey
	 * @param password
	 *            des PrivateKey falls notwendig
	 * @throws Exception
	 */
	public static void downloadFile(ServerAccessData serverEntry, String remoteHostPathAndFileName, String localPathAndFileName) throws Exception {
		Session session = openSession(serverEntry);
		Channel channel = null;
		FileOutputStream fos = null;
		try {
			// exec 'scp -f rfile' remotely
			String command = "scp -f " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				if (checkAcknowledge(in) != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long)(buf[0] - '0');
				}

				String filename = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte)0x0a) {
						filename = new String(buf, 0, i, "UTF-8");
						break;
					}
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(new File("c:\\temp\\test.txt"));
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int)filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				checkAcknowledge(in);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Download\n" + ExceptionUtils.toString(e), e);
		} finally {
			IOUtils.closeQuietly(fos);
			closeQuietly(channel);
			closeQuietly(session);
		}
	}

	/**
	 * Löschen von Dateien via SCP
	 * 
	 * @param remoteHostname
	 * @param checkHostKey
	 * @param remoteHostPathAndFileName
	 *            auch Wildcards(*?) sind erlaubt
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public static void deleteFile(String remoteHostname, boolean checkHostKey, String remoteHostPathAndFileName, String username, String password) throws Exception {
		deleteFile(remoteHostname, checkHostKey, remoteHostPathAndFileName, username, password, null);
	}

	public static void deleteFile(boolean checkHostKey, SCPFile fileToDelete, String username, String password) throws Exception {
		deleteFile(fileToDelete.getHostname(), checkHostKey, fileToDelete.getPathAndFilename(), username, password, null);
	}

	public static void deleteFile(boolean checkHostKey, SCPFile fileToDelete, String username, String password, byte[] privateKey) throws Exception {
		deleteFile(fileToDelete.getHostname(), checkHostKey, fileToDelete.getPathAndFilename(), username, password, privateKey);
	}

	/**
	 * Löschen von Dateien via SCP
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
	public static void deleteFile(String remoteHostname, boolean checkHostKey, String remoteHostPathAndFileName, String username, String password, byte[] privateKey) throws Exception {
		Session session = null;
		Channel channel = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username, remoteHostname, 22);
			if (privateKey != null) {
				// privateKey wird durch die Benutzung geleert
				jsch.addIdentity(username, privateKey.clone(), null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			if (checkHostKey)
				setKnownHostKeys(jsch);
			else
				session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			// exec 'rm /full/path/to/file' remotely
			String command = "rm -f " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O stream for remote scp
			InputStream in = channel.getInputStream();

			channel.connect();

			checkAcknowledge(in);

			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Delete: " + e.getMessage());
		} finally {
			if (channel != null) {
				try {
					channel.disconnect();
					channel = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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
	 * @param password
	 * @throws Exception
	 */
	public static void uploadFile(String remoteHostname, boolean checkHostKey, String remoteHostPathAndFileName, String localPathAndFileName, String username, String password) throws Exception {
		uploadFile(remoteHostname, checkHostKey, remoteHostPathAndFileName, localPathAndFileName, username, password, null);
	}

	public static void uploadFile(boolean checkHostKey, SCPFile fileToUpload, String localPathAndFileName, String username, String password) throws Exception {
		uploadFile(fileToUpload.getHostname(), checkHostKey, fileToUpload.getPathAndFilename(), localPathAndFileName, username, password, null);
	}

	public static void uploadFile(boolean checkHostKey, SCPFile fileToUpload, String localPathAndFileName, String username, String password, byte[] privateKey) throws Exception {
		uploadFile(fileToUpload.getHostname(), checkHostKey, fileToUpload.getPathAndFilename(), localPathAndFileName, username, password, privateKey);
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
	public static void uploadFile(String remoteHostname, boolean checkHostKey, String remoteHostPathAndFileName, String localPathAndFileName, String username, String password, byte[] privateKey)
			throws Exception {
		if (localPathAndFileName.contains("*") || localPathAndFileName.contains("?"))
			throw new Exception("Fehler bei SCP-Upload: Wildcards in localPathAndFileName sind bei SCP-Upload nicht erlaubt");

		if (!new File(localPathAndFileName).exists())
			throw new Exception("Fehler bei SCP-Upload: Quelldatei '" + localPathAndFileName + "' existiert nicht");

		String remoteHostPath = getPathOfPathAndFileName(remoteHostPathAndFileName);
		String remoteHostFileName = getFileNameOfPathAndFileName(remoteHostPathAndFileName);
		if (StringUtils.isEmpty(remoteHostFileName))
			remoteHostFileName = getFileNameOfPathAndFileName(localPathAndFileName);

		List<String> directory = listFiles(remoteHostname, checkHostKey, remoteHostPath, username, password, privateKey);
		if (!directory.contains("."))
			throw new Exception("Fehler bei SCP-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		if (directory.contains(remoteHostFileName))
			throw new Exception("Fehler bei SCP-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");

		Session session = null;
		Channel channel = null;
		BufferedInputStream bufferedFileInputStream = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username, remoteHostname, 22);
			if (privateKey != null) {
				jsch.addIdentity(username, privateKey, null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			if (checkHostKey)
				setKnownHostKeys(jsch);
			else
				session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPathAndFileName;
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			checkAcknowledge(in);

			// send "C0664 filesize filename", where filename should not include '/'
			long filesize = (new File(localPathAndFileName)).length();
			command = "C0664 " + filesize + " " + remoteHostFileName + "\n";
			out.write(command.getBytes("UTF-8"));
			out.flush();
			checkAcknowledge(in);

			// send a content of lfile
			bufferedFileInputStream = new BufferedInputStream(new FileInputStream(localPathAndFileName));
			byte[] buf = new byte[1024];
			while (true) {
				int len = bufferedFileInputStream.read(buf);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			bufferedFileInputStream.close();
			bufferedFileInputStream = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			checkAcknowledge(in);
			out.close();

			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Upload: " + e.getMessage());
		} finally {
			if (bufferedFileInputStream != null) {
				try {
					bufferedFileInputStream.close();
					bufferedFileInputStream = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (channel != null) {
				try {
					channel.disconnect();
					channel = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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

	public static void uploadFileData(boolean checkHostKey, SCPFile fileToUpload, byte[] fileData, String username, String password, byte[] privateKey) throws Exception {
		uploadFileData(fileToUpload.getHostname(), checkHostKey, fileToUpload.getPathAndFilename(), fileToUpload.getFilename(), fileData, username, password, privateKey);
	}

	public static void uploadFileData(String remoteHostname, boolean checkHostKey, String remoteHostPath, String remoteHostFileName, byte[] fileData, String username, String password,
			byte[] privateKey) throws Exception {
		// Directory auslesen um zu Prüfen, ob das Verzeichnis existiert aber die Datei noch nicht
		List<String> directory = listFiles(remoteHostname, checkHostKey, remoteHostPath, username, password, privateKey);
		if (!directory.contains("."))
			throw new Exception("Fehler bei SCP-Upload: ZielVerzeichnis '" + remoteHostPath + "' existiert nicht auf Zielrechner");
		if (directory.contains(remoteHostFileName))
			throw new Exception("Fehler bei SCP-Upload: Datei '" + remoteHostFileName + "' existiert bereits auf Zielrechner");

		Session session = null;
		Channel channel = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username, remoteHostname, 22);
			if (privateKey != null) {
				jsch.addIdentity(username, privateKey, null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			if (checkHostKey)
				setKnownHostKeys(jsch);
			else
				session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remoteHostPath + "/" + remoteHostFileName;
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

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
			out.write(0);
			out.flush();
			checkAcknowledge(in);
			out.close();

			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Upload: " + e.getMessage());
		} finally {
			if (channel != null) {
				try {
					channel.disconnect();
					channel = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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

	public static List<String> listFiles(String remoteHostname, boolean checkHostKey, String remoteHostPath, String username, String password) throws Exception {
		return listFiles(remoteHostname, checkHostKey, remoteHostPath, username, password, null);
	}

	public static List<String> listFiles(String remoteHostname, boolean checkHostKey, String remoteHostPath, String username, String password, byte[] privateKey) throws Exception {
		Session session = null;
		Channel channel = null;
		try {
			List<String> returnList = new ArrayList<String>();

			JSch jsch = new JSch();
			session = jsch.getSession(username, remoteHostname, 22);
			if (privateKey != null) {
				// privateKey wird durch die Benutzung geleert
				jsch.addIdentity(username, privateKey.clone(), null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			if (checkHostKey)
				setKnownHostKeys(jsch);
			else
				session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			// exec 'ls -1 pathn' remotely
			String command = "ls -1a " + remoteHostPath;
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			// OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			// byte[] buf = new byte[1024];

			// send '\0'
			// buf[0] = 0;
			// out.write(buf, 0, 1);
			// out.flush();

			int charValue = checkAcknowledge(in);
			StringBuilder inputString = new StringBuilder();

			while (charValue != -1) { // EOF
				if (charValue == (int)'\n') { // EOL
					returnList.add(inputString.toString());
					inputString = new StringBuilder();
				} else {
					inputString.append((char)charValue);
				}
				charValue = checkAcknowledge(in);
			}

			session.disconnect();

			return returnList;
		} catch (Exception e) {
			throw new Exception("Fehler bei SCP-Download\n" + ExceptionUtils.toString(e), e);
		} finally {
			if (channel != null) {
				try {
					channel.disconnect();
					channel = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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

	public static void addHostKeyToKnownHostKeys(String remoteHostname, String username, String password) throws Exception {
		addHostKeyToKnownHostKeys(remoteHostname, username, password, null);
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

	public static String executeCommand(String remoteHostname, boolean checkHostKey, String command, String username, String password, byte[] privateKey) throws Exception {
		Session session = null;
		Channel channel = null;
		try {
			String returnText = "";

			JSch jsch = new JSch();
			session = jsch.getSession(username, remoteHostname, 22);
			if (privateKey != null) {
				// privateKey wird durch die Benutzung geleert
				jsch.addIdentity(username, privateKey.clone(), null, password == null ? new byte[0] : password.getBytes("UTF-8"));
			} else {
				session.setPassword(password);
			}
			if (checkHostKey)
				setKnownHostKeys(jsch);
			else
				session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			InputStream inError = channel.getExtInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			int charValue = checkAcknowledge(in);
			StringBuilder inputString = new StringBuilder();

			while (charValue != -1) { // EOF
				if (charValue == (int)'\n') { // EOL
					returnText += inputString + "\n";
					inputString = new StringBuilder();
				} else {
					inputString.append((char)charValue);
				}
				charValue = checkAcknowledge(in);
			}

			String returnError = "";
			int charValueErrorStream = checkAcknowledge(inError);
			StringBuilder inputStringErrorStream = new StringBuilder();

			while (charValueErrorStream != -1) { // EOF
				if (charValueErrorStream == (int)'\n') { // EOL
					returnError += inputStringErrorStream + "\n";
					inputStringErrorStream = new StringBuilder();
				} else {
					inputStringErrorStream.append((char)charValueErrorStream);
				}
				charValueErrorStream = checkAcknowledge(inError);
			}

			session.disconnect();

			if (returnError.length() > 0)
				throw new Exception(returnError);
			else
				return returnText;
		} catch (Exception e) {
			throw new Exception("Fehler bei SSH-Command\n" + ExceptionUtils.toString(e), e);
		} finally {
			if (channel != null) {
				try {
					channel.disconnect();
					channel = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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

	private static void setKnownHostKeys(JSch jsch) throws Exception {
		try {
			// Nicht mit der URI arbeiten, einen Pfad zu einer Resource in einem JAR-File gibts nicht
			// Wann immer möglich im JavaEE-Umfeld mit Streams arbeiten!
			// So nicht: InputStream knownHostsKeysInputStream = Utilities.getResource("KnownHostsKeysForSCP");
			InputStream knownHostsKeysInputStream = Utilities.getResourceAsStream("KnownHostsKeysForSCP");
			logger.debug("KnownHostsKeysForSCP " + (knownHostsKeysInputStream == null ? "NOT " : "") + "found");
			if (knownHostsKeysInputStream != null) {
				jsch.setKnownHosts(knownHostsKeysInputStream);
			} else {
				throw new Exception("KnownHostsKeysForSCP für SCP-Verbindung nicht gefunden");
			}

		} catch (JSchException e) {
			e.printStackTrace();
		}
	}

	private static void printKnownHostKeys(JSch jsch) {
		HostKeyRepository hkr = jsch.getHostKeyRepository();
		HostKey[] hks = hkr.getHostKey();
		if (hks != null) {
			System.out.println("Host keys in " + hkr.getKnownHostsRepositoryID());
			for (int i = 0; i < hks.length; i++) {
				HostKey hk = hks[i];
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
				returnTextBuffer.append((char)charReadBuffer);
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
		if (StringUtils.isEmpty(pathAndFileName))
			return null;
		else {
			int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(lastIndexOfPathSeparator + 1);
		}
	}

	private static String getPathOfPathAndFileName(String pathAndFileName) {
		if (StringUtils.isEmpty(pathAndFileName))
			return null;
		else {
			int lastIndexOfPathSeparator = Math.max(pathAndFileName.lastIndexOf("/"), pathAndFileName.lastIndexOf("\\"));
			return pathAndFileName.substring(0, lastIndexOfPathSeparator + 1);
		}
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
