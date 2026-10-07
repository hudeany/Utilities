package de.soderer.utilities.remote;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/**
 * Remote connection via the outdated SMB version 1 protocol, based on jcifs. Use {@link SmbConnection} for SMB 2 and 3.
 */
public class Smb1Connection implements RemoteConnection {
	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The password. */
	private char[] password = null;

	/** The domain. */
	private String sharename = null;

	/**
	 * Currently connected client
	 */
	private NtlmPasswordAuthentication auth = null;

	/**
	 * Creates a connection to a share. SMBv1 uses the ports 137-139 by default.
	 *
	 * @param hostname host name of the server
	 * @param username user name, blank for anonymous access
	 * @param sharename name of the share
	 * @param password password
	 * @throws Exception if the access data is invalid
	 */
	public Smb1Connection(final String hostname, final String username, final String sharename, final char[] password) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.sharename = sharename;
		this.password = password;

		connect();
	}

	/**
	 * Creates a connection from access data.
	 *
	 * @param serverEntry access data with protocol "smb1" or "smb"
	 * @throws Exception if the protocol is not supported or the access data is invalid
	 */
	public Smb1Connection(final ServerAccessData serverEntry) throws Exception {
		if (!"smb1".equalsIgnoreCase(serverEntry.getProtocol()) && !"smb".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for smb v1 connection. Only 'smb1' and 'smb' are allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			sharename = serverEntry.getSharename();
			password = serverEntry.getPassword();

			connect();
		}
	}

	/**
	 * Connect.
	 */
	@SuppressWarnings("unused")
	private void connect() throws Exception {
		try {
			if (Utilities.isNotBlank(username)) {
				auth = new NtlmPasswordAuthentication("", username, password == null ? "" : new String(password));
			} else {
				auth = NtlmPasswordAuthentication.ANONYMOUS;
			}
			new SmbFile(getPathWithProtocol(), auth);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed", e);
		}
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(folderPath) + "/", auth);
		smbFile.delete();
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(parentFolderPath) + "/" + folderName, auth);
		smbFile.mkdir();
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		smbFile.delete();
	}

	@Override
	public FileInfo getFileInfo(final String filePath) throws Exception {
		checkConnection();
		String fileName;
		if (filePath.contains("/")) {
			fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			fileName = filePath;
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		if (!smbFile.exists() || smbFile.isDirectory()) {
			// Like the other RemoteConnection implementations: null for missing paths and for folders
			return null;
		}
		long fileSize = smbFile.getContentLengthLong();
		if (fileSize < 0) {
			fileSize = smbFile.getContentLength();
		}
		final ZonedDateTime changeDate = Instant.ofEpochMilli(smbFile.getDate()).atZone(ZoneId.systemDefault());
		return new FileInfo(fileName, fileSize, changeDate);
	}

	@Override
	public InputStream downloadFile(final String filePath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		return smbFile.getInputStream();
	}

	@Override
	public void uploadFile(final String filePath, final InputStream fileInputStream) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		try (OutputStream outputStream = smbFile.getOutputStream()) {
			IoUtilities.copy(fileInputStream, outputStream);
		}
	}

	private void checkConnection() throws Exception {
		if (auth == null) {
			throw new Exception("Not connected");
		}
	}

	@Override
	public void close() {
		auth = null;
	}

	private String getPathWithProtocol() {
		return "smb://" + hostname + "/" + sharename + "/";
	}

	private String getPath(String path) {
		path = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(path.replace("\\", "/")));
		return FileUtilities.trimTrailingFileSeparator(getPathWithProtocol() + path);
	}

	@Override
	public FolderInfo getFolderInfo(String folderPath) throws Exception {
		checkConnection();

		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));

		final SmbFile smbFile = new SmbFile(getPath(folderPath) + "/", auth);
		if (smbFile.isDirectory()) {
			String folderName;
			if (folderPath.contains("/")) {
				folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
			} else {
				folderName = folderPath;
			}
			if ("".equals(folderName)) {
				folderName = "/";
			}

			final ZonedDateTime changeDate = Instant.ofEpochMilli(smbFile.getDate()).atZone(ZoneId.systemDefault());
			return new FolderInfo(folderName, changeDate);
		} else {
			return null;
		}
	}

	@Override
	public Tuple<List<FolderInfo>, List<FileInfo>> getDirectory(String folderPath) throws Exception {
		checkConnection();

		folderPath = FileUtilities.trimLeadingFileSeparator(FileUtilities.trimTrailingFileSeparator(folderPath.replace("\\", "/")));

		final SmbFile smbFile = new SmbFile(getPath(folderPath) + "/", auth);
		if (smbFile.isDirectory()) {
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
			final List<FileInfo> fileInfoList = new ArrayList<>();

			for (final SmbFile subFile : smbFile.listFiles()) {
				if (subFile.isDirectory()) {
					final String subFolderName = FileUtilities.trimTrailingFileSeparator(subFile.getName());
					folderInfoList.add(getFolderInfo(folderPath + "/" + subFolderName));
				} else if (subFile.isFile()) {
					long fileSize = subFile.getContentLengthLong();
					if (fileSize < 0) {
						fileSize = subFile.getContentLength();
					}
					// Date of the file itself, not of the listed folder
					final ZonedDateTime changeDate = Instant.ofEpochMilli(subFile.getDate()).atZone(ZoneId.systemDefault());
					fileInfoList.add(new FileInfo(subFile.getName(), fileSize, changeDate));
				}
			}

			return new Tuple<>(folderInfoList, fileInfoList);
		} else {
			return null;
		}
	}
}
