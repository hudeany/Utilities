package de.soderer.utilities;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

public class SmbConnection implements RemoteConnection {
	/** The Constant logger. */
	@SuppressWarnings("unused")
	private static final transient Logger logger = Logger.getLogger(SmbConnection.class);

	/** The host. */
	private String hostname = null;

	/** The user. */
	private String username = null;

	/** The password. */
	private char[] password = null;

	/** The domain. */
	private String domain = null;

	/** The domain. */
	private String shareName = null;

	/**
	 * Currently connected client
	 */
	private SMBClient client = null;
	private Connection connection = null;
	private Session session = null;
	private DiskShare diskShare = null;

	public SmbConnection(final String hostname, final String username, final String domain, final String shareName, final char[] password) throws Exception {
		this.hostname = hostname;
		this.username = username;
		this.domain = domain;
		this.shareName = shareName;
		this.password = password;

		connect();
	}

	public SmbConnection(final ServerAccessData serverEntry) throws Exception {
		if (!"smb".equalsIgnoreCase(serverEntry.getProtocol())) {
			throw new Exception("Invalid protocol '" + serverEntry.getProtocol() + "' for smb connection. Only 'smb' is allowed");
		} else {
			hostname = serverEntry.getHostname();
			username = serverEntry.getUsername();
			domain = serverEntry.getDomain();
			shareName = serverEntry.getShareName();
			password = serverEntry.getPassword();

			connect();
		}
	}

	/**
	 * Connect.
	 */
	private void connect() throws Exception {
		try {
			client = new SMBClient();
			connection = client.connect(hostname);
			session = connection.authenticate(new AuthenticationContext(username, password, domain));
			diskShare = (DiskShare) session.connectShare(shareName);
		} catch (final Exception e) {
			close();
			throw new Exception("Connection failed");
		}
	}

	@Override
	public List<String> getFolders(final String folderPath) throws Exception {
		final List<String> folderNames = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			if (!".".equals(fileIdBothDirectoryInformation.getFileName()) && !"..".equals(fileIdBothDirectoryInformation.getFileName())) {
				if (EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					folderNames.add(fileIdBothDirectoryInformation.getFileName());
				}
			}
		}
		return folderNames;
	}

	@Override
	public boolean folderExists(final String folderPath) throws Exception {
		final int lastPathSeparator = folderPath.replace("\\", "/").lastIndexOf("/");
		final String folderParentPath = lastPathSeparator >= 0 ? folderPath.substring(0, lastPathSeparator) : "";
		final String folderName = lastPathSeparator >= 0 ? folderPath.substring(lastPathSeparator + 1) : folderPath;
		try {
			for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderParentPath, folderName)) {
				return EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
			}
		} catch (final Exception e) {
			return false;
		}
		return false;
	}

	@Override
	public void deleteFolder(final String folderPath) throws Exception {
		diskShare.rmdir(folderPath, true);
	}

	@Override
	public void createFolder(final String parentFolderPath, final String folderName) throws Exception {
		diskShare.mkdir((parentFolderPath != null && parentFolderPath.length() > 0 ? parentFolderPath + "/" : "") + folderName);
	}

	@Override
	public List<String> getFiles(final String folderPath) throws Exception {
		final List<String> fileNames = new ArrayList<>();
		for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, "*")) {
			if (!".".equals(fileIdBothDirectoryInformation.getFileName()) && !"..".equals(fileIdBothDirectoryInformation.getFileName())) {
				if (!EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
					fileNames.add(fileIdBothDirectoryInformation.getFileName());
				}
			}
		}
		return fileNames;
	}

	@Override
	public boolean fileExists(final String filePath) throws Exception {
		final int lastPathSeparator = filePath.replace("\\", "/").lastIndexOf("/");
		final String folderPath = lastPathSeparator >= 0 ? filePath.substring(0, lastPathSeparator) : "";
		final String fileName = lastPathSeparator >= 0 ? filePath.substring(lastPathSeparator + 1) : filePath;
		try {
			for (final FileIdBothDirectoryInformation fileIdBothDirectoryInformation : diskShare.list(folderPath, fileName)) {
				return !EnumWithValue.EnumUtils.isSet(fileIdBothDirectoryInformation.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
			}
		} catch (final Exception e) {
			return false;
		}
		return false;
	}

	@Override
	public void deleteFile(final String filePath) throws Exception {
		diskShare.rm(filePath);
	}

	@Override
	public RemoteFile getFileInfo(final String filePath) throws Exception {
		final File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
		final FileAllInformation fileAllInformation = remoteSmbjFile.getFileInformation();
		return new RemoteFile("/" + domain + fileAllInformation.getNameInformation().replace("\\", "/"), fileAllInformation.getStandardInformation().getAllocationSize(), fileAllInformation.getBasicInformation().getLastWriteTime().toDate());
	}

	@Override
	public InputStream getFile(final String filePath) throws Exception {
		final File remoteSmbjFile = diskShare.openFile(filePath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
		return remoteSmbjFile.getInputStream();
	}

	@Override
	public void uploadFile(final String filePath, final InputStream fileInputStream) throws Exception {
		final File newFile = diskShare.openFile(filePath, new HashSet<>(Arrays.asList(new AccessMask[]{AccessMask.GENERIC_ALL})), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null);
		try (OutputStream outputStream = newFile.getOutputStream()) {
			IOUtils.copy(fileInputStream, outputStream);
		}
	}

	@Override
	public void close() {
		if (diskShare != null) {
			try {
				diskShare.close();
			} catch (final Exception e) {
				// do nothing
			}
			diskShare = null;
		}

		if (session != null) {
			try {
				session.close();
			} catch (final Exception e) {
				// do nothing
			}
			session = null;
		}

		if (connection != null) {
			try {
				connection.close();
			} catch (final Exception e) {
				// do nothing
			}
			connection = null;
		}

		if (client != null) {
			try {
				client.close();
			} catch (final Exception e) {
				// do nothing
			}
			client = null;
		}
	}
}
