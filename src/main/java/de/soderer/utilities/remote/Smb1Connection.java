package de.soderer.utilities.remote;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.soderer.utilities.RemoteFileInfo;
import de.soderer.utilities.RemoteFolderInfo;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Utilities;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class Smb1Connection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(Smb1Connection.class);

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

	/** The port. SMBv1 uses the ports 137–139 by default */
	public Smb1Connection(final String hostname, final String username, final String sharename, final char[] password) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.sharename = sharename;
		this.password = password;

		connect();
	}

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
	private void connect() throws Exception {
		try {
			if (Utilities.isNotBlank(username)) {
				auth = new NtlmPasswordAuthentication("", username, new String(password));
			} else {
				auth = NtlmPasswordAuthentication.ANONYMOUS;
			}
			new SmbFile(getPathWithProtocol(), auth);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed");
		}
	}

	@Override
	public List<String> getFolders(final String folderPath) throws Exception {
		checkConnection();
		final List<String> folderNames = new ArrayList<>();
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		if (smbFile.isDirectory()) {
			for (final SmbFile subFile : smbFile.listFiles()) {
				if (subFile.isDirectory()) {
					folderNames.add(subFile.getName());
				}
			}
		}
		return folderNames;
	}

	@Override
	public boolean folderExists(final String folderPath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		return smbFile.isDirectory();
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		smbFile.delete();
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(parentFolderPath) + "/" + folderName, auth);
		smbFile.mkdir();
	}

	@Override
	public List<String> getFiles(final String folderPath) throws Exception {
		checkConnection();
		final List<String> fileNames = new ArrayList<>();
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		if (smbFile.isDirectory()) {
			for (final SmbFile subFile : smbFile.listFiles()) {
				if (subFile.isFile()) {
					fileNames.add(subFile.getName());
				}
			}
		}
		return fileNames;
	}

	@Override
	public boolean fileExists(final String filePath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		return smbFile.isFile();
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		smbFile.delete();
	}

	@Override
	public RemoteFileInfo getFileInfo(final String filePath) throws Exception {
		checkConnection();
		String fileName;
		if (filePath.contains("/")) {
			fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			fileName = filePath;
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		final long size = smbFile.getContentLengthLong();
		final Date fileDate = new Date(smbFile.getDate());
		return new RemoteFileInfo(fileName, size, fileDate);
	}

	@Override
	public InputStream getFile(final String filePath) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		return smbFile.getInputStream();
	}

	@Override
	public void uploadFile(final String filePath, final InputStream fileInputStream) throws Exception {
		checkConnection();
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		try (OutputStream outputStream = smbFile.getOutputStream()) {
			IOUtils.copy(fileInputStream, outputStream);
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
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return getPathWithProtocol() + path;
	}

	@Override
	public RemoteFolderInfo getFolderInfo(final String folderPath) throws Exception {
		checkConnection();

		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		if (smbFile.isDirectory()) {
			String folderName;
			if (folderPath.contains("/")) {
				folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
			} else {
				folderName = folderPath;
			}

			final List<RemoteFolderInfo> folderInfoList = new ArrayList<>();
			final List<RemoteFileInfo> fileInfoList = new ArrayList<>();

			for (final SmbFile subFile : smbFile.listFiles()) {
				if (subFile.isDirectory()) {
					folderInfoList.add(getFolderInfo(folderPath + "/" + subFile.getName()));
				} else if (subFile.isFile()) {
					final long fileSize = smbFile.getContentLengthLong();
					final Date modificationDate = new Date(smbFile.getDate());
					fileInfoList.add(new RemoteFileInfo(subFile.getName(), fileSize, modificationDate));
				}
			}

			return new RemoteFolderInfo(folderName, folderInfoList, fileInfoList);
		} else {
			return null;
		}
	}
}
