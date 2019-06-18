package de.soderer.utilities;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class Smb1Connection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(Smb1Connection.class);

	/** The host. */
	private String hostname = null;

	/** The port. */
	private int port = 445;

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

	public Smb1Connection(final String hostname, final int port, final String username, final String sharename, final char[] password) throws Exception {
		this.hostname = hostname;
		if (0 <= port && port <= 65535) {
			this.port = port;
		}
		this.username = username;
		this.sharename = sharename;
		this.password = password;

		connect();
	}

	public Smb1Connection(final ServerAccessData serverEntry) throws Exception {
		if (!"smb1".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for smb1 connection. Only 'smb1' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			if (0 <= serverEntry.getPort() && serverEntry.getPort() <= 65535) {
				port = serverEntry.getPort();
			}
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
			NtlmPasswordAuthentication auth;
			if (Utilities.isNotBlank(username)) {
				auth = new NtlmPasswordAuthentication("", username, new String(password));
			} else {
				auth = NtlmPasswordAuthentication.ANONYMOUS;
			}
			new SmbFile(getPath(), auth);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed");
		}
	}

	@Override
	public List<String> getFolders(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
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
	public boolean folderExists(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		return smbFile.isDirectory();
	}

	@Override
	public void deleteFolder(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(folderPath), auth);
		smbFile.delete();
	}

	@Override
	public void createFolder(String parentFolderPath, final String folderName) throws Exception {
		if (parentFolderPath.startsWith("/")) {
			parentFolderPath = parentFolderPath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(parentFolderPath) + "/" + folderName, auth);
		smbFile.mkdir();
	}

	@Override
	public List<String> getFiles(String folderPath) throws Exception {
		if (folderPath.startsWith("/")) {
			folderPath = folderPath.substring(1);
		}
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
	public boolean fileExists(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		return smbFile.isDirectory();
	}

	@Override
	public void deleteFile(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		smbFile.delete();
	}

	@Override
	public RemoteFile getFileInfo(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		final long size = smbFile.getContentLengthLong();
		final Date fileDate = new Date(smbFile.getDate());
		return new RemoteFile(filePath, size, fileDate);
	}

	@Override
	public InputStream getFile(String filePath) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		return smbFile.getInputStream();
	}

	@Override
	public void uploadFile(String filePath, final InputStream fileInputStream) throws Exception {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		final SmbFile smbFile = new SmbFile(getPath(filePath), auth);
		try (OutputStream outputStream = smbFile.getOutputStream()) {
			IOUtils.copy(fileInputStream, outputStream);
		}
	}

	@Override
	public void close() {
		auth = null;
	}

	private String getPath() {
		return "smb://" + hostname + (port != 445 ? ":" + port : "") + "/" + sharename + "/";
	}

	private String getPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return getPath() + path;
	}
}
